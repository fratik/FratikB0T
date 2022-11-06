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
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.command.SubCommandGroup;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.fratikcoiny.entity.ChinczykState;
import pl.fratik.fratikcoiny.entity.ChinczykStateDao;
import pl.fratik.fratikcoiny.entity.ChinczykStats;
import pl.fratik.fratikcoiny.entity.ChinczykStatsDao;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

public class ChinczykCommand extends NewCommand {
    @NotNull
    private final ShardManager shardManager;
    private final EventBus eventBus;
    private final EventWaiter eventWaiter;
    private final ChinczykStatsDao chinczykStatsDao;
    private final ChinczykStateDao chinczykStateDao;
    private final Tlumaczenia tlumaczenia;
    private final Set<Chinczyk> instances;

    public ChinczykCommand(ShardManager shardManager, EventBus eventBus, EventWaiter eventWaiter, ChinczykStatsDao chinczykStatsDao, ChinczykStateDao chinczykStateDao, Tlumaczenia tlumaczenia) {
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.eventWaiter = eventWaiter;
        this.chinczykStatsDao = chinczykStatsDao;
        this.chinczykStateDao = chinczykStateDao;
        this.tlumaczenia = tlumaczenia;
        name = "chinczyk";
        instances = new HashSet<>();
        if (shardManager.getShards().stream().anyMatch(s -> !s.getStatus().equals(JDA.Status.CONNECTED)))
            eventBus.register(this);
        else loadSavedGames();
    }

    private void loadSavedGames() {
        for (ChinczykState state : chinczykStateDao.getAll()) {
            try {
                instances.add(new Chinczyk(new ByteArrayInputStream(Base64.getDecoder().decode(state.getState())),
                        shardManager, eventBus, this::endCallback, tlumaczenia));
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Nie udało się załadować stanu chińczyka!", e);
            }
            chinczykStateDao.delete(state.getChannelId());
        }
    }

    @Subscribe
    public void onConnected(ConnectedEvent e) {
        eventBus.unregister(this);
        loadSavedGames();
    }

    @Override
    public void onUnregister() {
        for (Chinczyk chi : new HashSet<>(instances)) chi.shutdown(chinczykStateDao);
    }

    @SubCommandGroup(name = "staty")
    @SubCommand(name = "global")
    public void globalStats(@NotNull NewCommandContext context) {
        stats("0", context, false);
    }

    @SubCommandGroup(name = "staty")
    @SubCommand(name = "osoba", usage = "[osoba:user]")
    public void userStats(@NotNull NewCommandContext context) {
        User usr = context.getArgumentOr("osoba", context.getSender(), OptionMapping::getAsUser);
        stats(usr.getId(), context, true);
    }

    public boolean stats(String id, @NotNull NewCommandContext context, boolean withWins) {
        InteractionHook hook = context.defer(false);
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
                    true, withWins, withWins, true).setFooter(sdf.format(new Date(time))));
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus, pages.size()).setCustomFooter(true).create(hook);
        return true;
    }

    @SubCommand(name = "zagraj")
    public void game(@NotNull NewCommandContext context) {
        if (instances.stream().anyMatch(i -> i.getChannel().equals(context.getChannel()))) {
            context.replyEphemeral(context.getTranslated("chinczyk.game.in.progress"));
            return;
        }
        if (context.getChannel() instanceof ThreadChannel &&
                instances.stream().anyMatch(i -> i.getChannel().equals(((ThreadChannel) context.getChannel()).getParentChannel()))) {
            context.replyEphemeral(context.getTranslated("chinczyk.parent.game.in.progress"));
            return;
        }
        GuildMessageChannel chan = context.getChannel().asGuildMessageChannel();
        if (!chan.canTalk() || !context.getGuild().getSelfMember()
                .hasPermission(chan, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            context.replyEphemeral(context.getTranslated("chinczyk.game.no.perms"));
            return;
        }
        context.deferAsync(true);
        instances.add(new Chinczyk(context, eventBus, this::endCallback));
        context.sendMessage(context.getTranslated("chinczyk.game.started.message"));
    }

    private void endCallback(Chinczyk chinczyk) {
        instances.remove(chinczyk);
        if (chinczyk.getStatus() == Chinczyk.Status.CANCELLED || chinczyk.getStatus() == Chinczyk.Status.ERRORED) return;
        if (chinczyk.isCheats()) return;
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
