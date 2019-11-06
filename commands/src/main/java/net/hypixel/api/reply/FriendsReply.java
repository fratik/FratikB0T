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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class FriendsReply extends AbstractReply {
    private List<FriendShip> records;

    public List<FriendShip> getFriendShips() {
        return records;
    }

    @Override
    public String toString() {
        return "FriendsReply{" +
                "records=" + records +
                "} " + super.toString();
    }

    static class FriendShip {

        private UUID uuidSender, uuidReceiver;
        private ZonedDateTime started;

        public UUID getUuidSender() {
            return uuidSender;
        }

        public UUID getUuidReceiver() {
            return uuidReceiver;
        }

        public ZonedDateTime getStarted() {
            return started;
        }

        @Override
        public String toString() {
            return "FriendShip{" +
                    "uuidSender=" + uuidSender +
                    ", uuidReceiver=" + uuidReceiver +
                    ", started=" + started +
                    '}';
        }
    }
}
