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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.ScheduleContent;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class AutoAkcja extends ScheduleContent {

    @Setter private static ShardManager shardManager;

    private final int caseId;
    private final Kara akcjaDoWykonania;
    private final String guildId;

    @JsonIgnore
    public Case getCase() {
        return Case.getCaseById(caseId, shardManager.getGuildById(guildId));
    }

}


