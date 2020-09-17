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

package pl.fratik.commands.narzedzia;

import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.core.webhook.WebhookManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static pl.fratik.core.util.CommonUtil.ID_REGEX;

public class CytujCommand extends Command {

    private final ShardManager shardManager;
    private final EventBus eventBus;
    private final WebhookManager webhookManager;
    private final GuildDao guildDao;

    private final ExecutorService executor;

    private static final String STRINGARGTYPE = "string";
    private static final Pattern MESSAGE_LINK_PATTERN =
            Pattern.compile(String.format("^https?://((ptb|canary)?\\.?(discordapp|discord)\\.com)" +
                    "/channels/(%s)/(%s)/(%s)/?$", ID_REGEX, ID_REGEX, ID_REGEX));

    public CytujCommand(ShardManager shardManager, EventBus eventBus, WebhookManager webhookManager, GuildDao guildDao) {
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.webhookManager = webhookManager;
        this.guildDao = guildDao;
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
        aliases = new String[] {"zacytuj", "cytat"};
        allowPermLevelChange = false;
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onUnregister() {
        executor.shutdown();
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        TextChannel kanal = null;
        Message msg;
        String msgID = (String) context.getArgs()[0];
        Matcher matcher = MESSAGE_LINK_PATTERN.matcher(msgID);
        if (matcher.find()) {
            try {
                TextChannel tc = shardManager.getTextChannelById(matcher.group(5));
                if (checkPerms(context, tc)) kanal = tc;
                if (kanal == null) {
                    context.send(context.getTranslated("cytuj.nochannel"));
                    return false;
                }
                msg = kanal.retrieveMessageById(matcher.group(6)).complete();
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
        } else {
            String[] splitted = msgID.split("-");
            if (splitted.length == 1) {
                try {
                    kanal = context.getTextChannel();
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
            } else {
                try {
                    TextChannel tc = shardManager.getTextChannelById(splitted[0]);
                    if (checkPerms(context, tc)) kanal = tc;
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
        }
        if (msg == null) {
            context.send(context.getTranslated("cytuj.invalid.message"));
            return false;
        }
        String tresc = context.getArgs().length > 1 ?
                Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                        .map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(uzycieDelim)) : null;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(msg.getAuthor()), null, msg.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(UserUtil.getPrimColor(msg.getAuthor()));
        if (msg.getContentRaw().isEmpty() && !msg.getAttachments().isEmpty()) {
            if (msg.getEmbeds().stream().noneMatch(e -> e.getType() == EmbedType.RICH))
                eb.setDescription(context.getTranslated("cytuj.empty.message"));
            else eb.setDescription(context.getTranslated("cytuj.empty.message.embed"));
        } else {
            eb.setDescription(msg.getContentRaw());
        }
        eb.setTimestamp(msg.getTimeCreated());
        eb.addField(context.getTranslated("cytuj.jump"), String.format("[\\[%s\\]](%s)",
                context.getTranslated("cytuj.jump.to"), msg.getJumpUrl()), false);
        String link = CommonUtil.getImageUrl(msg);
        if (link != null) eb.setImage(link);
        if (tresc == null || tresc.isEmpty()) {
            context.send(eb.build());
            if (!msg.getEmbeds().isEmpty()) {
                try {
                    context.send(msg.getEmbeds().get(0));
                } catch (IllegalArgumentException e) {
                    // nieprawidłowy embed
                }
            }
            return true;
        }
        tresc = Pattern.compile("[(http(s)?)://(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9" +
                "@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(tresc).replaceAll("[URL]");
        EnumSet<Message.MentionType> alments = MessageAction.getDefaultMentions();
        if (context.getMember().hasPermission(context.getTextChannel(), Permission.MESSAGE_MENTION_EVERYONE)) {
            alments.add(Message.MentionType.EVERYONE);
            alments.add(Message.MentionType.HERE);
        }
        try {
            eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" + context.getMessage().getId()));
            context.getMessage().delete().queue();
        } catch (Exception ignored) {/*lul*/}
        if (webhookManager.hasWebhook(context.getTextChannel())) {
            List<WebhookEmbed> embeds = new ArrayList<>();
            //#region FIXME: wywalić jak https://github.com/MinnDevelopment/discord-webhooks/pull/14 zostanie wprowadzone
            MessageEmbed emb = eb.build();
            WebhookEmbedBuilder web = WebhookEmbedBuilder.fromJDA(eb.build());
            if (emb.getImage() != null) web.setImageUrl(emb.getImage().getUrl());
            embeds.add(web.build());
            //#endregion
            //skrócony kod: embeds.add(WebhookEmbedBuilder.fromJDA(eb.build()).build());
            if (!msg.getEmbeds().isEmpty()) {
                for (int i = 0; i < msg.getEmbeds().size(); i++) {
                    MessageEmbed embed = msg.getEmbeds().get(i);
                    if (embed.getType() != EmbedType.RICH) continue;
                    embeds.add(WebhookEmbedBuilder.fromJDA(embed).build());
                    if (embeds.size() >= 3) break;
                }
            }
            AllowedMentions allments = AllowedMentions.none();
            for (Message.MentionType alment : alments) {
                switch (alment) {
                    case ROLE:
                        allments.withParseRoles(true);
                        break;
                    case USER:
                        allments.withParseUsers(true);
                        break;
                    case EVERYONE:
                    case HERE:
                        allments.withParseEveryone(true);
                        break;
                }
            }
            webhookManager.send(new WebhookMessageBuilder().setAvatarUrl(context.getSender().getEffectiveAvatarUrl())
                    .setUsername(context.getMember().getEffectiveName()).setContent(tresc).setAllowedMentions(allments)
                    .addEmbeds(embeds).build(), context.getTextChannel());
            return true;
        } else {
            executor.submit(() -> {
                webhookManager.getWebhook(context.getTextChannel()); //tworzenie webhooka, na przyszłość - póki co wyślij normalnie
            });
        }
        context.getTextChannel().sendMessage(eb.build()).allowedMentions(alments)
                .content("**" + UserUtil.formatDiscrim(context.getSender()) + "**: " + tresc).queue();
        if (!msg.getEmbeds().isEmpty()) {
            context.send(msg.getEmbeds().get(0));
        }
        return true;
    }

    private boolean checkPerms(@NotNull CommandContext context, TextChannel tc) {
        return tc != null && (tc.equals(context.getTextChannel()) || (
                (tc.getGuild().equals(context.getGuild()) && // właściciel, jeśli poza serwerem
                        UserUtil.getPermlevel(context.getMember(), guildDao, shardManager, PermLevel.OWNER)
                                .getNum() >= PermLevel.ADMIN.getNum()) || // min. admin na serwerze
                        (!tc.getGuild().equals(context.getGuild()) &&
                                tc.getGuild().getOwnerId().equals(context.getSender().getId()))));
    }
}
