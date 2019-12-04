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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.RepeatMode;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class QueueSerializer extends StdSerializer<Queue> {

    public QueueSerializer() {
        this(null);
    }

    public QueueSerializer(Class<Queue> vc) {
        super(vc);
    }

    @Override
    public void serialize(Queue queue, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        ParsedQueue pqueue = new ParsedQueue(queue.getId(), queue.getPiosenki(), queue.isAutoZapisane(),
                queue.getAnnounceChannel(), queue.getAktualnaPiosenka(), queue.getAktualnaPozycja(),
                queue.isPauza(), queue.getRepeatMode(), queue.getVoiceChannel(), queue.getVolume());
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", pqueue.id);
        jsonGenerator.writeArrayFieldStart("piosenki");
        for (ParsedPiosenka pios : pqueue.piosenki) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("track", pios.track);
            jsonGenerator.writeStringField("requester", pios.requester);
            jsonGenerator.writeStringField("requesterLanguage", pios.requesterLanguage.name());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        if (pqueue.autoZapisane) {
            jsonGenerator.writeBooleanField("autoZapisane", true);
            jsonGenerator.writeStringField("announceChannel", pqueue.announceChannel);
            jsonGenerator.writeObjectField("aktualnaPiosenka", pqueue.aktualnaPiosenka);
            jsonGenerator.writeNumberField("aktualnaPozycja", pqueue.aktualnaPozycja);
            jsonGenerator.writeBooleanField("paused", pqueue.pauza);
            jsonGenerator.writeStringField("repeatMode", pqueue.repeatMode.name());
            jsonGenerator.writeStringField("voiceChannel", pqueue.voiceChannel);
            jsonGenerator.writeNumberField("volume", pqueue.volume);
        } else jsonGenerator.writeBooleanField("autoZapisane", false);
        jsonGenerator.writeEndObject();
    }

    @Getter
    @AllArgsConstructor
    static class ParsedQueue {

        private final String id;
        private List<ParsedPiosenka> piosenki;
        private boolean autoZapisane;
        private String announceChannel;
        private ParsedPiosenka aktualnaPiosenka;
        private long aktualnaPozycja;
        private boolean pauza;
        private RepeatMode repeatMode;
        private String voiceChannel;
        private int volume;

        ParsedQueue(String id, List<Piosenka> piosenki, boolean autoZapisane, //NOSONAR
                    MessageChannel announceChannel, Piosenka aktualnaPiosenka, long aktualnaPozycja,
                    boolean pauza, RepeatMode repeatMode, VoiceChannel voiceChannel, int volume) {
            this.id = id;
            this.piosenki = piosenki.stream().map(p -> new ParsedPiosenka(p.getAudioTrack().getInfo().uri,
                    p.getRequester(), p.getRequesterLanguage())).collect(Collectors.toList());
            this.autoZapisane = autoZapisane;
            this.announceChannel = announceChannel == null ? null : announceChannel.getId();
            this.aktualnaPiosenka = new ParsedPiosenka(aktualnaPiosenka.getAudioTrack().getInfo().uri,
                    aktualnaPiosenka.getRequester(), aktualnaPiosenka.getRequesterLanguage());
            this.aktualnaPozycja = aktualnaPozycja;
            this.pauza = pauza;
            this.repeatMode = repeatMode;
            this.voiceChannel = voiceChannel.getId();
            this.volume = volume;
        }
    }

    @Data
    @AllArgsConstructor
    static class ParsedPiosenka {
        private String track;
        private String requester;
        private Language requesterLanguage;
    }
}
