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

package pl.fratik.dev.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;

public class ModulesCommand extends Command {

    private final ManagerModulow managerModulow;
    private final ManagerKomend managerKomend;

    public ModulesCommand(ManagerModulow managerModulow, ManagerKomend managerKomend) {
        this.managerModulow = managerModulow;
        this.managerKomend = managerKomend;
        name = "modules";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed("Moduły", null);
        Emote gtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.greenTick);
        Emote rtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("emotki są null");
        for (String nazwa : managerModulow.getModules().keySet()) {
            if (managerModulow.isStarted(nazwa)) eb.getDescriptionBuilder().append(gtick.getAsMention()).append(" ")
                    .append(nazwa).append(" (")
                    .append(managerKomend.getRegisteredPerModule().getOrDefault(nazwa, 0).toString())
                    .append(" zarejestrowanych komend)");
            else eb.getDescriptionBuilder().append(rtick.getAsMention()).append(" ").append(nazwa);
            eb.getDescriptionBuilder().append("\n");
        }
        context.send(eb.build());
        return true;
    }
}
