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

import java.util.Set;
import java.util.UUID;

public class SessionReply extends AbstractReply {
    private Session session;

    /**
     * Session can be null if
     * 1) The player is in a lobby or offline
     * 2) The player has a staff rank
     *
     * @return The session, or null if either of above reasons is met
     */
    public Session getSession() {
        return session;
    }

    @Override
    public String toString() {
        return "SessionReply{" +
                "session=" + session +
                "} " + super.toString();
    }

    public static class Session {
        /**
         * GameType could be null if a new game has been released
         * and GameType is not yet added to {@link GameType}.
         * <p>
         * This will NOT throw an exception.
         */
        private GameType gameType;
        /**
         * Server name for session
         */
        private String server;
        /**
         * Set of UUIDs of players currently in this session
         */
        private Set<UUID> players;

        public GameType getGameType() {
            return gameType;
        }

        @Deprecated
        public String getServer() {
            return server;
        }

        @Deprecated
        public Set<UUID> getPlayers() {
            return players;
        }

        @Override
        public String toString() {
            return "Session{" +
                    "gameType=" + gameType +
                    ", server='" + server + '\'' +
                    ", players=" + players +
                    '}';
        }
    }
}
