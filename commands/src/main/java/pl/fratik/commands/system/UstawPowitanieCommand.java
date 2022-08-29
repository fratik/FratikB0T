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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;

public class UstawPowitanieCommand extends NewCommand {
    private final GuildDao guildDao;

    public UstawPowitanieCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "ustawpowitanie";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @SubCommand(name = "usun", usage = "<kanal:textchannel>")
    public void delete(@NotNull NewCommandContext context) {
        context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        String usunieto = gc.getPowitania().remove(context.getArguments().get("kanal").getAsChannel().getId());
        if (usunieto == null) {
            context.sendMessage(context.getTranslated("ustawpowitanie.delete.failure"));
            return;
        }
        context.sendMessage(context.getTranslated("ustawpowitanie.delete.success"));
        guildDao.save(gc);
    }
    @SubCommand(name = "ustaw", usage = "<kanal:textchannel> <tekst:string>")
    public void set(@NotNull NewCommandContext context) {
        context.defer(false);
        String tekst = context.getArguments().get("tekst").getAsString();
        GuildConfig gc = guildDao.get(context.getGuild());
        gc.getPowitania().put(context.getArguments().get("kanal").getAsChannel().getId(), tekst);
        guildDao.save(gc);
        context.sendMessage(context.getTranslated("ustawpowitanie.response"));
    }
}
