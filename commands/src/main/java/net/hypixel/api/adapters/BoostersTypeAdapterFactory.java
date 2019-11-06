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

package net.hypixel.api.adapters;

import net.hypixel.api.reply.BoostersReply;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BoostersTypeAdapterFactory<T extends BoostersReply.Booster> extends CustomizedTypeAdapterFactory<T> {

    public BoostersTypeAdapterFactory(Class<T> customizedClass) {
        super(customizedClass);
    }

    @Override
    protected void afterRead(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();

        JsonElement stackedElement = obj.get("stacked");
        if (stackedElement != null) {
            if (stackedElement.isJsonPrimitive()) {
                if (stackedElement.getAsJsonPrimitive().isBoolean()) {
                    obj.addProperty("queuedToStack", stackedElement.getAsJsonPrimitive().getAsBoolean());
                    obj.remove("stacked");
                }
            }
        }
    }
}