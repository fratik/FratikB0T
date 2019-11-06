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

public class ReloadCommand extends Command {

    private final ManagerModulow managerModulow;
    private static final String ODMO = " Odszukuje moduł";
    private static final String UNLOAD = " Unload";
    private static final String LOAD = " Load";

    public ReloadCommand(ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        name = "reload";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        uzycie = new Uzycie("modul", "string");
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed("Reload modułu...", null);
        Emote gtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.greenTick);
        Emote rtick = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("ni ma emotek");
        String pytajnik = "\u2753";
        eb.appendDescription(pytajnik + ODMO + "\n");
        eb.appendDescription(pytajnik + UNLOAD + "\n");
        eb.appendDescription(pytajnik + LOAD + "\n");
        Message msg = context.getChannel().sendMessage(eb.build()).complete();
        File path = managerModulow.getPath((String) context.getArgs()[0]);
        if (path == null) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                    rtick.getAsMention() + ODMO + ": moduł nie znaleziony"));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        if (context.getArgs()[0].equals("core")) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                    rtick.getAsMention() + ODMO + ": moduł core nie może być przeładowany"));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                gtick.getAsMention() + ODMO));
        msg.editMessage(eb.build()).override(true).complete();
        try {
            boolean odp = managerModulow.stopModule((String) context.getArgs()[0]);
            if (!odp) throw new Exception("Unload modułu nieudany - sprawdź konsolę.");
            managerModulow.unload((String) context.getArgs()[0], true);
        } catch (Exception e) {
            logger.error("Błąd w komendzie reload:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                    rtick.getAsMention() + " Unload: " + e.getMessage()));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                gtick.getAsMention() + UNLOAD));
        msg.editMessage(eb.build()).override(true).complete();
        try {
            managerModulow.load(path.getAbsolutePath());
            boolean odp = managerModulow.startModule((String) context.getArgs()[0]);
            if (!odp) throw new Exception("Nie udało się wczytać modułu - sprawdź konsolę.");
        } catch (Exception e) {
            logger.error("Błąd w komendzie reload:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  LOAD,
                    rtick.getAsMention() + LOAD + ": " + e.getMessage()));
            eb.setColor(Color.red);
            msg.editMessage(eb.build()).override(true).complete();
            return false;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  LOAD,
                gtick.getAsMention() + LOAD));
        eb.setColor(Color.green);
        msg.editMessage(eb.build()).override(true).complete();
        return true;
    }
}
