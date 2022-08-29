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

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

public class GiveCommand extends NewCommand {

    private final MemberDao memberDao;

    public GiveCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "give";
        usage = "<osoba:user> <ile:int>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member komu = context.getArguments().get("osoba").getAsMember();
        if (komu == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        context.deferAsync(false);
        int ile = context.getArguments().get("ile").getAsInt();
        MemberConfig od = memberDao.get(context.getMember());
        MemberConfig kurwaDo = memberDao.get(komu);
        if (od.getFratikCoiny() < ile) {
            context.sendMessage(context.getTranslated("give.no.money"));
            return;
        }
        if (context.getSender().getId().contains(komu.getUser().getId())) {
            context.sendMessage(context.getTranslated("give.no.self"));
            return;
        }
        od.setFratikCoiny(od.getFratikCoiny() - ile);
        kurwaDo.setFratikCoiny(kurwaDo.getFratikCoiny() + ile);
        memberDao.save(od);
        memberDao.save(kurwaDo);
        context.sendMessage(context.getTranslated("give.success"));
    }

}
