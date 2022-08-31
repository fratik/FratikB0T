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

import com.google.common.eventbus.EventBus;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.TimeUtil;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class QueueCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private static final String QUEMLI = "queue.embed.live";
    @Setter private static SearchManager searchManager;

    public QueueCommand(NowyManagerMuzyki managerMuzyki, EventWaiter eventWaiter, EventBus eventBus) {
        this.managerMuzyki = managerMuzyki;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "queue";
        requireConnection = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms == null || (mms.getKolejka().isEmpty() && mms.getAktualnaPiosenka() == null)) {
            context.replyEphemeral(context.getTranslated("queue.empty"));
            return;
        }
        InteractionHook hook = context.defer(false);
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        pages.add(new FutureTask<>(() -> generateEmbed(mms.getAktualnaPiosenka(), context)));
        for (Piosenka piosenka : mms.getKolejka()) {
            pages.add(new FutureTask<>(() -> generateEmbed(piosenka, context)));
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus).create(hook);
    }

    private static EmbedBuilder generateEmbed(Piosenka piosenka, NewCommandContext context) {
        AudioTrackInfo info = piosenka.getAudioTrack().getInfo();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(context.getTranslated("queue.embed.header"), info.uri);
        eb.setTitle(info.title, piosenka.getAudioTrack().getInfo().uri);
        piosenka.fillThumbnailURL(searchManager);
        eb.setImage(piosenka.getThumbnailURL());
        eb.addField(context.getTranslated("queue.embed.added.by"), piosenka.getRequester(), true);
        eb.addField(context.getTranslated("queue.embed.length"), info.isStream ?
                context.getTranslated(QUEMLI) : TimeUtil.getStringFromMillis(info.length), true);
        eb.setColor(Color.decode("#ff0000"));
        return eb;
    }
}
