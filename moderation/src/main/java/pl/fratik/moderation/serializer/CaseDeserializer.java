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
import java.util.EnumSet;
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
        long guildId = elements.get(GUILD_ID).asLong();
        long userId = elements.get(USER_ID).asLong();
        long caseNumber = elements.get(CASE_NUMBER).asLong();
        String reason;
        Long issuerId, messageId, dmMessageId;
        int ileRazy;
        TemporalAccessor validTo;
        boolean valid = elements.get(VALID).asBoolean();
        Kara kara = Kara.getByNum(elements.get(KARA).asInt());
        if (kara == null) throw new NullPointerException();
        EnumSet<Case.Flaga> flagi = Case.Flaga.getFlagi(elements.get(FLAGI).asInt());
        if (elements.has(REASON)) reason = elements.get(REASON).asText();
        else reason = null;
        if (elements.has(ISSUER_ID)) issuerId = elements.get(ISSUER_ID).asLong();
        else issuerId = null;
        if (elements.has(MESSAGE_ID)) messageId = elements.get(MESSAGE_ID).asLong();
        else messageId = null;
        if (elements.has(COUNT)) ileRazy = elements.get(COUNT).asInt();
        else ileRazy = 1;
        if (elements.has(VALID_TO)) validTo = Instant.ofEpochMilli(elements.get(VALID_TO).asLong());
        else validTo = null;
        if (elements.has(DM_MESSAGE_ID)) dmMessageId = elements.get(DM_MESSAGE_ID).asLong();
        else dmMessageId = null;
        List<Dowod> dowody = new ArrayList<>();
        if (elements.has(DOWODY)) {
            for (JsonNode dowodRaw : elements.get(DOWODY)) {
                if (dowodRaw == null) continue;
                ObjectNode dowod = (ObjectNode) dowodRaw;
                dowody.add(new Dowod(dowod.get(DOWOD_ID).asLong(), dowod.get(DOWOD_ATTACHED_BY).asLong(),
                        dowod.get(DOWOD_CONTENT).asText()));
            }
        }
        return new Case(guildId + "." + caseNumber, guildId, userId, caseNumber, timestamp, kara, valid, validTo,
                messageId, dmMessageId, issuerId, reason, ileRazy, flagi, dowody,
                elements.has(NEEDS_UPDATE) && elements.get(NEEDS_UPDATE).asBoolean());
    }
}
