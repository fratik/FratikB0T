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

package pl.fratik.stats.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;

@Table("messagesStats")
@GIndex("guildId")
@Data
@AllArgsConstructor
public class MessagesStats implements DatabaseEntity {

    @PrimaryKey
    private final String uniqid;
    private final long date;
    private final String guildId;
    private int count;

    public MessagesStats(long date, String guildId) {
        this.date = date;
        this.guildId = guildId;
        uniqid = date + guildId;
    }

    @Override
    @JsonIgnore
    public String getTableName() {
        return "messagesStats";
    }

}
