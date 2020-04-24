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
import pl.fratik.core.entity.*;

import java.util.LinkedHashMap;

public class GiveCommand extends CoinCommand {

    public GiveCommand(MemberDao memberDao, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        super(memberDao, guildDao, redisCacheManager);
        name = "give";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("osoba", "member");
        hmap.put("hajs", "integer");
        uzycie = new Uzycie(hmap, new boolean[] {true, true});
        uzycieDelim = " ";
        aliases = new String[] {"daj", "dk", "dajmukase", "dajmutrochekasy", "dajmumojakase", "pay"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig.Moneta m = resolveMoneta(context);
        Member komu = (Member) context.getArgs()[0];
        int ile = (int) context.getArgs()[1];
        MemberConfig od = memberDao.get(context.getMember());
        MemberConfig kurwaDo = memberDao.get(komu);
        if (od.getKasa() < ile) {
            context.send(context.getTranslated("give.no.money", m.getShort(context)));
            return false;
        }
        if (context.getSender().getId().contains(komu.getUser().getId())) {
            context.send(context.getTranslated("give.no.self", m.getShort(context)));
            return false;
        }
        od.setKasa(od.getKasa() - ile);
        kurwaDo.setKasa(kurwaDo.getKasa() + ile);
        memberDao.save(od);
        memberDao.save(kurwaDo);
        context.send(context.getTranslated("give.success", m.getShort(context)));
        return true;
    }
}
