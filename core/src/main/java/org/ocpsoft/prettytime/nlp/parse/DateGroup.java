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

package org.ocpsoft.prettytime.nlp.parse;

import java.util.Date;
import java.util.List;

/**
 * Represents a {@link Date} instanced parsed out of natural language text.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com>Lincoln Baxter, III</a>
 */
public interface DateGroup
{
    /**
     * Get the line in which this {@link DateGroup} was found.
     */
    int getLine();

    /**
     * Get the text fragment parsed into this {@link DateGroup}.
     */
    String getText();

    /**
     * Get the {@link Date} to which this {@link DateGroup} recurs.
     */
    Date getRecursUntil();

    /**
     * Get the starting position of this {@link DateGroup} in the language text.
     */
    int getPosition();

    /**
     * Get all {@link Date} instances parsed from the language text.
     */
    List<Date> getDates();

    /**
     * Return <code>true</code> if this {@link DateGroup} is a recurring event.
     */
    boolean isRecurring();

    /**
     * If this {@link DateGroup} is recurring, return the interval in milliseconds with which this {@link DateGroup}
     * recurs, otherwise return -1;
     */
    long getRecurInterval();
}