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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;

import java.util.Objects;

public class PowiadomOPomocyCommand extends NewCommand {
    private final ShardManager shardManager;

    public PowiadomOPomocyCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
        name = "powiadomopomocy";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!UserUtil.isStaff(context.getSender(), context.getShardManager())) {
            context.replyEphemeral("nunu");
            return false;
        }
        return true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        Role rola = Objects.requireNonNull(fdev).getRoleById(Ustawienia.instance.popRole);
        Member mem = fdev.getMember(context.getSender());
        context.deferAsync(false);
        if (Objects.requireNonNull(mem).getRoles().contains(rola)) {
            fdev.removeRoleFromMember(mem, Objects.requireNonNull(rola)).complete();
            context.reply(context.getTranslated("powiadomopomocy.success.removed"));
            return;
        }
        fdev.addRoleToMember(mem, Objects.requireNonNull(rola)).complete();
        context.reply(context.getTranslated("powiadomopomocy.success"));
    }
}
