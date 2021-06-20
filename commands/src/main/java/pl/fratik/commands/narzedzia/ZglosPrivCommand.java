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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
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
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.StringUtil;

import java.util.Arrays;
import java.util.Collections;
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
        uzycieDelim = " ";
        allowPermLevelChange = false;
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
                    .map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(uzycieDelim));
        if (powod.isEmpty()) {
            CommonErrors.usage(context);
            return false;
        }
        Priv priv = privDao.get(id);
        if (priv == null || !priv.getDoKogo().equals(context.getSender().getId())) {
            context.reply(context.getTranslated("zglospriv.no.priv"));
            return false;
        }
        if (priv.getZgloszone() != null) {
            if (priv.getZgloszone()) {
                context.reply(context.getTranslated("zglospriv.reported"));
                return false;
            }
            context.reply(context.getTranslated("zglospriv.reported.answered"));
            return false;
        }
        Message msg = context.getMessageChannel().sendMessage(context.getTranslated("zglospriv.confirmation"))
                .setActionRows(ActionRow.of(
                        Button.danger("YES", context.getTranslated("generic.yes")),
                        Button.secondary("NO", context.getTranslated("generic.no"))
                ))
                .reference(context.getMessage()).complete();
        ButtonWaiter waiter = new ButtonWaiter(eventWaiter, context, msg.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        waiter.setTimeoutHandler(() -> {
            msg.editMessage(msg.getContentRaw()).setActionRows(Collections.emptySet()).queue();
            context.reply(context.getTranslated("zglospriv.cancelled"));
        });
        String finalPowod = powod;
        waiter.setButtonHandler(e -> {
            msg.editMessage(msg.getContentRaw()).setActionRows(Collections.emptySet()).queue();
            if (e.getComponentId().equals("NO")) {
                e.getHook().editOriginal(context.getTranslated("zglospriv.cancelled")).queue();
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
            eb.addField("Aby ukarać zgłoszoną osobę", "użyj \u2757 i podejmij właściwą akcję" +
                    " (gban albo blacklistpriv)", false);
            eb.addField("ID osoby zgłaszającej", context.getSender().getId(), false);
            eb.setFooter(id, null);
            Role popRole = Objects.requireNonNull(botgild.getRoleById(Ustawienia.instance.popRole));
            popRole.getManager().setMentionable(true).complete();
            Objects.requireNonNull(shardManager.getTextChannelById(Ustawienia.instance.zglosPrivChannel))
                    .sendMessage(popRole.getAsMention()).mention(popRole).embed(eb.build()).setActionRows(
                            ActionRow.of(
                                    Button.success("CLOSE", "Uniewinnij"),
                                    Button.danger("ACTION", "Ukarz (po wciśnięciu " +
                                            "wykonaj odpowiednią komendę (gban albo blacklistpriv) na innym kanalę)")
                            )).complete();
            popRole.getManager().setMentionable(false).complete();
            priv.setZgloszone(true);
            privDao.save(priv);
            e.getHook().editOriginal(context.getTranslated("zglospriv.success")).queue();
        });
        waiter.create();
        return true;
    }

    @Subscribe
    private void onButtonClick(ButtonClickEvent e) {
        if (!e.getChannel().getId().equals(Ustawienia.instance.zglosPrivChannel)) return;
        if (e.getUser().isBot()) return;
        if (e.getComponentId().equals("CLOSE")) {
            Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
            if (msg.getEmbeds().isEmpty() || !msg.getAuthor().equals(e.getJDA().getSelfUser())) return;
            Priv priv = privDao.get(Objects.requireNonNull(msg.getEmbeds().get(0).getFooter()).getText());
            priv.setZgloszone(false);
            privDao.save(priv);
            e.deferEdit().queue();
            msg.delete().queue();
            shardManager.retrieveUserById(priv.getDoKogo()).complete().openPrivateChannel().complete()
                    .sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(shardManager.retrieveUserById
                            (priv.getDoKogo()).complete()), "zglospriv.response1", priv.getId())).queue();
        } else if (e.getComponentId().equals("ACTION")) {
            Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
            if (msg.getEmbeds().isEmpty() || !msg.getAuthor().equals(e.getJDA().getSelfUser())) return;
            Priv priv = privDao.get(Objects.requireNonNull(msg.getEmbeds().get(0).getFooter()).getText());
            priv.setZgloszone(false);
            privDao.save(priv);
            e.reply("Podejmij akcje na jakimkolwiek kanale. Zgłoszenie zostaje zapisane jako rozwiązane za minutę.")
                    .setEphemeral(true).queue();
            msg.delete().queue();
            shardManager.retrieveUserById(priv.getDoKogo()).complete().openPrivateChannel().complete()
                    .sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(shardManager.retrieveUserById
                            (priv.getDoKogo()).complete()), "zglospriv.response2", priv.getId())).queueAfter(1, TimeUnit.MINUTES);
        }
    }
}
