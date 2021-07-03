/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
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

package pl.fratik.core.entity;

import net.dv8tion.jda.api.entities.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigField {
    boolean dontDisplayInSettings() default false;

    Entities holdsEntity() default Entities.NULL;

    enum Entities {
        GUILD, USER, ROLE, CHANNEL, /* MESSAGE,*/ EMOJI, NULL, COMMAND, STRING;

        public static Class<?> resolveEntity(Entities holdsEntity) {
            switch (holdsEntity) {
                case NULL: return null;
                case ROLE: return Role.class;
                case USER: return User.class;
                case GUILD: return Guild.class;
                case EMOJI: return Emoji.class;
//                case MESSAGE: return Message.class
                case CHANNEL: return GuildChannel.class;
                case COMMAND:
                case STRING:
                    return String.class;
            }
            return null;
        }
    }
}
