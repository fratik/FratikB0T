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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

import java.util.LinkedHashMap;

public class DodajFcCommand extends NewCommand {

    private final MemberDao memberDao;

    public DodajFcCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
        name = "dodajfc";
        usage = "<osoba:user> <ile:number>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member komu = context.getArguments().get("osoba").getAsMember();
        int ile = context.getArguments().get("ile").getAsInt();
        if (ile == 0) {
            context.reply(context.getTranslated("dodajfc.badnumber"));
            return;
        }
        if (komu.getUser().isBot()) {
            context.reply(context.getTranslated("dodajfc.bot"));
            return;
        }
        MemberConfig mc = memberDao.get(komu);
        mc.setFratikCoiny(mc.getFratikCoiny() + ile);
        memberDao.save(mc);
        context.reply(context.getTranslated("dodajfc.success"));
    }
}
