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
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.core.webhook.WebhookManager;
import pl.fratik.moderation.entity.LogMessage;

import java.io.IOException;
import java.time.OffsetDateTime;
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
    private final UserDao userDao;
    private final Tlumaczenia tlumaczenia;

    private final ExecutorService executor;
    private final Cache<GuildConfig> gcCache;
    private final Cache<UserConfig> ucCache;
    private final Cache<List<LogMessage>> lmCache;

    private static final String STRINGARGTYPE = "string";
    private static final Pattern MESSAGE_LINK_PATTERN =
            Pattern.compile(String.format("^https?://((ptb|canary)?\\.?(discordapp|discord)\\.com)" +
                    "/channels/(%s)/(%s)/(%s)/?$", ID_REGEX, ID_REGEX, ID_REGEX));
    private static final Pattern URL_PATTERN = Pattern.compile("[(http(s)?)://(www\\.)?a-zA-Z0-9@:-]{2,256}\\." +
            "[a-z]{2,24}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern CYTUJ_PATTERN_1 = Pattern.compile(String.format("^Replying to( (<@!?%s>) from)? %s",
            ID_REGEX, MESSAGE_LINK_PATTERN.toString().substring(1, MESSAGE_LINK_PATTERN.toString().length() - 1)));
    private static final Pattern CYTUJ_PATTERN_2 = Pattern.compile("^> (.*?)$", Pattern.MULTILINE);

    public CytujCommand(ShardManager shardManager,
                        EventBus eventBus,
                        WebhookManager webhookManager,
                        GuildDao guildDao,
                        UserDao userDao,
                        Tlumaczenia tlumaczenia,
                        RedisCacheManager rcm) {
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.webhookManager = webhookManager;
        this.guildDao = guildDao;
        this.userDao = userDao;
        this.tlumaczenia = tlumaczenia;
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
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
        ucCache = rcm.new CacheRetriever<UserConfig>(){}.getCache();
        Cache<List<LogMessage>> logMessageCache;
        try {
            logMessageCache = rcm.new CacheRetriever<List<LogMessage>>(){}.getCache();
        } catch (TypeNotPresentException | NoClassDefFoundError e) {
            logMessageCache = null;
        }
        lmCache = logMessageCache;
        eventBus.register(this);
    }

    @Override
    public void onUnregister() {
        executor.shutdown();
        try {
            eventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {}
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent e) {
        if (lmCache == null) return;
        if (!e.isFromGuild()) return;
        if (!(gcCache.get(e.getGuild().getId(), guildDao::get).isCytujFbot() ||
                ucCache.get(e.getAuthor().getId(), userDao::get).isCytujFbot())) return;
        Matcher m1 = CYTUJ_PATTERN_1.matcher(e.getMessage().getContentRaw());
        Matcher m2 = CYTUJ_PATTERN_2.matcher(e.getMessage().getContentRaw());
        Message msg = null;
        String cnt = null;
        int hits = 0;
        if (m1.find()) {
            try {
                String cid = m1.group(7);
                if (!cid.equals(e.getTextChannel().getId())) return;
                msg = Objects.requireNonNull(shardManager.getTextChannelById(cid)).retrieveMessageById(m1.group(8)).complete();
            } catch (Exception ignored) {}
            String userment = m1.group(2);
            if (userment != null) cnt = userment + " ";
            else cnt = "";
            cnt += m1.replaceFirst("").trim();
        } else {
            if (m2.find()) {
                if (m2.start() != 0) return;
                StringBuilder msgCntBld = new StringBuilder();
                String[] splat = e.getMessage().getContentRaw().split("\n");
                int lastI = 0;
                for (int i = 0; i < splat.length; i++) { // wykrywanie czy wszystkie spoilery jeden pod drugim
                    if (splat[i].startsWith("> ")) {
                        /*
                        jeżeli wiadomość będzie w stylu:
                        > cytat (i = 0)
                        > cytat (i = 1)
                        nie cytat (i = 2)
                        > cytat (i = 3)
                        wykonywanie zostanie przerwane - 1 (drugi cytat) + 1 == 3 zwróci false
                        */
                        if (!(lastI == 0 && i == 0)) {
                            if (lastI + 1 == i) lastI = i;
                            else return;
                        }
                    }
                }
                do {
                    msgCntBld.append(m2.group(1)).append("\n");
                } while (m2.find());
                msgCntBld.setLength(msgCntBld.length() - 1);
                String msgCnt = msgCntBld.toString();
                if (msgCnt.isEmpty()) return;
                cnt = m2.replaceAll("").trim();
                List<LogMessage> lista = lmCache.getIfPresent(e.getChannel().getId());
                if (lista == null) lista = Collections.emptyList();
                Collections.reverse(lista);
                for (LogMessage m : lista) {
                    if (m == null || m.getTimeCreated().isBefore(OffsetDateTime.now().minusMinutes(5))) continue;
                    if (m.getContentRaw().equals(msgCnt)) {
                        try {
                            if (hits == 0) {
                                msg = e.getChannel().retrieveMessageById(m.getId()).complete();
                                if (!msg.getContentRaw().equals(msgCnt)) msg = null;
                            }
                            hits++;
                        } catch (Exception er) {
                            msg = null;
                        }
                    }
                }
            }
        }
        if (msg == null) return;
        try {
            sendCytujMessage(msg, tlumaczenia, tlumaczenia.getLanguage(e.getMember()), e.getTextChannel(), cnt,
                    Objects.requireNonNull(e.getMember()).hasPermission(e.getTextChannel(),
                            Permission.MESSAGE_MENTION_EVERYONE), e.getMessage(), hits <= 1,
                    URL_PATTERN.matcher(cnt).find() || !e.getMessage().getAttachments().isEmpty(), e.getMessage().getAttachments());
        } catch (Exception ignored) {}
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
        sendCytujMessage(msg, context.getTlumaczenia(), context.getLanguage(), context.getTextChannel(), tresc,
                context.getMember().hasPermission(context.getTextChannel(), Permission.MESSAGE_MENTION_EVERYONE),
                context.getMessage(), true, false, null);
        return true;
    }

    public void sendCytujMessage(Message msg,
                                 Tlumaczenia t,
                                 Language l,
                                 TextChannel ch,
                                 String trescCytatu,
                                 boolean mentionEveryone,
                                 Message execMsg,
                                 boolean jumpTo,
                                 boolean webhookOnly,
                                 List<Message.Attachment> attachments) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(msg.getAuthor()), null, msg.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(UserUtil.getPrimColor(msg.getAuthor()));
        if (msg.getContentRaw().isEmpty() && !msg.getAttachments().isEmpty()) {
            if (msg.getEmbeds().stream().noneMatch(e -> e.getType() == EmbedType.RICH))
                eb.setDescription(t.get(l, "cytuj.empty.message"));
            else eb.setDescription(t.get(l, "cytuj.empty.message.embed"));
        } else {
            eb.setDescription(msg.getContentRaw());
        }
        eb.setTimestamp(msg.getTimeCreated());
        if (jumpTo) eb.addField(t.get(l, "cytuj.jump"), String.format("[\\[%s\\]](%s)",
                t.get(l, "cytuj.jump.to"), msg.getJumpUrl()), false);
        else eb.addField(t.get(l, "cytuj.auto"), t.get(l, "cytuj.auto.cnt"), false);
        String link = CommonUtil.getImageUrl(msg);
        if (link != null) eb.setImage(link);
        EnumSet<Message.MentionType> alments = MessageAction.getDefaultMentions();
        if (mentionEveryone) {
            alments.add(Message.MentionType.EVERYONE);
            alments.add(Message.MentionType.HERE);
        }
        if (webhookOnly && !webhookManager.hasWebhook(ch)) {
            webhookManager.getWebhook(ch);
            return;
        }
        if (webhookManager.hasWebhook(ch)) {
            List<WebhookEmbed> embeds = new ArrayList<>();
            embeds.add(WebhookEmbedBuilder.fromJDA(eb.build()).build());
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
            Member member = execMsg.getMember();
            if (member == null) throw new NullPointerException("jak do tego doszło nie wiem");
            WebhookMessageBuilder wmb = new WebhookMessageBuilder()
                    .setAvatarUrl(execMsg.getAuthor().getEffectiveAvatarUrl()).setUsername(member.getEffectiveName());
            if (attachments != null && !attachments.isEmpty()) {
                for (Message.Attachment attachment : attachments) {
                    try {
                        wmb.addFile(attachment.getFileName(), NetworkUtil.download(attachment.getUrl()));
                    } catch (IOException e) {
                        return; // w razie błędu, po prostu nie wysyłaj
                    }
                }
            }
            if (trescCytatu != null && !trescCytatu.isEmpty()) wmb.setContent(trescCytatu);
            // wyślij webhookiem - w razie nie wysłania (brak permów), wyślij tradycyjną metodą
            if (webhookManager.send(wmb.setAllowedMentions(allments).addEmbeds(embeds).build(), ch) != null) {
                try {
                    eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" + execMsg.getId()));
                    execMsg.delete().queue(null, e -> {});
                } catch (Exception ignored) {}
                return;
            }
        } else {
            executor.submit(() -> {
                webhookManager.getWebhook(ch); //tworzenie webhooka, na przyszłość - póki co wyślij normalnie
            });
        }
        if (webhookOnly) return; // jeżeli tylko webhook, a doszliśmy tu to anuluj
        MessageAction ma = ch.sendMessage(eb.build()).allowedMentions(alments);
        if (trescCytatu != null && !trescCytatu.isEmpty()) {
            trescCytatu = URL_PATTERN.matcher(trescCytatu).replaceAll("[URL]");
            ma = ma.content("**" + UserUtil.formatDiscrim(execMsg.getAuthor()) + "**: " + trescCytatu);
        }
        ma.queue();
        if (!msg.getEmbeds().isEmpty()) {
            ch.sendMessage(msg.getEmbeds().get(0)).complete();
        }
        try {
            if ((trescCytatu != null && !trescCytatu.isEmpty()) || webhookManager.hasWebhook(ch)) {
                eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" + execMsg.getId()));
                execMsg.delete().queue(null, e -> {});
            }
        } catch (Exception ignored) {/*lul*/}
    }

    private boolean checkPerms(@NotNull CommandContext context, TextChannel tc) {
        return checkPerms(context.getTextChannel(), context.getGuild(), context.getMember(), tc);
    }

    private boolean checkPerms(TextChannel ctxTc, Guild guild, Member member, TextChannel tc) {
        return tc != null && (tc.equals(ctxTc) || (
                (tc.getGuild().equals(guild) && // właściciel, jeśli poza serwerem
                        UserUtil.getPermlevel(member, guildDao, shardManager, PermLevel.OWNER)
                                .getNum() >= PermLevel.ADMIN.getNum()) || // min. admin na serwerze
                        (!tc.getGuild().equals(guild) &&
                                tc.getGuild().getOwnerId().equals(member.getId()))));
    }
}
