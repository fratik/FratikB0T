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

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.TimeUtil;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;

import java.awt.*;

public class NowplayingCommand extends MusicCommand {
    private final NowyManagerMuzyki managerMuzyki;
    @Setter private static SearchManager searchManager;

    public NowplayingCommand(NowyManagerMuzyki managerMuzyki) {
        this.managerMuzyki = managerMuzyki;
        name = "nowplaying";
        requireConnection = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        Piosenka piosenka = mms.getAktualnaPiosenka();
        EmbedBuilder eb = generateEmbed(piosenka, context, mms);
        context.reply(eb.build());
    }

    private static EmbedBuilder generateEmbed(Piosenka piosenka, NewCommandContext context, ManagerMuzykiSerwera mms) {
        AudioTrackInfo info = piosenka.getAudioTrack().getInfo();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(context.getTranslated("nowplaying.embed.header"), info.uri);
        eb.setTitle(info.title, piosenka.getAudioTrack().getInfo().uri);
        piosenka.fillThumbnailURL(searchManager);
        eb.setImage(piosenka.getThumbnailURL());
        eb.addField(context.getTranslated("nowplaying.embed.added.by"), piosenka.getRequester(), true);
        eb.addField(context.getTranslated("nowplaying.embed.length"), info.isStream ?
                context.getTranslated("queue.embed.live") :
                mms.getPosition() + " - " + TimeUtil.getStringFromMillis(info.length), true);
        eb.setColor(Color.decode("#ff0000"));
        return eb;
    }
}
