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
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Blacklist;
import pl.fratik.commands.entity.BlacklistDao;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;
import pl.fratik.core.util.UserUtil;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.awt.Color.decode;

public class PopCommand extends NewCommand {
    private static final String BUTTON_CLOSE = "CLOSE_POP_REQUEST";
    private final ShardManager shardManager;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final Tlumaczenia tlumaczenia;
    private final BlacklistDao blacklistDao;
    private final Cache<Blacklist> blacklistCache;
    boolean bypass = false;

    public PopCommand(ShardManager shardManager,
                      EventWaiter eventWaiter,
                      EventBus eventBus,
                      Tlumaczenia tlumaczenia,
                      BlacklistDao blacklistDao,
                      RedisCacheManager rcm) {
        this.shardManager = shardManager;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.tlumaczenia = tlumaczenia;
        this.blacklistDao = blacklistDao;
        blacklistCache = rcm.new CacheRetriever<Blacklist>(){}.getCache();
        name = "pop";
        cooldown = 10; // TODO: 22.02.19 pomoc 2.0
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!context.getGuild().getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE)) {
            context.replyEphemeral(context.getTranslated("pop.no.perms"));
            return false;
        }
        if (context.getChannel().getType() != ChannelType.TEXT) {
            context.replyEphemeral(context.getTranslated("pop.only.text"));
            return false;
        }
        return true;
    }

    @Override
    public void onRegister() {
        eventBus.register(this);
    }

    @Override
    public void onUnregister() {
        try {eventBus.unregister(this);} catch (Exception ignored) {/*lul*/}
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na fdev");
        context.defer(false);
        Blacklist ubl = blacklistCache.get(context.getSender().getId(), blacklistDao::get);
        if (ubl.isBlacklisted()) {
            String tag = context.getShardManager().retrieveUserById(ubl.getExecutor()).complete().getAsTag();
            context.sendMessage(context.getTranslated("pop.user.blacklisted", tag, ubl.getReason(), tag));
            return;
        }
        Blacklist sbl = blacklistCache.get(context.getGuild().getId(), blacklistDao::get);
        if (sbl.isBlacklisted()) {
            String tag = context.getShardManager().retrieveUserById(sbl.getExecutor()).complete().getAsTag();
            context.sendMessage(context.getTranslated("pop.server.blacklisted", tag, sbl.getReason(), tag));
            return;
        }
        if (context.getGuild().getTimeCreated().toInstant().toEpochMilli() - Instant.now().toEpochMilli() > -1209600000
                && !bypass) {
            context.sendMessage(context.getTranslated("pop.server.young"));
            return;
        }
        if (!context.getGuild().getRolesByName(context.getTranslated("pop.role.name"), false).isEmpty()) {
            context.sendMessage(context.getTranslated("pop.inprogress"));
            return;
        }
        if (CommonUtil.isPomoc(shardManager, context.getGuild())) {
            context.sendMessage(context.getTranslated("pop.pomoc.isset"));
            if (UserUtil.isStaff(context.getMember(), shardManager)) {
                TextChannel kanal = Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild))
                        .getTextChannelById(Ustawienia.instance.popChannel);
                if (kanal == null) throw new NullPointerException("nieprawidłowy popChannel");
                List<Message> wiads = kanal.getHistory().retrievePast(50).complete();
                for (Message mess : wiads) {
                    if (mess.getEmbeds().isEmpty()) continue;
                    //noinspection ConstantConditions
                    String id = mess.getEmbeds().get(0).getFooter().getText().split(" \\| ")[1];
                    if (id.equals(context.getGuild().getId())) {
                        context.sendMessage(mess.getEmbeds().get(0));
                        return;
                    }
                }
            }
            return;
        }
        context.sendMessage(context.getTranslated("pop.start"));
        MessageWaiter mw = new MessageWaiter(eventWaiter, context) {
            @Override
            public void create() {
                eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                        this::handleMessage, 300, TimeUnit.SECONDS, this::onTimeout);
            }
        };
        mw.setMessageHandler(e -> {
            if (e.getMessage().getContentRaw().trim().equalsIgnoreCase(context.getTranslated("pop.abort"))) {
                context.reply(context.getTranslated("pop.aborted"));
                return;
            }
            if (e.getMessage().getContentRaw().length() < 15) {
                context.reply(context.getTranslated("pop.min.length"));
                return;
            }
            if (e.getMessage().getContentRaw().length() >= 1000) {
                context.reply(context.getTranslated("pop.max.length"));
                return;
            }
            Role role = context.getGuild().createRole().setColor(decode("#f11515"))
                    .setName(context.getTranslated("pop.role.name")).setMentionable(false).complete();
            context.getChannel().asGuildMessageChannel().getPermissionContainer().getManager().putPermissionOverride(role,
                    EnumSet.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL), Set.of()).complete();
            Invite invite = context.getChannel().asTextChannel().createInvite().setMaxAge(86400).setMaxUses(15)
                    .reason(context.getTranslated("pop.invite.reason")).complete();
            //skonwertowane z js
            String permissions = context.getMember().hasPermission(Permission.ADMINISTRATOR) ?
                    "[ADMIN]" : context.getMember().getPermissions().stream().map(Permission::getName)
                    .collect(Collectors.joining(", "));
            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(context.getSender().getAsTag())
                    .setFooter("Prośba pomocy! | " + context.getGuild().getId(),
                            context.getGuild().getJDA().getSelfUser().getEffectiveAvatarUrl()
                                    .replace(".webp", ".png") + "?size=128")
                    .addField("Treść prośby", e.getMessage().getContentRaw(), false)
                    .addField("Uprawnienia", permissions, false)
                    .addField("Zaproszenie na serwer", "zostało wysłane w tej wiadomości.", false)
                    .addField("Aby zamknąć wiadomość, zareaguj 🗑.", "Pomoc zostanie uznana za gotową.",
                            false);
            Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
            if (fdev == null) throw new IllegalStateException("bot nie na fdev");
            Role popRole = fdev.getRoleById(Ustawienia.instance.popRole);
            if (popRole == null) throw new IllegalStateException("nie ma popRoli/nieprawidłowa");
            popRole.getManager().setMentionable(true).complete();
            TextChannel ch = fdev.getTextChannelById(Ustawienia.instance.popChannel);
            if (ch == null) throw new IllegalStateException("nie ma popChannel/nieprawidłowy");
            Message msg = ch.sendMessage("<@&" + Ustawienia.instance.popRole + ">\nhttp://discord.gg/" +
                            invite.getCode()).setEmbeds(eb.build())
                    .setActionRow(Button.danger(BUTTON_CLOSE, "Zamknij prośbę"))
                    .mentionRoles(Ustawienia.instance.popRole).complete();
            popRole.getManager().setMentionable(false).complete();
            context.sendMessage(context.getTranslated("pop.success"));
            TextChannel poplch = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
            if (poplch == null) throw new IllegalStateException("nie ma popLogChannel/nieprawidłowy");
            poplch.sendMessage(String.format("%s(%s) wysłał prośbę o pomoc dla serwera %s[%s]\nTreść pomocy to: `%s`." +
                            "\nJego uprawnienia to %s.",
                    UserUtil.formatDiscrim(context.getMember()),
                    context.getSender().getId(), context.getGuild().getName(), context.getGuild().getId(),
                    e.getMessage().getContentRaw(), permissions)).complete();
        });
        mw.setTimeoutHandler(() -> context.sendMessage(context.getTranslated("pop.aborted")));
        mw.create();
    }

    @Subscribe
    public void onGuildBanEvent(GuildBanEvent e) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        TextChannel logi = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
        if (logi == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        if (!UserUtil.isGadm(e.getUser(), shardManager)) return;
        if (!CommonUtil.isPomoc(shardManager, e.getGuild())) return;
        logi.sendMessage((String.format("%s(%s) dostał bana na serwerze %s[%s]. Czy nie należy się gban?",
                UserUtil.formatDiscrim(e.getUser()), e.getUser().getId(),
                e.getGuild().getName(), e.getGuild().getId()))).queue();
    }

    @Subscribe
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        if (!Globals.inFratikDev) return;
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        TextChannel logi = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
        if (logi == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        if (!UserUtil.isStaff(e.getMember(), shardManager)) return;
        Role rola = null;
        for (Language lang : Language.values()) {
            if (lang == Language.DEFAULT) continue;
            if (e.getGuild().getRoles().stream().map(Role::getName).collect(Collectors.toList())
                    .contains(tlumaczenia.get(lang, "pop.role.name"))) {
                rola = e.getGuild().getRoles().stream().filter(r -> r.getName()
                        .equals(tlumaczenia.get(lang, "pop.role.name"))).findFirst().orElse(null);
                break;
            }
        }
        if (rola == null) return;
        e.getGuild().addRoleToMember(e.getMember(), rola).queue();
        logi.sendMessage((String.format("%s(%s) dołączył na" + " serwer %s[%s]", UserUtil.formatDiscrim(e.getMember()),
                e.getUser().getId(), e.getGuild().getName(), e.getGuild().getId()))).queue();

    }

    @Subscribe
    public void onReactionAdd(ButtonInteractionEvent e) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        TextChannel logi = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
        if (logi == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        if (e.getChannel().getId().equals(Ustawienia.instance.popChannel) && e.getComponentId().equals(BUTTON_CLOSE)) {
            e.deferEdit().queue();
            Message msg;
            try {
                msg = e.getChannel().retrieveMessageById(e.getMessageIdLong()).complete();
            } catch (Exception er) {
                //wiadomosci nie ma
                return;
            }
            User user = e.getUser();
            //noinspection ConstantConditions
            String id = msg.getEmbeds().get(0).getFooter().getText().split(" \\| ")[1];
            Guild g = shardManager.getGuildById(id);
            try {
                msg.delete().complete();
                logi.sendMessage((String.format("%s(%s) zamknął" + " prośbę o pomoc serwera %s[%s]",
                        UserUtil.formatDiscrim(user), user.getId(),
                        g != null ? g.getName() : "[bot nie na serwerze]", id))).queue();
            } catch (Exception ignored) {/*lul*/}
            if (g == null) return;
            Role rola = null;
            for (Language lang : Language.values()) {
                if (g.getRoles().stream().map(Role::getName).collect(Collectors.toList())
                        .contains(tlumaczenia.get(lang, "pop.role.name"))) {
                    rola = g.getRoles().stream().filter(r -> r.getName()
                            .equals(tlumaczenia.get(lang, "pop.role.name"))).findFirst().orElse(null);
                    break;
                }
            }
            if (rola == null) return;
            rola.delete().queue();
        }
    }
}
