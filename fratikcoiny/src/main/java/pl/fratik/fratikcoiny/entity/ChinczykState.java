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

package pl.fratik.fratikcoiny.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.entity.DatabaseEntity;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Table("chinczykState")
@Data
@AllArgsConstructor
@GIndex({"id", "userId", "timestamp"})
public class ChinczykState implements DatabaseEntity {
    @PrimaryKey
    private final String channelId;
    private final String state; //base64 zakodowany stan gry

    public ChinczykState(String channelId, ByteArrayOutputStream capturedState) {
        this.channelId = channelId;
        state = Base64.getEncoder().encodeToString(capturedState.toByteArray());
    }

    @Transient
    @Override
    @JsonIgnore
    public String getTableName() {
        return "chinczykState";
    }
}
