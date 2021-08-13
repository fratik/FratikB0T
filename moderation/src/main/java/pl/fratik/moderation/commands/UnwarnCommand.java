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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

public class UnwarnCommand extends ModerationCommand {

    private final CaseDao caseDao;

    public UnwarnCommand(CaseDao caseDao) {
        super(true);
        this.caseDao = caseDao;
        name = "unwarn";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"usunwarna", "uniwarn", "odwarnuj", "odwajnowywuj", "odwarnowany", "odostrzezenie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("unwarn.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.reply(context.getTranslated("unwarn.cant.unwarn.yourself"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.reply(context.getTranslated("unwarn.no.bot"));
            return false;
        }
        if (uzytkownik.isOwner()) {
            context.reply(context.getTranslated("unwarn.cant.unwarn.owner"));
            return false;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.reply(context.getTranslated("unwarn.user.cant.interact"));
            return false;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.reply(context.getTranslated("unwarn.bot.cant.interact"));
            return false;
        }
        List<Case> caseList = caseDao.getCasesByMember(uzytkownik);
        int cases = WarnUtil.countCases(caseList, uzytkownik.getId());
        try {
            if (cases < 0) {
                context.reply(context.getTranslated("unwarn.too.many.unwarns.fixing"));
                try {
                    TemporalAccessor timestamp = Instant.now();
                    Case c = new Case.Builder(uzytkownik, timestamp, Kara.WARN).setIssuerId(Globals.clientId)
                            .setReasonKey("unwarn.too.many.unwarns.fix.reason").build();
                    int aaa = 0;
                    for (int i = 0; i >= cases; i--) aaa++;
                    c.setIleRazy(aaa);
                    caseDao.createNew(null, c, false);
                    caseList.add(c);
                } catch (Exception e1) {
                    context.reply(context.getTranslated("unwarn.too.many.unwarns.cant.fix"));
                    return false;
                }
                context.reply(context.getTranslated("unwarn.too.many.unwarns.fixed"));
                cases = WarnUtil.countCases(caseList, uzytkownik.getId());
            }
        } catch (Exception e) {
            String prefix = context.getPrefix();
            context.reply(context.getTranslated("unwarn.unexpected.error", prefix, prefix));
            return false;
        }
        int ileRazy = 1;
        List<String> powodSplat = new ArrayList<>(Arrays.asList(powod.split(" ")));
        if (!powodSplat.isEmpty()) {
            String ileRazyStr = powodSplat.remove(0);
            if (ileRazyStr.matches("^\\d+$")) {
                int ileRazyA;
                try {
                    ileRazyA = Integer.parseInt(ileRazyStr);
                } catch (Exception e) {
                    ileRazyA = -1;
                }
                if (ileRazyA >= 1) ileRazy = ileRazyA;
                else powodSplat.add(ileRazyStr);
                powod = String.join(" ", powodSplat);
            }
        }
        if (cases - ileRazy < 0) {
            context.reply(context.getTranslated("unwarn.no.warns"));
            return false;
        }
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new Case.Builder(uzytkownik, timestamp, Kara.UNWARN).setIleRazy(ileRazy)
                .setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        caseDao.createNew(null, aCase, false);
        caseList.add(aCase);
        context.reply(context.getTranslated("unwarn.success", UserUtil.formatDiscrim(uzytkownik),
                WarnUtil.countCases(caseList, uzytkownik.getId())), m -> {
        });
        return true;
    }

}
