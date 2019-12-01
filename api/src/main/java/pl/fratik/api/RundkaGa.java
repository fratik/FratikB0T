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

package pl.fratik.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.undertow.server.RoutingHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.api.entity.*;
import pl.fratik.api.event.RundkaAnswerVoteEvent;
import pl.fratik.api.event.RundkaNewAnswerEvent;
import pl.fratik.api.internale.Exchange;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.util.UserUtil;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

class RundkaGa {
    private final EventBus eventBus;
    private final RundkaDao rundkaDao;
    private final ShardManager shardManager;

    public RundkaGa(Module module, EventBus eventBus, RundkaDao rundkaDao, ShardManager shardManager) {
        this.eventBus = eventBus;
        this.rundkaDao = rundkaDao;
        this.shardManager = shardManager;
        RoutingHandler routes = module.getRoutes();
        routes.get("/api/rundka", ex -> {
            String userId = Exchange.queryParams().queryParam(ex, "userId").orElse("");
            if (RundkaCommand.isRundkaOn()) {
                List<RundkaOdpowiedz> odpowiedzi = new ArrayList<>();
                for (RundkaOdpowiedzFull odp : rundkaDao.get(RundkaCommand.getNumerRundy()).getZgloszenia()) {
                    if (odp.getUserId().equals(userId) ||
                            UserUtil.isStaff(shardManager.retrieveUserById(userId).complete(), shardManager))
                        odpowiedzi.add(new RundkaOdpowiedzSanitized(odp, true, shardManager));
                    else odpowiedzi.add(new RundkaOdpowiedzSanitized(odp, false, shardManager));
                }
                Exchange.body().sendJson(ex,
                        new Rundka.RundkaWrapper(RundkaCommand.getNumerRundy(), true, odpowiedzi));
            } else {
                Exchange.body().sendJson(ex, new Rundka.RundkaWrapper(null, false, null));
            }
        });
        routes.post("/api/rundka", ex -> {
            synchronized (this) {
                if (!RundkaCommand.isRundkaOn()) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("rundka nie jest w toku"), 400);
                    return;
                }
                RundkaOdpowiedzFull odp;
                try {
                    odp = Exchange.body().parseJson(ex, new TypeReference<RundkaOdpowiedzFull>() {});
                    if (odp.getOceny() == null) odp.setOceny(new Oceny());
                } catch (Exception e) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("podanie ma braki odpowiedzi"), 400);
                    return;
                }
                if (odp.getRundka() != RundkaCommand.getNumerRundy()) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("nieprawidłowy numer rundy"), 400);
                    return;
                }
                if (anyFieldNull(odp, new String[] {"messageid", "oceny"})) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("podanie ma braki odpowiedzi"), 400);
                    return;
                }
                User user = shardManager.retrieveUserById(odp.getUserId()).complete();
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", Language.POLISH.getLocale());
                Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
                if (fdev == null) throw new NullPointerException("FDev null");
                Member mem = fdev.getMember(user);
                if (mem == null) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("nie jest na fdev"), 400);
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl());
                eb.setTitle("Nowe podanie!");
                eb.setColor(UserUtil.getPrimColor(user));
                eb.addField("Co Twoim zdaniem należy do obowiązków Global Admina?", odp.getObowiazki(), false);
                eb.addField("Jakiej jesteś płci?", odp.getPlec() == Plec.KOBIETA ? "Kobieta" :
                        odp.getPlec() == Plec.MEZCZYZNA ? "Mężczyzna" : "Inna/Wolę nie podawać", false); //NOSONAR
                eb.addField("Podaj sposób ustawiania konfiguracji serwera", odp.getKonfiguracja(), false);
                eb.addField("Jak ustawić powitanie/pożegnanie?", odp.getUstawPowitaniePozegnanie(), false);
                eb.addField("Jak usunąć powitanie/pożegnanie?", odp.getUsunPowitaniePozegnanie(), false);
                eb.addField("Jaki poziom uprawnień ma Global Admin?", String.valueOf(odp.getPozUpr()), false);
                eb.addField("Jak często przebywasz na Discordzie?",
                        odp.getCzestotliwoscPrzebywania() == CzestotliwoscPrzebywania.JednaDwieGodziny ?
                                "1-2h dziennie" : odp.getCzestotliwoscPrzebywania() == CzestotliwoscPrzebywania //NOSONAR
                                .TrzySiedemGodzin ? "3-7h dziennie" : odp.getCzestotliwoscPrzebywania() //NOSONAR
                                == CzestotliwoscPrzebywania.DwanasciePietnascieGodzin ? "12-15h dziennie" : //NOSONAR
                                odp.getCzestotliwoscPrzebywania() == CzestotliwoscPrzebywania.CalyDzien ? //NOSONAR
                                        "Cały czas jestem, wystarczy mnie @! (o ile nie śpię)" : //NOSONAR
                                        "N/a", false);
                eb.addField("Jesteś aktywny na FratikDev?", odp.getAktywnoscFdev() ? "Tak" : "Nie", false);
                eb.addField("Dlaczego chcesz zostać Global Adminem?", odp.getDlaczegoGa(), false);
                eb.addField("Dlaczego mamy Cię wybrać?", odp.getDlaczegoWybrac(), false);
                eb.addField("Data stworzenia konta", sdf.format(new Date(user.getTimeCreated()
                        .toInstant().toEpochMilli())), false);
                eb.addField("Data dołączenia", sdf.format(new Date(mem.getTimeJoined()
                        .toInstant().toEpochMilli())), false);
                eb.addField("ID", user.getId(), false);
                Rundka rundka = rundkaDao.get(odp.getRundka());
                TextChannel ch = shardManager.getTextChannelById(rundka.getVoteChannel());
                Objects.requireNonNull(shardManager.getRoleById(Ustawienia.instance.gadmRole))
                        .getManager().setMentionable(true).complete();
                Message msg = Objects.requireNonNull(ch)
                        .sendMessage("<@&" + Ustawienia.instance.gadmRole + ">").embed(eb.build()).complete();
                Objects.requireNonNull(shardManager.getRoleById(Ustawienia.instance.gadmRole))
                        .getManager().setMentionable(false).complete();
                msg.addReaction(Objects.requireNonNull(fdev.getEmoteById(Ustawienia.instance.emotki.greenTick))).complete();
                msg.addReaction(Objects.requireNonNull(fdev.getEmoteById(Ustawienia.instance.emotki.redTick))).complete();
                msg.pin().complete();
                odp.setMessageId(msg.getId());
                eventBus.post(new RundkaNewAnswerEvent(odp));
                rundka.getZgloszenia().add(odp);
                rundkaDao.save(rundka);
            }
        });
    }

    private boolean anyFieldNull(Object obj, String[] ignore) {
        Method[] metody = obj.getClass().getDeclaredMethods();
        for (Method metoda : metody) {
            if (Arrays.stream(ignore).anyMatch(ig -> metoda.getName().replace("get", "")
                    .equalsIgnoreCase(ig)) || !metoda.getName().startsWith("get"))
                continue;
            try {
                if (metoda.invoke(obj, (Object[]) null) == null) return true;
            } catch (Exception e) {
                //nic
            }
        }
        return false;
    }

    @Subscribe
    private void onReactionAdd(MessageReactionAddEvent e) {
        if (e.getUser().equals(e.getJDA().getSelfUser())) return;
        Rundka rundka = rundkaDao.get(RundkaCommand.getNumerRundy());
        if (rundka == null) return;
        TextChannel ch = shardManager.getTextChannelById(rundka.getVoteChannel());
        if (!e.getChannel().equals(ch)) return;
        Message msg = ch.retrieveMessageById(e.getMessageId()).complete();
        if (msg.getEmbeds().isEmpty()) return;
        RundkaOdpowiedzFull odp = rundka.getZgloszenia().stream()
                .filter(o -> o.getMessageId().equals(msg.getId())).findFirst().orElse(null);
        if (odp == null) return;
        if (!e.getReactionEmote().isEmote() ||
                !(e.getReactionEmote().getEmote().getId().equals(Ustawienia.instance.emotki.greenTick) ||
                        e.getReactionEmote().getEmote().getId().equals(Ustawienia.instance.emotki.redTick))) return;
        if (e.getReactionEmote().getId().equals(Ustawienia.instance.emotki.greenTick))
            odp.getOceny().getTak().add(e.getUser().getId());
        if (e.getReactionEmote().getId().equals(Ustawienia.instance.emotki.redTick))
            odp.getOceny().getNie().add(e.getUser().getId());
        eventBus.post(new RundkaAnswerVoteEvent(odp));
        rundkaDao.save(rundka);
    }

    @Subscribe
    private void onReactionRemove(MessageReactionRemoveEvent e) {
        Rundka rundka = rundkaDao.get(RundkaCommand.getNumerRundy());
        if (rundka == null || rundka.getVoteChannel() == null) return;
        TextChannel ch = shardManager.getTextChannelById(rundka.getVoteChannel());
        if (!e.getChannel().equals(ch)) return;
        Message msg = ch.retrieveMessageById(e.getMessageId()).complete();
        if (msg.getEmbeds().isEmpty()) return;
        RundkaOdpowiedzFull odp = rundka.getZgloszenia().stream()
                .filter(o -> o.getMessageId().equals(msg.getId())).findFirst().orElse(null);
        if (odp == null) return;
        if (!e.getReactionEmote().isEmote() ||
                !(e.getReactionEmote().getEmote().getId().equals(Ustawienia.instance.emotki.greenTick) ||
                        e.getReactionEmote().getEmote().getId().equals(Ustawienia.instance.emotki.redTick))) return;
        if (e.getReactionEmote().getId().equals(Ustawienia.instance.emotki.greenTick))
            odp.getOceny().getTak().remove(e.getUser().getId());
        if (e.getReactionEmote().getId().equals(Ustawienia.instance.emotki.redTick))
            odp.getOceny().getNie().remove(e.getUser().getId());
        eventBus.post(new RundkaAnswerVoteEvent(odp));
        rundkaDao.save(rundka);
    }
}
