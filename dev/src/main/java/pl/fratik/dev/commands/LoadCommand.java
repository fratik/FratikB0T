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

public class LoadCommand extends NewCommand {

    private final ManagerModulow managerModulow;
    private final Logger logger;
    private static final String SPSC = " Sprawdzam ścieżkę";
    private static final String WCZ = " Wczytuje";

    public LoadCommand(ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        logger = LoggerFactory.getLogger(getClass());
        name = "load";
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
        EmbedBuilder eb = context.getBaseEmbed("Wczytywanie modułu...", null);
        Emoji gtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.greenTick);
        Emoji rtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("emotki są null");
        String pytajnik = "\u2753";
        eb.appendDescription(pytajnik + SPSC + "\n");
        eb.appendDescription(pytajnik + WCZ + "\n");
        InteractionHook hook = context.replyEphemeral(eb.build());
        File path = new File(context.getArguments().get("modul").getAsString());
        if (!path.exists()) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                    rtick.getFormatted() + SPSC + ": ścieżka nie istnieje"));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        if (managerModulow.isLoaded(managerModulow.getDescription(path).getName())) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                    rtick.getFormatted() + SPSC + ": moduł jest już wczytany"));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  SPSC,
                gtick.getFormatted() + SPSC));
        hook.editOriginalEmbeds(eb.build()).complete();
        try {
            managerModulow.load(path.getAbsolutePath());
            boolean odp = managerModulow.startModule(managerModulow.getDescription(path).getName());
            if (!odp) throw new Exception("Nie udało się wczytać modułu — sprawdź konsolę.");
        } catch (Exception e) {
            logger.error("Błąd w komendzie load:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  WCZ,
                    rtick.getFormatted() + WCZ + ": " + e.getMessage()));
            eb.setColor(Color.red);
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  WCZ,
                gtick.getFormatted() + WCZ));
        eb.setColor(Color.green);
        hook.editOriginalEmbeds(eb.build()).complete();
    }
}
