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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;

import java.util.Objects;

public class PowiadomOPomocyCommand extends Command {
    private final ShardManager shardManager;

    public PowiadomOPomocyCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
        name = "powiadomopomocy";
        aliases = new String[] {"powiadompop"};
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.GADMIN;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        Role rola = Objects.requireNonNull(fdev).getRoleById(Ustawienia.instance.popRole);
        Member mem = fdev.getMember(context.getSender());
        if (Objects.requireNonNull(mem).getRoles().contains(rola)) {
            fdev.removeRoleFromMember(mem, Objects.requireNonNull(rola)).complete();
            context.send(context.getTranslated("powiadomopomocy.success.removed"));
            return false;
        }
        fdev.addRoleToMember(mem, Objects.requireNonNull(rola)).complete();
        context.send(context.getTranslated("powiadomopomocy.success"));
        return true;
    }
}
