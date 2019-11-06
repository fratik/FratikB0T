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

public class TakeTagCommand extends Command {
    private final TagsDao tagsDao;

    public TakeTagCommand(TagsDao tagsDao) {
        this.tagsDao = tagsDao;
        name = "taketag";
        category = CommandCategory.TAG;
        aliases = new String[] {"claimtag"};
        permLevel = PermLevel.ADMIN;
        uzycie = new Uzycie("tag", "string", true);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Tags tags = tagsDao.get(context.getGuild().getId());
        Tag tag = tags.getTagi().stream()
                .filter(t -> t.getName().equals(context.getArgs()[0])).findFirst().orElse(null);
        if (tag == null) {
            context.send(context.getTranslated("taketag.tag.notfound"));
            return false;
        }
        if (tag.getCreatedBy() != null) {
            context.send(context.getTranslated("taketag.tag.alreadyclaimed", context.getShardManager()
                    .retrieveUserById(tag.getCreatedBy()).complete().getAsTag()));
            return false;
        }
        context.send(context.getTranslated("taketag.tag.claimed"));
        tags.getTagi().remove(tag);
        tags.getTagi().add(new Tag(tag.getName(), context.getSender().getId(), tag.getContent()));
        tagsDao.save(tags);
        return true;
    }
}
