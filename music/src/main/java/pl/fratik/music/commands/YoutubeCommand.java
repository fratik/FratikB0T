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

package pl.fratik.music.commands;

import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.ModalWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class YoutubeCommand extends MusicCommand {

    private static final String YOUTUBE_MODAL = "YTMODAL";
    private static final String YOUTUBE_MODAL_TEXT = "YTMODAL_TEXT";
    private final NowyManagerMuzyki managerMuzyki;
    private final SearchManager searchManager;
    private final EventWaiter eventWaiter;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final Cache<UserConfig> userCache;

    public YoutubeCommand(NowyManagerMuzyki managerMuzyki, SearchManager searchManager, EventWaiter eventWaiter, GuildDao guildDao, UserDao userDao, RedisCacheManager rcm) {
        this.managerMuzyki = managerMuzyki;
        this.searchManager = searchManager;
        this.eventWaiter = eventWaiter;
        this.guildDao = guildDao;
        this.userDao = userDao;
        this.userCache = rcm.new CacheRetriever<UserConfig>(){}.getCache();
        name = "youtube";
        usage = "<tekst:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        UserConfig userConfig = userCache.get(context.getSender().getId(), userDao::get);
        if (!userConfig.isYtWarningSeen()) {
            context.replyEphemeral(context.getTranslated("youtube.warning"));
            userConfig.setYtWarningSeen(true);
            userDao.save(userConfig);
            return;
        }
        AudioChannel kanal = null;
        if (context.getMember().getVoiceState() != null) kanal = context.getMember().getVoiceState().getChannel();
        if (context.getMember().getVoiceState() == null || !context.getMember().getVoiceState().inAudioChannel() ||
                kanal == null) {
            context.replyEphemeral(context.getTranslated("play.not.connected"));
            return;
        }
        EnumSet<Permission> upr = context.getGuild().getSelfMember().getPermissions(kanal);
        if (!Stream.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).allMatch(upr::contains)) {
            context.replyEphemeral(context.getTranslated("play.no.permissions"));
            return;
        }
        String arg = context.getArguments().get("tekst").getAsString();
        InteractionHook hook = context.defer(false);
        SearchManager.SearchResult result = searchManager.searchYouTube(arg);
        Wiadomosc odp = generateResultMessage(result.getEntries(), context.getTranslated("youtube.message.header", arg), false);
        int liczba = odp.liczba;
        AtomicBoolean deleted = new AtomicBoolean(false);
        AudioChannel finalKanal = kanal;
        ButtonWaiter bw = new ButtonWaiter(eventWaiter, context, hook.getInteraction(), null);
        Message message = context.sendMessage(odp.tresc);
        bw.setButtonHandler(e -> {
            deleted.set(true);
            String modalId = YOUTUBE_MODAL + message.getIdLong();
            ModalWaiter mw = new ModalWaiter(eventWaiter, context, modalId, ModalWaiter.ResponseType.REPLY);
            mw.setButtonHandler(ev -> {
                try {
                    String content = ev.getValue(YOUTUBE_MODAL_TEXT).getAsString();
                    int[] numerkiFilmow;
                    numerkiFilmow = Arrays.stream(content.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
                    if (context.getMember().getVoiceState().getChannel() != finalKanal) {
                        ev.getHook().sendMessage(context.getTranslated("youtube.badchannel")).complete();
                        return;
                    }
                    for (int numerek : numerkiFilmow) {
                        if (numerek < 1 || numerek > liczba) {
                            ev.getHook().sendMessage(context.getTranslated("youtube.invalid.reply")).complete();
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
                        ev.getHook().sendMessage(context.getTranslated("youtube.cant.find")).complete();
                        return;
                    }
                    if (!mms.isConnected()) {
                        if (!context.getChannel().canTalk()) context.sendMessage(context.getTranslated("play.no.perms.warning"));
                        mms.setAnnounceChannel(context.getChannel());
                        mms.connect(finalKanal);
                    }
                    if (!mms.isConnected()) return;
                    List<AudioTrack> added = new ArrayList<>(); // można by to było zrobić mniejszą ilością varów ale jestem juz taki śpiący ;_; ej sonar-lint to nie kod, uspokój sie
                    for (AudioTrack at : audioTracks) {
                        mms.addToQueue(context.getSender(), at, context.getLanguage());
                        added.add(at);
                    }
                    if (added.size() == 1) {
                        ev.getHook().sendMessage(context.getTranslated("play.queued",
                                added.get(0).getInfo().title)).queue();
                        if (!mms.isPlaying()) mms.play();
                    } else {
                        ev.getHook().sendMessage(context.getTranslated("play.queued.multiple",
                                added.size())).queue();
                        if (!mms.isPlaying()) mms.play();
                    }
                } catch (NumberFormatException error) {
                    ev.getHook().sendMessage(context.getTranslated("youtube.invalid.reply")).queue();
                } catch (Exception error) {
                    Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(),
                            UserUtil.formatDiscrim(context.getSender()), null, null));
                    Sentry.capture(error);
                    Sentry.clearContext();
                    ev.getHook().sendMessage(context.getTranslated("youtube.errored")).queue();
                }
            });
            mw.setTimeoutHandler(() -> e.editMessage(context.getTranslated("youtube.timeout")).setActionRows(Set.of()).queue());
            e.replyModal(Modal.create(modalId, context.getTranslated("youtube.modal.header"))
                    .addActionRow(TextInput.create(YOUTUBE_MODAL_TEXT,
                            context.getTranslated("youtube.modal.input"), TextInputStyle.SHORT).build()
                    ).build()).complete();
            mw.create();
        });
        bw.setTimeoutHandler(() -> message.editMessage(context.getTranslated("youtube.timeout")).setActionRows(Set.of()).queue());
        bw.create();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (deleted.get()) return;
        List<SearchManager.SearchResult.SearchEntry> eList = new ArrayList<>();
        for (SearchManager.SearchResult.SearchEntry entry : result.getEntries()) {
            if (deleted.get()) break;
            AudioTrack track = managerMuzyki.getAudioTracks(entry.getUrl()).stream().findFirst().orElse(null);
            if (track == null) continue;
            SearchManager.SearchResult res = new SearchManager.SearchResult();
            res.addEntry(entry.getTitle(), entry.getUrl(), track.getDuration(), null);
            eList.add(res.getEntries().get(0));
        }
        if (deleted.get()) return;
        odp = generateResultMessage(eList, context.getTranslated("youtube.message.header", arg), true);
        hook.editOriginal(odp.tresc).queue(a -> {}, b -> {});
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!hasFullDjPerms(context.getMember(), guildDao)) {
            context.replyEphemeral(context.getTranslated("play.dj"));
            return false;
        }
        return super.permissionCheck(context);
    }

    private Wiadomosc generateResultMessage(List<SearchManager.SearchResult.SearchEntry> entries, String header, boolean durations) {
        StringBuilder sb = new StringBuilder(header).append("\n");
        int liczba = 0;
        for (SearchManager.SearchResult.SearchEntry entry : entries) {
            liczba++;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("**").append(liczba).append("**: ").append(entry.getTitle());
            if (durations) sb2.append(" (").append(entry.getDurationAsInt() == Units.DURATION_MS_UNKNOWN ? "LIVE" : entry.getDuration()).append(")");
            sb2.append("\n");
            if (sb.length() + sb2.length() > 2000) {
                liczba--;
                break;
            }
            else sb.append(sb2);
        }
        return new Wiadomosc(sb.toString(), liczba);
    }

    @AllArgsConstructor
    private static class Wiadomosc {
        private final String tresc;
        private final int liczba;
    }
}
