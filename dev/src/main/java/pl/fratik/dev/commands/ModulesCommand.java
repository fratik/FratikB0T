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

package pl.fratik.dev.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.UserUtil;

import java.util.Map;
import java.util.Set;

public class ModulesCommand extends NewCommand {

    private final ManagerModulow managerModulow;
    private final NewManagerKomend managerKomend;

    public ModulesCommand(ManagerModulow managerModulow, NewManagerKomend managerKomend) {
        this.managerModulow = managerModulow;
        this.managerKomend = managerKomend;
        name = "modules";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        EmbedBuilder eb = context.getBaseEmbed("Moduły", null);
        Emoji gtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.greenTick);
        Emoji rtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("emotki są null");
        for (Map.Entry<String, Modul> entry : managerModulow.getModules().entrySet()) {
            String nazwa = entry.getKey();
            Modul modul = entry.getValue();
            if (managerModulow.isStarted(nazwa)) eb.getDescriptionBuilder().append(gtick.getFormatted()).append(" ")
                    .append(nazwa).append(" (")
                    .append(managerKomend.getRegistered().getOrDefault(modul, Set.of()).size())
                    .append(" zarejestrowanych komend)");
            else eb.getDescriptionBuilder().append(rtick.getFormatted()).append(" ").append(nazwa);
            eb.getDescriptionBuilder().append("\n");
        }
        context.replyEphemeral(eb.build());
    }
}
