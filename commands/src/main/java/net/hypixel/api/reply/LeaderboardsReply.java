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

package net.hypixel.api.reply;

import net.hypixel.api.util.GameType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardsReply extends AbstractReply {
    private Map<GameType, List<Leaderboard>> leaderboards;

    public Map<GameType, List<Leaderboard>> getLeaderboards() {
        return leaderboards;
    }

    @Override
    public String toString() {
        return "LeaderboardsReply{" +
                "leaderboards=" + leaderboards +
                "} " + super.toString();
    }

    static class Leaderboard {

        private String path;
        private String prefix;
        private int count;
        private List<UUID> leaders;
        private String title;

        public Leaderboard() {
        }

        public String getPath() {
            return path;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getCount() {
            return count;
        }

        public List<UUID> getLeaders() {
            return leaders;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return "Leaderboard{" +
                    "path='" + path + '\'' +
                    ", prefix='" + prefix + '\'' +
                    ", count=" + count +
                    ", leaders=" + leaders +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

}
