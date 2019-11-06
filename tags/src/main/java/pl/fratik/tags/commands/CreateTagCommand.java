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
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.tags.entity.Tag;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class CreateTagCommand extends Command {
    private final TagsDao tagsDao;
    private final ManagerKomend managerKomend;

    public CreateTagCommand(TagsDao tagsDao, ManagerKomend managerKomend) {
        this.tagsDao = tagsDao;
        this.managerKomend = managerKomend;
        name = "createtag";
        category = CommandCategory.TAG;
        permLevel = PermLevel.ADMIN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("tag", "string");
        hmap.put("treść", "string");
        hmap.put("[...]", "string");
        uzycieDelim = " ";
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"addtag", "createtag", "addTag", "dodajTaga", "dodajtaga"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String tagName = ((String) context.getArgs()[0]).toLowerCase();
        String content = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        Tags tags = tagsDao.get(context.getGuild().getId());
        if (tags.getTagi().stream().anyMatch(t -> t.getName().equals(tagName))) {
            context.send(context.getTranslated("createtag.exists"));
            return false;
        }
        if (managerKomend.getRegistered().stream().anyMatch(c -> c.getName().equals(tagName))) {
            context.send(context.getTranslated("createtag.reserved"));
            return false;
        }
        tags.getTagi().add(new Tag(tagName, context.getSender().getId(), content));
        tagsDao.save(tags);
        context.send(context.getTranslated("createtag.success"));
        return true;
    }
}
