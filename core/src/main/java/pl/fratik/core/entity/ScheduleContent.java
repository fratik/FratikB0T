/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

public class ScheduleContent {

    @Getter protected static final Set<Class<? extends ScheduleContent>> extenders = new HashSet<>();

    protected ScheduleContent() {
        synchronized (this) {
            extenders.add(this.getClass());
        }
    }

}
