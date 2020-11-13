/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

@Table("giveaways")
@GIndex({"id"})
@Data
@AllArgsConstructor
public class GiveawayConfig implements DatabaseEntity {
    public GiveawayConfig() { }

    public GiveawayConfig(String id) {
        this.id = id;
    }

    @PrimaryKey
    private String id;

    private String guildId;

    private String organizator;
    private String prize;
    private String messageId;
    private String channelId;

    private long start;
    private long end;

    private int wygranychOsob = 1;

    private boolean aktywna = true;

    private List<String> winners = new ArrayList<>();

    @Transient
    @JsonIgnore
    @Override
    public String getTableName() {
        return "giveaways";
    }

    @Transient
    @JsonIgnore
    public String getDecodeGuild() {
        return id.split("-")[1];
    }

    @Transient
    @JsonIgnore
    public String getDecodeId() {
        return id.split("-")[0];
    }

}
