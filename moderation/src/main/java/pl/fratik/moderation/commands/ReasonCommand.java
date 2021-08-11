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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.*;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.moderation.entity.AutoAkcja;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReasonCommand extends ModerationCommand {
    private final ScheduleDao scheduleDao;
    private final CaseDao caseDao;

    public ReasonCommand(ScheduleDao scheduleDao, CaseDao caseDao) {
        this.scheduleDao = scheduleDao;
        this.caseDao = caseDao;
        name = "reason";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("caseid", "integer");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"powod", "powód"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Integer caseId = (Integer) context.getArgs()[0];
        String reason = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        Case aCase = caseDao.getLocked(CaseDao.getId(context.getGuild(), caseId));
        if (aCase == null) {
            context.reply(context.getTranslated("reason.invalid.case"));
            return false;
        }
        try {
            DurationUtil.Response durationResp;
            try {
                durationResp = DurationUtil.parseDuration(reason);
            } catch (IllegalArgumentException e) {
                context.reply(context.getTranslated("reason.max.duration"));
                return false;
            }
            String powod = durationResp.getTekst();
            Instant akcjaDo = durationResp.getDoKiedy();
            if (powod.equals("")) {
                context.reply(context.getTranslated("reason.reason.empty"));
                return false;
            }
            aCase.setIssuerId(context.getSender().getIdLong());
            aCase.getFlagi().remove(Case.Flaga.NOBODY); // usuwa -n, -n może zostać ponownie dodane w parseFlags
            if (akcjaDo != null) {
                if (aCase.getValidTo() != null) {
                    for (Schedule schedule : scheduleDao.getByDate(Instant.from(aCase.getValidTo()).toEpochMilli())) {
                        if (schedule.getContent() instanceof AutoAkcja &&
                                ((AutoAkcja) schedule.getContent()).getCaseId() == aCase.getCaseNumber())
                            scheduleDao.delete(Integer.toString(schedule.getId()));
                    }
                }
                aCase.setValidTo(akcjaDo);
                //noinspection ConstantConditions - ustawiane wyżej
                scheduleDao.createNew(akcjaDo.toEpochMilli(), Long.toUnsignedString(aCase.getIssuerId()), Akcja.EVENT,
                        new AutoAkcja(aCase.getCaseNumber(), aCase.getType().opposite(), Long.toUnsignedString(aCase.getGuildId())));
            }
            ReasonUtils.parseFlags(aCase, powod, Case.Flaga.SILENT);
            caseDao.save(aCase);
            context.send(context.getTranslated("reason.success"));
            return true;
        } finally {
            caseDao.unlock(aCase);
        }
    }

}
