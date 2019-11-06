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

package pl.fratik.punkty.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;

import java.util.*;

public class PunktyDao implements Dao<PunktyRow> {

    private final EventBus eventBus;
    private final ShardManager shardManager;
    private final PgMapper<PunktyRow> mapper;

    public PunktyDao(ManagerBazyDanych managerBazyDanych, ShardManager shardManager, EventBus eventBus) {
        this.shardManager = shardManager;
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        mapper = managerBazyDanych.getPgStore().mapSync(PunktyRow.class);
        this.eventBus = eventBus;
    }

    @Override
    public PunktyRow get(String id) {
        return mapper.load(id).orElseGet(() -> {
            PunktyRow punktyRow;
            if (id.split("-").length == 2) {
                punktyRow = newObject(id, PunktyRow.Typ.MEMBER);
            } else {
                Optional<User> userOptional;
                Optional<Guild> guildOptional;
                try {
                    User user = shardManager.retrieveUserById(id).complete();
                    userOptional = Optional.ofNullable(user);
                } catch (Exception e) {
                    userOptional = Optional.empty();
                }
                try {
                    Guild guild = shardManager.getGuildById(id);
                    guildOptional = Optional.ofNullable(guild);
                } catch (Exception e) {
                    guildOptional = Optional.empty();
                }
                PunktyRow.Typ user = userOptional.isPresent() ? PunktyRow.Typ.USER : null;
                punktyRow = newObject(id, guildOptional.isPresent() ? PunktyRow.Typ.GUILD : user);
            }
            return punktyRow;
        });
    }

    public PunktyRow get(User user) {
        return get(user.getId());
    }

    public PunktyRow get(Member member) {
        return get(member.getUser().getId() + "-" + member.getGuild().getId());
    }

    public PunktyRow get(Guild guild) {
        return get(guild.getId());
    }

    @Override
    public List<PunktyRow> getAll() {
        return mapper.loadAll();
    }

    public boolean delete(PunktyRow inst) {
        return mapper.delete(inst.getId()).orElse(false);
    }

    @Override
    public void save(PunktyRow toCos) {
        ObjectMapper objMapper = new ObjectMapper();
        String jsoned;
        try { jsoned = objMapper.writeValueAsString(toCos); } catch (Exception ignored) { jsoned = toCos.toString(); }
        LoggerFactory.getLogger(getClass()).debug("Zmiana danych w DB: {} -> {} -> {}", toCos.getTableName(),
                toCos.getClass().getName(), jsoned);
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
    }

    private PunktyRow newObject(String id, PunktyRow.Typ typ) {
        return new PunktyRow(id, typ);
    }

    public LinkedHashMap<String, Integer> getAllUserPunkty() { //NOSONAR
        LinkedHashMap<String, Integer> odp = new LinkedHashMap<>();
        List<PunktyRow> data = mapper.loadManyBySubkey("data->>'typ'", PunktyRow.Typ.USER.name());
        data.sort(Comparator.comparingInt(PunktyRow::getPunkty).reversed());
        for (PunktyRow punkt : data) {
            odp.put(punkt.getId(), punkt.getPunkty());
        }
        return odp;
    }

    public LinkedHashMap<String, Integer> getAllGuildPunkty() { //NOSONAR
        List<PunktyRow> data = mapper.loadManyBySubkey("data->>'typ'", PunktyRow.Typ.GUILD.name());
        data.sort(Comparator.comparingInt(PunktyRow::getPunkty).reversed());
        LinkedHashMap<String, Integer> mapInt = new LinkedHashMap<>();

        data.forEach(element -> {
            String id = element.getId();
            int punkty = element.getPunkty();
            mapInt.merge(id, punkty, Integer::sum);
        });

        return mapInt;
    }

    public Map<String, Integer> getTopkaPoziomow(Guild serwer) {
        List<PunktyRow> data = mapper.loadManyBySubkey("data->>'guildId'", serwer.getId());
        data.sort(Comparator.comparingInt(this::calculateLevelFromConfig).reversed());
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();

        data.forEach(element -> {
            String id = element.getUserId();
            int poziom = calculateLevel(element.getPunkty());
            if (id == null || poziom == 0) return;
            map.put(id, poziom);
        });

        return map;
    }

    public Map<String, Integer> getTotalPoints(User user) {
        List<PunktyRow> data = mapper.loadManyBySubkey("data->>'userId'", user.getId());
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();

        data.forEach(element -> {
            String id = element.getGuildId();
            int punkty = element.getPunkty();
            if (id == null || punkty == 0) return;
            map.put(id, punkty);
        });

        return map;
    }

    public Map<String, Integer> getTopkaPunktow(Guild serwer) {
        List<PunktyRow> data = mapper.loadManyBySubkey("data->>'guildId'", serwer.getId());
        data.sort(Comparator.comparingInt(PunktyRow::getPunkty).reversed());
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();

        data.forEach(element -> {
            if (map.size() >= 10) return;
            String id = element.getUserId();
            int punkty = element.getPunkty();
            if (id == null || punkty == 0) return;
            map.put(id, punkty);
        });

        return map;
    }

    private int calculateLevel(int points) {
        return (int) Math.floor(0.1 * Math.sqrt((double) points * 4));
    }

    private int calculateLevelFromConfig(PunktyRow punktyRow) {
        return calculateLevel(punktyRow.getPunkty());
    }

}
