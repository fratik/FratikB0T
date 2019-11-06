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
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.fratik.api.entity.User;
import pl.fratik.core.entity.DatabaseEntity;

import java.util.ArrayList;
import java.util.List;

@Table("purge")
@GIndex({"guildId", "channelId"})
@Data
public class Purge implements DatabaseEntity {
    @PrimaryKey private final String id;
    private String channelId;
    private String guildId;
    @Nullable private User purgedBy;
    private long purgedOn;
    @JsonDeserialize(contentAs = ResolvedWiadomosc.class)
    @NotNull private List<Wiadomosc> wiadomosci = new ArrayList<>();
    private boolean deleted = false;
    @Nullable private User deletedBy;
    @Nullable private Long deletedOn;
    @NotNull private PurgePrivacy privacy = PurgePrivacy.PUBLIC;
    private int minPermLevel;
    @Override
    @JsonIgnore
    public String getTableName() {
        return "purge";
    }

    @Getter
    @AllArgsConstructor
    public static class ResolvedWiadomosc implements Wiadomosc {
        @NotNull private final String id;
        @Setter @NotNull private User author;
        @NotNull private final String content;
        private final long createdAt;
        @Nullable private final Long editedAt;
        private final boolean fake;
    }
}
