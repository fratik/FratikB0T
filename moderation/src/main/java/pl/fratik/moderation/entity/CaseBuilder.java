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

package pl.fratik.moderation.entity;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.entity.Kara;

import java.time.temporal.TemporalAccessor;

@SuppressWarnings("WeakerAccess")
public class CaseBuilder {
    private String userId;
    private String guildId;
    private Integer caseId;
    private TemporalAccessor timestamp;
    private String messageId;
    private Kara type;
    private String issuerId;
    private String reason;

    public CaseBuilder() {}

    public CaseBuilder(Guild guild) {
        guildId = guild.getId();
        caseId = Case.getNextCaseId(guild);
    }

    public CaseBuilder setUser(String userId) {
        this.userId = userId;
        return this;
    }

    public CaseBuilder setUser(User user) {
        this.userId = user.getId();
        return this;
    }

    public CaseBuilder setGuild(String guildId) {
        this.guildId = guildId;
        return this;
    }

    public CaseBuilder setGuild(Guild guild) {
        this.guildId = guild.getId();
        return this;
    }

    public CaseBuilder setCaseId(int caseId) {
        this.caseId = caseId;
        return this;
    }

    public CaseBuilder setTimestamp(TemporalAccessor timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CaseBuilder setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public CaseBuilder setKara(Kara type) {
        this.type = type;
        return this;
    }

    public CaseBuilder setIssuer(User issuer) {
        return setIssuer(issuer.getId());
    }

    public CaseBuilder setIssuer(Member issuer) {
        return setIssuer(issuer.getUser());
    }

    public CaseBuilder setIssuer(String issuerId) {
        this.issuerId = issuerId;
        return this;
    }

    public CaseBuilder setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Case createCase() {
        if (userId == null || guildId == null || caseId == null || timestamp == null || type == null) {
            throw new IllegalArgumentException("Jeden lub więcej z argumentów jest null");
        }
        Case c = new Case(userId, guildId, caseId, timestamp, messageId, type);
        if (issuerId != null) c.setIssuerId(issuerId);
        if (reason != null) c.setReason(reason);
        return c;
    }
}
