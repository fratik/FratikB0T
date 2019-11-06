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

import java.util.LinkedHashMap;

public class UngwarnCommand extends Command {
    private final GwarnDao gwarnDao;

    public UngwarnCommand(GwarnDao gwarnDao) {
        this.gwarnDao = gwarnDao;
        name = "ungwarn";
        category = CommandCategory.GADM;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        permLevel = PermLevel.ZGA;
        hmap.put("gadmin", "gadmin");
        hmap.put("id", "integer");
        uzycie = new Uzycie(hmap, new boolean[]{true, true});
        uzycieDelim = " ";
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        User gadm = (User) context.getArgs()[0];
        Integer idGwarna = (Integer) context.getArgs()[1];
        if (gadm == null || idGwarna == null) {
            CommonErrors.usage(context);
            return false;
        }
        GwarnData gwarnData = gwarnDao.get(gadm);
        Gwarn gwarn;
        try {
            gwarn = gwarnData.getGwarny().get(idGwarna - 1);
        } catch (Exception e) {
            context.send(context.getTranslated("ungwarn.invalid.gwarn"));
            return false;
        }
        if (gwarn == null) {
            context.send(context.getTranslated("ungwarn.invalid.gwarn"));
            return false;
        }
        if (gwarn.isActive()) {
            context.send(context.getTranslated("ungwarn.inactive"));
            return false;
        }
        gwarn.setActive(false);
        context.send(context.getTranslated("ungwarn.success", GwarnUtils.getActiveGwarns(gwarnData.getGwarny()).size()));
        gwarnDao.save(gwarnData);
        return true;
    }
}
