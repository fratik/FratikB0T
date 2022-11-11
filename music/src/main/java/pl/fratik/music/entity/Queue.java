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

package pl.fratik.music.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Data;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.serializer.QueueDeserializer;
import pl.fratik.music.serializer.QueueSerializer;

import java.util.ArrayList;
import java.util.List;

@Table("queue")
@GIndex("id")
@Data
@JsonSerialize(using = QueueSerializer.class)
@JsonDeserialize(using = QueueDeserializer.class)
public class Queue implements DatabaseEntity {

    @PrimaryKey
    private final String id;
    private List<Piosenka> piosenki = new ArrayList<>();
    private boolean autoZapisane;
    private MessageChannel announceChannel;
    private Piosenka aktualnaPiosenka;
    private long aktualnaPozycja;
    private boolean pauza;
    private RepeatMode repeatMode;
    private AudioChannel voiceChannel;
    private int volume;

    public void autoSave(ManagerMuzykiSerwera mms) {
        autoZapisane = true;
        piosenki = mms.getKolejka();
        announceChannel = mms.getAnnounceChannel();
        aktualnaPiosenka = mms.getAktualnaPiosenka();
        aktualnaPozycja = mms.getPositionLong();
        pauza = !mms.isPlaying();
        repeatMode = mms.getRepeatMode();
        voiceChannel = mms.getChannel();
        volume = mms.getVolume();
    }

    @Override
    @JsonIgnore
    public String getTableName() {
        return "queue";
    }
}
