/*
 * Copyright (C) 2020 FratikB0T Contributors
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

import org.joda.time.Period;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class DurationUtilTest {

    @Test
    public void parseReasonDays() {
        DurationUtil.Response res = DurationUtil.parseDuration("bo tak 1d");
        Instant in = getInstant(0, 0, 0, 1, 0, 0, 0, 0);
        asserts(res, in, "bo tak");
    }

    @Test
    public void parseReasonMonthsHoursMinutes() {
        DurationUtil.Response res = DurationUtil.parseDuration("1m chuj wie czemu 2h 3m 4s");
        Instant in = getInstant(0, 1, 0, 0, 2, 3, 4, 0);
        asserts(res, in, "chuj wie czemu");
    }

    @Test
    public void parseReasonMinutes() {
        DurationUtil.Response res = DurationUtil.parseDuration("mee 6m");
        Instant in = getInstant(0, 0, 0, 0, 0, 6, 0, 0);
        asserts(res, in, "mee");
    }

    @Test
    public void parseLongMix1() {
        DurationUtil.Response res = DurationUtil.parseDuration("1 hour sprzedam opla 20 minutes w dieselu 40 seconds");
        Instant in = getInstant(0, 0, 0, 0, 1, 20, 40, 0);
        asserts(res, in, "sprzedam opla w dieselu");
    }

    @Test
    public void parseLongMix2() {
        DurationUtil.Response res = DurationUtil.parseDuration("1 year sprzedam 2 months teslę 4 weeks hours 8 days w 16 hours gazie 32 minutes days 64 seconds");
        Instant in = getInstant(1, 2, 4, 8, 16, 32, 64, 0);
        asserts(res, in, "sprzedam teslę hours w gazie days");
    }

    @Test
    public void parseReasonMix1() {
        DurationUtil.Response res = DurationUtil.parseDuration("21h papaj 37m");
        Instant in = getInstant(0, 0, 0, 0, 21, 37, 0, 0);
        asserts(res, in, "papaj");
    }

    @Test
    public void parseBackwards() {
        DurationUtil.Response res = DurationUtil.parseDuration("1 minutes 1 hours");
        Instant in = getInstant(0, 0, 0, 0, 1, 1, 0, 0);
        asserts(res, in, "");
    }

    @Test
    public void parseEmpty() {
        DurationUtil.Response res = DurationUtil.parseDuration("mogę tu nasrać cokolwiek nie  nawet  2  spacje");
        asserts(res, null, "mogę tu nasrać cokolwiek nie  nawet  2  spacje");
    }

    @Test
    public void parseTwicePL() {
        DurationUtil.Response res = DurationUtil.parseDuration("30d masz bana na 30 dni");
        Instant in = getInstant(0, 0, 0, 30, 0, 0, 0, 0);
        asserts(res, in, "masz bana na 30 dni");
    }

    @Test
    public void parseTwiceEN() {
        DurationUtil.Response res = DurationUtil.parseDuration("30d you're banned for 30 days");
        Instant in = getInstant(0, 0, 0, 30, 0, 0, 0, 0);
        asserts(res, in, "you're banned for 30 days");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseTooLongThrows() {
        DurationUtil.parseDuration("412412412h");
        // się nie wykona
    }

    @Test
    public void parse0() {
        DurationUtil.Response res = DurationUtil.parseDuration("0d");
        Instant in = getInstant(0, 0, 0, 0, 0, 0, 0, 0);
        asserts(res, in, "");
    }

    private Instant getInstant(int years, int months, int weeks, int days,
                               int hours, int minutes, int seconds, int millis) {
        org.joda.time.Instant instaa = org.joda.time.Instant.now();
        Period p = new Period(years, months, weeks, days, hours, minutes, seconds, millis);
        return Instant.ofEpochMilli(instaa.plus(p.toDurationFrom(instaa)).getMillis());
    }


    private void asserts(DurationUtil.Response res, Instant in, String s) {
        assertEquals(s, res.getTekst());
        if (in == null) assertNull(res.getDoKiedy());
        else assertEquals(in.getEpochSecond(), res.getDoKiedy().getEpochSecond());
    }
}
