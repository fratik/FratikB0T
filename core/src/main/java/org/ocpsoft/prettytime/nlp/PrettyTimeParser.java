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

package org.ocpsoft.prettytime.nlp;

import com.joestelmach.natty.Parser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.util.*;
import java.util.Map.Entry;

/**
 * A utility for parsing natural language date and time expressions. (e.g. "Let's get lunch at two pm",
 * "I did it 3 days ago")
 * <p>
 * <b>Usage:</b>
 * <p>
 * <code>
 * PrettyTimeParser p = new PrettyTimeParser();<br/>
 * List&lt;Date&gt; parsed = p.parse("I'll be there at two");<br/>
 * //result: Date - 2:00PM
 * <p>
 * </code>
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com>Lincoln Baxter, III</a>
 */
public class PrettyTimeParser
{

    private Parser parser;
    private final Map<String, String> translations = new HashMap<>();

    private static final String[] tensNames = {
            "",
            " ten",
            " twenty",
            " thirty",
            " forty",
            " fifty",
            " sixty",
            " seventy",
            " eighty",
            " ninety"
    };

    private static final String[] numNames = {
            "",
            " one",
            " two",
            " three",
            " four",
            " five",
            " six",
            " seven",
            " eight",
            " nine",
            " ten",
            " eleven",
            " twelve",
            " thirteen",
            " fourteen",
            " fifteen",
            " sixteen",
            " seventeen",
            " eighteen",
            " nineteen"
    };

    /**
     * Create a new {@link PrettyTimeParser} with the given {@link TimeZone}.
     */
    private PrettyTimeParser(TimeZone timezone)
    {
        parser = new Parser(timezone);
    }

    /**
     * Create a new {@link PrettyTimeParser} with the current system default {@link TimeZone}.
     */
    public PrettyTimeParser()
    {
        this(TimeZone.getDefault());
        for (int hours = 0; hours < 24; hours++)
            for (int min = 0; min < 60; min++)
                translations.put(provideRepresentation(hours * 100 + min), "" + hours * 100 + min);
        translations.put(provideRepresentation(60), "" + 60);
        translations.put(provideRepresentation(70), "" + 70);
        translations.put(provideRepresentation(80), "" + 80);
        translations.put(provideRepresentation(90), "" + 90);
        translations.put(provideRepresentation(100), "" + 100);

        Set<String> periods = new HashSet<>();
        periods.add("morning");
        periods.add("afternoon");
        periods.add("evening");
        periods.add("night");
        periods.add("am");
        periods.add("pm");
        periods.add("ago");
        periods.add("from now");
    }

    /**
     * Provides a string representation for the number passed. This method works for limited set of numbers as parsing
     * will only be done at maximum for 2400, which will be used in military time format.
     */
    private String provideRepresentation(int number)
    {
        String key;

        if (number == 0)
            key = "zero";
        else if (number < 20)
            key = numNames[number];
        else if (number < 100)
        {
            int unit = number % 10;
            key = tensNames[number / 10] + numNames[unit];
        }
        else
        {
            int unit = number % 10;
            int ten = number % 100 - unit;
            int hundred = (number - ten) / 100;
            if (hundred < 20)
                key = numNames[hundred] + " hundred";
            else
                key = tensNames[hundred / 10] + numNames[hundred % 10] + " hundred";
            if (ten + unit < 20 && ten + unit > 10)
                key += numNames[ten + unit];
            else
                key += tensNames[ten / 10] + numNames[unit];
        }
        return key.trim();
    }

    /**
     * Parse the given language and return a {@link List} with all discovered {@link Date} instances.
     */
    public List<Date> parse(String language)
    {
        language = words2numbers(language);

        List<Date> result = new ArrayList<>();
        List<com.joestelmach.natty.DateGroup> groups = parser.parse(language);
        for (com.joestelmach.natty.DateGroup group : groups) {
            result.addAll(group.getDates());
        }
        return result;
    }

    /**
     * Parse the given language and return a {@link List} with all discovered {@link DateGroup} instances.
     */
    public List<DateGroup> parseSyntax(String language)
    {
        language = words2numbers(language);

        List<DateGroup> result = new ArrayList<>();
        List<com.joestelmach.natty.DateGroup> groups = parser.parse(language);
        Date now = new Date();
        for (com.joestelmach.natty.DateGroup group : groups) {
            result.add(new DateGroupImpl(now, group));
        }
        return result;
    }

    private String words2numbers(String language)
    {
        for (Entry<String, String> entry : translations.entrySet()) {
            language = language.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return language;
    }

    private static class DateGroupImpl implements DateGroup
    {
        private final List<Date> dates;
        private final int line;
        private final int position;
        private final Date recursUntil;
        private final String text;
        private final boolean recurring;
        private final Date now;

        DateGroupImpl(Date now, com.joestelmach.natty.DateGroup group)
        {
            this.now = now;
            dates = group.getDates();
            line = group.getLine();
            position = group.getPosition();
            recursUntil = group.getRecursUntil();
            text = group.getText();
            recurring = group.isRecurring();
        }

        @Override
        public List<Date> getDates()
        {
            return dates;
        }

        @Override
        public int getLine()
        {
            return line;
        }

        @Override
        public int getPosition()
        {
            return position;
        }

        @Override
        public Date getRecursUntil()
        {
            return recursUntil;
        }

        @Override
        public String getText()
        {
            return text;
        }

        @Override
        public boolean isRecurring()
        {
            return recurring;
        }

        @Override
        public long getRecurInterval()
        {
            if (isRecurring())
                return getDates().get(0).getTime() - now.getTime();
            else
                return -1;
        }

    }
}