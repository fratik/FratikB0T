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

package pl.fratik.moderation.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import pl.fratik.moderation.entity.Case;
import pl.fratik.core.entity.Kara;
import pl.fratik.moderation.entity.CaseBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class CaseDeserializer extends StdDeserializer<List<Case>> {

    private static final String VALIDTO = "validTo";

    protected CaseDeserializer() {
        this(null);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    protected CaseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Case> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object[] caseMaybeArr = p.readValueAs(Object[].class);
        ArrayList<Case> caseList = new ArrayList<>();
        for (Object caseMaybe : caseMaybeArr) {
            LinkedHashMap elements = (LinkedHashMap) caseMaybe;
            TemporalAccessor timestamp;
            try {
                timestamp = Instant.ofEpochMilli(((Long) elements.get("timestamp")));
            } catch (ClassCastException ignored) {
                timestamp = Instant.ofEpochMilli(((Integer) elements.get("timestamp")));
            }
            Kara kara = Kara.getByNum((int) elements.get("kara"));
            Case aCase = new CaseBuilder().setUser((String) elements.get("userId"))
                    .setGuild((String) elements.get("guildId")).setCaseId((int) elements.get("caseId"))
                    .setTimestamp(timestamp).setMessageId((String) elements.get("messageId"))
                    .setKara(Objects.requireNonNull(kara)).createCase();
            aCase.setReason((String) elements.get("reason"));
            aCase.setIssuerId((String) elements.get("issuerId"));
            aCase.setValid((boolean) elements.get("valid"));
            try {
                aCase.setValidTo(Instant.ofEpochMilli(((Long) elements.get(VALIDTO))), true);
            } catch (ClassCastException ignored) {
                if ((Integer) elements.get(VALIDTO) != 0)
                    aCase.setValidTo(Instant.ofEpochMilli(((Integer) elements.get(VALIDTO))), true);
            }
            caseList.add(aCase);
        }
        return caseList;
    }
}
