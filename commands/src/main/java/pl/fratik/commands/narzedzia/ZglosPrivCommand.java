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
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Priv;
import pl.fratik.commands.entity.PrivDao;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.ReactionWaiter;
import pl.fratik.core.util.StringUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ZglosPrivCommand extends Command {
    private final PrivDao privDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private static final String POTW = "\u2705";
    private static final String ODRZ = "\u274c";

    public ZglosPrivCommand(PrivDao privDao, EventWaiter eventWaiter, EventBus eventBus, ShardManager shardManager, Tlumaczenia tlumaczenia) {
        this.privDao = privDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        name = "zglospriv";
        allowInDMs = true;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("id", "string"); //NOSONAR
        hmap.put("powod", "string"); //NOSONAR
        hmap.put("[...]", "string"); //NOSONAR
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"reportpriv", "reportdm", "reportmsg", "reportpv", "reportprivee"};
        uzycieDelim = " ";
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
        Guild botgild = Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild));
        String powod = "";
        String id = (String) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        if (powod.isEmpty()) {
            CommonErrors.usage(context);
            return false;
        }
        Priv priv = privDao.get(id);
        if (priv == null || !priv.getDoKogo().equals(context.getSender().getId())) {
            context.send(context.getTranslated("zglospriv.no.priv"));
            return false;
        }
        if (priv.getZgloszone() != null) {
            if (priv.getZgloszone()) {
                context.send(context.getTranslated("zglospriv.reported"));
                return false;
            }
            context.send(context.getTranslated("zglospriv.reported.answered"));
            return false;
        }
        Message msg = context.getChannel().sendMessage(context.getTranslated("zglospriv.confirmation")).complete();
        msg.addReaction(POTW).queue();
        msg.addReaction(ODRZ).queue();
        ReactionWaiter waiter = new ReactionWaiter(eventWaiter, context) {
            @Override
            protected boolean checkReaction(MessageReactionAddEvent event) {
                return super.checkReaction(event) && !event.getReactionEmote().isEmote() &&
                        (event.getReactionEmote().getName().equals(POTW) ||
                                event.getReactionEmote().getName().equals(ODRZ));
            }
        };
        Runnable cancel = () -> context.send(context.getTranslated("zglospriv.cancelled"));
        waiter.setTimeoutHandler(cancel);
        String finalPowod = powod;
        waiter.setReactionHandler(e -> {
            if (e.getReactionEmote().getName().equals(ODRZ)) {
                cancel.run();
                return;
            }
            User odKogo = shardManager.getUserById(priv.getOdKogo());
            if (odKogo == null) odKogo = shardManager.retrieveUserById(priv.getOdKogo()).complete();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(context.getSender().getAsTag());
            eb.addField("Osoba nadużywająca", String.format("%s (%s)",
                    odKogo.getAsTag(), priv.getOdKogo()), false);
            eb.addField("Treść priv", priv.getContent().length() >= 1000 ? "[Treść za długa]("
                    + StringUtil.haste(finalPowod) + ")" : priv.getContent(), false);
            eb.addField("Powód nadużycia", finalPowod.length() >= 1000 ? "[Powód za długi]("
                    + StringUtil.haste(finalPowod) + ")" : finalPowod, false);
            eb.addField("Osoba jest tymczasowo zablokowana", "z używania fb!priv.", false);
            eb.addField("Aby uniewinnić zgłoszoną osobę", "użyj \u2705", false);
            eb.addField("Aby ukarać zgłoszoną osobę", "użyj \u2757 i podejmij właściwą akcję" +
                    " (gban albo blacklistpriv)", false);
            eb.addField("ID osoby zgłaszającej", context.getSender().getId(), false);
            eb.setFooter(id, null);
            Objects.requireNonNull(botgild.getRoleById(Ustawienia.instance.popRole))
                    .getManager().setMentionable(true).complete();
            Message msgpop = Objects.requireNonNull(shardManager.getTextChannelById(Ustawienia.instance
                    .zglosPrivChannel)).sendMessage(eb.build()).complete();
            msgpop.addReaction(POTW).queue();
            msgpop.addReaction("\u2757").queue();
            Objects.requireNonNull(botgild.getRoleById(Ustawienia.instance.popRole)).getManager().setMentionable(false)
                    .complete();
            priv.setZgloszone(true);
            privDao.save(priv);
            context.send(context.getTranslated("zglospriv.success"));
        });
        return true;
    }

    @Subscribe
    private void onReactionAdd(MessageReactionAddEvent e) {
        if (!e.getChannel().getId().equals(Ustawienia.instance.zglosPrivChannel)) return;
        if (e.getReactionEmote().isEmote()) return;
        String s = e.getReactionEmote().getName();
        if (POTW.equals(s)) {
            Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
            if (msg.getEmbeds().isEmpty() || !msg.getAuthor().equals(e.getJDA().getSelfUser())) return;
            Priv priv = privDao.get(Objects.requireNonNull(msg.getEmbeds().get(0).getFooter()).getText());
            priv.setZgloszone(false);
            privDao.save(priv);
            msg.delete().queue();
            Objects.requireNonNull(shardManager.getUserById(priv.getDoKogo())).openPrivateChannel().complete()
                    .sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(shardManager
                            .getUserById(priv.getDoKogo())), "zglospriv.response1")).queue();
        } else if (ODRZ.equals(s)) {
            Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
            if (msg.getEmbeds().isEmpty() || !msg.getAuthor().equals(e.getJDA().getSelfUser())) return;
            Priv priv = privDao.get(Objects.requireNonNull(msg.getEmbeds().get(0).getFooter()).getText());
            priv.setZgloszone(false);
            privDao.save(priv);
            msg.delete().queue();
            Message msg2 = e.getChannel().sendMessage(e.getUser().getAsMention() +
                    " podejmij akcje na jakimkolwiek kanale. Zgłoszenie zostaje zapisane jako rozwiązane za minutę.")
                    .complete();
            msg2.delete().queueAfter(1, TimeUnit.MINUTES, woid -> {
                msg.delete().queue();
                Objects.requireNonNull(shardManager.getUserById(priv.getDoKogo())).openPrivateChannel().complete()
                        .sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(shardManager
                                .getUserById(priv.getDoKogo())), "zglospriv.response2")).queue();
            });
        }
    }
}
