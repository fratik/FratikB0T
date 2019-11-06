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

package pl.fratik.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pl.fratik.core.deserializer.ScheduleContentDeserializer;
import pl.fratik.core.deserializer.ScheduleContentSerializer;

import java.util.List;

@Table("schedule")
@GIndex({"id", "data"})
@Data
@AllArgsConstructor
public class Schedule implements DatabaseEntity {

    @PrimaryKey
    private final int id;
    private long data;
    private final String scheduledBy;
    private final Akcja akcja;
    @JsonSerialize(using = ScheduleContentSerializer.class)
    @JsonDeserialize(using = ScheduleContentDeserializer.class)
    private final ScheduleContent content;

    @Override
    @JsonIgnore
    public String getTableName() {
        return "scheduler";
    }

    @Data
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Przypomnienie extends ScheduleContent {
        private String osoba;
        private String tresc;
        private List<String> murl;
    }

}
