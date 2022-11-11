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
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.UserUtil;

public class UsunFcCommand extends MoneyCommand {

    private final MemberDao memberDao;

    public UsunFcCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "usunfc";
        usage = "<osoba:user> <ile:int>";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member komu = context.getArguments().get("osoba").getAsMember();
        int ile = context.getArguments().get("ile").getAsInt();
        if (ile == 0) {
            context.replyEphemeral(context.getTranslated("usunfc.badnumber"));
            return;
        }
        if (komu.getUser().isBot()) {
            context.replyEphemeral(context.getTranslated("usunfc.bot"));
            return;
        }
        context.defer(false);
        MemberConfig mc = memberDao.get(komu);
        long hajs = mc.getFratikCoiny() - ile;
        if (hajs < 0) {
            context.sendMessage(context.getTranslated("usunfc.badnumber.sub"));
            return;
        }
        Emoji fc = getFratikCoin(context);
        mc.setFratikCoiny(hajs);
        memberDao.save(mc);
        context.sendMessage(context.getTranslated("usunfc.success", UserUtil.formatDiscrim(komu), hajs, fc.getFormatted()));
    }
}
