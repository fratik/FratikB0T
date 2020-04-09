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

import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang.ArrayUtils;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtil {

    //#region Patterny
    private static final String WSZYSTKO = "(\\d+)( )?(years?|months?|weeks?|days?|hours?|minutes?|min|" +
            "secou?nds?|sec|([ywdhms]))( |$|\\b)";
    private static final Pattern YEARS = Pattern.compile("(\\d+) ?years?");
    private static final Pattern MONTHS = Pattern.compile("(\\d+) ?months?");
    private static final Pattern WEEKS = Pattern.compile("(\\d+) ?weeks?");
    private static final Pattern DAYS = Pattern.compile("(\\d+) ?days?");
    private static final Pattern HOURS = Pattern.compile("(\\d+) ?hours?");
    private static final Pattern MINUTES = Pattern.compile("(\\d+) ?(minutes?|min)");
    private static final Pattern SECONDS = Pattern.compile("(\\d+) ?(secou?nds?|sec)");
    //#endregion Patterny

    private DurationUtil() {}

    private static Instant getDuration(List<DateGroup> dList) {
        if (dList.isEmpty()) return null;
        return dList.get(0).getDates().isEmpty() ? null : dList.get(0).getDates()
                .get(dList.get(0).getDates().size() - 1).toInstant();
    }

    public static Response parseDuration(String input) {
        MutablePeriod period = new MutablePeriod();
        String aaa = getParsableString(input);
        String[] tokeny = aaa.split(" ");
        List<String> reason = new ArrayList<>();
        period.addYears(0);
        period.addMonths(0);
        period.addWeeks(0);
        period.addDays(0);
        period.addHours(0);
        period.addMinutes(0);
        period.addSeconds(0);
        boolean[] parsed = new boolean[] {false, false, false, false, false, false, false};
        for (String token : tokeny) {
            token = token.replace("\u200b\u200btojestkuźwaspacja\u200b\u200b", " ");
            Matcher years = YEARS.matcher(token);
            Matcher months = MONTHS.matcher(token);
            Matcher weeks = WEEKS.matcher(token);
            Matcher days = DAYS.matcher(token);
            Matcher hours = HOURS.matcher(token);
            Matcher minutes = MINUTES.matcher(token);
            Matcher seconds = SECONDS.matcher(token);
            if (!parsed[0] && years.find()) {
                period.addYears(Integer.parseInt(years.group(1)));
                parsed[0] = true;
            }
            else if (!parsed[1] && months.find()) {
                period.addMonths(Integer.parseInt(months.group(1)));
                parsed[1] = true;
            }
            else if (!parsed[2] && weeks.find()) {
                period.addWeeks(Integer.parseInt(weeks.group(1)));
                parsed[2] = true;
            }
            else if (!parsed[3] && days.find()) {
                period.addDays(Integer.parseInt(days.group(1)));
                parsed[3] = true;
            }
            else if (!parsed[4] && hours.find()) {
                period.addHours(Integer.parseInt(hours.group(1)));
                parsed[4] = true;
            }
            else if (!parsed[5] && minutes.find()) {
                period.addMinutes(Integer.parseInt(minutes.group(1)));
                parsed[5] = true;
            }
            else if (!parsed[6] && seconds.find()) {
                period.addSeconds(Integer.parseInt(seconds.group(1)));
                parsed[6] = true;
            } else reason.add(token);
        }
        org.joda.time.Instant instaa = org.joda.time.Instant.now();
        Instant inst = Instant.ofEpochMilli(instaa.plus(period.toDurationFrom(instaa)).getMillis());
        if (inst.toEpochMilli() - instaa.getMillis() >= 63113904000L)
            throw new IllegalArgumentException("2 lata to maks!");
        if (inst.toEpochMilli() == instaa.getMillis()) return new Response(null, input);
        return new Response(inst, String.join(" ", reason).replaceAll(" +", " ").trim());
    }

    private static String getParsableString(String input) {
        Matcher matcher = Pattern.compile(WSZYSTKO).matcher(input);
        if (!matcher.find()) return input;
        List<Tak> tekstyTmp = new ArrayList<>();
        int iloscM = 0;
        do {
            tekstyTmp.add(new Tak(Integer.parseInt(matcher.group(1)), matcher.group(2), matcher.group(3),
                    matcher.group(4) != null));
            if (Objects.equals(matcher.group(4), "m")) iloscM++;
        } while (matcher.find());
        int[][] sraka = new int[tekstyTmp.size()][];
        matcher.reset();
        int i = 0;
        while (matcher.find()) {
            sraka[i++] = new int[] {matcher.start(), matcher.end()};
        }
        ArrayUtils.reverse(sraka);
        List<String> teksty = new ArrayList<>();
        StringBuilder sb = new StringBuilder(input);
        for (int[] bounds : sraka) {
            sb.replace(bounds[0], bounds[1], "");
        }
        teksty.add(sb.toString());
        boolean replacedFirstM = false;
        boolean[] parsed = new boolean[] {false, false/*to akurat nie ma znaczenia*/, false, false, false, false, false};
        for (Tak tekst : tekstyTmp) {
            int number = tekst.getNumber();
            String unit = tekst.getUnit();
            if (tekst.isReplace()) {
                if (tekst.getUnit().equals("y") && !parsed[0]) {
                    parsed[0] = true;
                    unit = tekst.getUnit().replace("y", "years");
                }
                if (tekst.getUnit().equals("m")) {
                    if (iloscM == 2) {
                        if (!replacedFirstM) {
                            unit = tekst.getUnit().replace("m", "months");
                            replacedFirstM = true;
                        } else if (!parsed[5]) {
                            parsed[5] = true;
                            unit = tekst.getUnit().replace("m", "minutes");
                        }
                    } else if (!parsed[5]) {
                        parsed[5] = true;
                        unit = tekst.getUnit().replace("m", "minutes");
                    }
                }
                if (tekst.getUnit().equals("w") && !parsed[2]) {
                    parsed[2] = true;
                    unit = tekst.getUnit().replace("w", "weeks");
                }
                if (tekst.getUnit().equals("d") && !parsed[3]) {
                    parsed[3] = true;
                    unit = tekst.getUnit().replace("d", "days");
                }
                if (tekst.getUnit().equals("h") && !parsed[4]) {
                    parsed[4] = true;
                    unit = tekst.getUnit().replace("h", "hours");
                }
                if (tekst.getUnit().equals("s") && !parsed[6]) {
                    parsed[6] = true;
                    unit = tekst.getUnit().replace("s", "seconds");
                }
            }
            //noinspection SpellCheckingInspection
            teksty.add((number + tekst.getDelim() + unit).replaceAll(" ", "\u200b\u200btojestkuźwaspacja\u200b\u200b"));
            // co ja robię ze swoim życiem
        }
        return String.join(" ", teksty);
    }

//    public static Response parseDuration1(String input) {
//        PeriodFormatter parser = new PeriodFormatterBuilder().append(null, new HumanParser()).toFormatter();
//        Period period = parser.parsePeriod(input);
//        org.joda.time.Instant instaa = org.joda.time.Instant.now();
//        Instant inst = Instant.ofEpochMilli(instaa.plus(period.toDurationFrom(instaa)).getMillis());
//        if (inst.toEpochMilli() - Instant.now().toEpochMilli() >= 63113904000L)
//            throw new IllegalArgumentException("2 lata to maks!");
//        return new Response(inst, trimResponse(input).trim());
//    }

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

    @Data
    private static class Tak {
        private final int number;
        private final String delim;
        private final String unit;
        private final boolean replace;

        public Tak(int number, String delim, String unit, boolean replace) {
            this.number = number;
            this.delim = delim == null ? "" : delim;
            this.unit = unit;
            this.replace = replace;
        }
    }
}
