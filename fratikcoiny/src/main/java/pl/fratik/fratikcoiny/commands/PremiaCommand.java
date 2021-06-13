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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        aliases = new String[] {"odbierz"};
        category = CommandCategory.MONEY;
        uzycieDelim = " ";
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        allowPermLevelChange = false;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        Map<Role, GuildConfig.Wyplata> wyplaty = new LinkedHashMap<>();
        for (Role role : context.getMember().getRoles()) {
            GuildConfig.Wyplata wyplata = gc.getWyplaty().get(role.getId());
            if (wyplata != null) wyplaty.put(role, wyplata);
        }
        if (wyplaty.isEmpty()) {
            context.reply(context.getTranslated("premia.nothing", context.getPrefix()));
            return false;
        }
        MemberConfig mc = memberDao.get(context.getMember());
        Date teraz = new Date();
        Emote emotkaFc = getFratikCoin(context);
        StringBuilder sb = new StringBuilder(context.getTranslated("premia.start")).append("\n\n");
        long fc = 0;
        int appended = 0;
        for (Map.Entry<Role, GuildConfig.Wyplata> e : wyplaty.entrySet()) {
            Role role = e.getKey();
            GuildConfig.Wyplata wyplata = e.getValue();
            sb.append(role.getAsMention()).append(": ");
            Date date = mc.getWyplatyDate().get(role.getId());
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.MINUTE, wyplata.getCooldown());
                if (cal.getTime().after(teraz)) {
                    if (++appended <= 15) sb.append(context.getTranslated("premia.timeout",
                            DurationUtil.humanReadableFormat(cal.getTimeInMillis() - teraz.getTime(), false))).append('\n');
                    continue;
                }
            }
            fc += wyplata.getKwota();
            mc.getWyplatyDate().put(role.getId(), teraz);
            if (++appended <= 15) sb.append(wyplata.getKwota()).append(emotkaFc.getAsMention()).append('\n');
        }
        if (appended > 15) sb.append(context.getTranslated("premia.more", appended - 15));
        else sb.setLength(sb.length() - 1);
        mc.setFratikCoiny(mc.getFratikCoiny() + fc);
        if (checkTooMuch(mc.getFratikCoiny(), null)) {
            context.reply(context.getTranslated("premia.too.much.money"));
            return false;
        }
        if (fc != 0) {
            sb.append("\n\n").append(context.getTranslated("premia.summary", fc, emotkaFc.getAsMention(),
                    mc.getFratikCoiny(), emotkaFc.getAsMention()));
            memberDao.save(mc);
        }
        context.reply(sb.toString());
        return true;
    }

    @SubCommand(name = "set")
    public boolean set(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2)
            return execute(context);
        LinkedHashMap<String, String> ktoWymyslilTenTepySystem = new LinkedHashMap<>();
        ktoWymyslilTenTepySystem.put("role", "role");
        ktoWymyslilTenTepySystem.put("fc", "long");
        ktoWymyslilTenTepySystem.put("cooldown", "string");
        ktoWymyslilTenTepySystem.put("[...]", "string");
        try {
            Object[] args = new Uzycie(ktoWymyslilTenTepySystem, new boolean[] {true, true, true, false}).resolveArgs(context);
            Role role = (Role) args[0];
            Long fc = (Long) args[1];
            String cooldown = Arrays.stream(Arrays.copyOfRange(args, 2, args.length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
            DurationUtil.Response response = DurationUtil.parseDuration(cooldown);
            GuildConfig gc = guildDao.get(context.getGuild());
            int minut = Math.toIntExact(TimeUnit.MINUTES.convert(response.getDuration(), TimeUnit.MILLISECONDS));
            if (minut < 5) {
                context.reply(context.getTranslated("premia.set.cooldown"));
                return false;
            }
            if (fc < 0 || fc > 1_000_000_000L) {
                context.reply(context.getTranslated("premia.set.limit"));
                return false;
            }
            gc.getWyplaty().put(role.getId(), new GuildConfig.Wyplata(fc, minut));
            guildDao.save(gc);
            context.reply(context.getTranslated("premia.set.success"));
            return true;
        } catch (IllegalArgumentException | ArgsMissingException | ArithmeticException e) {
            context.reply(context.getTranslated("premia.set.missing.arguments", context.getPrefix(), context.getLabel()));
            return false;
        }
    }

    @SubCommand(name = "remove")
    public boolean remove(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2)
            return execute(context);
        try {
            Object[] args = new Uzycie("role", "role", true).resolveArgs(context);
            Role role = (Role) args[0];
            GuildConfig gc = guildDao.get(context.getGuild());
            if (gc.getWyplaty().remove(role.getId()) == null) {
                context.reply(context.getTranslated("premia.unset.null", context.getPrefix(), context.getLabel()));
                return false;
            }
            guildDao.save(gc);
            context.reply(context.getTranslated("premia.unset.success"));
            return true;
        } catch (IllegalArgumentException | ArgsMissingException | ArithmeticException e) {
            context.reply(context.getTranslated("premia.set.missing.arguments", context.getPrefix(), context.getLabel()));
            return false;
        }
    }

    @SubCommand(name = "list", aliases = "lista")
    public boolean list(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 2)
            return execute(context);
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
            return false;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(context.getMessageChannel());
        return true;
    }
}
