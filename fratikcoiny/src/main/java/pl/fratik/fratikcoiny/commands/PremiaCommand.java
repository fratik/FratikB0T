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

package pl.fratik.fratikcoiny.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PremiaCommand extends MoneyCommand {
    private final GuildDao guildDao;
    private final MemberDao memberDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public PremiaCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "premia";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        Map<Role, GuildConfig.Wyplata> wyplaty = new LinkedHashMap<>();
        for (Role role : context.getMember().getRoles()) {
            GuildConfig.Wyplata wyplata = gc.getWyplaty().get(role.getId());
            if (wyplata != null) wyplaty.put(role, wyplata);
        }
        if (wyplaty.isEmpty()) {
            context.reply(context.getTranslated("premia.nothing", "/"));
            return;
        }
        MemberConfig mc = memberDao.get(context.getMember());
        Date teraz = new Date();
        Emoji emotkaFc = getFratikCoin(context);
        StringBuilder sb = new StringBuilder(context.getTranslated("premia.start")).append("\n\n");
        long fc = 0;
        int appended = 0;
        for (Map.Entry<Role, GuildConfig.Wyplata> e : wyplaty.entrySet()) {
            Role role = e.getKey();
            GuildConfig.Wyplata wyplata = e.getValue();
            Date date = mc.getWyplatyDate().get(role.getId());
            String textToAppend = role.getAsMention() + ": ";
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.MINUTE, wyplata.getCooldown());
                if (cal.getTime().after(teraz)) {
                    textToAppend += context.getTranslated("premia.timeout",
                            DurationUtil.humanReadableFormat(cal.getTimeInMillis() - teraz.getTime(), false));
                    if (++appended <= 15) sb.append(textToAppend).append('\n');
                    continue;
                }
            }
            textToAppend += wyplata.getKwota() + emotkaFc.getFormatted();
            fc += wyplata.getKwota();
            mc.getWyplatyDate().put(role.getId(), teraz);
            if (++appended <= 15) sb.append(textToAppend).append('\n');
        }
        if (appended > 15) sb.append(context.getTranslated("premia.more", appended - 15));
        else sb.setLength(sb.length() - 1);
        mc.setFratikCoiny(mc.getFratikCoiny() + fc);
        if (checkTooMuch(mc.getFratikCoiny(), null)) {
            context.reply(context.getTranslated("premia.too.much.money"));
            return;
        }
        if (fc != 0) {
            sb.append("\n\n").append(context.getTranslated("premia.summary", fc, emotkaFc.getFormatted(),
                    mc.getFratikCoiny(), emotkaFc.getFormatted()));
            memberDao.save(mc);
        }
        context.reply(sb.toString());
    }

    @SubCommand(name = "ustaw", usage = "<rola:role> <coiny:number> <cooldown:string>")
    public void set(@NotNull NewCommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2) {
            execute(context);
            return;
        }

        try {
            Role role = context.getArguments().get("rola").getAsRole();
            Long fc = context.getArguments().get("coiny").getAsLong();
            String cooldown = context.getArguments().get("cooldown").getAsString();
            DurationUtil.Response response = DurationUtil.parseDuration(cooldown);
            GuildConfig gc = guildDao.get(context.getGuild());
            int minut = Math.toIntExact(TimeUnit.MINUTES.convert(response.getDuration(), TimeUnit.MILLISECONDS));
            if (minut < 5) {
                context.reply(context.getTranslated("premia.set.cooldown"));
                return;
            }
            if (fc < 0 || fc > 1_000_000_000L) {
                context.reply(context.getTranslated("premia.set.limit"));
                return;
            }
            gc.getWyplaty().put(role.getId(), new GuildConfig.Wyplata(fc, minut));
            guildDao.save(gc);
            context.reply(context.getTranslated("premia.set.success"));
        } catch (IllegalArgumentException | ArithmeticException e) {
            context.reply(context.getTranslated("sklep.kup.failed"));
        }
    }

    @SubCommand(name = "usuń", usage = "<rola:role>")
    public void remove(@NotNull NewCommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2) {
            execute(context);
            return;
        }
        try {
            Role role = context.getArguments().get("rola").getAsRole();
            GuildConfig gc = guildDao.get(context.getGuild());
            if (gc.getWyplaty().remove(role.getId()) == null) {
                context.reply(context.getTranslated("premia.unset.null"));
                return;
            }
            guildDao.save(gc);
            context.reply(context.getTranslated("premia.unset.success"));
        } catch (IllegalArgumentException | ArithmeticException e) {
            context.reply(context.getTranslated("sklep.kup.failed"));
        }
    }

    @SubCommand(name = "lista")
    public void list(@NotNull NewCommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2) {
            execute(context);
            return;
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        List<EmbedBuilder> pages = new ArrayList<>();
        //noinspection ConstantConditions
        gc.getWyplaty().entrySet().stream().filter(e -> context.getGuild().getRoleById(e.getKey()) != null)
                .sorted((a, b) -> context.getGuild().getRoleById(a.getKey()).getName()
                        .compareToIgnoreCase(context.getGuild().getRoleById(b.getKey()).getName()))
                .forEachOrdered(e -> {
            Role role = context.getGuild().getRoleById(e.getKey());
            if (role == null) return; // filter wyżej sprawdza, ale potem IDE się pluje więc niech zostanie
            GuildConfig.Wyplata wyplata = e.getValue();
            pages.add(new EmbedBuilder()
                    .setColor(role.getColorRaw())
                    .setTitle(role.getName())
                    .addField(context.getTranslated("premia.list.kwota"), String.valueOf(wyplata.getKwota()), false)
                    .addField(context.getTranslated("premia.list.cooldown"),
                            DurationUtil.humanReadableFormat((long) wyplata.getCooldown() * 60 * 1000, false), false)
            );
        });
        if (pages.isEmpty()) {
            context.reply(context.getTranslated("premia.list.empty"));
            return;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(context.getChannel());
    }
}
