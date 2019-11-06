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
import lavalink.client.io.Link;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlayCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final SearchManager searchManager;
    private final GuildDao guildDao;

    private static final Pattern URLPATTERN = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
            "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})");

    public PlayCommand(NowyManagerMuzyki managerMuzyki, SearchManager searchManager, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.searchManager = searchManager;
        this.guildDao = guildDao;
        name = "play";
        uzycie = new Uzycie("utwor", "string", true);
        aliases = new String[] {"odtworz", "p", "add"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("play.dj"));
            return false;
        }
        GuildVoiceState memVS = context.getMember().getVoiceState();
        GuildVoiceState selfVS = context.getGuild().getSelfMember().getVoiceState();
        if (memVS == null || !memVS.inVoiceChannel()) {
            context.send(context.getTranslated("play.not.connected"));
            return false;
        }
        if (managerMuzyki.getLavaClient().getLink(context.getGuild()).getState() == Link.State.CONNECTED &&
                selfVS != null && !Objects.equals(memVS.getChannel(), selfVS.getChannel())) {
            context.send(context.getTranslated("music.different.channels"));
            return false;
        }
        VoiceChannel kanal = memVS.getChannel();
        if (kanal == null) throw new IllegalStateException("połączony ale nie na kanale, co do");
        EnumSet<Permission> upr = context.getGuild().getSelfMember().getPermissions(kanal);
        if (!Stream.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).allMatch(upr::contains)) {
            context.send(context.getTranslated("play.no.permissions"));
            return false;
        }
        String identifier;
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.isPaused()) {
            context.send(context.getTranslated("play.use.pause"));
            return false;
        }
        if (!URLPATTERN.matcher((String) context.getArgs()[0]).matches()) {
            SearchManager.SearchResult wynik = searchManager.searchYouTube((String) context.getArgs()[0], false);
            if (wynik == null || wynik.getEntries().isEmpty()) {
                context.send(context.getTranslated("play.no.results"));
                return false;
            }
            identifier = wynik.getEntries().get(0).getUrl();
        } else {
            identifier = (String) context.getArgs()[0];
        }
        if (!mms.isConnected()) {
            mms.setAnnounceChannel(context.getChannel());
            mms.connect(kanal);
        }
        if (!mms.isConnected()) return false;
        managerMuzyki.getAudioTracksAsync(identifier, audioTrackList -> {
            if (audioTrackList.isEmpty()) {
                context.send(context.getTranslated("play.not.found"));
                mms.disconnect();
                return;
            }
            if (audioTrackList.size() == 1) {
                AudioTrack at = audioTrackList.get(0);
                SearchManager.SearchResult.SearchEntry result = searchManager
                        .getThumbnail(searchManager.extractIdFromUri(at.getInfo().uri));
                if (result != null) mms.addToQueue(context.getSender(), at, context.getLanguage(), result.getThumbnailURL());
                else mms.addToQueue(context.getSender(), at, context.getLanguage(), null);
            }
            else {
                for (AudioTrack at : audioTrackList) mms.addToQueue(context.getSender(), at, context.getLanguage());
                context.send(context.getTranslated("play.queued.playlist", audioTrackList.size()));
            }
            if (!mms.isPlaying()) mms.play();
            else context.getChannel().sendMessage(new MessageBuilder(context.getTranslated("play.queued",
                    audioTrackList.get(0).getInfo().title)).stripMentions(context.getGuild()).build()).queue();
        });
        return true;
    }
}
