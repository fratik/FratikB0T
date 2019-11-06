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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DegradCommand extends Command {

    private final ShardManager shardManager;

    public DegradCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
        name = "degrad";
        category = CommandCategory.UTILITY;
        permLevel = PermLevel.ZGA;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        uzycie = new Uzycie("gadmin", "user");
        aliases = new String[] {"papa", "plynik"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User gadmin = (User) context.getArgs()[0];
        if (!Globals.inFratikDev) throw new IllegalStateException("bot nie na FDev");
        @SuppressWarnings("ConstantConditions") // sprawdzamy to wyżej
        Member czlonek = shardManager.getGuildById(Ustawienia.instance.botGuild).getMember(gadmin);
        if (czlonek == null) {
            context.send(context.getTranslated("degrad.no.member"));
            return false;
        }
        if (!UserUtil.isGadm(czlonek, shardManager)) {
            context.send(context.getTranslated("degrad.no.role"));
            return false;
        }
        if (context.getSender().getId().equals(czlonek.getUser().getId())) {
            context.send(context.getTranslated("degrad.selfdegrad"));
            return false;
        }
        byte[] zdjecie;
        try {
            zdjecie = IOUtils.toByteArray(getClass().getResourceAsStream("/image_degrad.jpg"));
        } catch (NullPointerException | IOException e) {
            logger.warn("Zdjęcie z degradem nie znalezione!");
            zdjecie = null;
        }
        MessageAction maction = context.getChannel()
                .sendMessage(context.getTranslated("degrad.inprogress", UserUtil.formatDiscrim(czlonek)));
        if (zdjecie != null) maction = maction.addFile(zdjecie, "degrad.jpg");
        maction.queue(msg -> czlonek.getGuild()
                .removeRoleFromMember(czlonek, Objects.requireNonNull(czlonek.getGuild().getRoleById(Ustawienia.instance.gadmRole)))
                .queueAfter(5, TimeUnit.SECONDS, success -> msg
                        .editMessage(context.getTranslated("degrad.success", UserUtil.formatDiscrim(czlonek)))
                        .queue()));
        return true;
    }
}
