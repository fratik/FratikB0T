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

package pl.fratik.invite.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;
import pl.fratik.invite.entity.InviteData;

import java.time.Instant;

public class InvitesCommand extends SharedInvitesCommand {

    public InvitesCommand(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, EventWaiter eventWaiter, EventBus eventBus) {
        super(inviteDao, invitesCache, guildDao, eventWaiter, eventBus);
        name = "zaproszenia";
        cooldown = 5;
    }

    @SubCommand(name = "info", usage = "[osoba:user]")
    public boolean info(@NotNull NewCommandContext context) {
        if (!checkEnabled(context)) return false;
        User osoba = context.getArgumentOr("osoba", context.getSender(), OptionMapping::getAsUser);
        context.deferAsync(false);
        InviteData dao = inviteDao.get(osoba.getId(), context.getGuild().getId());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(osoba));
        eb.setThumbnail(UserUtil.getAvatarUrl(osoba));
        eb.setTitle(UserUtil.formatDiscrim(osoba));
        eb.setTimestamp(Instant.now());
        if (!context.getGuild().getSelfMember().hasPermission(Permission.MANAGE_SERVER)) {
            eb.setDescription(context.getTranslated("invites.maybe.doesnt.work"));
        }
        eb.addField(context.getTranslated("invites.stats"),
                context.getTranslated("invites.fieldvalue", dao.getTotalInvites() - dao.getLeaveInvites(),
                        dao.getLeaveInvites(), dao.getTotalInvites()
                ), false);

        context.sendMessage(eb.build());
        return true;
    }
}
