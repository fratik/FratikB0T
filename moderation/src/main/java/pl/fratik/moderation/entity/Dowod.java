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

package pl.fratik.moderation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.CheckReturnValue;
import java.util.Comparator;
import java.util.List;

@Data
@AllArgsConstructor
public class Dowod {
    private final long id;
    private final long attachedBy;
    private final String content;

    public static long getNextId(List<Dowod> dowody) {
        long lastId = 0;
        for (Dowod dowod : dowody.stream().sorted(Comparator.comparingLong(Dowod::getId)).toArray(Dowod[]::new)) {
            if (lastId < dowod.getId()) // sanity check
                lastId = dowod.getId();
        }
        return lastId + 1;
    }

    public static Dowod getDowodById(int id, List<Dowod> dowody) {
        for (Dowod dowod : dowody) if (dowod.getId() == id) return dowod;
        return null;
    }

    @CheckReturnValue
    public RestAction<User> retrieveAttachedBy(ShardManager sm) {
        return sm.retrieveUserById(attachedBy);
    }
}
