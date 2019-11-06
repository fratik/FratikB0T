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

package pl.fratik.arguments;

import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.moduly.Modul;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private ShardManager shardManager;
    private ArrayList<Argument> arguments;

    public Module() {
        arguments = new ArrayList<>();
    }

    public boolean startUp() {
        arguments = new ArrayList<>();

        arguments.add(new StringArgument());
        arguments.add(new IntegerArgument());
        arguments.add(new CategoryArgument());
        arguments.add(new LanguageArgument());
        arguments.add(new UserArgument(shardManager));
        arguments.add(new EmoteArgument(shardManager));
        arguments.add(new RoleArgument());
        arguments.add(new MemberArgument());
        arguments.add(new ChannelArgument());
        arguments.add(new LongArgument());
        arguments.add(new MessageArgument());

        arguments.forEach(managerArgumentow::registerArgument);

        return true;
    }

    public boolean shutDown() {
        arguments.forEach(managerArgumentow::unregisterArgument);
        return true;
    }
}