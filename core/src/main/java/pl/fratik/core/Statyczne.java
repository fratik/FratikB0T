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

package pl.fratik.core;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Statyczne {

    private Statyczne() {}

    public static final String WERSJA;

    public static final String CORE_VERSION;

    public static final String DBOTS = "https://discordbots.org/bot/338359366891732993";

    public static final List<String> BOTLIST_GUILDS = Collections.unmodifiableList(Lists.newArrayList(
            "110373943822540800", // Discord Bots
            "264445053596991498", // Discord Bot List
            "459854572534366208", // ListCord
            "387812458661937152"  // botlist.space
    ));

    public static final Date startDate; //NOSONAR

    static {
        String version = Statyczne.class.getPackage().getImplementationVersion();

        if (version == null)
            WERSJA = "?.?.?";
        else
            WERSJA = version;

        CORE_VERSION = WERSJA;

        startDate = new Date();
    }
}
