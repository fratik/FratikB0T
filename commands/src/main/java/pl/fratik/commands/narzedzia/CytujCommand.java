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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.UserUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CytujCommand extends Command {

    private final ShardManager shardManager;
    private final EventBus eventBus;

    private static final String STRINGARGTYPE = "string";

    public CytujCommand(ShardManager shardManager, EventBus eventBus) {
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        name = "cytuj";
        category = CommandCategory.UTILITY;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_HISTORY);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        uzycieDelim = " ";
        hmap.put("wiadomosc", STRINGARGTYPE);
        hmap.put("tekst", STRINGARGTYPE);
        hmap.put("[...]", STRINGARGTYPE);
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"zacytuj", "cytat", "ktośkiedyśnapisał..."};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        TextChannel kanal = null;
        Message msg;
        String[] splitted = ((String) context.getArgs()[0]).split("-");
        if (splitted.length == 1) {
            try {
                kanal = context.getChannel();
                msg = kanal.retrieveMessageById(splitted[0]).complete();
            } catch (IllegalArgumentException e) {
                context.send(context.getTranslated("cytuj.invalid.id"));
                return false;
            } catch (PermissionException e) {
                context.send(context.getTranslated("cytuj.target.noperms"));
                return false;
            } catch (ErrorResponseException e) {
                context.send(context.getTranslated("cytuj.invalid.message"));
                return false;
            }
        }
        else {
            try {
                TextChannel tc = shardManager.getTextChannelById(splitted[0]);
                if (tc != null && tc.getGuild().getOwnerId().equals(context.getSender().getId())) {
                    kanal = tc;
                }
                if (kanal == null) {
                    context.send(context.getTranslated("cytuj.nochannel"));
                    return false;
                }
                msg = kanal.retrieveMessageById(splitted[1]).complete();
            } catch (IllegalArgumentException e) {
                context.send(context.getTranslated("cytuj.invalid.id"));
                return false;
            } catch (PermissionException e) {
                context.send(context.getTranslated("cytuj.target.noperms"));
                return false;
            } catch (ErrorResponseException e) {
                context.send(context.getTranslated("cytuj.invalid.message"));
                return false;
            }
        }
        if (msg == null) {
            context.send(context.getTranslated("cytuj.invalid.message"));
            return false;
        }
        String tresc = context.getArgs().length > 1 ?
                Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                        .map(Object::toString).collect(Collectors.joining(uzycieDelim)) : null;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(msg.getAuthor()), null, msg.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(UserUtil.getPrimColor(msg.getAuthor()));
        if (msg.getContentRaw().isEmpty()) {
            if (msg.getEmbeds().isEmpty()) eb.setDescription(context.getTranslated("cytuj.empty.message"));
            else eb.setDescription(context.getTranslated("cytuj.empty.message.embed"));
        } else {
            eb.setDescription(msg.getContentRaw());
        }
        eb.setTimestamp(msg.getTimeCreated());
        if (!msg.getAttachments().isEmpty()) eb.setImage(msg.getAttachments().get(0).getUrl());
        if (tresc == null || tresc.isEmpty()) {
            context.send(eb.build());
            if (!msg.getEmbeds().isEmpty()) {
                context.send(msg.getEmbeds().get(0));
            }
            return true;
        }
        Matcher matcher = Pattern.compile("[(http(s)?)://(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6" +
                "}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(tresc);
        tresc = matcher.replaceAll("[URL]");
        try {
            eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" + context.getMessage().getId()));
            context.getMessage().delete().queue();
        } catch (Exception ignored) {/*lul*/}
        context.getChannel().sendMessage(eb.build()).content("**" + UserUtil.formatDiscrim(context.getSender())
                + "**: " + tresc).queue();
        if (!msg.getEmbeds().isEmpty()) {
            context.send(msg.getEmbeds().get(0));
        }
        return true;
    }
}
