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
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.SilentExecutionFail;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;
import pl.fratik.core.util.UserUtil;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.awt.Color.decode;

public class PopCommand extends Command {
    private final ShardManager shardManager;
    private final GuildDao guildDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final Tlumaczenia tlumaczenia;
    private boolean bypass = false;

    public PopCommand(ShardManager shardManager, GuildDao guildDao, EventWaiter eventWaiter, EventBus eventBus, Tlumaczenia tlumaczenia) {
        this.shardManager = shardManager;
        this.guildDao = guildDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.tlumaczenia = tlumaczenia;
        name = "pop";
        category = CommandCategory.SYSTEM;
        cooldown = 10; // TODO: 22.02.19 pomoc 2.0
        aliases = new String[] {"helpme", "support", "suport"};
        permissions.add(Permission.CREATE_INSTANT_INVITE);
        permissions.add(Permission.MANAGE_ROLES);
        permissions.add(Permission.MANAGE_CHANNEL);
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
    public boolean execute(@NotNull CommandContext context) {
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na fdev");
        if (context.getGuild().getTimeCreated().toInstant().toEpochMilli() - Instant.now().toEpochMilli() > -1209600000
                && !bypass) {
            context.send(context.getTranslated("pop.server.young"));
            return false;
        }
        if (!context.getGuild().getRolesByName(context.getTranslated("pop.role.name"), false).isEmpty()) {
            context.send(context.getTranslated("pop.inprogress"));
            return false;
        }
        if (isPomoc(context.getGuild())) {
            context.send(context.getTranslated("pop.pomoc.isset"));
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
                        context.send(mess.getEmbeds().get(0));
                        return false;
                    }
                }
            }
            return false;
        }
        PermLevel permLevel = UserUtil.getPermlevel(context.getMember(), guildDao, shardManager);
        context.send(context.getTranslated("pop.start"));
        MessageWaiter mw = new MessageWaiter(eventWaiter, context) {
            @Override
            public void create() {
                eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                        this::handleMessage, 300, TimeUnit.SECONDS, this::onTimeout);
            }
        };
        mw.setMessageHandler(e -> {
            if (e.getMessage().getContentRaw().length() >= 1000) {
                context.send(context.getTranslated("pop.max.length"));
                return;
            }
            if (e.getMessage().getContentRaw().trim().equalsIgnoreCase(context.getTranslated("pop.abort"))) {
                context.send(context.getTranslated("pop.aborted"));
                return;
            }
            Role role = context.getGuild().createRole().setColor(decode("#f11515"))
                    .setName(context.getTranslated("pop.role.name")).setMentionable(false).complete();
            context.getChannel().createPermissionOverride(role)
                    .setAllow(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL).complete();
            Invite invite = context.getChannel().createInvite().setMaxAge(86400).setMaxUses(5)
                    .reason(context.getTranslated("pop.invite.reason")).complete();
            //skonwertowane z js
            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(context.getSender().getAsTag())
                    .setFooter("Prośba pomocy! | " + context.getGuild().getId(),
                            context.getGuild().getJDA().getSelfUser().getEffectiveAvatarUrl()
                                    .replace(".webp", ".png") + "?size=128")
                    .addField("Treść prośby", e.getMessage().getContentRaw(), false)
                    .addField("Poziom uprawnień", String.valueOf(permLevel.getNum()), false)
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
            Message msg = ch
                    .sendMessage("<@&423855296415268865>\nhttp://discord.gg/" + invite.getCode()).embed(eb.build())
                    .complete();
            popRole.getManager().setMentionable(false).complete();
            msg.addReaction("\uD83D\uDDD1").queue();
            context.send(context.getTranslated("pop.success"));
            PermLevel lvl = UserUtil.getPermlevel(e.getMember(), guildDao, shardManager);
            TextChannel poplch = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
            if (poplch == null) throw new IllegalStateException("nie ma popLogChannel/nieprawidłowy");
            poplch.sendMessage(String.format("%s(%s) wysłał prośbę o pomoc dla serwera %s[%s]\nTreść pomocy to: `%s`." +
                            "\nJego uprawnienia to %s (%s)",
                    UserUtil.formatDiscrim(context.getMember()),
                    context.getSender().getId(), context.getGuild().getName(), context.getGuild().getId(),
                    e.getMessage().getContentRaw(), lvl.getNum(), context.getTranslated(lvl.getLanguageKey()))).complete();
        });
        mw.setTimeoutHandler(() -> context.send(context.getTranslated("pop.aborted")));
        mw.create();
        return true;
    }

    @Override
    public boolean preExecute(CommandContext context) {
        if (context.getRawArgs().length != 0) {
            String subcommand = context.getRawArgs()[0].toLowerCase();
            if (subcommand.equalsIgnoreCase("-h") || subcommand.equalsIgnoreCase("--help")) {
                CommonErrors.usage(context);
                return false;
            }
        }
        if (UserUtil.getPermlevel(context.getMember(), guildDao, shardManager).getNum() < PermLevel.MOD.getNum()) {
            context.send(context.getTranslated("pop.no.perms", context.getPrefix()));
            return false;
        }
        return super.preExecute(context);
    }

    @SubCommand(name="bypass")
    public boolean bypass(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, shardManager).getNum() < PermLevel.ZGA.getNum()) {
            throw new SilentExecutionFail();
        }
        bypass = !bypass;
        if (bypass) context.send("Ignoruje czas stworzenia serwera");
        else context.send("Już nie ignoruje czasu stworzenia serwera");
        return true;
    }

    @Subscribe
    public void onGuildBanEvent(GuildBanEvent e) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        TextChannel logi = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
        if (logi == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        if (!UserUtil.isGadm(e.getUser(), shardManager)) return;
        if (!isPomoc(e.getGuild())) return;
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
        if (UserUtil.getPermlevel(e.getMember(), guildDao, shardManager).getNum() < 5) return;
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
    public void onReactionAdd(MessageReactionAddEvent e) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        TextChannel logi = fdev.getTextChannelById(Ustawienia.instance.popLogChannel);
        if (logi == null) return; //nie możemy throw'nąć bo to mogło być podczas ładowania shard'ów
        if (e.getChannel().getId().equals(Ustawienia.instance.popChannel) &&
                e.getReactionEmote().getName().equals("\uD83D\uDDD1")) {
            Message msg;
            try {
                msg = e.getChannel().retrieveMessageById(e.getMessageIdLong()).complete();
            } catch (Exception er) {
                //wiadomosci nie ma
                return;
            }
            if (e.getUser().isBot()) return;
            if (!msg.getAuthor().equals(e.getJDA().getSelfUser())) return;
            //noinspection ConstantConditions
            String id = msg.getEmbeds().get(0).getFooter().getText().split(" \\| ")[1];
            Guild g = shardManager.getGuildById(id);
            if (g == null) return;
            try {
                msg.delete().complete();
                logi.sendMessage((String.format("%s(%s) zamknął" + " prośbę o pomoc serwera %s[%s]",
                        UserUtil.formatDiscrim(e.getUser()), e.getUser().getId(), g.getName(), g.getId()))).queue();

            } catch (Exception ignored) {/*lul*/}
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

    private boolean isPomoc(Guild g) {
        TextChannel kanal = Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild))
                .getTextChannelById(Ustawienia.instance.popChannel);
        if (kanal == null) throw new NullPointerException("nieprawidłowy popChannel");
        List<Message> wiads = kanal.getHistory().retrievePast(50).complete();
        for (Message mess : wiads) {
            if (mess.getEmbeds().isEmpty()) continue;
            //noinspection ConstantConditions
            String id = mess.getEmbeds().get(0).getFooter().getText().split(" \\| ")[1];
            if (id.equals(g.getId())) {
                return true;
            }
        } return false;
    }
}
