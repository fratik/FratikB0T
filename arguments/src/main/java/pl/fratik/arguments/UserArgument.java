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

package pl.fratik.arguments;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserArgument extends Argument {

    private static final Pattern MENTION_REGEX = Pattern.compile("<@!?(\\d{17,18})>");
    private static final Pattern TAG_REGEX = Pattern.compile("(.{2,32}#\\d{4})");

    protected final ShardManager shardManager;

    public UserArgument(ShardManager shardManager) {
        name = "user";
        this.shardManager = shardManager;
    }

    @Override
    public User execute(@NotNull ArgumentContext context) {
        try {
            try { //NOSONAR
                if (shardManager.retrieveUserById(context.getArg()).complete() != null)
                    return shardManager.retrieveUserById(context.getArg()).complete();
            } catch (Exception e1) {
                // nic
            }
            Matcher matcher = MENTION_REGEX.matcher(context.getArg());
            if (matcher.matches()) {
                return shardManager.retrieveUserById(matcher.group(1)).complete();
            }
            Matcher matcher1 = TAG_REGEX.matcher(context.getArg());
            if (matcher1.matches()) {
                List<User> ul = shardManager.getUsers().stream().filter(u -> u.getAsTag().equals(matcher1.group(1)))
                        .collect(Collectors.toList());
                if (ul.size() == 1) return ul.get(0);
            }
        } catch (Exception ignored) {
            /* lul */
        }
        return null;
    }

    @Override
    public User execute(String argument, Tlumaczenia tlumaczenia, Language language) {
        try {
            if (shardManager.getUserById(argument) != null)
                return shardManager.getUserById(argument);
            Matcher matcher = MENTION_REGEX.matcher(argument);
            if (matcher.matches()) {
                return shardManager.retrieveUserById(matcher.group()).complete();
            }
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }
}