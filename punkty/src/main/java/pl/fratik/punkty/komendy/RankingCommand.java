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

package pl.fratik.punkty.komendy;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;
import pl.fratik.punkty.entity.PunktyDao;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

public class RankingCommand extends NewCommand {
    private final PunktyDao punktyDao;
    private final LicznikPunktow licznik;
    private final MemberDao memberDao;
    private final EventBus eventBus;
    private final EventWaiter eventWaiter;

    public RankingCommand(PunktyDao punktyDao, LicznikPunktow licznik, MemberDao memberDao, EventBus eventBus, EventWaiter eventWaiter) {
        this.punktyDao = punktyDao;
        this.licznik = licznik;
        this.memberDao = memberDao;
        this.eventBus = eventBus;
        this.eventWaiter = eventWaiter;
        name = "ranking";
        cooldown = 7;
    }

    @SuppressWarnings("squid:S1192")
    @SubCommand(name="punkty")
    public void punkty(@NotNull NewCommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.replyEphemeral(context.getTranslated("punkty.off"));
            return;
        }
        InteractionHook hook = context.defer(false);
        send("points", punktyDao.getTopkaPunktow(context.getGuild()), context, hook,
                i -> context.getTranslated("ranking.ranking.points", i, LicznikPunktow.calculateLvl(i)),
                i -> context.getTranslated("ranking.ranking.points.summary", i, LicznikPunktow.calculateLvl(i)));
    }

    @SubCommand(name="fratikcoin")
    public void fratikcoin(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        List<MemberConfig> mc = new ArrayList<>();
        List<MemberConfig> mcAa = memberDao.getAll();
        if (mcAa.isEmpty()) {
            context.sendMessage(context.getTranslated("ranking.fratikcoin.empty"));
            return;
        }
        mcAa.sort(Comparator.comparingLong(MemberConfig::getFratikCoiny).reversed());
        for (MemberConfig c : mcAa) {
            if (c.getGuildId().equals(context.getGuild().getId())) mc.add(c);
            if (mc.size() == 10) break;
        }
        Map<String, Long> dane = new LinkedHashMap<>();
        for (MemberConfig c : mc) dane.put(c.getUserId(), c.getFratikCoiny());
        send("fratikcoin", dane, context, hook,
                i -> context.getTranslated("ranking.ranking.fratikcoin", i),
                Object::toString);
    }

    private <T> void send(String type, Map<String, T> datas, NewCommandContext context, InteractionHook hook, Function<T, String> dataMapper, Function<T, String> summaryMapper) {
        if (datas.isEmpty()) {
            hook.sendMessage(context.getTranslated("ranking.empty")).queue();
            return;
        }

        Color primColor = UserUtil.getPrimColor(context.getSender());

        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();

        StringBuilder summary = new StringBuilder();

        int index = 1;
        for (Map.Entry<String, T> entry : datas.entrySet()) {
            summary.append(index).append(". ").append(User.fromId(entry.getKey()).getAsMention()).append(": ")
                    .append(summaryMapper.apply(entry.getValue())).append("\n");
            final int finalIndex = index;
            FutureTask<EmbedBuilder> task = new FutureTask<>(() -> {
                User uzytkownik = context.getShardManager().retrieveUserById(entry.getKey()).complete();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(primColor);
                eb.setAuthor(uzytkownik.getAsTag(), null);
                eb.setTitle(context.getTranslated("ranking.ranking", finalIndex));
                eb.setDescription(dataMapper.apply(entry.getValue()));
                eb.setImage(uzytkownik.getEffectiveAvatar().getUrl(2048));
                eb.setThumbnail(context.getGuild().getIconUrl());

                return eb;
            });
            pages.add(task);
            if (index++ == 10) break;
        }

        FutureTask<EmbedBuilder> futureSummary = new FutureTask<>(() -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(primColor);
            eb.setTitle(context.getTranslated(String.format("ranking.%s.header", type)));
            eb.setDescription(summary);
            eb.setThumbnail(context.getGuild().getIconUrl());
            return eb;
        });

        pages.add(futureSummary);

        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus)
            .create(hook);
    }

}
