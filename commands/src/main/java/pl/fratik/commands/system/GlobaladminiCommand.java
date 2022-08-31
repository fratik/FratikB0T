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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;

import javax.annotation.Nonnull;
import java.util.*;

public class GlobaladminiCommand extends NewCommand {
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public GlobaladminiCommand(EventWaiter eventWaiter, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "globaladmini";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na fdev");
        InteractionHook hook = context.defer(false);
        Map<User, Status> map = new HashMap<>();
        @Nonnull Guild fdev = Objects.requireNonNull(context.getShardManager().getGuildById(Ustawienia.instance.botGuild));
        for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.gadmRole))) map.put(member.getUser(), Status.GLOBALADMIN);
        for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.zgaRole))) map.put(member.getUser(), Status.ZGA);
        for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.devRole))) map.put(member.getUser(), Status.DEV);
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
                .create(hook);
    }

    private String escapeMarkdown(String string) {
        return StringUtil.escapeMarkdown(string);
    }


    private enum Status {
        GLOBALADMIN, ZGA, DEV
    }
}
