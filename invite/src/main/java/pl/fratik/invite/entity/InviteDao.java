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

package pl.fratik.invite.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.List;

public class InviteDao implements Dao<InviteData> {

    private final EventBus eventBus;
    private final PgMapper<InviteData> mapper;

    public InviteDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(InviteData.class);
        this.eventBus = eventBus;
    }

    @Override
    public InviteData get(String primKey) {
        return mapper.load(primKey).orElseGet(() -> newObject(primKey));
    }

    public InviteData get(Member member) {
        return get(member.getUser().getId() + "." + member.getGuild().getId());
    }

    public InviteData get(String userId, String guildId) {
        return get(userId + "." + guildId);
    }

    @Override
    public List<InviteData> getAll() {
        return mapper.loadAll();
    }

    @Override
    public void save(InviteData toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    public void save(InviteData... toCos) {
        for (InviteData tak : toCos) {
            save(tak);
        }
    }

    private InviteData newObject(String id) {
        return new InviteData(id.split("\\.")[0], id.split("\\.")[1]);
    }

}
