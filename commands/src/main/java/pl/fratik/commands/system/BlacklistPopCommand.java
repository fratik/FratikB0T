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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Blacklist;
import pl.fratik.commands.entity.BlacklistDao;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.CommonUtil;

public class BlacklistPopCommand extends NewCommand {

    private final BlacklistDao blacklistDao;

    public BlacklistPopCommand(BlacklistDao blacklistDao) {
        this.blacklistDao = blacklistDao;
        name = "blacklistpop";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
        usage = "<id:string> <powod:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String id = context.getArguments().get("id").getAsString();
        String powod = context.getArguments().get("powod").getAsString();
        User user = null;
        Guild server = null;
        try {
            user = context.getShardManager().retrieveUserById(id).complete();
            if (user == null) throw new NullPointerException("jebać pis");
        } catch (Exception e) {
            server = context.getShardManager().getGuildById(id);
        }
        if (user == null && server == null) {
            if (!CommonUtil.ID_REGEX.matcher(id).matches()) {
                context.reply(context.getTranslated("blacklistpop.invalid.id"));
                return;
            }
        }
        Blacklist xd = blacklistDao.get(id);
        if (xd.isBlacklisted()) {
            xd.setBlacklisted(false);
            xd.setReason(null);
            xd.setExecutor(null);
            context.reply(context.getTranslated("blacklistpop.success.removed"));
        } else {
            xd.setBlacklisted(true);
            xd.setReason(powod);
            xd.setExecutor(context.getSender().getId());
            context.reply(context.getTranslated("blacklistpop.success.added"));
        }
        blacklistDao.save(xd);
    }
}
