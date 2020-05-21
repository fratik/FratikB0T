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

package pl.fratik.moderation.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.entity.Akcja;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Schedule;
import pl.fratik.core.entity.ScheduleDao;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Getter
@ToString
@AllArgsConstructor
public class Case {

    private static CasesDao casesDao;
    private static ScheduleDao scheduleDao;

    @NotNull  private final String userId;
    @NotNull  private final String guildId;
    @Nullable private final TemporalAccessor timestamp;
    @NotNull  private final Kara type;
              private final int caseId;
    @Setter   private       boolean valid = true;
    @Nullable private       TemporalAccessor validTo;
    @Setter   private       String messageId;
    @Nullable private       String issuerId;
    @Setter
    @Nullable private       String reason;
    @Setter
    @Nullable private       Integer ileRazy;
    @Setter
    @NotNull  private       EnumSet<Flaga> flagi = EnumSet.noneOf(Flaga.class);

    Case(@NotNull String userId, @NotNull String guildId, int caseId, @Nullable TemporalAccessor timestamp, String messageId, @NotNull Kara type) {
        this.userId = userId;
        this.guildId = guildId;
        this.caseId = caseId;
        this.timestamp = timestamp;
        this.messageId = messageId;
        this.type = type;
    }

    public static int getNextCaseId(Guild guild) {
        if (casesDao == null) throw new IllegalStateException("casesDao jest nie ustawiony!");
        return getNextCaseId(casesDao.get(guild));
    }

    public static int getNextCaseId(@NotNull CaseRow caseRow) {
        int lastId = 0;
        for (Case aCase : caseRow.getCases()) {
            lastId = Math.max(aCase.caseId, lastId);
        }
        return lastId + 1;
    }

    public static Case getCaseById(int caseId, Guild guild) {
        if (casesDao == null) throw new IllegalStateException("casesDao jest nie ustawiony");
        return getCaseById(caseId, casesDao.get(guild));
    }

    @SuppressWarnings("WeakerAccess")
    public static Case getCaseById(int caseId, CaseRow caseRow) {
        return getCaseById(caseId, caseRow.getCases());
    }

    public static Case getCaseById(int caseId, List<Case> cList) {
        for (Case aCase : cList) if (aCase.getCaseId() == caseId) return aCase;
        return null;
    }

    public static void setStaticVariables(CasesDao casesDao, ScheduleDao scheduleDao) {
        Case.casesDao = casesDao;
        Case.scheduleDao = scheduleDao;
    }

    public void setIssuerId(@Nullable String issuerId) {
        this.issuerId = issuerId;
    }

    public void setIssuerId(@NotNull User issuer) {
        this.issuerId = issuer.getId();
    }

    public void setValidTo(TemporalAccessor validTo) {
        setValidTo(validTo, false);
    }

    public void setValidTo(TemporalAccessor validTo, boolean deser) {
        if (deser) {
            this.validTo = validTo;
            return;
        }
        Instant inst = null;
        if (this.validTo != null) {
            inst = Instant.from(this.validTo);
        }
        this.validTo = validTo;
        if (validTo == null) {
            if (inst == null) return;
            scheduleDao.getByDate(inst.toEpochMilli()).stream().filter(
                    s -> s.getAkcja() == Akcja.EVENT && s.getContent() instanceof AutoAkcja &&
                            ((AutoAkcja) s.getContent()).getCaseId() == caseId
            ).findFirst().ifPresent(sch -> scheduleDao.delete(String.valueOf(sch.getId())));
            return;
        }
        Instant inst2 = Instant.from(validTo);
        if (inst != null && inst.equals(inst2)) return;
        Schedule sch = null;
        if (inst != null) sch = scheduleDao.getByDate(inst.toEpochMilli()).stream().filter(
                s -> s.getAkcja() == Akcja.EVENT && s.getContent() instanceof AutoAkcja &&
                        ((AutoAkcja) s.getContent()).getCaseId() == caseId).findFirst().orElse(null);
        if (sch == null) {
            sch = scheduleDao.createNew(inst2.toEpochMilli(), issuerId, Akcja.EVENT, new AutoAkcja(caseId,
                    Objects.requireNonNull(opposite(type)), guildId));
        }
        sch.setData(inst2.toEpochMilli());
        scheduleDao.save(sch);
    }

    private Kara opposite(Kara type) {
        switch (type) {
            case BAN:
                return Kara.UNBAN;
            case MUTE: return Kara.UNMUTE;
            case WARN: return Kara.UNWARN;
            default: return null;
        }
    }

    @Getter
    public enum Flaga {
        SILENT(0, 's', null),
        NOBODY(1, 'n', new String[] {"nikt"});

        private static final Flaga[] EMPTY_FLAGI = new Flaga[0];

        private final int offset;
        private final long raw;
        private final char shortName;
        private final String[] longNames;

        Flaga(int offset, char shortName, String[] longNames) {
            this.offset = offset;
            this.raw = 1 << offset;
            this.shortName = shortName;
            this.longNames = longNames == null ? new String[0] : longNames;
        }

        public static Flaga resolveFlag(String f, Flaga... ignore) {
            List<Flaga> lista = Arrays.asList(ignore);
            for (Flaga flaga : values()) {
                if (lista.contains(flaga)) continue;
                char sn = flaga.getShortName();
                if (f.equals("-" + sn)) return flaga;
                if (f.equals("--" + flaga.name().toLowerCase()) || f.equals("—" + flaga.name().toLowerCase()))
                    return flaga;
                for (String alias : flaga.getLongNames())
                    if (f.equals("--" + alias) || f.equals("—" + alias)) return flaga;
            }
            return null;
        }

        /* celem zaoszczędzenia miejsca w db, co się przyda xD */
        public static long getRaw(@NotNull Flaga... flagi) {
            long raw = 0;
            for (Flaga flaga : flagi) {
                raw |= flaga.raw;
            }
            return raw;
        }

        public static EnumSet<Flaga> getFlagi(long raw) {
            if (raw == 0)
                return EnumSet.noneOf(Flaga.class);
            EnumSet<Flaga> flagi = EnumSet.noneOf(Flaga.class);
            for (Flaga flaga : Flaga.values()) {
                if ((raw & flaga.raw) == flaga.raw)
                    flagi.add(flaga);
            }
            return flagi;
        }

        public static long getRaw(Collection<Flaga> flagi) {
            return getRaw(flagi.toArray(EMPTY_FLAGI));
        }
    }
}
