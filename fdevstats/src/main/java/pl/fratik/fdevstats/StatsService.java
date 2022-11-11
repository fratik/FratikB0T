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

package pl.fratik.fdevstats;

import com.google.common.util.concurrent.AbstractScheduledService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Statyczne;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.stats.entity.CommandCountStatsDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static pl.fratik.core.util.DurationUtil.humanReadableFormat;

class StatsService extends AbstractScheduledService {
    private final ShardManager shardManager;
    private final ManagerModulow managerModulow;
    private boolean firstStart;

    StatsService(ShardManager shardManager, ManagerModulow managerModulow) {
        this.shardManager = shardManager;
        this.managerModulow = managerModulow;
        firstStart = true;
    }

    @Override
    protected void runOneIteration() {
        try {
            run();
        } catch (Exception e) {
            // poczekaj na załadowanie
        }
    }

    void run() {
        Ustawienia.FdevStats ustst = Ustawienia.instance.fdevStats;
        VoiceChannel status = shardManager.getVoiceChannelById(ustst.status);
        VoiceChannel uptime = shardManager.getVoiceChannelById(ustst.uptime);
        VoiceChannel wersja = shardManager.getVoiceChannelById(ustst.wersja);
        VoiceChannel ping = shardManager.getVoiceChannelById(ustst.ping);
        VoiceChannel users = shardManager.getVoiceChannelById(ustst.users);
        VoiceChannel serwery = shardManager.getVoiceChannelById(ustst.serwery);
        VoiceChannel ram = shardManager.getVoiceChannelById(ustst.ram);
        VoiceChannel komdzis = shardManager.getVoiceChannelById(ustst.komdzis);
        VoiceChannel ostAkt = shardManager.getVoiceChannelById(ustst.ostakt);
        if (status == null || uptime == null || wersja == null || ping == null || users == null || serwery == null ||
                ram == null || komdzis == null || ostAkt == null) return;
        if (firstStart) {
            status.getManager().setName("\uD83D\uDDA5 Status: wczytywanie").queue();
            uptime.getManager().setName("\uD83D\uDDA5 Uptime: N/a").queue();
            wersja.getManager().setName(String.format("\uD83D\uDDA5 Wersja: v%s", Statyczne.WERSJA)).queue();
            ping.getManager().setName("Wczytywanie...").queue();
            users.getManager().setName("Wczytywanie...").queue();
            serwery.getManager().setName("Wczytywanie...").queue();
            ram.getManager().setName("Wczytywanie...").queue();
            komdzis.getManager().setName("Wczytywanie...").queue();
            ostAkt.getManager().setName("\uD83D\uDD52 Ost. akt: " + new SimpleDateFormat("HH:mm").format(new Date())).queue();
            firstStart = false;
            return;
        }
        status.getManager().setName("\uD83D\uDDA5 Status: " + getStatus()).queue();
        uptime.getManager().setName("\uD83D\uDDA5 Uptime: " +
                humanReadableFormat(Instant.now().toEpochMilli() -
                                Statyczne.startDate.toInstant().getEpochSecond() * 1000, true)).queue();
        ping.getManager().setName(String.format("\uD83C\uDFD3 %sms",
                (int) Math.floor(shardManager.getAverageGatewayPing()))).queue();
        int uz = shardManager.getGuilds().stream().map(Guild::getMemberCount).reduce(Integer::sum).orElse(0);
        users.getManager().setName(String.format("\uD83D\uDD17 %s użytkownik%s", uz, rzeczownik(uz, "ów")))
                .queue();
        serwery.getManager().setName(String.format("\uD83D\uDD17 %s serwer%s",
                shardManager.getGuilds().size(), rzeczownik(shardManager.getGuilds().size()))).queue();
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        double memoryUsage = round((double) (total - free) / 1024 / 1024);
        ram.getManager().setName(String.format("\uD83D\uDCBE %s MB", memoryUsage)).queue();
        komdzis.getManager().setName(String.format("\uD83D\uDCC8 Komend dzisiaj: %s", getKomendDzisiaj())).queue();
        ostAkt.getManager().setName("\uD83D\uDD52 Ost. akt: " + new SimpleDateFormat("HH:mm").format(new Date())).queue();
    }

    private static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private int getKomendDzisiaj() {
        Modul mod = managerModulow.getModules().get("stats");
        try {
            CommandCountStatsDao ccsd = (CommandCountStatsDao) mod.getClass()
                    .getDeclaredMethod("getCommandCountStatsDao").invoke(mod);
            return ccsd.get((Long) mod.getClass().getDeclaredMethod("getCurrentStorageDate").invoke(mod))
                    .getCount() + (int) mod.getClass().getDeclaredMethod("getKomend").invoke(mod);
        } catch (Exception e) {
            return 0;
        }
    }

    private String rzeczownik(int liczba) {
        return rzeczownik(liczba, "y");
    }

    private String rzeczownik(int liczba, String drugaZmiana) {
        return rzeczownik(liczba, drugaZmiana, "ów");
    }

    private String rzeczownik(int liczba, String drugaZmiana, String trzeciaZmiana) {
        return liczba == 1 ? "" : liczba <= 4 && liczba >= 2 ? drugaZmiana : trzeciaZmiana; //NOSONAR
    }

    private String getStatus() {
        return shardManager.getShards().stream().anyMatch(s -> !s.getStatus().equals(JDA.Status.CONNECTED)) ?
                String.format("online (\u26A0\uFE0F %s/%s)",
                        shardManager.getShards().stream().filter(s -> !s.getStatus().equals(JDA.Status.CONNECTED))
                                .count(), shardManager.getShardsTotal()) : "online";
    }

    @Override
    protected void shutDown() throws Exception {
        Ustawienia.FdevStats ustst = Ustawienia.instance.fdevStats;
        VoiceChannel status = shardManager.getVoiceChannelById(ustst.status);
        VoiceChannel uptime = shardManager.getVoiceChannelById(ustst.uptime);
        VoiceChannel wersja = shardManager.getVoiceChannelById(ustst.wersja);
        VoiceChannel ping = shardManager.getVoiceChannelById(ustst.ping);
        VoiceChannel users = shardManager.getVoiceChannelById(ustst.users);
        VoiceChannel serwery = shardManager.getVoiceChannelById(ustst.serwery);
        VoiceChannel ram = shardManager.getVoiceChannelById(ustst.ram);
        VoiceChannel komdzis = shardManager.getVoiceChannelById(ustst.komdzis);
        VoiceChannel ostAkt = shardManager.getVoiceChannelById(ustst.ostakt);
        if (status == null || uptime == null || wersja == null || ping == null || users == null || serwery == null ||
                ram == null || komdzis == null || ostAkt == null) return;
        List<Future<?>> futures = new ArrayList<>();
        futures.add(status.getManager()
                .setName("\uD83D\uDDA5 Status: offline").submit());
        futures.add(uptime.getManager().setName("\uD83D\uDDA5 Uptime: " +
                humanReadableFormat(Instant.now().toEpochMilli() -
                        Statyczne.startDate.toInstant().getEpochSecond() * 1000, true
                )).submit());
        futures.add(wersja.getManager().setName("Bot jest offline").submit());
        futures.add(ping.getManager().setName("Bot jest offline").submit());
        futures.add(users.getManager().setName("Bot jest offline").submit());
        futures.add(serwery.getManager().setName("Bot jest offline").submit());
        futures.add(ram.getManager().setName("Bot jest offline").submit());
        futures.add(komdzis.getManager().setName("Bot jest offline").submit());
        futures.add(ostAkt.getManager().setName("Bot jest offline").submit());
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                //nic
            }
        }
        super.shutDown();
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 15, TimeUnit.MINUTES);
    }
}
