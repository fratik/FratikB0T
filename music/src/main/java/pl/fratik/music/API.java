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

package pl.fratik.music;

import lombok.AccessLevel;
import lombok.Setter;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class API {
    @Setter(AccessLevel.PACKAGE) private static NowyManagerMuzyki mm;

    public static int getConnections() {
        return mm.getLavaClient().getLinks().size();
    }
}
