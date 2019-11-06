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

package pl.fratik.gwarny.commands;

import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.gwarny.GwarnUtils;
import pl.fratik.gwarny.entity.Gwarn;
import pl.fratik.gwarny.entity.GwarnDao;
import pl.fratik.gwarny.entity.GwarnData;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class GwarnCommand extends Command {
    private final GwarnDao gwarnDao;

    public GwarnCommand(GwarnDao gwarnDao) {
        this.gwarnDao = gwarnDao;
        name = "gwarn";
        category = CommandCategory.GADM;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        permLevel = PermLevel.ZGA;
        hmap.put("gadmin", "gadmin");
        hmap.put("powód", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[]{true, true, false});
        uzycieDelim = " ";
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        User gadm = (User) context.getArgs()[0];
        String powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        if (gadm == null || powod == null) {
            CommonErrors.usage(context);
            return false;
        }
        if (powod.length() > 1024) {
            context.send(context.getTranslated("gwarn.error.length"));
            return false;
        }
        GwarnData gwarnData = gwarnDao.get(gadm);
        Gwarn gwarn = new Gwarn(context.getSender().getId(), powod);
        gwarn.setTimestamp(Instant.now().toEpochMilli());
        gwarnData.getGwarny().add(gwarn);
        context.send(context.getTranslated("gwarn.success", GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size()));
        if (GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size() == 10) {
            context.send(context.getTranslated("gwarn.success.special", GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size()));
        }
        if (GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size() == 20) {
            context.send(context.getTranslated("gwarn.success.special", GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size()));
        }
        gwarnDao.save(gwarnData);
        return true;
    }
}
