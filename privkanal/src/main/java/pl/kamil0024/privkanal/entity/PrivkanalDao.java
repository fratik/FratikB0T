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

package pl.kamil0024.privkanal.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.List;

public class PrivkanalDao implements Dao<Privkanal> {

    private final EventBus eventBus;
    private final PgMapper<Privkanal> mapper;

    public PrivkanalDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(Privkanal.class);
        this.eventBus = eventBus;
    }

    @Override
    public Privkanal get(String id) {
        return mapper.load(id).orElseGet(() -> newObject(id));
    }

    public Privkanal get(Guild guild) {
        return get(guild.getId());
    }

    @Override
    public List<Privkanal> getAll() {
        return mapper.loadAll();
    }

    @Override
    public void save(Privkanal toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    private Privkanal newObject(String id) {
        return new Privkanal(id);
    }

    private void delete(String id) {
        mapper.delete(id);
    }

    public void delete(Guild g) {
        delete(g.getId());
    }

}
