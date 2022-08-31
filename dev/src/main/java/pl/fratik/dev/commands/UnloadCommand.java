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

public class UnloadCommand extends NewCommand {

    private final ManagerModulow managerModulow;
    private final Logger logger;
    private static final String SPMO = " Sprawdzam moduł";
    private static final String UNLOAD = " Unload";

    public UnloadCommand(ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        logger = LoggerFactory.getLogger(getClass());
        name = "unload";
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
        EmbedBuilder eb = context.getBaseEmbed("Wyłączanie modułu...", null);
        Emoji gtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.greenTick);
        Emoji rtick = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.redTick);
        if (gtick == null || rtick == null) throw new NullPointerException("nie znaleziono emotek");
        String pytajnik = "\u2753";
        eb.appendDescription(pytajnik + SPMO + "\n");
        eb.appendDescription(pytajnik + UNLOAD + "\n");
        InteractionHook hook = context.replyEphemeral(eb.build());
        if (!managerModulow.isLoaded(context.getArguments().get("modul").getAsString())) {
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik + SPMO,
                    rtick.getFormatted() + SPMO + ": moduł nie jest wczytany"));
            eb.setColor(Color.decode("#ff0000"));
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik + SPMO,
                gtick.getFormatted() + SPMO));
        hook.editOriginalEmbeds(eb.build()).complete();
        try {
            boolean odp = managerModulow.stopModule(context.getArguments().get("modul").getAsString());
            if (!odp) throw new Exception("Unload modułu nieudany - sprawdź konsolę.");
            managerModulow.unload(context.getArguments().get("modul").getAsString(), true);
        } catch (Exception e) {
            logger.error("Błąd w komendzie unload:", e);
            eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                    rtick.getFormatted() + UNLOAD + ": " + e.getMessage()));
            eb.setColor(Color.decode("#ff0000"));
            hook.editOriginalEmbeds(eb.build()).complete();
            return;
        }
        eb.setDescription(eb.getDescriptionBuilder().toString().replace(pytajnik +  UNLOAD,
                gtick.getFormatted() + UNLOAD));
        eb.setColor(Color.decode("#00ff00"));
        hook.editOriginalEmbeds(eb.build()).complete();
        return;
    }
}
