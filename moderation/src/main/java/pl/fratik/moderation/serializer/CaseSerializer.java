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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.Dowod;

import java.io.IOException;
import java.time.Instant;

public class CaseSerializer extends StdSerializer<Case> {
    public static final String GUILD_ID = "gid";
    public static final String USER_ID = "uid";
    public static final String CASE_NUMBER = "cn";
    public static final String TIMESTAMP = "ts";
    public static final String KARA = "k";
    public static final String VALID = "v";
    public static final String VALID_TO = "vts";
    public static final String MESSAGE_ID = "mid";
    public static final String DM_MESSAGE_ID = "dmmid";
    public static final String ISSUER_ID = "issId";
    public static final String REASON = "r";
    public static final String COUNT = "cnt";
    public static final String FLAGI = "f";
    public static final String DOWODY = "d";
    public static final String DOWOD_ID = "id";
    public static final String DOWOD_ATTACHED_BY = "aby";
    public static final String DOWOD_CONTENT = "cnt";

    public CaseSerializer() {
        this(null);
    }

    public CaseSerializer(Class<Case> vc) {
        super(vc);
    }

    @Override
    public void serialize(Case aCase, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(GUILD_ID, aCase.getGuildId());
        jsonGenerator.writeNumberField(USER_ID, aCase.getUserId());
        jsonGenerator.writeNumberField(CASE_NUMBER, aCase.getCaseNumber());
        jsonGenerator.writeNumberField(TIMESTAMP, Instant.from(aCase.getTimestamp()).toEpochMilli());
        jsonGenerator.writeNumberField(KARA, aCase.getType().getNumerycznie());
        jsonGenerator.writeBooleanField(VALID, aCase.isValid());
        if (aCase.getValidTo() != null) jsonGenerator.writeNumberField(VALID_TO, Instant.from(aCase.getValidTo()).toEpochMilli());
        if (aCase.getMessageId() != null) jsonGenerator.writeNumberField(MESSAGE_ID, aCase.getMessageId());
        if (aCase.getDmMsgId() != null) jsonGenerator.writeNumberField(DM_MESSAGE_ID, aCase.getDmMsgId());
        if (aCase.getIssuerId() != null) jsonGenerator.writeNumberField(ISSUER_ID, aCase.getIssuerId());
        if (aCase.getReason() != null) jsonGenerator.writeStringField(REASON, aCase.getReason());
        if (aCase.getIleRazy() != 1) jsonGenerator.writeNumberField(COUNT, aCase.getIleRazy());
        jsonGenerator.writeNumberField(FLAGI, Case.Flaga.getRaw(aCase.getFlagi()));
        if (!aCase.getDowody().isEmpty()) {
            jsonGenerator.writeFieldName(DOWODY);
            jsonGenerator.writeStartArray();
            for (Dowod dowod : aCase.getDowody()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField(DOWOD_ID, dowod.getId());
                jsonGenerator.writeNumberField(DOWOD_ATTACHED_BY, dowod.getAttachedBy());
                jsonGenerator.writeStringField(DOWOD_CONTENT, dowod.getContent());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }
}
