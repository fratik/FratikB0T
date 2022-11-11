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

import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.Dowod;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

@SuppressWarnings("squid:S00107")
public class ModLogBuilder {

    private ModLogBuilder() {}

    @Setter private static Tlumaczenia tlumaczenia;
    @Setter private static GuildDao guildDao;

    @NotNull
    public static MessageCreateData generate(@NotNull Case aCase, // UŻYWA COMPLETE!
                                   @NotNull Guild guild,
                                   @NotNull ShardManager sm,
                                   @NotNull Language lang,
                                   boolean modlog,
                                   boolean akcje,
                                   boolean dm) {
        Long iId = aCase.getIssuerId();
        User iUser = null;
        if (iId != null) {
            try {
                iUser = sm.retrieveUserById(iId).complete();
            } catch (ErrorResponseException er) {
                if (er.getErrorResponse() != ErrorResponse.UNKNOWN_USER) throw er;
                // else ignore
            }
        }
        return generate(aCase, guild, lang, modlog, akcje,
                dm, iUser, sm.retrieveUserById(aCase.getUserId()).complete());
    }

    @NotNull
    public static EmbedBuilder generateEmbed(@NotNull Case aCase, // UŻYWA COMPLETE!
                                   @NotNull Guild guild,
                                   @NotNull ShardManager sm,
                                   @NotNull Language lang,
                                             boolean modlog,
                                   boolean akcje) {
        Long iId = aCase.getIssuerId();
        User iUser = null;
        if (iId != null) {
            try {
                iUser = sm.retrieveUserById(iId).complete();
            } catch (ErrorResponseException er) {
                if (er.getErrorResponse() != ErrorResponse.UNKNOWN_USER) throw er;
                // else ignore
            }
        }
        return generateEmbed(aCase, guild, lang, modlog, akcje,
                iUser, sm.retrieveUserById(aCase.getUserId()).complete());
    }

    @NotNull
    public static MessageCreateData generate(@NotNull Case aCase,
                                   @NotNull Guild guild,
                                   @NotNull Language lang,
                                   boolean modlog,
                                   boolean akcje,
                                   boolean dm,
                                   @Nullable User issuer,
                                   @NotNull User karany) {
        String issuerStr;
        String reason = aCase.getReason(tlumaczenia, lang);
        if (reason == null || reason.isEmpty()) reason = tlumaczenia.get(lang, "modlog.reason.unknown");
        if (issuer == null) issuerStr = tlumaczenia.get(lang, "modlog.mod.unknown", aCase.getCaseNumber());
        else issuerStr = UserUtil.formatDiscrim(issuer);
        if (modlog && aCase.getFlagi().contains(Case.Flaga.NOBODY))
            issuerStr = tlumaczenia.get(lang, "modlog.mod.hidden", aCase.getCaseNumber());
        return generate(aCase.getType(), karany, issuerStr, reason,
                aCase.getType().getKolor(), aCase.getCaseNumber(), aCase.isValid(), dm, aCase.getValidTo(),
                aCase.getTimestamp(), aCase.getIleRazy(), lang, guild, (modlog || akcje) && !aCase.getDowody().isEmpty(), aCase.getDowody());
    }

    @NotNull
    public static EmbedBuilder generateEmbed(@NotNull Case aCase,
                                             @NotNull Guild guild,
                                             @NotNull Language lang,
                                             boolean modlog,
                                             boolean akcje,
                                             @Nullable User issuer,
                                             @NotNull User karany) {
        String issuerStr;
        String reason = aCase.getReason(tlumaczenia, lang);
        if (reason == null || reason.isEmpty()) reason = tlumaczenia.get(lang, "modlog.reason.unknown");
        if (issuer == null) issuerStr = tlumaczenia.get(lang, "modlog.mod.unknown", aCase.getCaseNumber());
        else issuerStr = UserUtil.formatDiscrim(issuer);
        if (modlog && aCase.getFlagi().contains(Case.Flaga.NOBODY)) {
            issuerStr = tlumaczenia.get(lang, "modlog.mod.hidden", aCase.getCaseNumber());
        }
        return generateEmbed(aCase.getType(), karany, issuerStr, reason,
                aCase.getType().getKolor(), aCase.getCaseNumber(), aCase.isValid(), aCase.getValidTo(),
                aCase.getTimestamp(), aCase.getIleRazy(), lang, guild, (modlog || akcje) && !aCase.getDowody().isEmpty());
    }

    @NotNull
    public static MessageCreateData generate(Kara kara,
                                             User karany,
                                             String moderator,
                                             String reason,
                                             Color kolor,
                                             long caseNumber,
                                             boolean valid,
                                             boolean dm,
                                             TemporalAccessor validTo,
                                             TemporalAccessor timestamp,
                                             int ileRazy,
                                             Language lang,
                                             Guild guild,
                                             boolean hasProof,
                                             List<Dowod> dowody) {
        StringBuilder contentBuilder = new StringBuilder();
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(generateEmbed(kara, karany, moderator, reason, kolor, caseNumber, valid,
            validTo, timestamp, ileRazy, lang, guild, hasProof).build());

        if (dm) {
            mb.setContent(tlumaczenia.get(lang, "modlog.dm.msg", guild.getName()));
            if (hasProof && !dowody.isEmpty()) {
                contentBuilder.append("\n\n");
                contentBuilder.append(tlumaczenia.get(lang, "modlog.dm.attached.proof" + (dowody.size() > 1 ? ".multi" : "")));
                contentBuilder.append("\n\n");
                for (int i = 0; i < dowody.size(); i++) {
                    String moreStr = tlumaczenia.get(lang, "modlog.dm.proof.more", dowody.size() - i);
                    if (contentBuilder.length() + 3 + dowody.get(i).getContent().length() >= (1900 - moreStr.length())) {
                        contentBuilder.append(moreStr);
                        contentBuilder.append("\n\n"); // setLength -2 wywali te znaki
                        break;
                    }
                    contentBuilder.append(dowody.get(i).getContent());
                    contentBuilder.append("\n\n");
                }
                contentBuilder.delete(contentBuilder.length() - 2, contentBuilder.length());
            }
        }
        return mb.build();
    }

    @NotNull
    public static EmbedBuilder generateEmbed(Kara kara,
                                             User karany,
                                             String moderator,
                                             String reason,
                                             Color kolor,
                                             long caseNumber,
                                             boolean valid,
                                             TemporalAccessor validTo,
                                             TemporalAccessor timestamp,
                                             int ileRazy,
                                             Language lang,
                                             Guild guild,
                                             boolean hasProof) {
        if (tlumaczenia == null) throw new IllegalStateException("Tlumaczenia nie ustawione!");
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(kolor);
        if (karany == null) {
            eb.setAuthor(tlumaczenia.get(lang, "modlog.unknown.user"));
        } else {
            eb.setAuthor(karany.getAsTag(), null,
                    karany.getEffectiveAvatarUrl().replace(".webp", ".png"));
        }
        eb
                .setTimestamp(timestamp)
                .setFooter(String.format(przyjaznaNazwa(lang, kara), "czas") + " | " +
                        tlumaczenia.get(lang, "modlog.caseid", Long.toString(caseNumber)), null);
        if (kara == Kara.NOTATKA) {
            eb
                    .addField(tlumaczenia.get(lang, "modlog.responsible"), moderator, false)
                    .addField(tlumaczenia.get(lang, "modlog.note"), reason, false);
        } else {
            eb
                    .addField(tlumaczenia.get(lang, "modlog.responsible"), moderator, false)
                    .addField(tlumaczenia.get(lang, "modlog.reason"), reason, false);
        }
        if (kara == Kara.MUTE || kara == Kara.BAN || kara == Kara.NOTATKA) {
            eb.addField(tlumaczenia.get(lang, "modlog.active"), valid ?
                    tlumaczenia.get(lang, "modlog.active.true") :
                    tlumaczenia.get(lang, "modlog.active.false"), false);
        }
        if ((kara == Kara.MUTE || kara == Kara.BAN || kara == Kara.NOTATKA || kara == Kara.WARN) && validTo != null) {
            GuildConfig gc = guildDao.get(guild);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", gc.getLanguage().getLocale());
            sdf.setTimeZone(GuildUtil.getTimeZone(guild, gc));
            eb.addField(tlumaczenia.get(lang, "modlog.active." + valid + ".to"),
                    sdf.format(Date.from(Instant.from(validTo))), false);
        }
        if ((kara == Kara.WARN || kara == Kara.UNWARN) && ileRazy > 1) {
            eb.addField(tlumaczenia.get(lang, "modlog.times.header"),
                    tlumaczenia.get(lang, "modlog.times.content." + kara.name().toLowerCase() + "s", ileRazy),
                    false);
        }
        if (hasProof) {
            eb.addField(tlumaczenia.get(lang, "modlog.proof.header"),
                    tlumaczenia.get(lang, "modlog.proof.content", caseNumber),
                    false);
        }
        return eb;
    }

    private static String przyjaznaNazwa(Language l, Kara kara) {
        return tlumaczenia.get(l, "modlog." + kara.name().toLowerCase());
    }

}
