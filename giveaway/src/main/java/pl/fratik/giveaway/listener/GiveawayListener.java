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

package pl.fratik.giveaway.listener;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.GiveawayConfig;
import pl.fratik.core.entity.GiveawayDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GiveawayListener {

    private static final String TADA = "\uD83C\uDF89";

    public final ShardManager shardManager;
    public final GiveawayDao giveawayDao;
    public final Tlumaczenia tlumaczenia;

    @Getter @Setter
    private boolean startup = true;

    public GiveawayListener(ShardManager shardManager, GiveawayDao giveawayDao, Tlumaczenia tlumaczenia) {
        this.shardManager = shardManager;
        this.giveawayDao = giveawayDao;
        this.tlumaczenia = tlumaczenia;
        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        // FIXME: 1 minuta tylko podczas testów
        executorSche.scheduleAtFixedRate(this::update, 0, 1, TimeUnit.MINUTES);
    }

    public void update(GiveawayConfig entry) {
        long date = new Date().getTime();
        Random rand = new Random();
        TextChannel txt;
        Message msg;
        Guild g;
        try {
            txt = shardManager.getTextChannelById(entry.getChannelId());
            msg = txt.retrieveMessageById(entry.getMessageId()).complete();
            g = shardManager.getGuildById(entry.getDecodeGuild());
            if (g == null) throw new Exception();
        } catch (Exception e) { // nie ma kanału/wiadomości/serwera to usuwamy z bazy
            giveawayDao.delete(entry.getId());
            return;
        }
        if (entry.getEnd() - date <= 0) {
            entry.setAktywna(false);
            List<String> listaLudzi = new ArrayList<>();
            List<String> wygrani = new ArrayList<>();
            for (MessageReaction rec : msg.getReactions()) {
                if (rec.getReactionEmote().isEmoji() && rec.getReactionEmote().getEmoji().equalsIgnoreCase(TADA)) {
                    listaLudzi = rec.retrieveUsers().complete().stream()
                            .filter(u -> !u.isBot())
                            .map(User::getId)
                            .collect(Collectors.toList());
                }
            }
            if (entry.getWygranychOsob() <= listaLudzi.size()) {
                entry.setWinners(listaLudzi);
            } else {
                while (entry.getWygranychOsob() < wygrani.size()) {
                    String wygral = listaLudzi.get(rand.nextInt(listaLudzi.size() - 1));
                    wygrani.add(wygral);
                    listaLudzi.remove(wygral);
                }
            }
            txt.sendMessage("Wygrana elo").queue();
            giveawayDao.save(entry);
            msg.editMessage(createEmbed(entry, tlumaczenia, shardManager).build()).queue();
        }
    }

    public void update() {
        if (!startup) return;
        giveawayDao.getAllAktywne().forEach(this::update);
    }

    public static EmbedBuilder createEmbed(GiveawayConfig config, Tlumaczenia tlumaczenia, ShardManager shardManager) {
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy @ HH:mm:ss");
        EmbedBuilder eb = new EmbedBuilder();

        eb.setFooter("ID: " + config.getDecodeId() + " | Ostatnia aktualizacja");
        eb.setTimestamp(Instant.now());
        eb.setTitle(TADA + " Konkurs");
        eb.addField("Nagroda", config.getPrize(), false);
        eb.addField("Wygra osób", config.getWygranychOsob() + "", false);

        User u = null;
        try {
            u = shardManager.retrieveUserById(config.getOrganizator()).complete();
        } catch (Exception ignored) { }
        eb.addField("Organizowany przez", u == null ? "???" : u.getAsMention(), false);

        if (config.isAktywna()) {
            eb.setColor(Color.cyan);
            eb.addField("Koniec o", sfd.format(config.getEnd()), false);
        } else {
            eb.setColor(Color.red);
            eb.addField("Koniec o", "Konkurs się zakończył!", false);
            StringBuilder sb = new StringBuilder();
            String f = "<@%s>";
            if (!config.getWinners().isEmpty()) {
                config.getWinners().forEach(w -> sb.append(String.format(f, w) + ", "));
                eb.addField("Wygrani", sb.toString(), false);
            }
        }

        return eb;
    }

    public void create(GiveawayConfig config, Guild guild) {
        LoggerFactory.getLogger(getClass()).debug(new Gson().toJson(config));
        config.setId("1");
        config.setGuildId(guild.getId());

        try {
            guild.getTextChannelById(config.getChannelId())
                    .sendMessage(createEmbed(config, tlumaczenia, shardManager).build()).queue();
            giveawayDao.save(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
