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

package pl.fratik.core.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.Emoji;

import java.io.IOException;

public class EmojiDeserializer extends StdDeserializer<Emoji> {
    @Setter private static ShardManager shardManager;

    public EmojiDeserializer() {
        this(null);
    }

    public EmojiDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Emoji deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (shardManager != null) return Emoji.resolve(jsonParser.getValueAsString(), shardManager);
        else return null;
    }
}
