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

package pl.fratik.fratikcoiny.games;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Statyczne;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.fratikcoiny.entity.ChinczykStats;
import pl.fratik.fratikcoiny.entity.ChinczykStatsDao;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.awt.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.List;
import java.util.*;

public class ChinczykCommand extends Command {
    private final EventBus eventBus;
    private final EventWaiter eventWaiter;
    private final ChinczykStatsDao chinczykStatsDao;
    private final Set<Chinczyk> instances;

    public ChinczykCommand(EventBus eventBus, EventWaiter eventWaiter, ChinczykStatsDao chinczykStatsDao) {
        this.eventBus = eventBus;
        this.eventWaiter = eventWaiter;
        this.chinczykStatsDao = chinczykStatsDao;
        name = "chinczyk";
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
        uzycieDelim = " ";
        instances = new HashSet<>();
    }

    @SubCommand(name = "stats")
    public boolean stats(@NotNull CommandContext context) {
        Message msg = context.reply(context.getTranslated("generic.loading"));
        List<EmbedBuilder> pages = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.add(Calendar.DAY_OF_MONTH, -29);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        for (int i = 0; i < 30; i++) {
            long time = cal.toInstant().toEpochMilli() + i * 86400000L;
            ChinczykStats stats = chinczykStatsDao.get(context.getSender().getId() + time);
            if (stats == null) {
                pages.add(new EmbedBuilder()
                        .setTitle(context.getTranslated("chinczyk.stats.title"))
                        .setDescription(context.getTranslated("chinczyk.stats.no.data"))
                        .setColor(Color.decode(Statyczne.BRAND_COLOR))
                        .setFooter(sdf.format(new Date(time)))
                );
                continue;
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
                    .setTitle(context.getTranslated("chinczyk.stats.title"))
                    .addField(context.getTranslated("chinczyk.stats.wins"),
                            context.getTranslated("chinczyk.stats.wins.text",
                                    formatNumber(context, totalWins),
                                    formatNumber(context, stats.getNormalWins()),
                                    formatNumber(context, stats.getWalkovers())), true)
                    .addField(context.getTranslated("chinczyk.stats.losses"),
                            context.getTranslated("chinczyk.stats.losses.text",
                                    formatNumber(context, totalLosses),
                                    formatNumber(context, stats.getNormalLosses()),
                                    formatNumber(context, stats.getLeaves())), true)
                    .addField(context.getTranslated("chinczyk.stats.win.percentage"),
                            context.getTranslated("chinczyk.stats.win.percentage.text",
                                    formatNumber(context, ((double) totalWins) / (totalWins + totalLosses) * 100),
                                    formatNumber(context, ((double) totalLosses) / (totalWins + totalLosses) * 100)), true)
                    .addField(context.getTranslated("chinczyk.stats.plays"), playsText.toString(), true)
                    .addField(context.getTranslated("chinczyk.stats.travelled"),
                            formatNumber(context, stats.getTravelledSpaces()), true)
                    .addField(context.getTranslated("chinczyk.stats.rolls"),
                            formatNumber(context, stats.getRolls()), true)
                    .addField(context.getTranslated("chinczyk.stats.rolls.total"),
                            formatNumber(context, stats.getRolledTotals()), true)
                    .addField(context.getTranslated("chinczyk.stats.kills"),
                            formatNumber(context, stats.getKills()), true)
                    .addField(context.getTranslated("chinczyk.stats.deaths"),
                            formatNumber(context, stats.getDeaths()), true)
                    .addField(context.getTranslated("chinczyk.stats.kdratio"),
                            formatNumber(context, ((double) stats.getKills()) / stats.getDeaths()), true)
                    .addField(context.getTranslated("chinczyk.stats.entered.home"),
                            formatNumber(context, stats.getEnteredHome()), true)
                    .addField(context.getTranslated("chinczyk.stats.left.start"),
                            formatNumber(context, stats.getLeftStart()), true);
            if (topPlace != null) eb.setColor(topPlace.getBgColor());
            else eb.setColor(Color.decode(Statyczne.BRAND_COLOR));
            eb.setFooter(sdf.format(new Date(time)));
            pages.add(eb);
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus).setCustomFooter(true).create(msg);
        return true;
    }

    private String formatNumber(CommandContext context, double l) {
        return NumberFormat.getInstance(context.getLanguage().getLocale()).format(CommonUtil.round(l, 2, RoundingMode.HALF_UP));
    }

    private String formatNumber(CommandContext context, long l) {
        return NumberFormat.getInstance(context.getLanguage().getLocale()).format(l);
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        if (instances.stream().anyMatch(i -> i.getChannel().equals(context.getMessageChannel()))) {
            context.reply(context.getTranslated("chinczyk.game.in.progress"));
            return false;
        }
        instances.add(new Chinczyk(context, eventBus, this::endCallback));
        return true;
    }

    private void endCallback(Chinczyk chinczyk) {
        instances.remove(chinczyk);
        if (chinczyk.getStatus() == Chinczyk.Status.CANCELLED || chinczyk.getStatus() == Chinczyk.Status.ERRORED) return;
        Map<String, ChinczykStats> stats = ChinczykStats.getStatsFromGame(chinczyk);
        long currentStorageDate = ChinczykStats.getCurrentStorageDate();
        chinczykStatsDao.getLock().lock();
        try {
            for (Map.Entry<String, ChinczykStats> stat : stats.entrySet()) {
                ChinczykStats s = chinczykStatsDao.get(stat.getKey() + currentStorageDate);
                if (s == null) s = new ChinczykStats(stat.getKey(), currentStorageDate);
                s.addStats(stat.getValue());
                chinczykStatsDao.save(s);
            }
        } finally {
            chinczykStatsDao.getLock().unlock();
        }
    }

}
