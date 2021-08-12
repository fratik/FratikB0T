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

package pl.fratik.moderation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.entity.OldCase;
import pl.fratik.moderation.entity.OldCaseRow;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public enum Migration {
    V0(null) {
        @Override
        public void migrate(Connection con) throws SQLException, IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Case> cases = new ArrayList<>();
            Map<String, Long> masterMap = new HashMap<>();
            try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM cases;")) {
                ResultSet set = stmt.executeQuery();
                if (!set.isBeforeFirst()) return;
                while (set.next()) {
                    List<Case> newCases = new ArrayList<>();
                    long caseId = 0;
                    long guildId = Long.parseUnsignedLong(set.getString("id"));
                    Set<Long> uzyteNumery = new HashSet<>();
                    for (OldCase data : objectMapper.readValue(set.getString("data"), OldCaseRow.class).getCases()) {
                        EnumSet<Case.Flaga> flagi = EnumSet.noneOf(Case.Flaga.class);
                        flagi.addAll(data.getFlagi().stream().map(f -> Case.Flaga.valueOf(f.name())).collect(Collectors.toSet()));
                        String reason = data.getReason();
                        if (reason != null && reason.startsWith("translate:")) reason = "\\" + reason;
                        caseId = data.getCaseId();
                        while (uzyteNumery.contains(caseId)) {
                            caseId++;
                        }
                        if (caseId != data.getCaseId())
                            log.warn("{}: ID sprawy się nie zgadza! ID odczytane: {}; nowe ID: {}", guildId, data.getCaseId(), caseId);
                        Case c = new Case(CaseDao.getId(guildId, caseId), guildId,
                                Long.parseUnsignedLong(data.getUserId()), caseId,
                                Objects.requireNonNull(data.getTimestamp()), data.getType(), data.isValid(),
                                data.getValidTo(),
                                data.getMessageId() == null ? null : Long.parseUnsignedLong(data.getMessageId()),
                                data.getDmMsgId() == null ? null : Long.parseUnsignedLong(data.getDmMsgId()),
                                data.getIssuerId() == null ? null : Long.parseUnsignedLong(data.getIssuerId()),
                                reason, data.getIleRazy() == null ? 1 : data.getIleRazy(), flagi, data.getDowody(), caseId != data.getCaseId());
                        uzyteNumery.add(caseId);
                        newCases.add(c);
                    }
                    cases.addAll(newCases);
                    masterMap.put(Long.toUnsignedString(guildId), caseId);
                }
            }
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM cases;")) {
                stmt.execute();
            }
            try (PreparedStatement stmt = con.prepareStatement("INSERT INTO cases (id, data) VALUES (?, jsonb_build_object('caseId', ?));")) {
                for (Map.Entry<String, Long> e : masterMap.entrySet()) {
                    stmt.setString(1, e.getKey());
                    stmt.setLong(2, e.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            try (PreparedStatement stmt = con.prepareStatement("INSERT INTO cases (id, data) VALUES (?, to_jsonb(?::jsonb));")) {
                for (Case c : cases) {
                    stmt.setString(1, c.getId());
                    stmt.setString(2, objectMapper.writeValueAsString(c));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    },
    V1("1") {
        @Override
        public void migrate(Connection con) {
            // najnowsza
        }
    };

    public static Migration getNewest() {
        return V1;
    }

    private static final Logger log = LoggerFactory.getLogger(Migration.class);

    @Getter private final String versionKey;

    Migration(String versionKey) {
        this.versionKey = versionKey;
    }

    public static Migration fromVersionName(String preMigrationVersion) {
        for (Migration v : values()) {
            if (Objects.equals(v.versionKey, preMigrationVersion)) return v;
        }
        return null;
    }

    public abstract void migrate(Connection con) throws SQLException, IOException;
}
