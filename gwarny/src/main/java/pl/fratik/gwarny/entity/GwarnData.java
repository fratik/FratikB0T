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

package pl.fratik.gwarny.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;

import java.util.ArrayList;
import java.util.List;

@Table("gwarny")
@GIndex("id")
@Data
@AllArgsConstructor
public class GwarnData implements DatabaseEntity {
    @PrimaryKey
    private final String id;
    private final List<Gwarn> gwarny;

    public GwarnData(String id) {
        this.id = id;
        gwarny = new ArrayList<>();
    }

    @Override
    @JsonIgnore
    public String getTableName() {
        return "gwarny";
    }

}
