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

package pl.fratik.liczek.listeners;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;
import pl.fratik.liczek.entity.Liczek;
import pl.fratik.liczek.entity.LiczekDao;

import java.time.OffsetDateTime;

public class LiczekListener {
    private final GuildDao guildDao;
    private final LiczekDao liczekDao;
    private final Tlumaczenia tlumaczenia;

    public LiczekListener(GuildDao guildDao, LiczekDao liczekDao, Tlumaczenia tlumaczenia) {
        this.guildDao = guildDao;
        this.liczekDao = liczekDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    public synchronized void onGuildMessageReceivedEvent(GuildMessageReceivedEvent e) {
        GuildConfig gc = guildDao.get(e.getGuild());
        Liczek licz = liczekDao.get(e.getGuild());
        if (e.getChannel().getType() != ChannelType.TEXT) {
            return;
        }
        if (e.getChannel().getId().equals(gc.getLiczekKanal())) {
            if (e.getAuthor().isBot() || e.isWebhookMessage()) {
                e.getMessage().delete().queue();
                return;
            }

            String[] kek = e.getMessage().getContentRaw().split(" ");
            int wyslanaLiczba;

            try {
                wyslanaLiczba = Integer.parseInt(kek[0]);
            } catch (NumberFormatException xd) {
                e.getMessage().delete().queue();
                return;
            }

            if (wyslanaLiczba != licz.getLiczekLiczba() + 1 ||
                    e.getAuthor().getId().equals(licz.getLiczekOstatniaOsoba())) {
                e.getMessage().delete().queue();
                return;
            }

            licz.setLiczekLiczba(wyslanaLiczba);
            licz.setLiczekOstatniaOsoba(e.getAuthor().getId());
            refreshTopic(e.getChannel(), e.getAuthor());
            liczekDao.save(licz);
        }
    }

    @Subscribe
    public void onGuildMessageDeleteEvent(GuildMessageDeleteEvent e) {
        GuildConfig gc = guildDao.get(e.getGuild());
        if (gc.getLiczekKanal() == null) return;
        if (gc.getLiczekKanal().isEmpty()) return;
        if (!e.getChannel().getId().equals(gc.getLiczekKanal())) return;
        try {
            e.getGuild().retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).queue(
                    audiologi -> {
                        User deletedBy = null;
                        for (AuditLogEntry log : audiologi) {
                            if (log.getType() == ActionType.MESSAGE_DELETE
                                    && log.getTimeCreated().isAfter(OffsetDateTime.now().minusMinutes(1))
                                    && e.getChannel().getId().equals(log.getOption(AuditLogOption.CHANNEL))) {
                                deletedBy = log.getUser();
                                break;
                            }
                        }
                        if (deletedBy != null) dyskwalifikacja(deletedBy, e.getChannel());
                    }

            );
        } catch (Exception kek) {
            /* lul */
        }
    }

    @Subscribe
    public void onMessageUpdateEvent(MessageUpdateEvent e) {
        GuildConfig gc = guildDao.get(e.getGuild());
        if (gc.getLiczekKanal() == null) return;
        if (gc.getLiczekKanal().isEmpty()) return;
        if (!e.getChannel().getId().equals(gc.getLiczekKanal())) return;
        if (e.getChannel().getType() != ChannelType.TEXT) return;
        TextChannel ok = e.getGuild().getTextChannelById(e.getChannel().getId());
        if (ok == null) return;
        dyskwalifikacja(e.getAuthor(), ok);
    }

    private void refreshTopic(TextChannel cha, User osoba) {
        refreshTopic(cha, osoba, liczekDao.get(cha.getGuild()));
    }

    private synchronized void refreshTopic(TextChannel cha, User osoba, Liczek licz) {
        Integer liczba = licz.getLiczekLiczba() + 1;

        Language l = tlumaczenia.getLanguage(cha.getGuild());
        String msg = tlumaczenia.get(l, "liczek.topic", osoba.getAsMention(), liczba);

        try {
            cha.getManager().setTopic(msg).queue();
        } catch (Exception xd) {
            /* brak permów, idziem sobie*/
        }
    }

    private void dyskwalifikacja(User user, TextChannel cha) {
        Member mem = cha.getGuild().getMemberById(user.getId());
        if (mem == null) return;
        GuildConfig gc = guildDao.get(mem.getGuild());
        Language l = tlumaczenia.getLanguage(mem.getGuild());
        String msg = tlumaczenia.get(l, "liczek.dyskwalifikacja", mem.getAsMention(), UserUtil.formatDiscrim(mem), user.getId());

        try {
            cha.putPermissionOverride(mem).deny(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_WRITE).complete();
        } catch (PermissionException xd) {
            /* lul */
        }

        if (gc.getKanalAdministracji() != null && !gc.getKanalAdministracji().isEmpty()) {
            TextChannel adm = mem.getGuild().getTextChannelById(gc.getKanalAdministracji());
            if (adm != null) {
                if (adm.canTalk()) adm.sendMessage(msg).queue();
            }
        }
    }

}