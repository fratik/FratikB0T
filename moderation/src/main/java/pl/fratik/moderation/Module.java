/*
 * Copyright (C) 2019 FratikB0T Contributors
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
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.ModuleLoadedEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.moderation.commands.*;
import pl.fratik.moderation.entity.*;
import pl.fratik.moderation.listeners.*;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

    private ArrayList<Command> commands;
    private ModLogListener modLogListener;
    private LogListener logListener;
    private PrzeklenstwaListener przeklenstwaListener;
    private AntiInviteListener antiInviteListener;
    private AntiRaidListener antiRaidListener;
    private CasesDao casesDao;
    private PurgeDao purgeDao;
    private AutobanListener autobanListener;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        EnumSet<Permission> permList = Permission.getPermissions(Globals.permissions);
        if (!permList.contains(Permission.VIEW_AUDIT_LOGS)) permList.add(Permission.VIEW_AUDIT_LOGS);
        if (Globals.permissions != Permission.getRaw(permList)) {
            LoggerFactory.getLogger(Module.class).debug("Zmieniam long uprawnień: {} -> {}", Globals.permissions, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
        AutoAkcja.setShardManager(shardManager);
        casesDao = new CasesDao(managerBazyDanych, eventBus);
        purgeDao = new PurgeDao(managerBazyDanych, eventBus);
        Case.setStaticVariables(casesDao, scheduleDao);
        ModLogBuilder.setTlumaczenia(tlumaczenia);
        ModLogBuilder.setGuildDao(guildDao);
        ModLogListener.setTlumaczenia(tlumaczenia);
        ModLogListener.setManagerKomend(managerKomend);
        LogListener.setTlumaczenia(tlumaczenia);
        modLogListener = new ModLogListener(guildDao, shardManager, casesDao);
        logListener = new LogListener(guildDao, purgeDao);
        przeklenstwaListener = new PrzeklenstwaListener(guildDao, tlumaczenia, managerKomend, shardManager, casesDao);
        autobanListener = new AutobanListener(guildDao, tlumaczenia);
        antiInviteListener = new AntiInviteListener(guildDao, tlumaczenia, managerKomend, shardManager, casesDao);
        antiRaidListener = new AntiRaidListener(guildDao, shardManager, eventBus, tlumaczenia);

        eventBus.register(this);
        eventBus.register(modLogListener);
        eventBus.register(logListener);
        eventBus.register(przeklenstwaListener);
        eventBus.register(autobanListener);
        eventBus.register(antiInviteListener);
        eventBus.register(antiRaidListener);

        commands = new ArrayList<>();

        commands.add(new PurgeCommand(logListener));
        commands.add(new BanCommand());
        commands.add(new UnbanCommand());
        commands.add(new KickCommand());
        commands.add(new WarnCommand(guildDao, casesDao, shardManager, managerKomend));
        commands.add(new UnwarnCommand(guildDao, casesDao, shardManager, managerKomend));
        commands.add(new AkcjeCommand(userDao, casesDao, shardManager, eventWaiter, eventBus, managerKomend));
        commands.add(new ReasonCommand(guildDao, casesDao, shardManager, managerKomend));
        commands.add(new RolesCommand());
        commands.add(new HideCommand(guildDao, managerKomend));
        commands.add(new LockCommand(guildDao, managerKomend));
        commands.add(new MuteCommand(guildDao));
        commands.add(new UnmuteCommand(guildDao));
        commands.add(new ZglosCommand(guildDao));
        commands.add(new RegulaminCommand());
        commands.add(new RolaCommand(guildDao));
        commands.add(new NotatkaCommand(guildDao, casesDao, shardManager, managerKomend));
        commands.add(new RolementionCommand());

        commands.forEach(managerKomend::registerCommand);

        new PurgeForApi(managerModulow.getModules().get("api"), shardManager, purgeDao, guildDao);

        return true;
    }

    @Override
    public boolean shutDown() {
        EnumSet<Permission> permList = Permission.getPermissions(Globals.permissions);
        permList.remove(Permission.VIEW_AUDIT_LOGS);
        if (Globals.permissions != Permission.getRaw(permList)) {
            LoggerFactory.getLogger(Module.class).debug("Zmieniam long uprawnień: {} -> {}", Globals.permissions, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
        commands.forEach(managerKomend::unregisterCommand);
        antiRaidListener.shutdown();
        try {
            eventBus.unregister(this);
            eventBus.unregister(modLogListener);
            eventBus.unregister(logListener);
            eventBus.unregister(przeklenstwaListener);
            eventBus.unregister(autobanListener);
            eventBus.unregister(antiInviteListener);
            eventBus.unregister(antiRaidListener);
        } catch (Exception ignored) {
            /*lul*/
        }
        return true;
    }

    @Subscribe
    private void onMemberJoin(GuildMemberJoinEvent e) {
        if (!e.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) return;
        CaseRow cases = casesDao.get(e.getGuild());
        List<Case> cList = cases.getCases().stream().filter(c -> c.getType() == Kara.MUTE &&
                c.getUserId().equals(e.getMember().getUser().getId()) && c.isValid()).collect(Collectors.toList());
        GuildConfig gc = guildDao.get(e.getGuild());
        Role rola;
        try {
            rola = e.getGuild().getRoleById(gc.getWyciszony());
        } catch (Exception ignored) {
            rola = null;
        }
        if (rola == null) return;
        if (cList.size() > 1) {
            for (Case aCase : cList) {
                aCase.setValid(false);
                aCase.setValidTo(Instant.now(), true);
                Case c = new CaseBuilder().setUser(aCase.getUserId()).setGuild(aCase.getGuildId())
                        .setCaseId(Case.getNextCaseId(cases)).setTimestamp(Instant.now()).setMessageId(null)
                        .setKara(Kara.UNMUTE).createCase();
                c.setIssuerId(String.valueOf(Globals.clientId));
                c.setReason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "modlog.reason.twomutesoneuser"));
                MessageEmbed em = ModLogBuilder.generate(c, e.getGuild(), shardManager,
                        tlumaczenia.getLanguage(e.getGuild()), managerKomend);
                TextChannel mlog = shardManager.getTextChannelById(gc.getModLog());
                if (mlog != null && mlog.canTalk()) {
                    mlog.sendMessage(em).complete();
                    c.setMessageId(mlog.sendMessage(em).complete().getId());
                }
                cases.getCases().add(c);
                casesDao.save(cases);
            }
        } else if (cList.size() == 1) {
            modLogListener.getIgnoredMutes().add(e.getUser().getId() + e.getGuild().getId());
            e.getGuild().addRoleToMember(e.getMember(), rola)
                    .reason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "modlog.reason.audit.autoreturnmute",
                            cList.get(0).getCaseId())).queue();
        }
    }

    @Subscribe
    private void onModuleLoad(ModuleLoadedEvent e) {
        if (e.getName().equals("api")) {
            new PurgeForApi(e.getModule(), shardManager, purgeDao, guildDao);
        }
    }

}
