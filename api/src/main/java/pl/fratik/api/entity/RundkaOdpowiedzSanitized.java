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

import lombok.Data;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.Objects;

@Data
public class RundkaOdpowiedzSanitized implements RundkaOdpowiedz {
    private final String userId;
    private final User user;
    private final int rundka;
    private Integer ocenyTak;
    private Integer ocenyNie;

    public RundkaOdpowiedzSanitized(RundkaOdpowiedzFull rundkaOdpowiedz, boolean dolaczOceny, ShardManager shardManager) {
        net.dv8tion.jda.api.entities.User u = Objects.requireNonNull(shardManager.getUserById(rundkaOdpowiedz.getUserId()));
        user = new User(u.getName(), u.getDiscriminator(), u.getEffectiveAvatarUrl(), u.getId(), null, null);
        userId = rundkaOdpowiedz.getUserId();
        rundka = rundkaOdpowiedz.getRundka();
        if (dolaczOceny) {
            ocenyTak = rundkaOdpowiedz.getOceny().getTak().size();
            ocenyNie = rundkaOdpowiedz.getOceny().getNie().size();
        }
    }
}
