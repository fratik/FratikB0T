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

package pl.fratik.moderation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.moderation.serializer.CaseDeserializer;
import pl.fratik.moderation.serializer.CaseSerializer;

import java.util.ArrayList;
import java.util.List;

@Table("cases")
@GIndex("guildId")
@Data
public class CaseRow implements DatabaseEntity {

    @PrimaryKey
    private final String guildId;
    @JsonSerialize(using=CaseSerializer.class)
    @JsonDeserialize(using=CaseDeserializer.class)
    private List<Case> cases = new ArrayList<>();

    @Override
    @JsonIgnore
    public String getTableName() {
        return "cases";
    }

}
