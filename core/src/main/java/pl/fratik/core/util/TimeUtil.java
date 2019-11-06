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

package pl.fratik.core.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    private TimeUtil() {}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Convert a millisecond duration to a string format
     *
     * @param millis
     *      A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis, final boolean showMS) {
        if (millis <= 0) {
            return "-";
        }

        final long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        final StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days);
            sb.append("d ");
        }
        if (hours > 0) {
            sb.append(String.format("%02d", hours));
            sb.append("h ");
        }
        if (minutes > 0) {
            sb.append(String.format("%02d", minutes));
                sb.append("min ");
        }
        if (seconds > 0) {
            sb.append(String.format("%02d", seconds));
            sb.append("s");
        }
        if ((seconds <= 0) && (millis > 0) && showMS) {
            sb.append(String.format("%02d", millis));
            sb.append("ms");
        }

        return sb.toString();
    }

    public static String getStringFromMillis(final Number millis) {
        return FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis.longValue()), ZoneId.of("GMT")));
    }

    public static String getStringFromISO8601Duration(String dur) {
        return getStringFromMillis(TimeUnit.MILLISECONDS.convert(Duration.parse(dur).get(ChronoUnit.SECONDS), TimeUnit.SECONDS));
    }
}
