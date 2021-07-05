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

package pl.fratik.stats;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import lombok.Getter;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.event.CommandDispatchEvent;
import pl.fratik.core.event.ModuleLoadedEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.stats.entity.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Module implements Modul {
    @Inject private EventBus eventBus;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ShardManager shardManager;
    @Inject private ManagerModulow managerModulow;
    @Inject private RedisCacheManager redisCacheManager;

    @Getter private CommandCountStatsDao commandCountStatsDao;
    @Getter private GuildCountStatsDao guildCountStatsDao;
    @Getter private MembersStatsDao membersStatsDao;
    @Getter private MessagesStatsDao messagesStatsDao;
    private final ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
    @Getter private int komend = 0;
    @Getter private final Map<String, Integer> wiadomosci = new HashMap<>();
    private GuildStats gs;

    public Module() {
    }

    @Override
    public boolean startUp() {
        commandCountStatsDao = new CommandCountStatsDao(managerBazyDanych, eventBus);
        guildCountStatsDao = new GuildCountStatsDao(managerBazyDanych, eventBus);
        membersStatsDao = new MembersStatsDao(managerBazyDanych, eventBus);
        messagesStatsDao = new MessagesStatsDao(managerBazyDanych, eventBus);
        executorSche.scheduleAtFixedRate(this::zrzut, 15, 15, TimeUnit.MINUTES);
        eventBus.register(this);
        if (managerModulow.getModules().get("api") != null) {
            gs = new GuildStats(this, managerModulow.getModules().get("api"), shardManager, redisCacheManager);
            eventBus.register(gs);
        }
        return true;
    }

    private void zrzut() {
        //#region Zapis statystyk
        CommandCountStats ccs = commandCountStatsDao.get(getCurrentStorageDate());
        if (ccs.getCount() + komend != ccs.getCount() || ccs.getCount() == 0) {
            ccs.setCount(ccs.getCount() + komend);
            commandCountStatsDao.save(ccs);
        }
        komend = 0;
        GuildCountStats gcs = guildCountStatsDao.get(getCurrentStorageDate());
        if (gcs.getCount() != shardManager.getGuilds().size() || ccs.getCount() == 0) {
            gcs.setCount(shardManager.getGuilds().size());
            guildCountStatsDao.save(gcs);
        }
        List<MembersStats> mss = membersStatsDao.getAllForDate(getCurrentStorageDate());
        for (Guild g : shardManager.getGuilds()) {
            if (Thread.currentThread().isInterrupted()) {
                LoggerFactory.getLogger(getClass()).info("Wątek przerwany, kończę loop'a");
                break;
            }
            List<MembersStats> zDzisiaj = mss.stream().filter(m -> m.getGuildId().equals(g.getId()))
                    .collect(Collectors.toList());
            if (zDzisiaj.isEmpty()) {
                MembersStats ms = new MembersStats(getCurrentStorageDate(), g.getId());
                ms.setCount(g.getMemberCount());
                membersStatsDao.save(ms);
            } else {
                MembersStats ms = zDzisiaj.get(0);
                if (ms.getCount() == g.getMemberCount() && ms.getCount() != 0) continue;
                ms.setCount(g.getMemberCount());
                membersStatsDao.save(ms);
            }
        }
        List<MessagesStats> msgs = messagesStatsDao.getAllForDate(getCurrentStorageDate());
        for (Guild g : shardManager.getGuilds()) {
            if (Thread.currentThread().isInterrupted()) {
                LoggerFactory.getLogger(getClass()).info("Wątek przerwany, kończę loop'a");
                break;
            }
            List<MessagesStats> zDzisiaj = msgs.stream().filter(m -> m.getGuildId().equals(g.getId()))
                    .collect(Collectors.toList());
            if (zDzisiaj.isEmpty()) {
                MessagesStats ms = new MessagesStats(getCurrentStorageDate(), g.getId());
                Integer wiad = wiadomosci.remove(g.getId());
                if (wiad == null) wiad = 0;
                if (ms.getCount() == wiad && ms.getCount() != 0) continue;
                ms.setCount(ms.getCount() + wiad);
                messagesStatsDao.save(ms);
            } else {
                MessagesStats ms = zDzisiaj.get(0);
                Integer wiad = wiadomosci.remove(g.getId());
                if (wiad == null) wiad = 0;
                if (ms.getCount() == wiad && ms.getCount() != 0) continue;
                ms.setCount(ms.getCount() + wiad);
                messagesStatsDao.save(ms);
            }
        }
        //#endregion
        //#region Usunięcie przestarzałych statystyk
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        cal.setTime(Date.from(Instant.ofEpochMilli(getCurrentStorageDate())));
        cal.add(Calendar.YEAR, -2);
        long twoYearsAgo = cal.toInstant().toEpochMilli();
        List<CommandCountStats> ccsToRemove = commandCountStatsDao.getBefore(twoYearsAgo);
        for (CommandCountStats cc : ccsToRemove) commandCountStatsDao.delete(cc);
        List<GuildCountStats> gcsToRemove = guildCountStatsDao.getBefore(twoYearsAgo);
        for (GuildCountStats gc : gcsToRemove) guildCountStatsDao.delete(gc);
        List<MembersStats> mssToRemove = membersStatsDao.getAllBefore(twoYearsAgo);
        for (MembersStats ms : mssToRemove) membersStatsDao.delete(ms);
        List<MessagesStats> msgsToRemove = messagesStatsDao.getAllBefore(twoYearsAgo);
        for (MessagesStats msg : msgsToRemove) messagesStatsDao.delete(msg);
        //#endregion
    }

    public long getCurrentStorageDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        return cal.toInstant().toEpochMilli();
    }

    @Subscribe
    private void onCommandDispatch(CommandDispatchEvent e) {
        komend++;
    }

    @Subscribe
    private void onMessage(MessageReceivedEvent e) {
        if (!e.isFromType(ChannelType.TEXT)) return;
        Integer liczba = wiadomosci.get(e.getGuild().getId());
        if (liczba == null) liczba = 0;
        wiadomosci.put(e.getGuild().getId(), liczba + 1);
    }

    @Subscribe
    private void onGuildLeave(GuildLeaveEvent e) {
        if (!e.getGuild().isAvailable()) return;
        wiadomosci.remove(e.getGuild().getId());
    }

    @Subscribe
    private void onModuleLoad(ModuleLoadedEvent e) {
        if (e.getName().equals("api")) {
            if (gs != null) eventBus.unregister(gs);
            gs = new GuildStats(this, e.getModule(), shardManager, redisCacheManager);
            eventBus.register(gs);
        }
    }

    @Override
    public boolean shutDown() {
        if (!Globals.wylaczanie) zrzut();
        executorSche.shutdown();
        eventBus.unregister(this);
        if (gs != null) {
            gs.shutdown();
            eventBus.unregister(gs);
        }
        return true;
    }
}
