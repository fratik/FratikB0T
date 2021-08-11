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

package pl.fratik.moderation.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.fratik.core.entity.Kara;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.Dowod;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import static pl.fratik.moderation.serializer.CaseSerializer.*;

public class CaseDeserializer extends StdDeserializer<Case> {

    protected CaseDeserializer() {
        this(null);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    protected CaseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Case deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode elements = p.readValueAsTree();
        TemporalAccessor timestamp = Instant.ofEpochMilli(elements.get(TIMESTAMP).asLong());
        Kara kara = Kara.getByNum(elements.get(KARA).asInt());
        Case aCase = new Case.Builder(elements.get(GUILD_ID).asLong(), elements.get(USER_ID).asLong(), timestamp, kara).build();
        aCase.setReason(elements.get(REASON).toString());
        if (elements.has(ISSUER_ID)) aCase.setIssuerId(elements.get(ISSUER_ID).asLong());
        if (elements.has(MESSAGE_ID)) aCase.setMessageId(elements.get(MESSAGE_ID).asLong());
        aCase.setValid(elements.get(VALID).asBoolean());
        if (elements.has(COUNT)) aCase.setIleRazy(elements.get(COUNT).asInt());
        aCase.setFlagi(Case.Flaga.getFlagi(elements.get("flagi").asInt()));
        if (elements.has(VALID_TO)) aCase.setValidTo(Instant.ofEpochMilli(elements.get(VALID_TO).asLong()));
        if (elements.has(DM_MESSAGE_ID)) aCase.setDmMsgId(elements.get(DM_MESSAGE_ID).asLong());
        List<Dowod> dowody = new ArrayList<>();
        if (elements.has(DOWODY)) {
            for (JsonNode dowodRaw : elements.get(DOWODY)) {
                if (dowodRaw == null) continue;
                ObjectNode dowod = (ObjectNode) dowodRaw;
                dowody.add(new Dowod(dowod.get(DOWOD_ID).asLong(), dowod.get(DOWOD_ATTACHED_BY).asLong(), dowod.get(DOWOD_CONTENT).toString()));
            }
        }
        aCase.setDowody(dowody);
        return aCase;
    }
}
