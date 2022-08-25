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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.io.File;

public class ReloadCommand extends NewCommand {

    private final ManagerModulow managerModulow;
    private final Logger logger;
    private static final String ODMO = " Odszukuje moduł";
    private static final String UNLOAD = " Unload";
    private static final String LOAD = " Load";

    public ReloadCommand(ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        logger = LoggerFactory.getLogger(getClass());
        name = "reload";
        usage = "<modul:string>";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        EmbedBuilder eb = context.getBaseEmbed("Reload modułu...", null);
        Emoji gtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.greenTick);
        Emoji rtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("ni ma emotek");
        String pytajnik = "\u2753";
        eb.appendDescription(pytajnik + ODMO + "\n");
        eb.appendDescription(pytajnik + UNLOAD + "\n");
        eb.appendDescription(pytajnik + LOAD + "\n");
        InteractionHook hook = context.replyEphemeral(eb.build());
        String modul = context.getArguments().get("modul").getAsString();
        File path = managerModulow.getPath(modul);
        if (path == null) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                    rtick.getFormatted() + ODMO + ": moduł nie znaleziony"));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        if (modul.equals("core")) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                    rtick.getFormatted() + ODMO + ": moduł core nie może być przeładowany"));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  ODMO,
                gtick.getFormatted() + ODMO));
        hook.editOriginalEmbeds(eb.build()).complete();
        try {
            boolean odp = managerModulow.stopModule(modul);
            if (!odp) throw new Exception("Unload modułu nieudany - sprawdź konsolę.");
            managerModulow.unload(modul, true);
        } catch (Exception e) {
            logger.error("Błąd w komendzie reload:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                    rtick.getFormatted() + " Unload: " + e.getMessage()));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                gtick.getFormatted() + UNLOAD));
        hook.editOriginalEmbeds(eb.build()).complete();
        try {
            managerModulow.load(path.getAbsolutePath());
            boolean odp = managerModulow.startModule(modul);
            if (!odp) throw new Exception("Nie udało się wczytać modułu - sprawdź konsolę.");
        } catch (Exception e) {
            logger.error("Błąd w komendzie reload:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  LOAD,
                    rtick.getFormatted() + LOAD + ": " + e.getMessage()));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  LOAD,
                gtick.getFormatted() + LOAD));
        eb.setColor(Color.green);
        hook.editOriginalEmbeds(eb.build()).complete();
    }
}
