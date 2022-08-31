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

import io.sentry.Sentry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public class UnwarnCommand extends ModerationCommand {

    private final CaseDao caseDao;

    public UnwarnCommand(CaseDao caseDao) {
        super(true);
        this.caseDao = caseDao;
        name = "unwarn";
        usage = "<osoba:user> [powod:string] [ilosc:int]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS, Permission.BAN_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        String powod = context.getArgumentOr("powod", context.getTranslated("unwarn.reason.default"), OptionMapping::getAsString);
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("unwarn.cant.unwarn.yourself"));
            return;
        }
        if (uzytkownik.getUser().isBot()) {
            context.replyEphemeral(context.getTranslated("unwarn.no.bot"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("unwarn.cant.unwarn.owner"));
            return;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("unwarn.user.cant.interact"));
            return;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("unwarn.bot.cant.interact"));
            return;
        }
        context.defer(false);
        List<Case> caseList = caseDao.getCasesByMember(uzytkownik);
        int cases = WarnUtil.countCases(caseList, uzytkownik.getId());
        try {
            if (cases < 0) {
                context.sendMessage(context.getTranslated("unwarn.too.many.unwarns.fixing"));
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
                    context.sendMessage(context.getTranslated("unwarn.too.many.unwarns.cant.fix"));
                    return;
                }
                context.sendMessage(context.getTranslated("unwarn.too.many.unwarns.fixed"));
                cases = WarnUtil.countCases(caseList, uzytkownik.getId());
            }
        } catch (Exception e) {
            Sentry.capture(e);
            context.sendMessage(context.getTranslated("unwarn.unexpected.error"));
            return;
        }
        int ileRazy = context.getArgumentOr("ilosc", 1, OptionMapping::getAsInt);
        if (cases - ileRazy < 0) {
            context.sendMessage(context.getTranslated("unwarn.no.warns"));
            return;
        }
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new Case.Builder(uzytkownik, timestamp, Kara.UNWARN).setIleRazy(ileRazy)
                .setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        caseDao.createNew(null, aCase, false);
        caseList.add(aCase);
        context.sendMessage(context.getTranslated("unwarn.success", UserUtil.formatDiscrim(uzytkownik),
                WarnUtil.countCases(caseList, uzytkownik.getId())));
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("ilosc")) option.setRequiredRange(1, Integer.MAX_VALUE);
    }
}
