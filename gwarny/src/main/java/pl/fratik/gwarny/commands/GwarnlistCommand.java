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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.gwarny.GwarnUtils;
import pl.fratik.gwarny.entity.Gwarn;
import pl.fratik.gwarny.entity.GwarnDao;
import pl.fratik.gwarny.entity.GwarnData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GwarnlistCommand extends Command {
    private final GwarnDao gwarnDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public GwarnlistCommand(GwarnDao gwarnDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.gwarnDao = gwarnDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "gwarnlist";
        category = CommandCategory.GADM;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        permLevel = PermLevel.GADMIN;
        hmap.put("gadmin", "gadmin");
        uzycie = new Uzycie("gadmin", "gadmin", false);
        uzycieDelim = " ";
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        final User gadm;
        if (context.getArgs().length >= 1 && context.getArgs()[0] != null) gadm = (User) context.getArgs()[0];
        else gadm = context.getSender();
        GwarnData gwarnData = gwarnDao.get(gadm);
        if (gwarnData.getGwarny().isEmpty()) {
            context.send(context.getTranslated("gwarnlist.empty"));
            return false;
        }
        Message m = context.send(context.getTranslated("generic.loading"));
        List<EmbedBuilder> pages = new ArrayList<>();
        pages.add(GwarnUtils.renderGwarnInfo(gwarnData, context.getShardManager(), context.getTlumaczenia(), context.getLanguage(), true));
        int i = 0;
        for (Gwarn g : gwarnData.getGwarny()) {
            i++;
            pages.add(GwarnUtils.renderGwarn(g, context.getShardManager(), context.getTlumaczenia(),
                    context.getLanguage(), i, true));
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(m);
        return true;
    }
}
