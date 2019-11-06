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

package pl.fratik.core.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import org.slf4j.LoggerFactory;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.List;

public class ScheduleDao implements Dao<Schedule> {

    private final EventBus eventBus;
    private final PgMapper<Schedule> mapper;

    public ScheduleDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(Schedule.class);
        this.eventBus = eventBus;
    }

    @Override
    public Schedule get(String id) {
        return mapper.load(Integer.parseInt(id)).orElse(null);
    }

    public List<Schedule> getByDate(long data) {
        return mapper.loadManyBySubkey("data->>'data'", String.valueOf(data));
    }

    public boolean delete(String id) {
        return mapper.delete(Integer.parseInt(id)).orElse(false);
    }

    public Schedule createNew(long data, String scheduledBy, Akcja akcja, ScheduleContent content) {
        return new Schedule(getNextId(), data, scheduledBy, akcja, content);
    }

    @Override
    public List<Schedule> getAll() {
        return mapper.loadAll();
    }

    @Override
    public void save(Schedule toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    private int getNextId() {
        List<Schedule> schList = mapper.loadRaw("SELECT * FROM %s ORDER BY id DESC;");
        int max = 0;
        for (Schedule sch : schList)
            max = Math.max(sch.getId(), max);
        return max + 1;
    }

}
