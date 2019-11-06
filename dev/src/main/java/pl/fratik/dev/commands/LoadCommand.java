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
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerModulow;

import java.awt.*;
import java.io.File;

public class LoadCommand extends Command {

    private final ManagerModulow managerModulow;
    private static final String SPSC = " Sprawdzam ścieżkę";
    private static final String WCZ = " Wczytuje";

    public LoadCommand(ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        name = "load";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        uzycie = new Uzycie("modul", "string");
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed("Wczytywanie modułu...", null);
        Emote gtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.greenTick);
        Emote rtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("emotki są null");
        String pytajnik = "\u2753";
        eb.appendDescription(pytajnik + SPSC + "\n");
        eb.appendDescription(pytajnik + WCZ + "\n");
        Message msg = context.getChannel().sendMessage(eb.build()).complete();
        File path = new File((String) context.getArgs()[0]);
        if (!path.exists()) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                    rtick.getAsMention() + SPSC + ": ścieżka nie istnieje"));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        if (managerModulow.isLoaded(managerModulow.getDescription(path).getName())) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                    rtick.getAsMention() + SPSC + ": moduł jest już wczytany"));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                gtick.getAsMention() + SPSC));
        msg.editMessage(eb.build()).override(true).complete();
        try {
            managerModulow.load(path.getAbsolutePath());
            boolean odp = managerModulow.startModule(managerModulow.getDescription(path).getName());
            if (!odp) throw new Exception("Nie udało się wczytać modułu - sprawdź konsolę.");
        } catch (Exception e) {
            logger.error("Błąd w komendzie load:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  WCZ,
                    rtick.getAsMention() + WCZ + ": " + e.getMessage()));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  WCZ,
                gtick.getAsMention() + WCZ));
        eb.setColor(Color.green);
        msg.editMessage(eb.build()).override(true).complete();
        return true;
    }
}
