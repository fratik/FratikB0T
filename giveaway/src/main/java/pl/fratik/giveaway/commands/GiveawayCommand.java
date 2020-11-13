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

package pl.fratik.giveaway.commands;

import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.giveaway.listener.GiveawayListener;
import pl.fratik.giveaway.utils.CreateGiveawayBuilder;

import java.util.LinkedHashMap;

public class GiveawayCommand extends Command {

    private final EventWaiter eventWaiter;
    private final ManagerArgumentow managerArgumentow;
    private final GiveawayListener giveawayListener;

    public GiveawayCommand(EventWaiter eventWaiter, ManagerArgumentow managerArgumentow, GiveawayListener giveawayListener) {
        name = "giveaway";
        aliases = new String[]{"konkurs"};
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.FUN;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("listaLubCreate", "string");
        uzycie = new Uzycie(hmap, new boolean[]{true});
        this.eventWaiter = eventWaiter;
        this.managerArgumentow = managerArgumentow;
        this.giveawayListener = giveawayListener;
    }

    @Override
    public boolean execute(CommandContext context) {
        String arg = ((String) context.getArgs()[0]).toLowerCase();
        if (!arg.equals("list") && !arg.equals("create")) {
            CommonErrors.usage(context);
            return false;
        }

        if (arg.equals("create")) {
            new CreateGiveawayBuilder(context.getMember().getId(), context.getTextChannel(), eventWaiter, context.getTlumaczenia(), managerArgumentow, giveawayListener).create();
        }

        return true;
    }

}
