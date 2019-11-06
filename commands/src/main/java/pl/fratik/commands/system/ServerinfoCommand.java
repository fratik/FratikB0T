/*
 * Copyright (C) 2019 FratikB0T Contributors
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
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.UserUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ServerinfoCommand extends Command {

    private final UserDao userDao;
    private final EventBus eventBus;

    public ServerinfoCommand(UserDao userDao, EventBus eventBus) {
        this.userDao = userDao;
        this.eventBus = eventBus;
        name = "serverinfo";
        category = CommandCategory.SYSTEM;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        aliases = new String[] {"serwerinfo"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(context.getTranslated("serverinfo.name", context.getGuild().getName()));
        eb.addField(context.getTranslated("serverinfo.id"), context.getGuild().getId(), true);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", context.getLanguage().getLocale());
        sdf.setTimeZone(UserUtil.getTimeZone(context.getSender(), userDao));
        eb.addField(context.getTranslated("serverinfo.created"),
                sdf.format(new Date(context.getGuild().getTimeCreated().toInstant().toEpochMilli())), true);
        Member ow = context.getGuild().getOwner();
        if (ow != null) eb.addField(context.getTranslated("serverinfo.owner"),
                UserUtil.formatDiscrim(ow), true);
        else eb.addField(context.getTranslated("serverinfo.owner"),
                context.getTranslated("serverinfo.no.owner"), false);
        PluginMessageEvent event = new PluginMessageEvent("commands", "punkty", "punktyDao-getPunkty:"
                + context.getGuild().getId());
        PluginMessageEvent event2 = new PluginMessageEvent("commands", "punkty",
                "punktyDao-getAllGuildPunkty:");
        eventBus.post(event);
        eventBus.post(event2);
        awaitPluginResponse(event);
        awaitPluginResponse(event2);
        Integer punkty = (Integer) event.getResponse();
        Integer pozycja = null;
        int pozycjaTmp = 0;
        if (event2.getResponse() != null) {
            //noinspection unchecked
            Map<String, Integer> guildPoints = (Map<String, Integer>) event2.getResponse();
            for (String id : guildPoints.keySet()) {
                pozycjaTmp++;
                if (id.equals(context.getGuild().getId())) pozycja = pozycjaTmp;
            }
        }
        if (punkty != null)
            eb.addField(context.getTranslated("serverinfo.points"), String.valueOf(punkty), true);
        else eb.addField(context.getTranslated("serverinfo.points"),
                context.getTranslated("serverinfo.points.errored"), true);
        if (pozycja != null)
            eb.addField(context.getTranslated("serverinfo.place"), String.valueOf(pozycja.intValue()), true);
        else eb.addField(context.getTranslated("serverinfo.place"), "???", true);
        eb.addField(context.getTranslated("serverinfo.members"),
                String.valueOf(context.getGuild().getMembers().size()), true);
        if (context.getGuild().getIconUrl() != null)
            eb.setThumbnail(context.getGuild().getIconUrl().replace(".webp", ".png") + "?size=2048");
        eb.setColor(GuildUtil.getPrimColor(context.getGuild()));
        context.send(eb.build());
        return true;
    }

    private void awaitPluginResponse(PluginMessageEvent event) {
        int waited = 0;
        while (event.getResponse() == null) {
            try {
                Thread.sleep(100);
                waited += 100;
                if (waited >= 3000) break;
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
