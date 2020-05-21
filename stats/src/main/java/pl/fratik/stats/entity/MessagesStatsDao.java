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

package pl.fratik.stats.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.List;

public class MessagesStatsDao implements Dao<MessagesStats> {
    private final EventBus eventBus;
    private final PgMapper<MessagesStats> mapper;

    public MessagesStatsDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(MessagesStats.class);
        this.eventBus = eventBus;
    }

    @Override
    public MessagesStats get(String primKey) {
        throw new UnsupportedOperationException("użyj get(Date, String)");
    }

    public MessagesStats get(long date, String id) {
        return mapper.loadManyBySubkey("data->>'guildId'", id).stream().filter(ms -> ms.getDate() == date).findAny().orElse(newObject(date, id));
    }

    @Override
    public List<MessagesStats> getAll() {
        return mapper.loadAll();
    }

    public List<MessagesStats> getAllForGuild(String guild) {
        return mapper.loadManyBySubkey("data->>'guildId'", guild);
    }

    public List<MessagesStats> getAllForDate(long date) {
        return mapper.loadManyBySubkey("data->>'date'", String.valueOf(date));
    }

    @Override
    public void save(MessagesStats toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    private MessagesStats newObject(long date, String guildId) {
        return new MessagesStats(date, guildId);
    }
}
