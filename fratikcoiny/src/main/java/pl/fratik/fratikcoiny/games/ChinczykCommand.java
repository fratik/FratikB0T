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

package pl.fratik.fratikcoiny.games;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.util.HashSet;
import java.util.Set;

public class ChinczykCommand extends Command {
    private final EventBus eventBus;
    private final Set<Chinczyk> instances;

    public ChinczykCommand(EventBus eventBus) {
        this.eventBus = eventBus;
        name = "chinczyk";
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
        instances = new HashSet<>();
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        if (instances.stream().anyMatch(i -> i.getChannel().equals(context.getMessageChannel()))) {
            context.reply(context.getTranslated("chinczyk.game.in.progress"));
            return false;
        }
        instances.add(new Chinczyk(context, eventBus, instances::remove));
        return true;
    }
}
