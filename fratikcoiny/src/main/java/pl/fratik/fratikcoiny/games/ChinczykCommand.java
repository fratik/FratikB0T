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
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.fratikcoiny.entity.ChinczykStats;
import pl.fratik.fratikcoiny.entity.ChinczykStatsDao;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

public class ChinczykCommand extends Command {
    private final ManagerArgumentow managerArgumentow;
    private final EventBus eventBus;
    private final EventWaiter eventWaiter;
    private final ChinczykStatsDao chinczykStatsDao;
    private final Set<Chinczyk> instances;

    public ChinczykCommand(ManagerArgumentow managerArgumentow, EventBus eventBus, EventWaiter eventWaiter, ChinczykStatsDao chinczykStatsDao) {
        this.managerArgumentow = managerArgumentow;
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

    @Override
    public void onUnregister() {
        for (Chinczyk chi : new HashSet<>(instances)) chi.shutdown();
    }

    @SubCommand(name = "globalStats")
    public boolean globalStats(@NotNull CommandContext context) {
        return stats("0", context, false);
    }

    @SubCommand(name = "stats")
    public boolean userStats(@NotNull CommandContext context) {
        User usr = null;
        if (context.getRawArgs().length >= 1) {
            if (context.getRawArgs()[0].equals("global")) return globalStats(context);
            usr = (User) managerArgumentow.getArguments().get("user")
                    .execute(context.getRawArgs()[0], context.getTlumaczenia(), context.getLanguage());
        }
        if (usr == null) usr = context.getSender();
        return stats(usr.getId(), context, true);
    }

    public boolean stats(String id, @NotNull CommandContext context, boolean withWins) {
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
            ChinczykStats stats = chinczykStatsDao.get(id + time);
            pages.add(ChinczykStats.renderEmbed(stats, null, context.getTlumaczenia(), context.getLanguage(),
                    true, withWins, true, true).setFooter(sdf.format(new Date(time))));
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus, pages.size()).setCustomFooter(true).create(msg);
        return true;
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
