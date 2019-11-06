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

package pl.fratik.stats.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.List;

public class GuildCountStatsDao implements Dao<GuildCountStats> {
    private final EventBus eventBus;
    private final PgMapper<GuildCountStats> mapper;

    public GuildCountStatsDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(GuildCountStats.class);
        this.eventBus = eventBus;
    }

    @Override
    public GuildCountStats get(String primKey) {
        throw new UnsupportedOperationException("użyj get(Date)");
    }

    public GuildCountStats get(long id) {
        return mapper.load(id).orElseGet(() -> newObject(id));
    }

    @Override
    public List<GuildCountStats> getAll() {
        return mapper.loadAll();
    }

    public List<GuildCountStats> getAll(int limit) {
        return mapper.loadRaw("SELECT * FROM %s ORDER BY data->>'date' DESC LIMIT " + limit + ";");
    }

    @Override
    public void save(GuildCountStats toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    private GuildCountStats newObject(long id) {
        return new GuildCountStats(id);
    }
}
