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

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
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
import java.util.stream.Stream;

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
        aliases = new String[] {"q", "que", "songlist", "songslist", "songlists", "listapiosenek", "listamuzyki", "kolejka"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms == null || (mms.getKolejka().isEmpty() && mms.getAktualnaPiosenka() == null)) {
            context.send(context.getTranslated("queue.empty"));
            return false;
        }
        if (Stream.of(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EMBED_LINKS)
                .allMatch(a -> context.getGuild().getSelfMember().getPermissions(context.getChannel()).contains(a))) {
            Message m = context.getChannel().sendMessage(context.getTranslated("generic.loading")).complete();
            List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
            pages.add(new FutureTask<>(() -> generateEmbed(mms.getAktualnaPiosenka(), context)));
            for (Piosenka piosenka : mms.getKolejka()) {
                pages.add(new FutureTask<>(() -> generateEmbed(piosenka, context)));
            }
            new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(),
                    eventBus).create(m);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(mms.getKolejka().size(), 10); i++) {
                if (i == 0) {
                    sb.append("[__`").append(Strings.padStart(String.valueOf(i + 1), 2, '0'))
                            .append("`__] *")
                            .append(mms.getAktualnaPiosenka().getAudioTrack().getInfo().title
                                    .replaceAll("`", "`" + "\u8203"))
                            .append("* ").append(context.getTranslated("queue.queued.by")).append(" **")
                            .append(mms.getAktualnaPiosenka().getRequester()).append("**\n");
                    sb.append("   └── <").append(mms.getAktualnaPiosenka().getAudioTrack().getInfo().uri).append("> (")
                            .append(mms.getAktualnaPiosenka().getAudioTrack().getInfo().isStream ?
                                    context.getTranslated(QUEMLI) :
                                    TimeUtil.getStringFromMillis(mms.getAktualnaPiosenka().getAudioTrack().getInfo().length))
                            .append(")\n\n");
                    continue;
                }
                sb.append("[__`").append(Strings.padStart(String.valueOf(i + 1), 2, '0'))
                        .append("`__] *")
                        .append(mms.getKolejka().get(i).getAudioTrack().getInfo().title
                                .replaceAll("`", "`\u8203"))
                        .append("* ").append(context.getTranslated("queue.queued.by")).append(" **").append(mms.getKolejka().get(i).getRequester())
                        .append("**\n");
                sb.append("   └── <").append(mms.getKolejka().get(i).getAudioTrack().getInfo().uri).append("> (")
                        .append(mms.getKolejka().get(i).getAudioTrack().getInfo().isStream ?
                                context.getTranslated(QUEMLI) :
                                TimeUtil.getStringFromMillis(mms.getKolejka().get(i).getAudioTrack().getInfo().length))
                        .append(")\n");
                if (i + 1 < Math.min(mms.getKolejka().size(), 10)) sb.append("\n");
            }
            context.send(sb.toString());
        }
        return true;
    }

    private static EmbedBuilder generateEmbed(Piosenka piosenka, CommandContext context) {
        AudioTrackInfo info = piosenka.getAudioTrack().getInfo();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(context.getTranslated("queue.embed.header"), info.uri);
        eb.setTitle(info.title, piosenka.getAudioTrack().getInfo().uri);
        if (piosenka.getThumbnailURL() != null) eb.setImage(piosenka.getThumbnailURL());
        else {
            piosenka.fillThumbnailURL(searchManager);
            eb.setImage(piosenka.getThumbnailURL());
        }
        eb.addField(context.getTranslated("queue.embed.added.by"), piosenka.getRequester(), true);
        eb.addField(context.getTranslated("queue.embed.length"), info.isStream ?
                context.getTranslated(QUEMLI) : TimeUtil.getStringFromMillis(info.length), true);
        eb.setColor(Color.decode("#ff0000"));
        return eb;
    }
}
