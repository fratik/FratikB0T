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

package pl.fratik.starboard.komendy;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.StringUtil;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

import java.util.ArrayList;

public class StarBlacklistCommand extends Command {
    private final StarDataDao starDataDao;

    public StarBlacklistCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starblacklist";
        permLevel = PermLevel.MOD;
        category = CommandCategory.STARBOARD;
        uzycie = new Uzycie("osoba", "member", true);
        ignoreGaPerm = true;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0) {
            CommonErrors.usage(context);
            return false;
        }
        Member member = (Member) context.getArgs()[0];
        if (member == null) {
            CommonErrors.usage(context);
            return false;
        }
        StarsData std = starDataDao.get(context.getGuild());
        if (std.getBlacklista() == null) std.setBlacklista(new ArrayList<>());
        String tag = StringUtil.escapeMarkdown(member.getUser().getAsTag());
        if (std.getBlacklista().remove(member.getId())) {
            context.reply(context.getTranslated("starblacklist.removed", tag));
            starDataDao.save(std);
            return true;
        }
        std.getBlacklista().add(member.getId());
        starDataDao.save(std);
        context.reply(context.getTranslated("starblacklist.added", tag));
        return true;
    }
}
