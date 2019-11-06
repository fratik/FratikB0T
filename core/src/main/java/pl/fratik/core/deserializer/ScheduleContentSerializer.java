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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.fratik.core.entity.ScheduleContent;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.StringUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;

public class ScheduleContentSerializer extends StdSerializer<ScheduleContent> {
    public ScheduleContentSerializer() {
        this(null);
    }

    protected ScheduleContentSerializer(Class<ScheduleContent> t) {
        super(t);
    }

    @Override
    public void serialize(ScheduleContent scheduleContent, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        JsonObject jsonDoB64 = new JsonObject();
        jsonDoB64.addProperty("clazz", scheduleContent.getClass().getName());
        JsonObject faktycznyObjekt = new JsonObject();
        for (Field f : scheduleContent.getClass().getDeclaredFields()) {
            try {
                String getterName = "get" + StringUtil.firstLetterUpperCase(f.getName());
                Method m = scheduleContent.getClass().getMethod(getterName);
                Object got = m.invoke(scheduleContent);
                if (got instanceof String) faktycznyObjekt.addProperty(f.getName(), (String) got);
                else if (got instanceof Number) faktycznyObjekt.addProperty(f.getName(), (Number) got);
                else if (got instanceof Boolean) faktycznyObjekt.addProperty(f.getName(), (Boolean) got);
                else if (got.getClass().isEnum()) faktycznyObjekt.addProperty(f.getName(), ((Enum) got).name());
                else if (got instanceof List) {
                    JsonArray lista = new JsonArray();
                    for (Object ob : (List) got) {
                        if (ob instanceof String) lista.add((String) ob);
                        else if (ob instanceof Number) lista.add((Number) ob);
                        else if (ob instanceof Boolean) lista.add((Boolean) ob);
                        else if (ob.getClass().isEnum()) faktycznyObjekt.addProperty(f.getName(), ((Enum) ob).name());
                        else lista.add(GsonUtil.GSON.toJson(got));
                    }
                    faktycznyObjekt.add(f.getName(), lista);
                }
                else faktycznyObjekt.addProperty(f.getName(), GsonUtil.GSON.toJson(got));
            } catch (Exception e) {
                // nie ma takiej metody lub inny problem
                continue;
            }
        }
        jsonDoB64.addProperty("objekt", GsonUtil.toJSON(faktycznyObjekt));
        byte[] b64 = Base64.getEncoder().encode(jsonDoB64.toString().getBytes());
        jsonGenerator.writeString(new String(b64));
    }
}
