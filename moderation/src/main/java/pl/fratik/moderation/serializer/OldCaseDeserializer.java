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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import pl.fratik.core.entity.Kara;
import pl.fratik.moderation.entity.Dowod;
import pl.fratik.moderation.entity.OldCase;
import pl.fratik.moderation.entity.OldCaseBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class OldCaseDeserializer extends StdDeserializer<List<OldCase>> {

    private static final String VALIDTO = "validTo";

    protected OldCaseDeserializer() {
        this(null);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    protected OldCaseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<OldCase> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<LinkedHashMap<String, Object>> cases = p.readValueAs(new TypeReference<List<LinkedHashMap<String, Object>>>(){});
        ArrayList<OldCase> caseList = new ArrayList<>();
        for (LinkedHashMap<String, Object> elements : cases) {
            TemporalAccessor timestamp;
            try {
                timestamp = Instant.ofEpochMilli(((Long) elements.get("timestamp")));
            } catch (ClassCastException ignored) {
                timestamp = Instant.ofEpochMilli(((Integer) elements.get("timestamp")));
            }
            Kara kara = Kara.getByNum((int) elements.get("kara"));
            OldCase aCase = new OldCaseBuilder().setUser((String) elements.get("userId"))
                    .setGuild((String) elements.get("guildId")).setCaseId((int) elements.get("caseId"))
                    .setTimestamp(timestamp).setMessageId((String) elements.get("messageId"))
                    .setKara(Objects.requireNonNull(kara)).createCase();
            aCase.setReason((String) elements.get("reason"));
            aCase.setIssuerId((String) elements.get("issuerId"));
            aCase.setValid((boolean) elements.get("valid"));
            if (elements.containsKey("ileRazy")) aCase.setIleRazy((Integer) elements.get("ileRazy"));
            if (elements.containsKey("flagi")) {
                try {
                    aCase.setFlagi(OldCase.Flaga.getFlagi((Long) elements.get("flagi")));
                } catch (ClassCastException ignored) {
                    aCase.setFlagi(OldCase.Flaga.getFlagi((Integer) elements.get("flagi")));
                }
            }
            try {
                aCase.setValidTo(Instant.ofEpochMilli(((Long) elements.get(VALIDTO))), true);
            } catch (ClassCastException ignored) {
                if ((Integer) elements.get(VALIDTO) != 0)
                    aCase.setValidTo(Instant.ofEpochMilli(((Integer) elements.get(VALIDTO))), true);
            }
            if (elements.containsKey("dmMsgId")) aCase.setDmMsgId((String) elements.get("dmMsgId"));
            List<Dowod> dowody = new ArrayList<>();
            if (elements.containsKey("dowody")) {
                for (Object dowodRaw : (List<?>) elements.get("dowody")) {
                    if (dowodRaw == null) continue;
                    LinkedHashMap<?, ?> dowod = (LinkedHashMap<?, ?>) dowodRaw;
                    long id;
                    Object idRaw = dowod.get("id");
                    if (idRaw instanceof Long) id = (Long) idRaw;
                    else if (idRaw instanceof Integer) id = ((Integer) idRaw).longValue();
                    else if (idRaw.getClass().equals(Long.TYPE)) id = (long) idRaw;
                    else if (idRaw.getClass().equals(Integer.TYPE)) id = (long) ((int) idRaw);
                    else throw new IllegalStateException("Nieprawidłowa klasa ID: " + idRaw.getClass().getName());
                    dowody.add(new Dowod(id, Long.parseUnsignedLong((String) dowod.get("aby")), (String) dowod.get("cnt")));
                }
            }
            aCase.setDowody(dowody);
            caseList.add(aCase);
        }
        return caseList;
    }
}
