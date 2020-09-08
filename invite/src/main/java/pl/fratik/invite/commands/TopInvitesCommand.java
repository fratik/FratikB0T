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

package pl.fratik.invite.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MapUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;
import pl.fratik.invite.entity.InviteData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopInvitesCommand extends AbstractInvitesCommand {

    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public TopInvitesCommand(InviteDao inviteDao, InvitesCache invitesCache, EventWaiter eventWaiter, EventBus eventBus) {
        super(inviteDao, invitesCache);
        name = "topinvites";
        aliases = new String[] {"topinvite", "topinv", "topinvs"};
        category = CommandCategory.INVITES;
        cooldown = 10;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        allowPermLevelChange = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!checkEnabled(context)) return false;
        Message msg = context.send(context.getTranslated("generic.loading"));
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();
        List<EmbedBuilder> pages = new ArrayList<>();
        HashMap<User, Integer> zaproszenia = new HashMap<>();

        for (InviteData ic : inviteDao.getByGuild(context.getGuild())) {
            int invites = ic.getTotalInvites() - ic.getLeaveInvites();
            if (invites <= 0) continue;
            zaproszenia.put(context.getShardManager().retrieveUserById(ic.getUserId()).complete(), invites);
        }

        if (zaproszenia.isEmpty()) {
            context.send(context.getTranslated("topinvites.empty"));
            return false;
        }

        int rank = 1;
        int tempRank = 1;
        for (Map.Entry<User, Integer> sorted : MapUtil.sortByValueAsc(zaproszenia).entrySet()) {
            User mem = sorted.getKey();
            sb.append("**#").append(rank).append("** ");
            sb.append(mem.getAsMention());
            sb.append(" [`").append(UserUtil.formatDiscrim(mem)).append("`] ");
            sb.append(context.getTranslated("topinvites.invtes", sorted.getValue())).append("\n");
            rank++;
            tempRank++;
            if (tempRank > 10) {
                eb.setColor(UserUtil.getPrimColor(context.getSender()));
                eb.setDescription(sb.toString());
                pages.add(eb);
                sb = new StringBuilder();
                eb = new EmbedBuilder();
                tempRank = 0;
            }
            if (pages.size() >= 5) break;
        }
        if (rank <= 10) {
            eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
            eb.setDescription(sb.toString());
            pages.add(eb);
        }

        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus)
                .create(msg);
        return true;
    }

}
