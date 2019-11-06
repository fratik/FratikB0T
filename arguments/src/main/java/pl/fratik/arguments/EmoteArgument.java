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

import com.vdurmont.emoji.EmojiManager;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.entity.Emoji;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmoteArgument extends Argument {

    private final ShardManager shardManager;
    private static final Pattern EMOJI_REGEX = Pattern.compile("<(a?):(\\w{2,32}):(\\d{17,19})>");

    EmoteArgument(ShardManager shardManager) {
        name = "emote";

        this.shardManager = shardManager;
    }

    @Override
    public Emoji execute(@NotNull ArgumentContext context) {
        Optional<Emote> emotka = shardManager.getEmotes().stream()
                .filter(emote -> context.getArg().equals(emote.getName()) || context.getArg().equals(emote.getId()) ||
                        context.getArg().equals(emote.getAsMention())).findFirst();
//        EmojiUtils.countEmojis(context.getArg())
        long id = getEmoteId(context.getArg());
        if (id == 0) {
            if (EmojiManager.isEmoji(context.getArg())) return new Emoji(EmojiManager.getByUnicode(context.getArg()).getUnicode());
            return null;
        }
        return emotka.map(emote -> new Emoji((EmoteImpl) emote))
                .orElseGet(() -> new Emoji(id, name, (JDAImpl) context.getEvent().getJDA())
                        .setAnimated(isAnimated(context.getArg())));
    }

    private boolean isAnimated(String arg) {
        Matcher m = EMOJI_REGEX.matcher(arg);
        if (!m.matches()) return false;
        try {
            return Boolean.parseBoolean(m.group(1));
        } catch (Exception e) {
            return false;
        }
    }

    private long getEmoteId(String arg) {
        Matcher m = EMOJI_REGEX.matcher(arg);
        if (!m.matches()) return 0;
        try {
            return Long.parseLong(m.group(3));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Emoji execute(String argument, Tlumaczenia tlumaczenia, Language language) {
        Optional<Emote> emotka = shardManager.getEmotes().stream()
                .filter(emote -> argument.equals(emote.getName()) || argument.equals(emote.getId()) ||
                        argument.equals(emote.getAsMention())).findFirst();
        long id = getEmoteId(argument);
        if (id == 0) {
            if (EmojiUtils.isEmoji(argument)) return new Emoji(argument);
            return null;
        }
        return new Emoji((EmoteImpl) emotka.orElse(new EmoteImpl(id, (JDAImpl) null).setAnimated(isAnimated(argument))));
    }
}