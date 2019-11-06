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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GbanDao;
import pl.fratik.core.entity.GbanData;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import static pl.fratik.core.entity.GbanData.Type.GUILD;
import static pl.fratik.core.entity.GbanData.Type.USER;

public class GbanlistCommand extends Command {

    private final GbanDao gbanDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public GbanlistCommand(GbanDao gbanDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.gbanDao = gbanDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "gbanlist";
        category = CommandCategory.UTILITY;
        permLevel = PermLevel.GADMIN;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_MANAGE);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        List<GbanData> gdataList = gbanDao.getAll();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        Message m = context.send(context.getTranslated("generic.loading"));
        for (GbanData gdata : gdataList) {
            if (!gdata.isGbanned()) continue;
            pages.add(new FutureTask<>(() -> {
                EmbedBuilder eb = new EmbedBuilder();
                User issuer;
                if (gdata.getIssuerId() == null) {
                    issuer = null;
                } else {
                    issuer = context.getShardManager().retrieveUserById(gdata.getIssuerId()).complete();
                    if (issuer == null)
                        issuer = context.getShardManager().retrieveUserById(gdata.getIssuerId()).complete();
                }
                if (gdata.getType() == GUILD) {
                    String gbannedString = gdata.getId();
                    gbannedString = context.getTranslated("gbanlist.different.name", gbannedString, gdata.getName());
                    eb.addField(context.getTranslated("gbanlist.banned.guild"), gbannedString, false);
                } else if (gdata.getType() == USER) {
                    User gbanned = context.getShardManager().getUserById(gdata.getId());
                    if (gbanned == null) gbanned = context.getShardManager().retrieveUserById(gdata.getId()).complete();
                    String gbannedString = UserUtil.formatDiscrim(gbanned);
                    if (!gbannedString.equals(gdata.getName())) gbannedString = context.getTranslated("gbanlist.different.name",
                            UserUtil.formatDiscrim(gbanned), gdata.getName());
                    eb.addField(context.getTranslated("gbanlist.banned.user"), gbannedString, false);
                    eb.setColor(Color.decode("#09cdff"));
                }
                if (gdata.getType() == null) throw new IllegalStateException("typeNull");
                String issuerString = issuer == null ? "N/a???" : UserUtil.formatDiscrim(issuer);
                if (!issuerString.equals(gdata.getName())) issuerString = context.getTranslated("gbanlist.different.name",
                        issuer == null ? "N/a???" : UserUtil.formatDiscrim(issuer), gdata.getIssuer());
                eb.addField(context.getTranslated("gbanlist.issuer"), issuerString, false);
                eb.addField(context.getTranslated(  "gbanlist.reason"), gdata.getReason(), false);
                eb.setColor(Color.decode("#09cdff"));
                return eb;
            }));
        }
        if (pages.isEmpty()) {
            context.send(context.getTranslated("gbanlist.empty"));
            return false;
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus).create(m);
        return true;
    }
}

