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

package pl.fratik.fratikcoiny.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.beans.Transient;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;

@Table("chinczykStats")
@Data
@AllArgsConstructor
@GIndex({"id", "userId", "timestamp"})
public class ChinczykStats implements DatabaseEntity {
    @PrimaryKey
    private final String id;
    private final String userId;
    private final long timestamp;
    private long bluePlays;
    private long greenPlays;
    private long yellowPlays;
    private long redPlays;
    private long travelledSpaces;
    private long rolls;
    private long rolledTotals;
    private long kills;
    private long deaths;
    private long enteredHome;
    private long leftStart;
    private long normalWins;
    private long walkovers;
    private long normalLosses;
    private long leaves;
    private long timePlayedSeconds;

    public ChinczykStats(String userId, long timestamp) {
        id = userId + timestamp;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public static Map<String, ChinczykStats> getStatsFromGame(Chinczyk chinczyk) {
        Map<String, ChinczykStats> map = new HashMap<>();
        long timestamp = Instant.now().toEpochMilli();
        Function<User, ChinczykStats> getStats = usr -> map.computeIfAbsent(usr.getId(), id -> new ChinczykStats(id, timestamp));
        for (Chinczyk.Player player : chinczyk.getPlayers()) {
            if (chinczyk.getWinner().equals(player)) {
                if (chinczyk.getPlayers().stream().filter(p -> p.getStatus() == Chinczyk.PlayerStatus.PLAYING).count() == 1)
                    getStats.apply(player.getUser()).walkovers++;
                else getStats.apply(player.getUser()).normalWins++;
            } else {
                if (player.getStatus() == Chinczyk.PlayerStatus.LEFT)
                    getStats.apply(player.getUser()).leaves++;
                else getStats.apply(player.getUser()).normalLosses++;
            }
            switch (player.getPlace()) {
                case BLUE:
                    getStats.apply(player.getUser()).bluePlays++;
                    break;
                case GREEN:
                    getStats.apply(player.getUser()).greenPlays++;
                    break;
                case YELLOW:
                    getStats.apply(player.getUser()).yellowPlays++;
                    break;
                case RED:
                    getStats.apply(player.getUser()).redPlays++;
                    break;
                default:
                    throw new IllegalStateException("Nieoczekiwana wartość: " + player.getPlace());
            }
            getStats.apply(player.getUser()).timePlayedSeconds +=
                    (chinczyk.getEnd().getEpochSecond() - chinczyk.getStart().getEpochSecond());
        }
        for (Chinczyk.Event event : chinczyk.getEvents()) {
            if (event.getType() != null) {
                switch (event.getType()) {
                    case GAME_START:
                    case WON:
                    case LEFT_GAME:
                        continue;
                    case LEFT_START:
                        getStats.apply(event.getPlayer().getUser()).leftStart++;
                        break;
                    case MOVE:
                        getStats.apply(event.getPlayer().getUser()).travelledSpaces += event.getRolled();
                        break;
                    case THROW:
                        getStats.apply(event.getPlayer().getUser()).travelledSpaces += event.getRolled();
                        getStats.apply(event.getPiece().getPlayer().getUser()).kills++;
                        getStats.apply(event.getPiece2().getPlayer().getUser()).deaths++;
                        break;
                    case ENTERED_HOME:
                        getStats.apply(event.getPlayer().getUser()).travelledSpaces += event.getRolled();
                        getStats.apply(event.getPlayer().getUser()).enteredHome++;
                        break;
                    default:
                        throw new IllegalStateException("Nieoczekiwana wartość " + event.getType());
                }
            }
            getStats.apply(event.getPlayer().getUser()).rolls++;
            getStats.apply(event.getPlayer().getUser()).rolledTotals += event.getRolled();
        }
        return map;
    }

    public static long getCurrentStorageDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        return cal.toInstant().toEpochMilli();
    }

    @Transient
    @Override
    @JsonIgnore
    public String getTableName() {
        return "chinczykStats";
    }

    public void addStats(ChinczykStats stats) {
        bluePlays += stats.bluePlays;
        greenPlays += stats.greenPlays;
        yellowPlays += stats.yellowPlays;
        redPlays += stats.redPlays;
        travelledSpaces += stats.travelledSpaces;
        rolls += stats.rolls;
        rolledTotals += stats.rolledTotals;
        kills += stats.kills;
        deaths += stats.deaths;
        enteredHome += stats.enteredHome;
        leftStart += stats.leftStart;
        normalWins += stats.normalWins;
        walkovers += stats.walkovers;
        normalLosses += stats.normalLosses;
        leaves += stats.leaves;
        timePlayedSeconds += stats.timePlayedSeconds;
    }
}
