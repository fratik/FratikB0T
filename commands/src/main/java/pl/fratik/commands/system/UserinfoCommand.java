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
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.UserUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class UserinfoCommand extends Command {

    private final UserDao userDao;
    private final ShardManager shardManager;
    private final EventBus eventBus;

    public UserinfoCommand(UserDao userDao, ShardManager shardManager, EventBus eventBus) {
        this.userDao = userDao;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        name = "userinfo";
        category = CommandCategory.SYSTEM;
        uzycie = new Uzycie("osoba", "user");
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User osoba = null;
        Member member;
        if (context.getArgs().length != 0) osoba = (User) context.getArgs()[0];
        if (osoba == null) osoba = context.getSender();
        member = context.getGuild().getMember(osoba);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(context.getTranslated("userinfo.name", UserUtil.formatDiscrim(osoba)));
        eb.addField(context.getTranslated("userinfo.id"), osoba.getId(), true);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", context.getLanguage().getLocale());
        sdf.setTimeZone(UserUtil.getTimeZone(context.getSender(), userDao));
        eb.addField(context.getTranslated("userinfo.created"),
                sdf.format(new Date(osoba.getTimeCreated().toInstant().toEpochMilli())), true);
        if (member != null) eb.addField(context.getTranslated("userinfo.joinedat"),
                sdf.format(new Date(member.getTimeJoined().toInstant().toEpochMilli())), true);
        else eb.addField(context.getTranslated("userinfo.joinedat"),
                context.getTranslated("userinfo.joinedat.nodata"), true);
        if (member != null) {
            eb.addField(context.getTranslated("userinfo.status"),
                    formatStatus(member.getOnlineStatus()), true);
            eb.addField(context.getTranslated("userinfo.playing"),
                    member.getActivities().isEmpty() ? context.getTranslated("userinfo.playing.not.playing")
                            : member.getActivities().get(0).getName(), true);
        }
        PluginMessageEvent event = new PluginMessageEvent("commands", "punkty", "punktyDao-getPunkty:"
                + osoba.getId());
        eventBus.post(event);
        awaitPluginResponse(event);
        Integer punkty = (Integer) event.getResponse();
        Integer pozycja = null;
        int pozycjaTmp = 0;
        PluginMessageEvent event2 = new PluginMessageEvent("commands", "punkty",
                "punktyDao-getAllUserPunkty:");
        eventBus.post(event2);
        awaitPluginResponse(event2);
        if (event2.getResponse() != null) {
            //noinspection unchecked
            Map<String, Integer> userPoints = (Map<String, Integer>) event2.getResponse();
            for (String id : userPoints.keySet()) {
                pozycjaTmp++;
                if (id.equals(osoba.getId())) pozycja = pozycjaTmp;
            }
        }
        if (punkty != null)
            eb.addField(context.getTranslated("userinfo.points"), String.valueOf(punkty), true);
        else eb.addField(context.getTranslated("userinfo.points"),
                context.getTranslated("userinfo.points.errored"), true);
        if (pozycja != null)
            eb.addField(context.getTranslated("userinfo.place"), String.valueOf(pozycja.intValue()), true);
        else eb.addField(context.getTranslated("userinfo.place"), "???", true);
        eb.setThumbnail(osoba.getEffectiveAvatarUrl().replace(".webp", ".png") + "?size=2048");
        eb.setColor(UserUtil.getPrimColor(osoba));
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

    private String formatStatus(OnlineStatus status) {
        Emote online = shardManager.getEmoteById(Ustawienia.instance.emotki.online);
        Emote idle = shardManager.getEmoteById(Ustawienia.instance.emotki.idle);
        Emote dnd = shardManager.getEmoteById(Ustawienia.instance.emotki.dnd);
        Emote offline = shardManager.getEmoteById(Ustawienia.instance.emotki.offline);
        if (online == null || idle == null || dnd == null || offline == null) {
            // lol xd
            return "???";
        }
        switch (status) {
            case ONLINE:
                return online.getAsMention();
            case IDLE:
                return idle.getAsMention();
            case DO_NOT_DISTURB:
                return dnd.getAsMention();
            case OFFLINE:
                return offline.getAsMention();
            default:
                return "???";
        }
    }
}
