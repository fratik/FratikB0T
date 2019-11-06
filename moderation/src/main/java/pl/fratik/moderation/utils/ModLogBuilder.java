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

package pl.fratik.moderation.utils;

import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.Ustawienia;
import pl.fratik.moderation.entity.Case;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

@SuppressWarnings("squid:S00107")
public class ModLogBuilder {

    private ModLogBuilder() {}

    @Setter private static Tlumaczenia tlumaczenia;
    @Setter private static GuildDao guildDao;

    public static MessageEmbed generate(Case aCase, Guild guild, ShardManager sm, Language lang, ManagerKomend managerKomend) {
        String iId = aCase.getIssuerId();
        if (iId == null || iId.isEmpty()) iId = "0";
        User iUser = sm.getUserById(iId);
        String reason = aCase.getReason();
        if (reason == null || reason.isEmpty()) reason = tlumaczenia.get(lang, "modlog.reason.unknown");
        if (iUser == null) iId = tlumaczenia.get(lang, "modlog.mod.unknown",
                managerKomend == null || managerKomend.getPrefixes(guild).isEmpty() ? Ustawienia.instance.prefix :
                        managerKomend.getPrefixes(guild).get(0), aCase.getCaseId());
        else iId = UserUtil.formatDiscrim(iUser);
        if (sm.getUserById(aCase.getUserId()) == null) {
            try {
                sm.retrieveUserById(aCase.getUserId()).complete();
            } catch (Exception e) {
                throw new RuntimeException("nie ma usera");
            }
        }
        return generate(aCase.getType(), sm.retrieveUserById(aCase.getUserId()).complete(), iId, reason,
                aCase.getType().getKolor(), aCase.getCaseId(), aCase.isValid(), aCase.getValidTo(),
                aCase.getTimestamp(), lang, guild);
    }

    private static MessageEmbed generate(Kara kara, User karany, String moderator, String reason, Color kolor, int caseId,
                                         boolean valid, TemporalAccessor validTo, TemporalAccessor timestamp, Language lang,
                                         Guild guild) {
        if (tlumaczenia == null) throw new IllegalStateException("Tlumaczenia nie ustawione!");
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(kolor)
                .setAuthor(karany.getAsTag(), null,
                        karany.getEffectiveAvatarUrl().replace(".webp", ".png"))
                .setTimestamp(timestamp)
                .setFooter(String.format(przyjaznaNazwa(lang, kara), "czas") + " | " +
                        tlumaczenia.get(lang, "modlog.caseid", Integer.toString(caseId)), null);
        if (kara == Kara.NOTATKA) {
            eb
                    .addField(tlumaczenia.get(lang, "modlog.responsible"), moderator, false)
                    .addField(tlumaczenia.get(lang, "modlog.note"), reason, false);
        } else {
            eb
                    .addField(tlumaczenia.get(lang, "modlog.responsible"), moderator, false)
                    .addField(tlumaczenia.get(lang, "modlog.reason"), reason, false);
        }
        if (kara == Kara.MUTE || kara == Kara.BAN || kara == Kara.TIMEDBAN || kara == Kara.NOTATKA) {
            eb.addField(tlumaczenia.get(lang, "modlog.active"), valid ?
                    tlumaczenia.get(lang, "modlog.active.true") :
                    tlumaczenia.get(lang, "modlog.active.false"), false);
            if (validTo != null) {
                GuildConfig gc = guildDao.get(guild);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", gc.getLanguage().getLocale());
                sdf.setTimeZone(GuildUtil.getTimeZone(guild, gc));
                eb.addField(tlumaczenia.get(lang, "modlog.active." + valid + ".to"),
                        sdf.format(Date.from(Instant.from(validTo))), false);
            }
        }
        return eb.build();
    }

    private static String przyjaznaNazwa(Language l, Kara kara) {
        return tlumaczenia.get(l, "modlog." + kara.name().toLowerCase());
    }

}
