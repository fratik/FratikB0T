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

package pl.fratik.gwarny;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.gwarny.entity.Gwarn;
import pl.fratik.gwarny.entity.GwarnData;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class GwarnUtils {
    private GwarnUtils() {
    }

    public static <T extends Gwarn> List<T> getActiveGwarns(List<T> lista) {
        return lista.stream().filter(Gwarn::isActive).collect(Collectors.toList());
    }

    public static EmbedBuilder renderGwarn(Gwarn gwarn, ShardManager shardManager, Tlumaczenia t, Language l, int id, boolean paginated) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(0x9cdff));
        final User nadal;
        try {
            nadal = shardManager.retrieveUserById(gwarn.getNadane()).complete();
        } catch (Exception e) {
            throw new IllegalStateException("uh nikt nie nadał????");
        }
        eb.setAuthor(nadal.getAsTag(), null, nadal.getEffectiveAvatarUrl());
        eb.addField(t.get(l, "gwarnlist.reason"), gwarn.getPowod(), false);
        eb.addField(t.get(l, "gwarnlist.active"), gwarn.isActive() ? t.get(l, "generic.yes") :
                t.get(l, "generic.no"), false);
        eb.setTimestamp(Instant.ofEpochMilli(gwarn.getTimestamp()));
        if (!paginated) eb.setFooter(t.get(l, "gwarnlist.id") + " " + id);
        else eb.setFooter(t.get(l, "gwarnlist.id") + " " + id + " (%s/%s)");
        return eb;
    }

    public static EmbedBuilder renderGwarnInfo(GwarnData gwarnData, ShardManager shardManager, Tlumaczenia t, Language l, boolean paginated) {
        final User dotyczy;
        try {
            dotyczy = shardManager.retrieveUserById(gwarnData.getId()).complete();
        } catch (Exception e) {
            throw new IllegalStateException("uh nikt nie jest właścicielem gwarnów????");
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(0x9cdff));
        eb.setAuthor(dotyczy.getAsTag(), null, dotyczy.getEffectiveAvatarUrl());
        int active = getActiveGwarns(gwarnData.getGwarny()).size();
        eb.addField(t.get(l, "gwarnlist.info.active"), String.valueOf(active), true);
//        eb.addField(t.get(l, "gwarnlist.info.revoked"), String.valueOf(gwarnData.getGwarny().size() - active), true)
        if (paginated) eb.setFooter(t.get(l, "gwarnlist.info") + " (%s/%s)");
        eb.setTimestamp(Instant.now());
        return eb;
    }
}