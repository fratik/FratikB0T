/*
 * Copyright (C) 2019 FratikB0T Contributors
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
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

import java.util.LinkedHashMap;

public class UsunFcCommand extends Command {

    private final MemberDao memberDao;

    public UsunFcCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "usunfc";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        permLevel = PermLevel.ADMIN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("osoba", "member");
        hmap.put("hajs", "integer");
        uzycie = new Uzycie(hmap, new boolean[] {true, true});
        uzycieDelim = " ";
        aliases = new String[] {"usunfratikcoiny", "deletefc", "deletecoin", "deletefratikcoins", "deletecoins"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member komu = (Member) context.getArgs()[0];
        int ile = (int) context.getArgs()[1];
        if (ile == 0) {
            context.send(context.getTranslated("usunfc.badnumber"));
            return false;
        }
        if (komu.getUser().isBot()) {
            context.send(context.getTranslated("usunfc.bot"));
            return false;
        }
        MemberConfig mc = memberDao.get(komu);
        int hajs = Math.toIntExact(mc.getFratikCoiny() - ile);
        if (hajs < 0) {
            context.send(context.getTranslated("usunfc.badnumber.sub"));
            return false;
        }
        Emote fc = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.fratikCoin);
        if (fc == null) {
            throw new NullPointerException("ni ma emotki");
        }
        mc.setFratikCoiny(hajs);
        memberDao.save(mc);
        context.send(context.getTranslated("usunfc.success",
                UserUtil.formatDiscrim(komu),
                hajs, fc.getAsMention()));
        return true;
    }
}
