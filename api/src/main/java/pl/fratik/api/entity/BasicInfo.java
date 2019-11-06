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

package pl.fratik.api.entity;

import lombok.Data;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;

import java.util.List;

@Data
@SuppressWarnings("squid:S1170")
public class BasicInfo {
    private final String username;
    private final String discriminator;
    private final String id;
    private final String description;
    private final String avatarUrl;
    private final boolean privateMode;
    private final String inviteLink;
    private final List<Language> languages;
    private final boolean inBotGuild = Globals.inFratikDev;
    private final String ownerId = String.valueOf(Globals.ownerId);
    private final String prefix = Ustawienia.instance.prefix;
}
