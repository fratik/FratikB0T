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

package pl.fratik.tags.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static pl.fratik.tags.Module.MAX_TAG_NAME_LENGTH;

public class ListTagCommand extends Command {
    private final TagsDao tagsDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public ListTagCommand(TagsDao tagsDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.tagsDao = tagsDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "listtag";
        category = CommandCategory.TAG;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
        aliases = new String[] {"listtags", "tags", "taglist", "tagslist"};
        cooldown = 5;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Tags tags = tagsDao.get(context.getGuild().getId());
        List<EmbedBuilder> pages = new ArrayList<>();
        final StringBuilder[] sb = {new StringBuilder()};
        tags.getTagi().stream().sorted(Comparator.comparing(t -> t.getName().toLowerCase())).forEachOrdered(tag -> {
            if (tag.getName().length() > MAX_TAG_NAME_LENGTH) return;
            sb[0].append(StringUtil.escapeMarkdown(tag.getName())).append(" ");
            if (sb[0].length() >= 1000) {
                pages.add(renderEmbed(sb[0], context));
                sb[0] = new StringBuilder();
            }
        });
        if (sb[0].length() != 0) pages.add(renderEmbed(sb[0], context));
        if (pages.isEmpty()) {
            context.reply(context.getTranslated("listtag.no.tags"));
            return false;
        }
        if (pages.size() == 1) {
            context.reply(pages.get(0).build());
            return true;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus).create(context.getMessage());
        return true;
    }

    @NotNull
    private EmbedBuilder renderEmbed(StringBuilder sb, CommandContext ctx) {
        return new EmbedBuilder()
                .setAuthor(ctx.getTranslated("listtag.embed.header"))
                .setColor(ctx.getMember() == null || ctx.getMember().getColor() == null ?
                        UserUtil.getPrimColor(ctx.getSender()) :
                        ctx.getMember().getColor())
                .setDescription(sb.toString().trim());
    }
}
