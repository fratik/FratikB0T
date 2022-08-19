/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.moderation;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import gg.amy.pgorm.PgStore;
import io.sentry.Sentry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.ModuleLoadedEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.moderation.commands.*;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.entity.LogMessage;
import pl.fratik.moderation.entity.PurgeDao;
import pl.fratik.moderation.events.UpdateCaseEvent;
import pl.fratik.moderation.listeners.*;
import pl.fratik.moderation.utils.Migration;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private UserDao userDao;
    @Inject private ScheduleDao scheduleDao;
    @Inject private ShardManager shardManager;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private EventBus eventBus;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ManagerModulow managerModulow;
    @Inject private RedisCacheManager redisCacheManager;

    private ArrayList<Command> commands;
    private ModLogListener modLogListener;
    private LogListener logListener;
    private PrzeklenstwaListener przeklenstwaListener;
    private LinkListener linkListener;
    private AntiInviteListener antiInviteListener;
    private AntiRaidListener antiRaidListener;
    private CaseDao caseDao;
    private PurgeDao purgeDao;
    private AutobanListener autobanListener;
//    private PublishListener publishListener;
    private boolean connected;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        Logger logger = LoggerFactory.getLogger(getClass());
        LogMessage.setShardManager(shardManager);
        EnumSet<Permission> permList = Permission.getPermissions(Globals.permissions);
        permList.add(Permission.VIEW_AUDIT_LOGS);
        permList.add(Permission.MANAGE_SERVER);
        permList.add(Permission.MANAGE_ROLES);
        permList.add(Permission.BAN_MEMBERS);
        permList.add(Permission.KICK_MEMBERS);
        if (Globals.permissions != Permission.getRaw(permList)) {
            logger.debug("Zmieniam long uprawnień: {} -> {}", Globals.permissions, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
        logger.info("Rozpoczynam migrację spraw!");
        try {
            AtomicReference<SQLException> sqlEx = new AtomicReference<>(); // pgStore to gówno i tylko loguje SQLException
            managerBazyDanych.getPgStore().sql(new PgStore.SqlConsumer<Connection>() {
                @Override
                public void accept(Connection con) {
                    try {
                        this.sql(con);
                    } catch (SQLException ex) {
                        sqlEx.set(ex);
                    }
                }

                @Override
                public void sql(Connection con) throws SQLException {
                    final boolean autoCommit = con.getAutoCommit();
                    con.setAutoCommit(false);
                    try {
                        String preMigrationVersion = null;
                        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM cases WHERE id = 'version';")) {
                            ResultSet set = stmt.executeQuery();
                            if (set.isBeforeFirst()) {
                                set.next();
                                String data = set.getString("data");
                                try {
                                    preMigrationVersion = GsonUtil.fromJSON(data, String.class);
                                } catch (Exception e) {
                                    preMigrationVersion = data;
                                }
                            }
                        }
                        Migration mig = Migration.fromVersionName(preMigrationVersion);
                        try {
                            if (mig != null) {
                                logger.info("Aktualna wersja: {}. Migruję do {}...", mig.getVersionKey(), Migration.getNewest().getVersionKey());
                                mig.migrate(con);
                            } else throw new IllegalArgumentException("Nieprawidłowa wersja!");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        String statement;
                        if (preMigrationVersion == null)
                            statement = "INSERT INTO cases (id, data) VALUES ('version', to_jsonb(?));";
                        else statement = "UPDATE cases SET data = to_jsonb(?) WHERE id = 'version';";
                        try (PreparedStatement stmt = con.prepareStatement(statement)) {
                            stmt.setString(1, Migration.getNewest().getVersionKey());
                            stmt.execute();
                        }
                        con.commit();
                        logger.info("Migracja ukończona!");
                    } catch (Throwable e) {
                        con.rollback();
                        throw e;
                    } finally {
                        con.setAutoCommit(autoCommit);
                    }
                }
            });
            if (sqlEx.get() != null) throw sqlEx.get();
        } catch (Exception e) {
            logger.error("Migracja nieudana!", e);
            Sentry.capture(e);
            return false;
        }
        caseDao = new CaseDao(managerBazyDanych, eventBus);
        purgeDao = new PurgeDao(managerBazyDanych, eventBus);
        ModLogBuilder.setTlumaczenia(tlumaczenia);
        ModLogBuilder.setGuildDao(guildDao);
        ModLogBuilder.setManagerKomend(managerKomend);
        LogListener.setTlumaczenia(tlumaczenia);
        modLogListener = new ModLogListener(shardManager, caseDao, guildDao, scheduleDao, tlumaczenia, managerKomend, redisCacheManager);
        logListener = new LogListener(guildDao, purgeDao, redisCacheManager);
        przeklenstwaListener = new PrzeklenstwaListener(guildDao, tlumaczenia, managerKomend, shardManager, caseDao, redisCacheManager);
        linkListener = new LinkListener(guildDao, tlumaczenia, managerKomend, shardManager, caseDao, redisCacheManager, eventBus);
        autobanListener = new AutobanListener(guildDao, tlumaczenia, modLogListener);
        antiInviteListener = new AntiInviteListener(guildDao, tlumaczenia, managerKomend, shardManager, caseDao, redisCacheManager);
        antiRaidListener = new AntiRaidListener(guildDao, shardManager, eventBus, tlumaczenia, redisCacheManager, managerKomend);
//        publishListener = new PublishListener(guildDao, tlumaczenia, managerKomend, shardManager, caseDao, redisCacheManager);

        eventBus.register(this);
        eventBus.register(modLogListener);
        eventBus.register(logListener);
        eventBus.register(przeklenstwaListener);
        eventBus.register(linkListener);
        eventBus.register(autobanListener);
        eventBus.register(antiInviteListener);
        eventBus.register(antiRaidListener);
//        eventBus.register(publishListener);

        commands = new ArrayList<>();

        commands.add(new PurgeCommand(logListener));
        commands.add(new BanCommand(modLogListener, scheduleDao));
        commands.add(new UnbanCommand(modLogListener));
        commands.add(new KickCommand(modLogListener));
        commands.add(new WarnCommand(caseDao));
        commands.add(new UnwarnCommand(caseDao));
        commands.add(new AkcjeCommand(caseDao, shardManager, eventWaiter, eventBus, managerKomend, guildDao));
        commands.add(new ReasonCommand(scheduleDao, caseDao));
        commands.add(new RolesCommand());
        commands.add(new HideCommand(guildDao, managerKomend));
        commands.add(new LockCommand(guildDao, managerKomend));
        commands.add(new MuteCommand(guildDao, modLogListener));
        commands.add(new UnmuteCommand(guildDao, modLogListener));
        commands.add(new ZglosCommand(guildDao));
        commands.add(new RegulaminCommand());
        commands.add(new RolaCommand(guildDao));
        commands.add(new NotatkaCommand(caseDao));
        commands.add(new RolementionCommand());
        commands.add(new DowodCommand(guildDao, caseDao, managerKomend, eventWaiter, eventBus));

        commands.forEach(managerKomend::registerCommand);

        if (managerModulow.getModules().get("api") != null)
            new PurgeForApi(managerModulow.getModules().get("api"), shardManager, purgeDao, guildDao);

        if (shardManager.getShards().stream().anyMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) return true;
        if (connected) return true;
        connected = true;
        fixCases();
        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        antiRaidListener.shutdown();
        try {
            eventBus.unregister(this);
            eventBus.unregister(modLogListener);
            eventBus.unregister(logListener);
            eventBus.unregister(przeklenstwaListener);
            eventBus.unregister(linkListener);
            eventBus.unregister(autobanListener);
            eventBus.unregister(antiInviteListener);
            eventBus.unregister(antiRaidListener);
//            eventBus.unregister(publishListener);
        } catch (Exception ignored) {
            /*lul*/
        }
        return true;
    }

    @Subscribe
    private void onMemberJoin(GuildMemberJoinEvent e) {
        if (!modLogListener.checkPermissions(e)) return;
        Role rola = modLogListener.getMuteRole(e.getGuild());
        if (rola == null) return;
        List<Case> cList = caseDao.getCasesByMember(e.getMember()).stream().filter(c -> c.getType() == Kara.MUTE &&
                c.isValid()).collect(Collectors.toList());
        if (cList.size() > 1) {
            for (Case aCase : cList) {
                aCase.setValid(false);
                aCase.setValidTo(Instant.now());
                Case c = new Case.Builder(aCase.getGuildId(), aCase.getUserId(), Instant.now(), Kara.UNMUTE)
                        .setIssuerId(Globals.clientId)
                        .setReasonKey("modlog.reason.twomutesoneuser")
                        .build();
                caseDao.createNew(aCase, c, false);
            }
        } else if (cList.size() != 1) return;
        e.getGuild().addRoleToMember(e.getMember(), rola)
                .reason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "modlog.reason.audit.autoreturnmute",
                        cList.get(0).getCaseNumber())).queue();
    }

    @Subscribe
    private void onModuleLoad(ModuleLoadedEvent e) {
        if (e.getName().equals("api")) {
            new PurgeForApi(e.getModule(), shardManager, purgeDao, guildDao);
        }
    }

    @Subscribe
    private void onConnected(StatusChangeEvent e) {
        if (connected) return;
        if (shardManager.getShards().stream().allMatch(s -> s.getStatus() == JDA.Status.CONNECTED)) {
            connected = true;
            fixCases();
        }
    }

    private synchronized void fixCases() {
        for (Case aCase : caseDao.getAllNeedsUpdate()) {
            if (Thread.interrupted()) break;
            Case c = caseDao.getLocked(aCase.getId());
            if (c == null) continue; // ?

            try {
                if (shardManager.getShards().stream().anyMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) {
                    connected = false;
                    break;
                }
                if (!c.isNeedsUpdate()) continue;
                if (shardManager.getGuildById(aCase.getGuildId()) != null)
                    modLogListener.onUpdateCase(new UpdateCaseEvent(aCase, false));
                c.setNeedsUpdate(false);
                caseDao.save(c, true);
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Nie udało się uaktualnić sprawy " + c.getId() + "!", e);
                Sentry.getContext().addExtra("caseId", c.getId());
                Sentry.capture(e);
                Sentry.clearContext();
            } finally {
                caseDao.unlock(c);
            }
        }
    }

}
