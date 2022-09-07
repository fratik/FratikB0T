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
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.crypto.AES;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.ModuleLoadedEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.StringUtil;
import pl.fratik.moderation.commands.*;
import pl.fratik.moderation.entity.*;
import pl.fratik.moderation.events.UpdateCaseEvent;
import pl.fratik.moderation.listeners.*;
import pl.fratik.moderation.utils.Migration;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Module implements Modul {
    @Inject private NewManagerKomend managerKomend;
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

    private ArrayList<NewCommand> commands;
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
    private Logger logger = LoggerFactory.getLogger(getClass());

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
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
            managerBazyDanych.getPgStore().sql(new PgStore.SqlConsumer<>() {
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
            if (e instanceof SQLException && ((SQLException) e).getSQLState().equals("42P01")) {
                logger.warn("Prawdopodobnie nie znaleziono tabeli cases, więc ignoruję", e);
            } else {
                logger.error("Migracja nieudana!", e);
                Sentry.capture(e);
                return false;
            }
        }
        caseDao = new CaseDao(managerBazyDanych, eventBus);
        purgeDao = new PurgeDao(managerBazyDanych, eventBus);
        String password;
        try {
            File file = new File("purgePassword.txt");
            if (!file.exists()) {
                logger.info("Nie wykryto hasła purge'ów. Rozpoczynam migrację (aka szyfrowanie). Aby przerwać, zatrzymaj proces i umieść hasło w pliku purgePassword.txt.");
                Thread.sleep(6000);
                try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
                    fw.write(password = StringUtil.generateId(64, true, true, true, true));
                    fw.flush();
                }
                List<Purge> allPurges = purgeDao.getAll();
                for (Purge purge : allPurges) {
                    for (Wiadomosc wiadomosc : purge.getWiadomosci()) {
                        if (wiadomosc instanceof Purge.ResolvedWiadomosc) {
                            ((Purge.ResolvedWiadomosc) wiadomosc).setContent(AES.encryptAsB64(wiadomosc.getContent(), password));
                        }
                    }
                }
                for (Purge purge : allPurges) {
                    purgeDao.save(purge);
                }
            } else {
                password = readPassword(file);
            }
        } catch (IOException e) {
            logger.error("Nie udało się utworzyć hasła purge'ów!", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.error("Szyfrowanie nieudane!", e);
            return false;
        }
        ModLogBuilder.setTlumaczenia(tlumaczenia);
        ModLogBuilder.setGuildDao(guildDao);
        LogListener.setTlumaczenia(tlumaczenia);
        modLogListener = new ModLogListener(shardManager, caseDao, guildDao, scheduleDao, tlumaczenia, redisCacheManager);
        logListener = new LogListener(guildDao, purgeDao, redisCacheManager, password);
        przeklenstwaListener = new PrzeklenstwaListener(guildDao, tlumaczenia, shardManager, caseDao, redisCacheManager);
        linkListener = new LinkListener(guildDao, tlumaczenia, shardManager, caseDao, redisCacheManager, eventBus);
        autobanListener = new AutobanListener(guildDao, tlumaczenia, modLogListener);
        antiInviteListener = new AntiInviteListener(guildDao, tlumaczenia, shardManager, caseDao, redisCacheManager);
        antiRaidListener = new AntiRaidListener(guildDao, shardManager, eventBus, tlumaczenia, redisCacheManager);
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
        commands.add(new BanCommand(modLogListener));
        commands.add(new UnbanCommand(modLogListener));
        commands.add(new KickCommand(modLogListener));
        commands.add(new WarnCommand(caseDao));
        commands.add(new UnwarnCommand(caseDao));
        commands.add(new AkcjeCommand(caseDao, shardManager, eventWaiter, eventBus, guildDao));
        commands.add(new ReasonCommand(scheduleDao, caseDao));
        commands.add(new MuteCommand(modLogListener));
        commands.add(new UnmuteCommand(modLogListener));
        commands.add(new ZglosCommand(guildDao));
        commands.add(new RegulaminCommand());
        commands.add(new RolaCommand(guildDao));
        commands.add(new NotatkaCommand(caseDao));
        commands.add(new DowodCommand(guildDao, caseDao, eventWaiter, eventBus));

        managerKomend.registerCommands(this, commands);

        if (managerModulow.getModules().get("api") != null)
            new PurgeForApi(managerModulow.getModules().get("api"), shardManager, purgeDao, guildDao, password);

        if (shardManager.getShards().stream().anyMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) return true;
        if (connected) return true;
        connected = true;
        fixCases();
        return true;
    }

    @NotNull
    private static String readPassword(File file) throws IOException {
        String password;
        try (FileReader fr = new FileReader(file, StandardCharsets.UTF_8)) {
            CharBuffer cb = CharBuffer.allocate(1024);
            int read;
            while ((read = fr.read()) != -1) {
                cb.put((char) read);
            }
            read = cb.position();
            cb.rewind();
            cb.limit(read);
            password = cb.toString();
        }
        return password;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
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
    private void onModuleLoad(ModuleLoadedEvent e) {
        if (e.getName().equals("api")) {
            try {
                new PurgeForApi(e.getModule(), shardManager, purgeDao, guildDao, readPassword(new File("purgePassword.txt")));
            } catch (IOException ex) {
                logger.error("Nie udało się odczytać hasła!", ex);
            }
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
