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

import com.google.gson.annotations.SerializedName;

public class WatchdogStatsReply extends AbstractReply {
    @SerializedName("staff_rollingDaily")
    private int staffRollingDaily;
    @SerializedName("staff_total")
    private int staffTotal;
    @SerializedName("watchdog_total")
    private int watchdogTotal;
    @SerializedName("watchdog_lastMinute")
    private int watchdogLastMinute;
    @SerializedName("watchdog_rollingDaily")
    private int watchdogRollingDaily;

    public int getStaffRollingDaily() {
        return staffRollingDaily;
    }

    public int getStaffTotal() {
        return staffTotal;
    }

    public int getWatchdogTotal() {
        return watchdogTotal;
    }

    public int getWatchdogLastMinute() {
        return watchdogLastMinute;
    }

    public int getWatchdogRollingDaily() {
        return watchdogRollingDaily;
    }

    @Override
    public String toString() {
        return "WatchdogStatsReply{" +
                "staffRollingDaily=" + staffRollingDaily +
                ", staffTotal=" + staffTotal +
                ", watchdogTotal=" + watchdogTotal +
                ", watchdogLastMinute=" + watchdogLastMinute +
                ", watchdogRollingDaily=" + watchdogRollingDaily +
                "} " + super.toString();
    }
}
