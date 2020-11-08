/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReasonCommand extends CaseEditingCommand {

    private final GuildDao guildDao;

    public ReasonCommand(GuildDao guildDao, CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        super(casesDao, shardManager, managerKomend);
        this.guildDao = guildDao;
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
        GuildConfig gc = guildDao.get(context.getGuild());
        CaseRow caseRow = casesDao.get(context.getGuild());
        if (caseRow.getCases().size() < caseId || (caseRow.getCases().size() >= caseId - 1 &&
                caseRow.getCases().get(caseId - 1) == null)) {
            context.send(context.getTranslated("reason.invalid.case"));
            return false;
        }
        DurationUtil.Response durationResp;
        try {
            durationResp = DurationUtil.parseDuration(reason);
        } catch (IllegalArgumentException e) {
            context.send(context.getTranslated("reason.max.duration"));
            return false;
        }
        String powod = durationResp.getTekst();
        Instant akcjaDo = durationResp.getDoKiedy();
        if (powod.equals("")) {
            context.send(context.getTranslated("reason.reason.empty"));
            return false;
        }
        Case aCase = caseRow.getCases().get(caseId - 1);
        if (akcjaDo != null && !aCase.isValid()) {
            context.send(context.getTranslated("reason.not.valid"));
            return false;
        }
        aCase.getFlagi().remove(Case.Flaga.NOBODY); // usuwa -n, -n może zostać ponownie dodane w parseFlags
        if (akcjaDo != null) aCase.setValidTo(akcjaDo);
        @Nullable TextChannel modLogChannel = gc.getModLog() != null && !Objects.equals(gc.getModLog(), "") ?
                context.getGuild().getTextChannelById(gc.getModLog()) : null;
        ReasonUtils.parseFlags(aCase, powod, Case.Flaga.SILENT);
        return updateCase(context, context.getTranslated("reason.success"), context.getTranslated("reason.failed"),
                caseRow, modLogChannel, aCase, gc);
    }

}
