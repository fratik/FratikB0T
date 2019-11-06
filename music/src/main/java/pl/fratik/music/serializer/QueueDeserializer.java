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

package pl.fratik.music.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.entity.PiosenkaImpl;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.RepeatMode;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueueDeserializer extends StdDeserializer<Queue> {

    @Setter private static NowyManagerMuzyki managerMuzyki;
    @Setter private static ShardManager shardManager;

    protected QueueDeserializer() {
        this(null);
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    protected QueueDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Queue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        LinkedHashMap elements = p.readValueAs(LinkedHashMap.class);
        Queue queue = new Queue((String) elements.get("id"));
        List piosenkaList = (List) elements.get("piosenki");
        for (Object piosenkaMapRaw : piosenkaList) {
            Piosenka piosenka = resolveSong(piosenkaMapRaw);
            if (piosenka != null) queue.getPiosenki().add(piosenka);
        }
        if ((Boolean) elements.get("autoZapisane")) {
            try {
                queue.setAktualnaPozycja((Long) elements.get("aktualnaPozycja"));
            } catch (ClassCastException e) {
                queue.setAktualnaPozycja((Integer) elements.get("aktualnaPozycja"));
            }
            queue.setAutoZapisane(true);
            queue.setAktualnaPiosenka(resolveSong(elements.get("aktualnaPiosenka")));
            queue.setAnnounceChannel(shardManager.getTextChannelById((String) elements.get("announceChannel")));
            if (elements.get("paused") != null) queue.setPauza((Boolean) elements.get("paused"));
            queue.setRepeatMode(RepeatMode.valueOf((String) elements.get("repeatMode")));
            queue.setVoiceChannel(shardManager.getVoiceChannelById((String) elements.get("voiceChannel")));
            if (elements.get("volume") != null) queue.setVolume((Integer) elements.get("volume"));
        } else queue.setAutoZapisane(false);
        return queue;
    }

    private Piosenka resolveSong(Object piosenkaMapRaw) {
        if (!(piosenkaMapRaw instanceof Map)) return null;
        Map piosenkaMap = (Map) piosenkaMapRaw;
        List<AudioTrack> trackList = managerMuzyki.getAudioTracks((String) piosenkaMap.get("track"));
        if (trackList.isEmpty()) return null;
        return new PiosenkaImpl((String) piosenkaMap.get("requester"), trackList.get(0),
                Language.valueOf((String) piosenkaMap.get("requesterLanguage")));
    }
}
