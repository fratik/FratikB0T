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

package pl.fratik.core.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.reflect.TypeToken;
import pl.fratik.core.entity.ScheduleContent;
import pl.fratik.core.manager.implementation.ManagerModulowImpl;
import pl.fratik.core.util.GsonUtil;

import java.util.Base64;
import java.util.Map;

public class ScheduleContentDeserializer extends StdDeserializer<ScheduleContent> {

    protected ScheduleContentDeserializer() {
        this(null);
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    protected ScheduleContentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ScheduleContent deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            byte[] b64 = p.readValueAs(String.class).getBytes();
            byte[] decoded = Base64.getDecoder().decode(b64);
            Map<String, String> xd = GsonUtil.fromJSON(decoded, new TypeToken<Map<String, String>>() {});
            Class<?> klasa;
            //noinspection SpellCheckingInspection
            try { //NOSONAR
                klasa = ManagerModulowImpl.moduleClassLoader.loadClass(xd.get("clazz"));
            } catch (Exception e) {
                klasa = Class.forName(xd.get("clazz"));
            }
            if (!ScheduleContent.class.isAssignableFrom(klasa)) throw new IllegalStateException("widzisz chyba");
            return (ScheduleContent) GsonUtil.fromJSON(xd.get("objekt"), klasa);
        } catch (Exception e) {
            return null;
        }
    }
}
