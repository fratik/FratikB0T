/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
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

package pl.fratik.moderation.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.eventbus.EventBus;
import gg.amy.pgorm.PgMapper;
import gg.amy.pgorm.PgStore;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.entity.Dao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.moderation.events.NewCaseEvent;
import pl.fratik.moderation.events.UpdateCaseEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static pl.fratik.moderation.serializer.CaseSerializer.*;

public class CaseDao implements Dao<Case> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final EventBus eventBus;
    private final PgStore pgStore;
    private final PgMapper<Case> mapper;
    private final Map<String, ReentrantLock> locks = Collections.synchronizedMap(new HashMap<>());

    public CaseDao(ManagerBazyDanych managerBazyDanych, EventBus eventBus) {
        if (managerBazyDanych == null) throw new IllegalStateException("managerBazyDanych == null");
        pgStore = managerBazyDanych.getPgStore();
        mapper = pgStore.mapSync(Case.class);
        this.eventBus = eventBus;
    }

    @Override
    public Case get(String id) {
        return mapper.load(id).orElse(null);
    }

    public Case getLocked(String id) {
        lock(id);
        return mapper.load(id).orElseGet(() -> {
            unlock(id);
            return null;
        });
    }

    public List<Case> getCasesByGuild(Guild g) {
        return getCasesByGuild(g.getId());
    }

    public List<Case> getCasesByGuild(String id) {
        return mapper.loadRaw("SELECT * FROM %s WHERE data->>'" + GUILD_ID + "' = ? " +
                "ORDER BY (data->>'" + CASE_NUMBER + "')::bigint DESC;", id);
    }

    public List<Case> getCasesByMember(Member mem) {
        return getCasesByMember(mem.getUser(), mem.getGuild());
    }

    public List<Case> getCasesByMember(User user, Guild guild) {
        return mapper.loadRaw("SELECT * FROM %s WHERE data->>'" + GUILD_ID + "' = ? AND data->>'" + USER_ID + "' = ? " +
                        "ORDER BY (data->>'" + CASE_NUMBER + "')::bigint DESC;",
                guild.getId(), user.getId());
    }

    @Override
    public List<Case> getAll() {
        throw new UnsupportedOperationException();
    }

    public List<Case> getAllNeedsUpdate() {
        return mapper.loadRaw("SELECT * FROM %s WHERE data @> jsonb_build_object('" + NEEDS_UPDATE + "', true);");
    }

    /**
     * Tworzy nową sprawę w bazie danych (wersja dla wszystkiego innego)
     * @param toModify Sprawa do edytowania w tej transakcji (np {@code setValid(false)} na istniejącej sprawie); może być {@code null}
     * @param aCase Sprawa do dodania, nigdy {@code null} - pola ID i caseNumber są ignorowane
     * @param sendDm Czy wysłać wiadomość prywatną o sprawie?
     * @return Dodana sprawa z poprawnym ID
     */
    public Case createNew(Case toModify, Case aCase, boolean sendDm) {
        return createNew(toModify, aCase, sendDm, null, null);
    }

    /**
     * Tworzy nową sprawę w bazie danych (wersja dla warnów)
     * @param toModify Sprawa do edytowania w tej transakcji (np {@code setValid(false)} na istniejącej sprawie); może być {@code null}
     * @param aCase Sprawa do dodania, nigdy {@code null} - pola ID i caseNumber są ignorowane
     * @param sendDm Czy wysłać wiadomość prywatną o sprawie?
     * @param channel Kanał gdzie bot powinien wysłać informację zwrotną o akcji za warny
     * @param language Język członka serwera
     * @return Dodana sprawa z poprawnym ID
     */
    public Case createNew(Case toModify, Case aCase, boolean sendDm, MessageChannel channel, Language language) {
        lock(aCase.getGuildId());
        try {
            AtomicReference<Case> createdCase = new AtomicReference<>();
            AtomicReference<Exception> ex = new AtomicReference<>();
            pgStore.sql(con -> {
                boolean autoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                try {
                    if (toModify != null) {
                        try (PreparedStatement c = con.prepareStatement("UPDATE " + aCase.getTableName() +
                                " SET data = to_jsonb(?::jsonb) WHERE id = ?;")) {
                            c.setString(1, OBJECT_MAPPER.writeValueAsString(toModify));
                            c.setObject(2, toModify.getId());
                            c.execute();
                        }
                    }
                    long caseId = getNextCaseId(aCase.getGuildId(), con);
                    final String pk = getId(aCase.getGuildId(), caseId);
                    final ObjectNode jsonTree = OBJECT_MAPPER.valueToTree(aCase);
                    jsonTree.put(CASE_NUMBER, caseId);
                    try (PreparedStatement c = con.prepareStatement("INSERT INTO " + aCase.getTableName() +
                            " (id, data) values (?, to_jsonb(?::jsonb));")) {
                        c.setObject(1, pk);
                        c.setString(2, jsonTree.toString());
                        c.execute();
                    }
                    Case newCase = OBJECT_MAPPER.treeToValue(jsonTree, Case.class);
                    con.commit();
                    if (toModify != null) {
                        eventBus.post(new DatabaseUpdateEvent(toModify));
                        eventBus.post(new UpdateCaseEvent(toModify, false));
                    }
                    eventBus.post(new DatabaseUpdateEvent(newCase));
                    eventBus.post(new NewCaseEvent(newCase, sendDm, channel, language));
                    createdCase.set(newCase);
                } catch (Exception e) {
                    con.rollback();
                    ex.set(e);
                } finally {
                    con.setAutoCommit(autoCommit);
                }
            });
            Exception exception = ex.get();
            if (exception != null || createdCase.get() == null) {
                if (exception instanceof RuntimeException) throw (RuntimeException) exception;
                throw new IllegalStateException("Nie udało się utworzyć sprawy!", exception);
            }
            return createdCase.get();
        } finally {
            unlock(aCase.getGuildId());
        }
    }

    @NotNull
    public static String getId(@NotNull Guild guild, long caseId) {
        return getId(guild.getIdLong(), caseId);
    }

    @NotNull
    public static String getId(@NotNull String guildId, long caseId) {
        return getId(Long.parseUnsignedLong(guildId), caseId);
    }

    @NotNull
    public static String getId(long guildId, long caseId) {
        return guildId + "." + caseId;
    }

    public void lock(long guildId) {
        lock(Long.toUnsignedString(guildId));
    }

    public void lock(Case aCase) {
        if (aCase.getId() == null) throw new IllegalArgumentException("nie można zablokować na null ID");
        lock(aCase.getId());
    }

    private void lock(String id) {
        getLock(id).lock();
    }

    public void unlock(long guildId) {
        unlock(Long.toUnsignedString(guildId));
    }

    public void unlock(Case aCase) {
        if (aCase.getId() == null) throw new IllegalArgumentException("nie można odblokować na null ID");
        unlock(aCase.getId());
    }

    private void unlock(String id) {
        synchronized (locks) {
            ReentrantLock lock = locks.get(id);
            if (lock == null) return;
            lock.unlock();
            if (lock.getHoldCount() == 0) locks.remove(id);
        }
    }

    private ReentrantLock getLock(String id) {
        synchronized (locks) {
            return locks.computeIfAbsent(id, i -> new ReentrantLock());
        }
    }

    @Override
    public void save(Case toCos) {
        save(toCos, false);
    }

    public void save(Case toCos, boolean internalChange) { //update()
        if (toCos.getId() == null) throw new IllegalArgumentException("użyj createNew(Case)");
        if (!toCos.getId().matches("^\\d+\\.\\d+$")) throw new IllegalArgumentException("nieprawidłowy format ID");
        mapper.save(toCos);
        eventBus.post(new DatabaseUpdateEvent(toCos));
        eventBus.post(new UpdateCaseEvent(toCos, internalChange));
    }

    public long getNextCaseId(long guildId) {
        AtomicLong id = new AtomicLong(-1);
        pgStore.sql(con -> id.set(getNextCaseId(guildId, con)));
        return id.get();
    }

    protected long getNextCaseId(long guildId, Connection con) throws SQLException {
        long caseId;
        lock(guildId);
        try (PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO cases (id, data) VALUES (?, jsonb_build_object('caseId', 1)) ON CONFLICT (id) DO " +
                        "UPDATE SET data = cases.data || jsonb_build_object('caseId', (cases.data->'caseId')::integer + 1) " +
                        "RETURNING data->'caseId';")) {
            String x = Long.toUnsignedString(guildId);
            stmt.setString(1, x);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            caseId = resultSet.getLong(1);
        } finally {
            unlock(guildId);
        }
        return caseId;
    }

    public void reset(long guildId) {
        pgStore.sql(con -> {
            lock(guildId);
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM cases WHERE data->>'" + GUILD_ID + "' = ? OR id = ?;")) {
                stmt.setString(1, Long.toUnsignedString(guildId));
                stmt.setString(2, Long.toUnsignedString(guildId));
                stmt.execute();
            } finally {
                unlock(guildId);
            }
        });
    }
}
