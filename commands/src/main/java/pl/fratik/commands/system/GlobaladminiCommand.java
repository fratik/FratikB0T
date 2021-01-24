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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;

import javax.annotation.Nonnull;
import java.util.*;

public class GlobaladminiCommand extends Command {
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public GlobaladminiCommand(EventWaiter eventWaiter, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "globaladmini";
        category = CommandCategory.SYSTEM;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
        permissions.add(Permission.MESSAGE_MANAGE); // TODO: 22.02.19 wersja bez tych permow, wersja w DMach
        aliases = new String[] {"ga", "globaladmin"};
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na fdev");
        Message msg = context.reply(context.getTranslated("generic.loading"));
        Map<User, Status> map = new HashMap<>();
        @Nonnull Guild lnodev = Objects.requireNonNull(context.getShardManager().getGuildById(Ustawienia.instance.botGuild));
        for (Member member : lnodev.getMembersWithRoles(lnodev.getRoleById(Ustawienia.instance.gadmRole))) map.put(member.getUser(), Status.GLOBALADMIN);
        for (Member member : lnodev.getMembersWithRoles(lnodev.getRoleById(Ustawienia.instance.zgaRole))) map.put(member.getUser(), Status.ZGA);
        for (Member member : lnodev.getMembersWithRoles(lnodev.getRoleById(Ustawienia.instance.devRole))) map.put(member.getUser(), Status.DEV);
        EmbedBuilder ebGa = context.getBaseEmbed(null, null);
        EmbedBuilder ebZga = context.getBaseEmbed(null, null);
        EmbedBuilder ebDev = context.getBaseEmbed(null, null);
        ebGa.setAuthor(context.getTranslated("globaladmini.gadmin"));
        ebZga.setAuthor(context.getTranslated("globaladmini.zga"));
        ebDev.setAuthor(context.getTranslated("globaladmini.dev"));
        for (User user : map.keySet()) { //NOSONAR
            switch (map.get(user)) {
                case GLOBALADMIN:
                    StringBuilder sb = ebGa.getDescriptionBuilder();
                    sb.append(escapeMarkdown(UserUtil.formatDiscrim(user))).append("\n");
                    break;
                case ZGA:
                    StringBuilder sb2 = ebZga.getDescriptionBuilder();
                    sb2.append(escapeMarkdown(UserUtil.formatDiscrim(user))).append("\n");
                    break;
                case DEV:
                    StringBuilder sb3 = ebDev.getDescriptionBuilder();
                    sb3.append(escapeMarkdown(UserUtil.formatDiscrim(user))).append("\n");
                    break;
            }
        }
        List<EmbedBuilder> pages = new ArrayList<>();
        Collections.addAll(pages, ebGa, ebZga, ebDev);
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus)
                .create(msg);
        return true;
    }

    private String escapeMarkdown(String string) {
        return StringUtil.escapeMarkdown(string);
    }


    private enum Status {
        GLOBALADMIN, ZGA, DEV
    }
}
