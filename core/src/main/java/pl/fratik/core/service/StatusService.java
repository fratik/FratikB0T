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

package pl.fratik.core.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.Statyczne;
import pl.fratik.core.Ustawienia;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StatusService extends AbstractScheduledService {

    private static ShardManager shardManager;
    private int last = 0;
    private static int customLast = 0;
    private static Activity[] customGames;
    private static OnlineStatus customStatus;

    @SuppressWarnings("squid:S3010")
    public StatusService(ShardManager shardManager) {
        StatusService.shardManager = shardManager;
    }

    @Override
    protected void runOneIteration() {
        Ustawienia ustawienia = Ustawienia.instance;
        if ((customGames != null && customGames.length != 0) && customStatus != null) {
            if (customLast >= customGames.length) customLast = 0;
            shardManager.setPresence(customStatus, customGames[customLast]);
            customLast++;
        }
        else if (!checkDates()) setPresence(ustawienia);
    }

    private boolean checkDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMM");
        String data = sdf.format(new Date());
        int rok = Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date()));
        if (data.equals("2412")) {
            shardManager.setActivity(Activity.playing("Wesołych świąt!"));
            return true;
        }
        if (Globals.clientId != 338359366891732993L) return false;
        if (data.equals("2207")) {
            shardManager.setActivity(Activity.watching("siebie świętującego swoje " + (rok - 2017) + " urodziny!"));
            return true;
        }
        return false;
    }

    private void setPresence(Ustawienia ustawienia) {
        if (ustawienia.games.games.isEmpty()) return;
        if (last >= ustawienia.games.games.size()) last = 0;
        for (JDA jda : shardManager.getShards()) {
            jda.getPresence().setActivity(Activity.of(ustawienia.games.type,
                    ustawienia.games.games.get(last)
                            .replace("{VERSION}", Statyczne.WERSJA)
                            .replace("{USERS:ALL}", String.valueOf(fetchUserCount()))
                            .replace("{USERS:SHARD}", String.valueOf(fetchUserCount(jda)))
                            .replace("{SHARDS}", String.valueOf(shardManager.getShards().size()))
                            .replace("{SERVERS:ALL}", String.valueOf(shardManager.getGuilds().size()))
                            .replace("{SERVERS:SHARD}", String.valueOf(jda.getGuilds().size()))
                            .replace("{PREFIX}", ustawienia.prefix)
                            .replace("{SHARD}", String.valueOf(jda.getShardInfo().getShardId()))));
        }
        last++;
    }

    private int fetchUserCount() {
        AtomicInteger res = new AtomicInteger();
        shardManager.getShards().forEach(jda -> {
            for (Guild g : jda.getGuilds()) {
                res.addAndGet(g.getMemberCount());
            }
        });
        return res.intValue();
    }

    private int fetchUserCount(JDA jda) {
        AtomicInteger res = new AtomicInteger();
        for (Guild g : jda.getGuilds()) {
            res.addAndGet(g.getMemberCount());
        }
        return res.intValue();
    }

    public static void setCustomGame(Activity customGame) {
        setCustomPresence(OnlineStatus.ONLINE, customGame);
    }

    public static void setCustomGames(List<Activity> customGame) {
        setCustomPresences(OnlineStatus.ONLINE, customGame);
    }

    public static void setCustomGames(Activity... customGame) {
        setCustomPresences(OnlineStatus.ONLINE, customGame);
    }

    public static void setCustomPresence(OnlineStatus status, Activity customGame) {
        setCustomPresences(status, customGame == null ? null : Collections.singletonList(customGame));
    }

    public static void setCustomPresences(OnlineStatus status, List<Activity> customGame) {
        setCustomPresences(status, customGame == null ? null : customGame.toArray(new Activity[0]));
    }

    public static void setCustomPresences(OnlineStatus status, Activity... customGames) {
        if (customGames == null) customGames = new Activity[0];
        StatusService.customStatus = status;
        StatusService.customGames = customGames.length == 0 || (customGames.length == 1 && customGames[0] == null) ? null : customGames;
        customLast = 0;
        shardManager.setPresence(status == null ? OnlineStatus.ONLINE : status, customGames.length > 0 ? customGames[0] : null);
        customLast++;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
    }
}
