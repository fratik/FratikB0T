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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.entity.Kara;

import java.time.temporal.TemporalAccessor;

@SuppressWarnings("WeakerAccess")
public class OldCaseBuilder {
    private String userId;
    private String guildId;
    private Integer caseId;
    private TemporalAccessor timestamp;
    private String messageId;
    private Kara type;
    private String issuerId;
    private String reason;
    private Integer ileRazy;

    public OldCaseBuilder() {}

    public OldCaseBuilder(Guild guild) {
        guildId = guild.getId();
        caseId = OldCase.getNextCaseId(guild);
    }

    public OldCaseBuilder setUser(String userId) {
        this.userId = userId;
        return this;
    }

    public OldCaseBuilder setUser(User user) {
        this.userId = user.getId();
        return this;
    }

    public OldCaseBuilder setGuild(String guildId) {
        this.guildId = guildId;
        return this;
    }

    public OldCaseBuilder setGuild(Guild guild) {
        this.guildId = guild.getId();
        return this;
    }

    public OldCaseBuilder setCaseId(int caseId) {
        this.caseId = caseId;
        return this;
    }

    public OldCaseBuilder setTimestamp(TemporalAccessor timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public OldCaseBuilder setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public OldCaseBuilder setKara(Kara type) {
        this.type = type;
        return this;
    }

    public OldCaseBuilder setIssuer(User issuer) {
        return setIssuer(issuer.getId());
    }

    public OldCaseBuilder setIssuer(Member issuer) {
        return setIssuer(issuer.getUser());
    }

    public OldCaseBuilder setIssuer(String issuerId) {
        this.issuerId = issuerId;
        return this;
    }

    public OldCaseBuilder setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public OldCaseBuilder setIleRazy(Integer ileRazy) {
        this.ileRazy = ileRazy;
        return this;
    }

    public OldCase createCase() {
        if (userId == null || guildId == null || caseId == null || timestamp == null || type == null) {
            throw new IllegalArgumentException("Jeden lub więcej z argumentów jest null");
        }
        OldCase c = new OldCase(userId, guildId, caseId, timestamp, messageId, type);
        if (issuerId != null) c.setIssuerId(issuerId);
        if (reason != null) c.setReason(reason);
        if (ileRazy != null) c.setIleRazy(ileRazy);
        return c;
    }
}
