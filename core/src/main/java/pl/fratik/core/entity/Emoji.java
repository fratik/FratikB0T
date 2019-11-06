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

package pl.fratik.core.entity;

import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ListedEmote;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Emoji extends EmoteImpl implements ListedEmote {
    private final Long id;
    private final String unicode;

    private String name;

    public Emoji(EmoteImpl e) {
        super(e.getIdLong(), Objects.requireNonNull(e.getGuild()), e.isFake());
        setUser(e.hasUser() ? e.getUser() : null);
        setManaged(e.isManaged());
        setAnimated(e.isAnimated());
        name = e.getName();
        id = e.getIdLong();
        unicode = null;
    }

    public Emoji(String unicode) {
        super(0L, (JDAImpl) null);
        this.unicode = unicode;
        id = null;
    }

    public Emoji(long id, String name, JDAImpl jda) {
        super(id, jda);
        this.name = name;
        this.id = id;
        unicode = null;
    }

    @NotNull
    @Override
    public String getName() {
        if (name != null) return name;
        return unicode;
    }

    @Override
    public long getIdLong() {
        if (id != null)
            return id;
        throw new IllegalStateException("Emotka to unicode");
    }

    @NotNull
    @Override
    public String getId() {
        if (id == null) return unicode;
        return Long.toUnsignedString(getIdLong());
    }

    public boolean isUnicode() {
        return unicode != null;
    }

    @NotNull
    @Override
    public String getImageUrl() {
        if (!isUnicode()) return "https://cdn.discordapp.com/emojis/" + getId() + (isAnimated() ? ".gif" : ".png");
        return "https://twemoji.maxcdn.com/2/72x72/" + String.join("-", unicodeToHex(unicode)) + ".png";
    }

    private List<String> unicodeToHex(String unicode) {
        List<String> hexy = new ArrayList<>();
        for (int i = 0; i < unicode.length(); i += 2)
            hexy.add(Integer.toHexString(unicode.codePointAt(i)));
        return hexy;
    }

    public static Emoji resolve(String emote, JDA jda) {
        if (EmojiUtils.isEmoji(emote)) return new Emoji(emote);
        else {
            try {
                return new Emoji((EmoteImpl) Objects.requireNonNull(jda.getEmoteById(emote)));
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static Emoji resolve(String emote, ShardManager jda) {
        if (EmojiUtils.isEmoji(emote)) return new Emoji(emote);
        else {
            try {
                return new Emoji((EmoteImpl) Objects.requireNonNull(jda.getEmoteById(emote)));
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Override
    public Emoji setAnimated(boolean animated) {
        super.setAnimated(animated);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Emoji))
            return false;

        Emoji oEmote = (Emoji) obj;
        return getId().equals(oEmote.getId()) && this.isUnicode() == oEmote.isUnicode() && getName().equals(oEmote.getName());
    }

    @Override
    public int hashCode() {
        if (isUnicode()) {
            return Long.hashCode(id);
        } else {
            List<String> unicodehexes = unicodeToHex(getName());
            int liczba = 0;
            for (String hexStr : unicodehexes) {
                liczba += Integer.parseInt(hexStr, 16);
            }
            return Long.hashCode(liczba);
        }
    }
}
