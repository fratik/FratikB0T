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

package pl.fratik.starboard.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.StringUtil;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

import java.util.ArrayList;

public class StarBlacklistCommand extends NewCommand {
    private final StarDataDao starDataDao;

    public StarBlacklistCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starblacklist";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
        usage = "<osoba:user>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member member = context.getArguments().get("osoba").getAsMember();
        StarsData std = starDataDao.get(context.getGuild());

        if (std.getBlacklista() == null) std.setBlacklista(new ArrayList<>());

        String tag = StringUtil.escapeMarkdown(member.getUser().getAsTag());

        if (std.getBlacklista().remove(member.getId())) {
            context.replyEphemeral(context.getTranslated("starblacklist.removed", tag));
            starDataDao.save(std);
            return;
        }

        std.getBlacklista().add(member.getId());
        starDataDao.save(std);
        context.replyEphemeral(context.getTranslated("starblacklist.added", tag));
    }
}
