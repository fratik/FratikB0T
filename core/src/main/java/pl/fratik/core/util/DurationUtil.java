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

import lombok.Getter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtil {

    private DurationUtil() {}

    private static Instant getDuration(List<DateGroup> dList) {
        if (dList.isEmpty()) return null;
        return dList.get(0).getDates().isEmpty() ? null : dList.get(0).getDates()
                .get(dList.get(0).getDates().size() - 1).toInstant();
    }

    private static String getParsableString(String input) {
        Matcher matcher = Pattern.compile("(\\d{1,2}[ydhms])", Pattern.CASE_INSENSITIVE).matcher(input);
        if (!matcher.find()) return input;
        List<String> teksty = new ArrayList<>();
        do {
            for (int i = 0; i < matcher.groupCount(); i++) {
                teksty.add(matcher.group(i + 1).replaceAll("(\\d{1,2})([ydhms])", "$1 $2"));
            }
        } while (matcher.find());
        return (input.replaceAll("(\\d{1,2})([ydhms])", "") + " " +
                String.join(" and ", teksty)).trim();
    }

    private static String trimResponse(List<DateGroup> dateGroups, String str) {
        String pl = "\u200b\u200b\u200bdatatextiooiooioo\u200b\u200b\u200b";
        for (DateGroup dg : dateGroups) {
            str = str.replace(dg.getText(), pl);
        }
        str = str.replace(pl + " and " + pl, "");
        str = str.replace("\u200b\u200b\u200bdatatextiooiooioo\u200b\u200b\u200b", "");
        return str;
    }

    public static Response parseDuration(String input) {
        input = getParsableString(input);
        input = input.replaceAll("\\by\\b", "year")
                .replaceAll("\\bd\\b", "day")
                .replaceAll("\\bh\\b", "hour")
                .replaceAll("\\bm\\b", "minute")
                .replaceAll("\\bs\\b", "seconds");
        List<DateGroup> dList = new PrettyTimeParser().parseSyntax(input);
        if (dList.isEmpty()) return new Response(null, input);
        return new Response(getDuration(dList), trimResponse(dList, input).trim());
    }

    public static String humanReadableFormat(long millis, boolean excludeSeconds) {
        org.joda.time.Duration durejszon = new org.joda.time.Duration(millis);
        PeriodFormatter formatter;
        if (excludeSeconds) {
            formatter = new PeriodFormatterBuilder()
                    .appendYears()
                    .appendSuffix("y ")
                    .appendWeeks()
                    .appendSuffix("w ")
                    .appendDays()
                    .appendSuffix("d ")
                    .appendHours()
                    .appendSuffix("h ")
                    .appendMinutes()
                    .appendSuffix("m ")
                    .toFormatter();
        } else {
            formatter = new PeriodFormatterBuilder()
                    .appendYears()
                    .appendSuffix("y ")
                    .appendWeeks()
                    .appendSuffix("w ")
                    .appendDays()
                    .appendSuffix("d ")
                    .appendHours()
                    .appendSuffix("h ")
                    .appendMinutes()
                    .appendSuffix("m ")
                    .appendSeconds()
                    .appendSuffix("s ")
                    .toFormatter();
        }
        return formatter.print(durejszon.toPeriod().normalizedStandard()).replaceAll(" 0[wdhms]", "");
    }

    public static String humanReadableFormatMillis(long millis) {
        double round = round(millis);
        String str;
        if (round >= 1000) {
            round(millis / 1000d);
            str = round + "s";
        }
        else str = round + "ms";
        if (str.length() == 5) str = str.replaceFirst(".(\\d)(m?s)", ".$10$2");
        return str;
    }

    private static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.DOWN);
        return bd.doubleValue();
    }

    @Getter
    public static class Response {
        private final Instant doKiedy;
        private final String tekst;

        Response(Instant doKiedy, String tekst) {
            this.doKiedy = doKiedy;
            this.tekst = tekst;
        }
    }
}
