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

package net.hypixel.api.reply;

import net.hypixel.api.util.GameType;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class BoostersReply extends AbstractReply {
    private List<Booster> boosters;
    private BoosterState boosterState;

    public List<Booster> getBoosters() {
        return boosters;
    }

    public BoosterState getBoosterState() {
        return boosterState;
    }

    @Override
    public String toString() {
        return "BoostersReply{" +
                "boosters=" + boosters +
                ", boosterState=" + boosterState +
                "} " + super.toString();
    }

    public static class BoosterState {
        private boolean decrementing;

        public boolean isDecrementing() {
            return decrementing;
        }

        @Override
        public String toString() {
            return "BoosterState{" +
                    "decrementing=" + decrementing +
                    '}';
        }
    }

    public static class Booster {
        private UUID purchaserUuid;
        private double amount;
        private int originalLength;
        private int length;
        private GameType gameType;
        private ZonedDateTime dateActivated;
        private List<UUID> stacked;
        private boolean queuedToStack;

        public UUID getPurchaserUuid() {
            return purchaserUuid;
        }

        public double getAmount() {
            return amount;
        }

        public int getOriginalLength() {
            return originalLength;
        }

        public int getLength() {
            return length;
        }

        public GameType getGameType() {
            return gameType;
        }

        public ZonedDateTime getDateActivated() {
            return dateActivated;
        }

        public List<UUID> getStacked() {
            return stacked;
        }

        public boolean isQueuedToStack() {
            return queuedToStack;
        }

        @Override
        public String toString() {
            return "Booster{" +
                    "purchaserUuid=" + purchaserUuid +
                    ", amount=" + amount +
                    ", originalLength=" + originalLength +
                    ", length=" + length +
                    ", gameType=" + gameType +
                    ", dateActivated=" + dateActivated +
                    ", stacked=" + stacked +
                    ", queuedToStack=" + queuedToStack +
                    '}';
        }
    }
}
