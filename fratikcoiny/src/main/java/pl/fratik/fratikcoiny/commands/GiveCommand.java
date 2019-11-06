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
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.entity.MemberDao;

import java.util.LinkedHashMap;

public class GiveCommand extends Command {

    private final MemberDao memberDao;

    public GiveCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "give";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("osoba", "member");
        hmap.put("hajs", "integer");
        uzycie = new Uzycie(hmap, new boolean[] {true, true});
        uzycieDelim = " ";
        aliases = new String[] {"daj", "dk", "dajmukase", "dajmutrochekasy", "dajmumojakase", "pay"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member komu = (Member) context.getArgs()[0];
        int ile = (int) context.getArgs()[1];
        MemberConfig od = memberDao.get(context.getMember());
        MemberConfig kurwaDo = memberDao.get(komu);
        if (od.getFratikCoiny() < ile) {
            context.send(context.getTranslated("give.no.money"));
            return false;
        }
        if (context.getSender().getId().contains(komu.getUser().getId())) {
            context.send(context.getTranslated("give.no.self"));
            return false;
        }
        od.setFratikCoiny(od.getFratikCoiny() - ile);
        kurwaDo.setFratikCoiny(kurwaDo.getFratikCoiny() + ile);
        memberDao.save(od);
        memberDao.save(kurwaDo);
        context.send(context.getTranslated("give.success"));
        return true;
    }
}
