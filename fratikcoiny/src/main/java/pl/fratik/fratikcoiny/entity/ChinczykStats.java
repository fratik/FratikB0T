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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.Statyczne;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.awt.*;
import java.beans.Transient;
import java.math.RoundingMode;
import java.text.NumberFormat;
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
    private long rolledSix;
    private long highestSixStreak;
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
        long timestamp = chinczyk.getEnd().toEpochMilli();
        long gameDuration = chinczyk.getEnd().getEpochSecond() - chinczyk.getStart().getEpochSecond();
        Function<User, ChinczykStats> getStats = usr -> map.computeIfAbsent(usr.getId(), id -> new ChinczykStats(id, timestamp));
        for (Chinczyk.Player player : chinczyk.getPlayers()) {
            ChinczykStats playerStats = getStats.apply(player.getUser());
            if (chinczyk.getWinner().equals(player)) {
                if (chinczyk.getPlayers().stream().filter(p -> p.getStatus() == Chinczyk.PlayerStatus.PLAYING).count() == 1)
                    playerStats.walkovers++;
                else playerStats.normalWins++;
            } else {
                if (player.getStatus() == Chinczyk.PlayerStatus.LEFT)
                    playerStats.leaves++;
                else playerStats.normalLosses++;
            }
            switch (player.getPlace()) {
                case BLUE:
                    playerStats.bluePlays++;
                    break;
                case GREEN:
                    playerStats.greenPlays++;
                    break;
                case YELLOW:
                    playerStats.yellowPlays++;
                    break;
                case RED:
                    playerStats.redPlays++;
                    break;
                default:
                    throw new IllegalStateException("Nieoczekiwana wartość: " + player.getPlace());
            }
            playerStats.timePlayedSeconds += gameDuration;
        }
        Map<Chinczyk.Player, Integer> lastRolled = new HashMap<>();
        Map<Chinczyk.Player, Integer> currentStreaks = new HashMap<>();
        for (Chinczyk.Event event : chinczyk.getEvents()) {
            ChinczykStats playerStats = getStats.apply(event.getPlayer().getUser());
            if (event.getType() != null) {
                switch (event.getType()) {
                    case GAME_START:
                    case WON:
                    case LEFT_GAME:
                        continue;
                    case LEFT_START:
                        playerStats.leftStart++;
                        break;
                    case MOVE:
                        playerStats.travelledSpaces += event.getRolled();
                        break;
                    case THROW:
                        playerStats.travelledSpaces += event.getRolled();
                        getStats.apply(event.getPiece().getPlayer().getUser()).kills++;
                        getStats.apply(event.getPiece2().getPlayer().getUser()).deaths++;
                        break;
                    case ENTERED_HOME:
                        playerStats.travelledSpaces += event.getRolled();
                        playerStats.enteredHome++;
                        break;
                    default:
                        throw new IllegalStateException("Nieoczekiwana wartość " + event.getType());
                }
            }
            playerStats.rolls++;
            playerStats.rolledTotals += event.getRolled();
            Integer l = lastRolled.put(event.getPlayer(), event.getRolled());
            if (event.getRolled() == 6) {
                playerStats.rolledSix++;
                currentStreaks.compute(event.getPlayer(), (p, i) -> {
                    if (i == null) i = 0;
                    if (l == null || l != 6) i = 1;
                    else i++;
                    return i;
                });
            } else playerStats.highestSixStreak = Math.max(playerStats.highestSixStreak,
                    currentStreaks.getOrDefault(event.getPlayer(), 0));
        }
        ChinczykStats gameStats = map.computeIfAbsent("0", id -> new ChinczykStats(id, timestamp));
        for (ChinczykStats s : map.values()) gameStats.addStats(s);
        gameStats.setTimePlayedSeconds(gameDuration);
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

    public static EmbedBuilder renderEmbed(ChinczykStats stats,
                                           User gamer,
                                           Tlumaczenia t,
                                           Language l,
                                           boolean withPlays,
                                           boolean renderDeaths,
                                           boolean renderTime) {
        if (stats == null) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(t.get(l, "chinczyk.stats.title"))
                    .setDescription(t.get(l, "chinczyk.stats.no.data"))
                    .setColor(Color.decode(Statyczne.BRAND_COLOR));
            if (gamer != null) eb.setAuthor(gamer.getAsTag(), null, gamer.getEffectiveAvatarUrl());
            return eb;
        }
        Chinczyk.Place topPlace = null;
        long topPlacePlays = 0;
        StringBuilder playsText = new StringBuilder();
        for (Chinczyk.Place p : Chinczyk.Place.values()) {
            playsText.append(p.getEmoji()).append(" ");
            long plays;
            switch (p) {
                case BLUE:
                    plays = stats.getBluePlays();
                    break;
                case GREEN:
                    plays = stats.getGreenPlays();
                    break;
                case YELLOW:
                    plays = stats.getYellowPlays();
                    break;
                case RED:
                    plays = stats.getRedPlays();
                    break;
                default:
                    throw new IllegalStateException("Nieoczekiwana wartość: " + p);
            }
            if (topPlacePlays < plays) {
                topPlace = p;
                topPlacePlays = plays;
            } else if (topPlacePlays == plays) {
                topPlace = null;
            }
            playsText.append(plays).append('\n');
        }
        long totalWins = stats.getNormalWins() + stats.getWalkovers();
        long totalLosses = stats.getNormalLosses() + stats.getLeaves();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(t.get(l, "chinczyk.stats.title"));
        if (withPlays) {
            eb
                    .addField(t.get(l, "chinczyk.stats.wins"),
                            t.get(l, "chinczyk.stats.wins.text", formatNumber(l, totalWins),
                                    formatNumber(l, stats.getNormalWins()), formatNumber(l, stats.getWalkovers())), true)
                    .addField(t.get(l, "chinczyk.stats.losses"),
                            t.get(l, "chinczyk.stats.losses.text",
                                    formatNumber(l, totalLosses), formatNumber(l, stats.getNormalLosses()),
                                    formatNumber(l, stats.getLeaves())), true)
                    .addField(t.get(l, "chinczyk.stats.win.percentage"),
                            t.get(l, "chinczyk.stats.win.percentage.text",
                                    formatNumber(l, ((double) totalWins) / (totalWins + totalLosses) * 100, false) + "%",
                                    formatNumber(l, ((double) totalLosses) / (totalWins + totalLosses) * 100, false) + "%"),
                            true)
                    .addField(t.get(l, "chinczyk.stats.plays"), playsText.toString(), true);
        }
        eb
                .addField(t.get(l, "chinczyk.stats.travelled"), formatNumber(l, stats.getTravelledSpaces()), true)
                .addField(t.get(l, "chinczyk.stats.rolls"), formatNumber(l, stats.getRolls()), true)
                .addField(t.get(l, "chinczyk.stats.rolled.six"), formatNumber(l, stats.getRolledSix()), true)
                .addField(t.get(l, "chinczyk.stats.highest.six.streak"), formatNumber(l, stats.getHighestSixStreak()), true)
                .addField(t.get(l, "chinczyk.stats.rolls.total"), formatNumber(l, stats.getRolledTotals()), true)
                .addField(t.get(l, "chinczyk.stats.kills"), formatNumber(l, stats.getKills()), true);
        if (renderDeaths) {
            eb
                    .addField(t.get(l, "chinczyk.stats.deaths"), formatNumber(l, stats.getDeaths()), true)
                    .addField(t.get(l, "chinczyk.stats.kdratio"),
                            formatNumber(l, ((double) stats.getKills()) / Math.max(stats.getDeaths(), 1), true), true);
        }
        eb
                .addField(t.get(l, "chinczyk.stats.entered.home"), formatNumber(l, stats.getEnteredHome()), true)
                .addField(t.get(l, "chinczyk.stats.left.start"), formatNumber(l, stats.getLeftStart()), true);
        if (renderTime) eb.addField(t.get(l, "chinczyk.stats.time"),
                DurationUtil.humanReadableFormat(stats.getTimePlayedSeconds() * 1000, false), true);
        if (topPlace != null) eb.setColor(topPlace.getBgColor());
        else eb.setColor(Color.decode(Statyczne.BRAND_COLOR));
        if (gamer != null) eb.setAuthor(gamer.getAsTag(), null, gamer.getEffectiveAvatarUrl());
        return eb;
    }

    private static String formatNumber(Language l, double d, boolean forceDecimal) {
        NumberFormat nf = NumberFormat.getInstance(l.getLocale());
        if (forceDecimal) nf.setMinimumFractionDigits(1);
        return nf.format(CommonUtil.round(d, 2, RoundingMode.HALF_UP));
    }

    private static String formatNumber(Language l, long i) {
        return NumberFormat.getInstance(l.getLocale()).format(i);
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
        rolledSix += stats.rolledSix;
        highestSixStreak = Math.max(stats.highestSixStreak, highestSixStreak);
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
