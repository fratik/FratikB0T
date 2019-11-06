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

package pl.fratik.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;

@Data
@AllArgsConstructor
public class User {
    private final String name;
    private final String discrim;
    private final String avatarUrl;
    private final String id;
    private Boolean inFdev;
    private Boolean admin;

    public User(net.dv8tion.jda.api.entities.User user) {
        name = user.getName();
        discrim = user.getDiscriminator();
        avatarUrl = user.getEffectiveAvatarUrl();
        id = user.getId();
        inFdev = null;
        admin = null;
    }

    public User(net.dv8tion.jda.api.entities.User user, ShardManager shardManager) {
        this(user);
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (fdev != null) {
            Member member = fdev.getMember(user);
            inFdev = Globals.inFratikDev && fdev.getMember(user) != null;
            admin = Globals.ownerId == user.getIdLong() ||
                    (Globals.inFratikDev &&
                            member != null &&
                            member.getRoles().contains(fdev.getRoleById(Ustawienia.instance.gadmRole)));
        }
    }

}
