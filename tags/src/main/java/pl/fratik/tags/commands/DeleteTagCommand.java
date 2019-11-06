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

package pl.fratik.tags.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.tags.entity.Tag;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

public class DeleteTagCommand extends Command {
    private final TagsDao tagsDao;

    public DeleteTagCommand(TagsDao tagsDao) {
        this.tagsDao = tagsDao;
        name = "deletetag";
        category = CommandCategory.TAG;
        permLevel = PermLevel.ADMIN;
        uzycie = new Uzycie("tag", "string", true);
        aliases = new String[] {"removetag", "deletetag", "delTag", "usunTaga", "removeTag", "usuntaga", "deltag", "deleteTag"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String tagName = (String) context.getArgs()[0];
        Tags tags = tagsDao.get(context.getGuild().getId());
        if (tags.getTagi().stream().noneMatch(t -> t.getName().equals(tagName))) {
            context.send(context.getTranslated("deletetag.doesnt.exist"));
            return false;
        }
        Tag tag = tags.getTagi().stream().filter(t -> t.getName().equals(tagName)).findFirst()
                .orElseThrow(IllegalStateException::new);
        tags.getTagi().remove(tag);
        tagsDao.save(tags);
        context.send(context.getTranslated("deletetag.success"));
        return true;
    }
}
