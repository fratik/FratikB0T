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

package pl.fratik.api.entity;

import lombok.Data;

@Data
public class Guild {
    private final String name;
    private final String id;
    private final String icon;
    private final Role myHighestRole;
    private final int members;
    private final int roles;
    private final int textChannels;
    private final int voiceChannels;
    private final long createdTimestamp;

    public Guild(net.dv8tion.jda.api.entities.Guild guild) {
        this(guild.getName(), guild.getId(),
                guild.getIconId(),
                !guild.getSelfMember().getRoles().isEmpty() ?
                        new pl.fratik.api.entity.Role(guild.getSelfMember().getRoles().get(0).getName(),
                                guild.getSelfMember().getRoles().get(0).getId(),
                                guild.getSelfMember().getRoles().get(0).getPermissionsRaw(),
                                guild.getSelfMember().getRoles().get(0).getPositionRaw(),
                                guild.getSelfMember().getRoles().get(0).isManaged()) : null,
                guild.getMemberCount(),
                guild.getRoles().size(), guild.getTextChannels().size(), guild.getVoiceChannels().size(),
                guild.getTimeCreated().toInstant().toEpochMilli());
    }

    public Guild(String name, String id, String icon, Role myHighestRole, int members, int roles, int textChannels, int voiceChannels, long createdTimestamp) {
        this.name = name;
        this.id = id;
        this.icon = icon;
        this.myHighestRole = myHighestRole;
        this.roles = roles;
        this.textChannels = textChannels;
        this.voiceChannels = voiceChannels;
        this.createdTimestamp = createdTimestamp;
        this.members = members;
    }
}
