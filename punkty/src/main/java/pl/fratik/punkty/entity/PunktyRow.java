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

package pl.fratik.punkty.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;

@Table("punkty")
@GIndex("id")
@Data
@AllArgsConstructor
public class PunktyRow implements DatabaseEntity {
    @PrimaryKey
    private final String id;
    private String guildId = null;
    private String userId = null;
    private final Typ typ;
    private int punkty;

    PunktyRow(String id, Typ typ) {
        if (id.split("-").length == 2) {
            userId = id.split("-")[0];
            guildId = id.split("-")[1];
        }
        this.id = id;
        this.typ = typ;
    }

    @Override
    @JsonIgnore
    public String getTableName() {
        return "punkty";
    }

    public enum Typ {
        GUILD, MEMBER, USER
    }
}
