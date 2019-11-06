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

package net.hypixel.api.pets;

import com.google.common.collect.Maps;

import java.util.Map;

class PetStats {

    private final Map<PetType, Pet> petMap = Maps.newHashMap();

    public PetStats(Map<String, Map<String, Object>> petStats) {
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : petStats.entrySet()) {
            try {
                petMap.put(PetType.valueOf(stringMapEntry.getKey()), new Pet(stringMapEntry.getValue()));
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid pet! " + stringMapEntry.getKey());
            }
        }
    }

    public Pet getPet(PetType type) {
        return petMap.get(type);
    }

    public Map<PetType, Pet> getAllPets() {
        return petMap;
    }

    @Override
    public String toString() {
        return "PetStats{" +
                "petMap=" + petMap +
                '}';
    }
}
