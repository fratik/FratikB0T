/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.*;
import pl.fratik.core.util.UserUtil;

import java.util.LinkedHashMap;

public class UsunCommand extends CoinCommand {

    public UsunCommand(MemberDao memberDao, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        super(memberDao, guildDao, redisCacheManager);
        name = "usun";
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
        GuildConfig.Moneta m = resolveMoneta(context);
        Member komu = (Member) context.getArgs()[0];
        int ile = (int) context.getArgs()[1];
        if (ile == 0) {
            context.send(context.getTranslated("usun.badnumber", m.getShort(context)));
            return false;
        }
        if (komu.getUser().isBot()) {
            context.send(context.getTranslated("usun.bot", m.getShort(context)));
            return false;
        }
        MemberConfig mc = memberDao.get(komu);
        long hajs = mc.getKasa() - ile;
        if (hajs < 0) {
            context.send(context.getTranslated("usun.badnumber.sub", m.getShort(context)));
            return false;
        }
        mc.setKasa(hajs);
        memberDao.save(mc);
        context.send(context.getTranslated("usun.success", m.getShort(context), UserUtil.formatDiscrim(komu),
                hajs, m.getShort(context)));
        return true;
    }
}
