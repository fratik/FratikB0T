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

package pl.fratik.liczek.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.tlumaczenia.Language;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; //TODO: Wywal niepotrzebne importy

@Table("liczek")
@GIndex({"id"})
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Liczek implements DatabaseEntity {
    
    public Liczek(String id) {
        this.id = id;
    }
    
    @PrimaryKey
    private final String id;
    private Integer liczekLiczba = 0;
    private String liczekOstatniaOsoba;

    @Override
    @JsonIgnore
    @Transient
    public String getTableName() {
        return "liczek";
    }
}
