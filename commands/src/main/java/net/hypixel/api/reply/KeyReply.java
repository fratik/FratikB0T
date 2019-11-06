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

import java.util.UUID;

public class KeyReply extends AbstractReply {
    private Key record;

    public Key getRecord() {
        return record;
    }

    @Override
    public String toString() {
        return "KeyReply{" +
                "record=" + record +
                "} " + super.toString();
    }

    public static class Key {
        private UUID key;
        private UUID ownerUuid;
        private int totalQueries;
        private int queriesInPastMin;

        public UUID getKey() {
            return key;
        }

        public UUID getOwnerUuid() {
            return ownerUuid;
        }

        public int getTotalQueries() {
            return totalQueries;
        }

        public int getQueriesInPastMin() {
            return queriesInPastMin;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "key=" + key +
                    ", ownerUuid=" + ownerUuid +
                    ", totalQueries=" + totalQueries +
                    ", queriesInPastMin=" + queriesInPastMin +
                    '}';
        }
    }
}
