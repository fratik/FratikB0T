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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.UserUtil;

import java.util.*;
import java.util.stream.Collectors;

public class PremiaCommand extends MoneyCommand {
    private final GuildDao guildDao;
    private final MemberDao memberDao;

    public PremiaCommand(GuildDao guildDao, MemberDao memberDao) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
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
        String id = null;
        GuildConfig.Wyplata wyplata = null;
        for (Role role : context.getMember().getRoles()) {
            id = role.getId();
            if ((wyplata = gc.getWyplaty().get(id)) != null) break;
        }
        if (id == null || wyplata == null) {
            context.reply(context.getTranslated("premia.nothing", context.getPrefix()));
            return false;
        }
        MemberConfig mc = memberDao.get(context.getMember());
        Date teraz = new Date();
        Date date = mc.getWyplatyDate().get(id);
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MINUTE, wyplata.getCooldown());
            if (cal.getTime().after(teraz)) {
                context.reply(context.getTranslated("premia.timeout",
                        DurationUtil.humanReadableFormat(cal.getTimeInMillis() - teraz.getTime(), false)));
                return false;
            }
        }
        mc.setFratikCoiny(mc.getFratikCoiny() + wyplata.getKwota());
        mc.getWyplatyDate().put(id, teraz);
        memberDao.save(mc);
        Emote emotkaFc = getFratikCoin(context);
        context.reply(context.getTranslated("premia.success", wyplata.getKwota(), emotkaFc.getAsMention(),
                mc.getFratikCoiny(), emotkaFc.getAsMention()));
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
            gc.getWyplaty().put(role.getId(), new GuildConfig.Wyplata(fc, Math.toIntExact(response.getDuration())));
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
}
