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

package pl.fratik.moderation.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.entity.Kara;
import pl.fratik.moderation.entity.Case;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CaseSerializer extends StdSerializer<List<Case>> {

    public CaseSerializer() {
        this(null);
    }

    public CaseSerializer(Class<List<Case>> vc) {
        super(vc);
    }

    @Override
    public void serialize(List<Case> caseList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        List<ParsedCase> parsedCaseList = new ArrayList<>();
        for (Case aCase : caseList) {
            ParsedCase pCase = new ParsedCase(aCase.getUserId(), aCase.getGuildId(), aCase.getCaseId(), aCase.getTimestamp(), aCase.getMessageId(), aCase.getType());
            pCase.setReason(aCase.getReason());
            pCase.setIssuerId(aCase.getIssuerId());
            pCase.setValid(aCase.isValid());
            if (aCase.getValidTo() != null) {
                pCase.setValidTo(Instant.from(aCase.getValidTo()).toEpochMilli());
            }
            pCase.setIleRazy(aCase.getIleRazy());
            pCase.setFlagi(Case.Flaga.getRaw(aCase.getFlagi()));
            if (aCase.getDowody() != null && !aCase.getDowody().isEmpty()) {
                pCase.setDowody(aCase.getDowody().stream().map(d -> new ParsedDowod(d.getAttachedBy(), d.getContent()))
                        .collect(Collectors.toList()));
            }
            parsedCaseList.add(pCase);
        }
        jsonGenerator.writeStartArray();
        for (ParsedCase pCase : parsedCaseList) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("issuerId", pCase.getIssuerId());
            jsonGenerator.writeStringField("userId", pCase.getUserId());
            jsonGenerator.writeStringField("guildId", pCase.getGuildId());
            jsonGenerator.writeNumberField("caseId", pCase.getCaseId());
            jsonGenerator.writeNumberField("timestamp", pCase.getTimestamp());
            jsonGenerator.writeStringField("reason", pCase.getReason());
            jsonGenerator.writeStringField("messageId", pCase.getMessageId());
            jsonGenerator.writeNumberField("kara", pCase.getType());
            jsonGenerator.writeBooleanField("valid", pCase.isValid());
            jsonGenerator.writeNumberField("validTo", pCase.getValidTo());
            if (pCase.getIleRazy() != null) jsonGenerator.writeNumberField("ileRazy", pCase.getIleRazy());
            if (pCase.getFlagi() != 0) jsonGenerator.writeNumberField("flagi", pCase.getFlagi());
            if (pCase.getDowody() != null) jsonGenerator.writeObjectField("dowody", pCase.getDowody());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    @Getter
    @AllArgsConstructor
    static class ParsedDowod {
        private final String aBy;
        private final String cnt;
    }
    @Getter
    static class ParsedCase {
        private final String userId;
        private final String guildId;
        private final Long timestamp;
        private final int caseId;
        @Setter
        private boolean valid = false;
        @Setter private long validTo;
        private final int type;
        @Setter private String messageId;
        @Nullable @Setter private String dmMsgId;
        @Nullable
        @Setter private String issuerId;
        @Nullable @Setter private String reason;
        @Nullable @Setter private Integer ileRazy;
        @Setter private long flagi;
        @Setter private List<ParsedDowod> dowody;

        ParsedCase(String userId, String guildId, int caseId, TemporalAccessor timestamp, String messageId, Kara type) {
            this.userId = userId;
            this.guildId = guildId;
            this.caseId = caseId;
            if (timestamp != null) this.timestamp = Instant.from(timestamp).toEpochMilli();
            else this.timestamp = null;
            this.messageId = messageId;
            this.type = type.getNumerycznie();
        }

        public ParsedCase(User user, String guildId, int caseId, TemporalAccessor timestamp, String messageId, Kara type) {
            userId = user.getId();
            this.guildId = guildId;
            this.caseId = caseId;
            if (timestamp != null) this.timestamp = Instant.from(timestamp).toEpochMilli();
            else this.timestamp = null;
            this.messageId = messageId;
            this.type = type.getNumerycznie();
        }

    }
}
