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

package pl.fratik.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class YoutubeCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final SearchManager searchManager;
    private final EventWaiter eventWaiter;
    private final ManagerArgumentow managerArgumentow;
    private final GuildDao guildDao;

    public YoutubeCommand(NowyManagerMuzyki managerMuzyki, SearchManager searchManager, EventWaiter eventWaiter, ManagerArgumentow managerArgumentow, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.searchManager = searchManager;
        this.eventWaiter = eventWaiter;
        this.managerArgumentow = managerArgumentow;
        this.guildDao = guildDao;
        name = "youtube";
        aliases = new String[] {"yt", "youtube", "szukajwyt", "graj", "puść"};
        uzycie = new Uzycie("tytul", "string", true);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("play.dj"));
            return false;
        }
        VoiceChannel kanal = null;
        if (context.getMember().getVoiceState() != null) kanal = context.getMember().getVoiceState().getChannel();
        if (context.getMember().getVoiceState() == null || !context.getMember().getVoiceState().inVoiceChannel() ||
                kanal == null) {
            context.send(context.getTranslated("play.not.connected"));
            return false;
        }
        EnumSet<Permission> upr = context.getGuild().getSelfMember().getPermissions(kanal);
        if (!Stream.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).allMatch(upr::contains)) {
            context.send(context.getTranslated("play.no.permissions"));
            return false;
        }
        SearchManager.SearchResult result = searchManager.searchYouTube((String) context.getArgs()[0], false);
        Wiadomosc odp = generateResultMessage(result.getEntries(), context.getTranslated("youtube.message.header", (String) context.getArgs()[0]), false);
        int liczba = odp.liczba;
        Message m = context.getChannel().sendMessage(odp.tresc).complete();
        MessageWaiter waiter = new MessageWaiter(eventWaiter, context);
        AtomicBoolean deleted = new AtomicBoolean(false);
        AtomicReference<Boolean> udaloSie = new AtomicReference<>();
        VoiceChannel finalKanal = kanal;
        waiter.setMessageHandler(e -> new Thread(() -> {
            try {
                String content = e.getMessage().getContentRaw();
                int[] numerkiFilmow;
                numerkiFilmow = Arrays.stream(content.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
                deleted.set(true);
                m.delete().queue();
                if (context.getMember().getVoiceState().getChannel() != finalKanal) {
                    context.send(context.getTranslated("youtube.badchannel"));
                    udaloSie.set(false);
                    return;
                }
                for (int numerek : numerkiFilmow) {
                    if (numerek < 1 || numerek > liczba) {
                        context.send(context.getTranslated("youtube.invalid.reply"));
                        udaloSie.set(false);
                        return;
                    }
                }
                List<SearchManager.SearchResult.SearchEntry> entries = new ArrayList<>();
                for (int numerek : numerkiFilmow)
                    entries.add(result.getEntries().get(numerek - 1));
                ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
                List<AudioTrack> audioTracks = new ArrayList<>();
                for (SearchManager.SearchResult.SearchEntry entry : entries) {
                    List<AudioTrack> tracks = managerMuzyki.getAudioTracks(entry.getUrl());
                    if (tracks.isEmpty()) continue;
                    audioTracks.add(tracks.get(0));
                }
                if (audioTracks.isEmpty()) {
                    context.send(context.getTranslated("youtube.cant.find"));
                    udaloSie.set(false);
                    return;
                }
                if (!mms.isConnected()) {
                    mms.setAnnounceChannel(context.getChannel());
                    mms.connect(finalKanal);
                }
                if (!mms.isConnected()) {
                    udaloSie.set(false);
                    return;
                }
                List<AudioTrack> added = new ArrayList<>(); // można by to było zrobić mniejszą ilością varów ale jestem juz taki śpiący ;_; ej sonar-lint to nie kod, uspokój sie
                for (AudioTrack at : audioTracks) {
                    mms.addToQueue(context.getSender(), at, context.getLanguage());
                    added.add(at);
                }
                if (added.size() == 1) {
                    if (!mms.isPlaying()) mms.play();
                    else context.getChannel().sendMessage(new MessageBuilder(context.getTranslated("play.queued",
                            added.get(0).getInfo().title)).stripMentions(context.getGuild()).build()).queue();
                } else {
                    context.getChannel().sendMessage(new MessageBuilder(context.getTranslated("play.queued.multiple",
                            added.size())).stripMentions(context.getGuild()).build()).queue();
                    if (!mms.isPlaying()) mms.play();
                }
                udaloSie.set(true);
            } catch (NumberFormatException error) {
                deleted.set(true);
                m.delete().queue(null, a -> {});
                context.send(context.getTranslated("youtube.invalid.reply"));
                udaloSie.set(false);
            } catch (Exception error) {
                Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(),
                        UserUtil.formatDiscrim(context.getSender()), null, null));
                Sentry.capture(error);
                Sentry.clearContext();
                context.send(context.getTranslated("youtube.errored"));
                udaloSie.set(false);
            }
        }).start());
        waiter.setTimeoutHandler(() -> {
            deleted.set(true);
            m.delete().queue(null, a -> {});
            context.send(context.getTranslated("youtube.invalid.reply"));
            udaloSie.set(false);
        });
        waiter.create();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (deleted.get()) return udaloSie.get() == null ? false : udaloSie.get(); //NOSONAR
        List<SearchManager.SearchResult.SearchEntry> eList = new ArrayList<>();
        for (SearchManager.SearchResult.SearchEntry entry : result.getEntries()) {
            if (deleted.get()) break;
            SearchManager.SearchResult res = new SearchManager.SearchResult();
            res.addEntry(entry.getTitle(), entry.getUrl(),
                    searchManager.getDuration(searchManager.extractIdFromUri(entry.getUrl())).getDurationAsInt(),
                    null);
            eList.add(res.getEntries().get(0));
        }
        if (deleted.get()) return false;
        odp = generateResultMessage(eList, context.getTranslated("youtube.message.header", (String) context.getArgs()[0]), true);
        m.editMessage(odp.tresc).queue(a -> {}, b -> {});
        while (udaloSie.get() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return udaloSie.get();
    }

    private Wiadomosc generateResultMessage(List<SearchManager.SearchResult.SearchEntry> entries, String header, boolean durations) {
        StringBuilder sb = new StringBuilder(header).append("\n");
        int liczba = 0;
        for (SearchManager.SearchResult.SearchEntry entry : entries) {
            liczba++;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("**").append(liczba).append("**: ").append(entry.getTitle());
            if (durations) sb2.append(" (").append(entry.getDuration()).append(")");
            sb2.append("\n");
            if (sb.length() + sb2.length() > 2000) {
                liczba--;
                break;
            }
            else sb.append(sb2.toString());
        }
        return new Wiadomosc(sb.toString(), liczba);
    }

    @AllArgsConstructor
    private static class Wiadomosc {
        private final String tresc;
        private final int liczba;
    }
}
