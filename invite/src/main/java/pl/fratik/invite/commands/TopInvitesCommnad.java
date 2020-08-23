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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.entity.InviteConfig;
import pl.fratik.invite.entity.InviteDao;

import java.util.*;

public class TopInvitesCommnad extends Command {

    private InviteDao inviteDao;
    private EventWaiter eventWaiter;
    private EventBus eventBus;

    public TopInvitesCommnad(InviteDao inviteDao, EventWaiter eventWaiter, EventBus eventBus) {
        name = "topinvites";
        aliases = new String[] {"topinvite", "topinv", "topinvs"};
        category = CommandCategory.INVITES;
        cooldown = 10;
        this.inviteDao = inviteDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
    }

    @Override
    public boolean execute(CommandContext context) {
        Task<List<Member>> task = context.getGuild().loadMembers();
        List<Member> members = new ArrayList<>();
        task.onSuccess(members::addAll);

        Message msg = context.send(context.getTranslated("generic.loading"));

        int sec = 0;
        while (!members.isEmpty()) {
            try {
                sec++;
                Thread.sleep(1000);
                if (sec == 10) {
                    msg.editMessage(context.getTranslated("topinvites.error")).queue();
                    return false;
                }
            } catch (InterruptedException ignored) { }
        }

        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();
        List<EmbedBuilder> pages = new ArrayList<>();
        HashMap<Member, Integer> zaproszenia = new HashMap<>();

        for (Member member : members) {
            InviteConfig ic = inviteDao.get(member);
            int invites = ic.getTotalInvites() - ic.getLeaveInvites();
            if (invites <= 0) continue;
            zaproszenia.put(member, invites);
        }

        int rank = 1;
        int tempRank = 1;
        for (Map.Entry<Member, Integer> sorted : sortByValue(zaproszenia).entrySet()) {
            sb.append("**#").append(rank).append("** ");
            sb.append(sorted.getKey().getAsMention());
            sb.append(" [`").append(UserUtil.formatDiscrim(sorted.getKey())).append("`] ");
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

    public static HashMap<Member, Integer> sortByValue(HashMap<Member, Integer> hm) {
        List<Map.Entry<Member, Integer> > list =
                new LinkedList<>(hm.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        HashMap<Member, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<Member, Integer> aa : list) { temp.put(aa.getKey(), aa.getValue()); }
        return temp;
    }

}
