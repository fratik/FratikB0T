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

package pl.fratik.arguments;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;

public class MessageArgument extends Argument {

    MessageArgument() {
        name = "message";
    }

    @Override
    public Message execute(@NotNull ArgumentContext context) {
        Message msg;
        try {
            msg = context.getEvent().getChannel().retrieveMessageById(context.getArg()).complete();
        } catch (Exception e) {
            msg = null;
        }
        return msg;
    }
}