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

package pl.fratik.api.entity;

import lombok.Data;

@Data
public class Credits {
    private final String id;
    private final String krotkiPowod;
    private final String dluzszyPowod;
    private final Sociale sociale;

    @Data
    public static class ParsedCredits {
        private final String username;
        private final String discrim;
        private final String avatarUrl;
        private final String krotkiPowod;
        private final String dluzszyPowod;
        private final Sociale sociale;
    }

    @Data
    public static class Sociale {
        private final String twitter;
        private final String facebook;
        private final String instagram;
        private final String reddit;
        private final String steam;
        private final String youtube;
        private final String twitch;
    }
}
