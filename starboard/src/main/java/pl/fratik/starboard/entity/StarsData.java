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

package pl.fratik.starboard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;

import java.util.HashMap;
import java.util.Map;

@Table("starboard")
@GIndex("id")
@Data
@AllArgsConstructor
public class StarsData implements DatabaseEntity {

    public StarsData(String id) {
        this.id = id;
    }

    @PrimaryKey
    private String id;
    private Map<String, StarData> starData = new HashMap<>();
    private String starboardChannel = "";
    private String starEmoji = "\u2b50";
    private int starThreshold = 1;

    @Override
    @JsonIgnore
    public String getTableName() {
        return "stars";
    }

}
