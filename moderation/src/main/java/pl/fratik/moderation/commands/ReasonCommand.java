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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Akcja;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Schedule;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.moderation.entity.AutoAkcja;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;

public class ReasonCommand extends ModerationCommand {
    private final ScheduleDao scheduleDao;
    private final CaseDao caseDao;

    public ReasonCommand(ScheduleDao scheduleDao, CaseDao caseDao) {
        super(true);
        this.scheduleDao = scheduleDao;
        this.caseDao = caseDao;
        name = "powod";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS);
        usage = "<numer_sprawy:int> <powod:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        int caseId = context.getArguments().get("numer_sprawy").getAsInt();
        String reason = context.getArguments().get("powod").getAsString();
        context.defer(false);
        Case aCase = caseDao.getLocked(CaseDao.getId(context.getGuild(), caseId));
        if (aCase == null) {
            context.sendMessage(context.getTranslated("reason.invalid.case"));
            return;
        }
        try {
            DurationUtil.Response durationResp;
            try {
                durationResp = DurationUtil.parseDuration(reason);
            } catch (IllegalArgumentException e) {
                context.sendMessage(context.getTranslated("reason.max.duration"));
                return;
            }
            String powod = durationResp.getTekst();
            Instant akcjaDo = durationResp.getDoKiedy();
            if (powod.equals("")) {
                context.sendMessage(context.getTranslated("reason.reason.empty"));
                return;
            }
            aCase.setIssuerId(context.getSender().getIdLong());
            aCase.getFlagi().remove(Case.Flaga.NOBODY); // usuwa -n, -n może zostać ponownie dodane w parseFlags
            if (akcjaDo != null) {
                if (aCase.getType() == Kara.MUTE) {
                    try {
                        context.getGuild().timeoutUntil(User.fromId(aCase.getUserId()), akcjaDo).complete();
                    } catch (Exception e) {
                        context.sendMessage(context.getTranslated("reason.timeout.change.error"));
                        return;
                    }
                } else {
                    if (aCase.getValidTo() != null) {
                        for (Schedule schedule : scheduleDao.getByDate(Instant.from(aCase.getValidTo()).toEpochMilli())) {
                            if (schedule.getContent() instanceof AutoAkcja &&
                                    ((AutoAkcja) schedule.getContent()).getCaseId() == aCase.getCaseNumber())
                                scheduleDao.delete(Integer.toString(schedule.getId()));
                        }
                    }
                    //noinspection ConstantConditions - ustawiane wyżej
                    scheduleDao.save(scheduleDao.createNew(akcjaDo.toEpochMilli(), Long.toUnsignedString(aCase.getIssuerId()), Akcja.EVENT,
                            new AutoAkcja(aCase.getCaseNumber(), aCase.getType().opposite(), Long.toUnsignedString(aCase.getGuildId()))));
                }
                aCase.setValidTo(akcjaDo);
            }
            ReasonUtils.parseFlags(aCase, powod, Case.Flaga.SILENT);
            caseDao.save(aCase);
            context.sendMessage(context.getTranslated("reason.success"));
        } finally {
            caseDao.unlock(aCase);
        }
    }

}
