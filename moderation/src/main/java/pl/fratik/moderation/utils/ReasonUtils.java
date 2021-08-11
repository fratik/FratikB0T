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

package pl.fratik.moderation.utils;

import pl.fratik.moderation.entity.Case;

import java.util.ArrayList;
import java.util.List;

public class ReasonUtils {
    private ReasonUtils() {}

    public static void parseFlags(Case c, String reason, Case.Flaga... ignore) {
        String[] splatReason = reason.split(" ");
        List<String> parsedReason = new ArrayList<>();
        for (String r : splatReason) {
            Case.Flaga f = Case.Flaga.resolveFlag(r, ignore);
            if (f == null) parsedReason.add(r);
            else {
                if (c.getFlagi().contains(f)) parsedReason.add(r);
                else c.getFlagi().add(f);
            }
        }
        String r = String.join(" ", parsedReason);
        if (r.startsWith("translate:")) r = "\\" + r;
        c.setReason(r);
    }
}
