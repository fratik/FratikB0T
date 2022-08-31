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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.serializer.CaseDeserializer;
import pl.fratik.moderation.serializer.CaseSerializer;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Table("cases")
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@RequiredArgsConstructor
@GIndex
@JsonSerialize(using = CaseSerializer.class)
@JsonDeserialize(using = CaseDeserializer.class)
public class Case implements DatabaseEntity, Comparable<Case> {
    @PrimaryKey
    private final String id; // guildId + '.' + caseNumber
    private final long guildId;
    private final long userId;
    private final long caseNumber;
    @NotNull private final TemporalAccessor timestamp;
    @NotNull private final Kara type;
    @Setter private boolean valid = true;
    @Setter @Nullable private TemporalAccessor validTo;
    @Setter @Nullable private Long messageId;
    @Setter @Nullable private Long dmMsgId;
    @Setter @Nullable private Long issuerId;
    @Setter @Nullable private String reason;
    @Setter private int ileRazy = 1;
    @Setter @NotNull private EnumSet<Flaga> flagi = EnumSet.noneOf(Flaga.class);
    @Setter @NotNull private List<Dowod> dowody = new ArrayList<>();
    @Setter private boolean needsUpdate = false; // wiadomość o sprawie powinna zostać zaktualizowana; flaga awaryjna, do użycia w migracji

    public String getReason(NewCommandContext ctx) {
        return getReason(ctx.getTlumaczenia(), ctx.getLanguage());
    }

    public String getReason(Tlumaczenia t, Language l) {
        if (reason == null) return null;
        if (reason.startsWith("\\translate:")) return reason.substring(1);
        if (reason.startsWith("translate:")) {
            l = l == null ? Language.DEFAULT : l;
            String key;
            String[] arguments;
            Pattern p = Pattern.compile("^translate:(.+?):(.*)$");
            Matcher m = p.matcher(reason);
            if (m.matches()) {
                key = m.group(1);
                arguments = Arrays.stream(m.group(2).split(";"))
                        .map(a -> new String(Base64.getDecoder().decode(a), StandardCharsets.UTF_8)).toArray(String[]::new);
            } else {
                key = reason.substring(10);
                arguments = null;
            }
            if (arguments != null) return t.get(l, key, (Object[]) arguments);
            else return t.get(l, key);
        }
        return reason;
    }

    @Override
    public String getTableName() {
        return "cases";
    }

    @Override
    public int compareTo(@NotNull Case o) { // 1 -> 2 -> 3 -> ...
        Objects.requireNonNull(o);
        if (getGuildId() != o.getGuildId()) throw new IllegalArgumentException("Nie można sortować spraw między serwerami!");
        return Long.compare(getCaseNumber(), o.getCaseNumber());
    }

    public static class Builder {
        private final Case aCase;

        public Builder(Member mem, TemporalAccessor timestamp, Kara type) {
            this(mem.getGuild(), mem.getUser(), timestamp, type);
        }

        public Builder(Guild guild, User user, TemporalAccessor timestamp, Kara type) {
            this(guild.getIdLong(), user.getIdLong(), timestamp, type);
        }

        public Builder(long guildId, long userId, TemporalAccessor timestamp, Kara type) {
            aCase = new Case(null, guildId, userId, -1, timestamp, type);
        }
        
        public Builder setValidTo(TemporalAccessor validTo) {
            aCase.setValidTo(validTo);
            return this;
        }
        
        public Builder setMessageId(Long messageId) {
            aCase.setMessageId(messageId);
            return this;
        }
        
        public Builder setDmMsgId(Long dmMsgId) {
            aCase.setDmMsgId(dmMsgId);
            return this;
        }

        public Builder setIssuerId(Long issuerId) {
            aCase.setIssuerId(issuerId);
            return this;
        }

        public Builder setReason(String reason, boolean escape) {
            if (escape && reason != null && reason.startsWith("translate:")) reason = "\\" + reason;
            aCase.setReason(reason);
            return this;
        }

        public Builder setReasonKey(String key) {
            return setReason("translate:" + key, false);
        }

        public Builder setReasonKey(String key, String... args) {
            return setReason("translate:" + key + ":" +
                    Arrays.stream(args).map(a -> Base64.getEncoder().encodeToString(a.getBytes(StandardCharsets.UTF_8)))
                            .collect(Collectors.joining(";")), false);
        }

        public Builder setIleRazy(Integer ileRazy) {
            aCase.setIleRazy(ileRazy);
            return this;
        }

        public Builder setFlags(Flaga... flags) {
            aCase.getFlagi().clear();
            aCase.getFlagi().addAll(Arrays.asList(flags));
            return this;
        }

        public Builder addFlags(Flaga... flags) {
            aCase.getFlagi().addAll(Arrays.asList(flags));
            return this;
        }

        public Builder setDowody(Dowod... dowod) {
            aCase.getDowody().clear();
            aCase.getDowody().addAll(Arrays.asList(dowod));
            return this;
        }

        public Builder addDowod(Dowod... dowod) {
            aCase.getDowody().addAll(Arrays.asList(dowod));
            return this;
        }

        public Case build() {
            return aCase;
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
            this.raw = 1L << offset;
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
