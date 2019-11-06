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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

public class AvatarCommand extends Command {
    public AvatarCommand() {
        name = "avatar";
        category = CommandCategory.FUN;
        uzycie = new Uzycie("osoba", "user");
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        aliases = new String[] {"prof", "profilowe", "awatar"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User osoba = context.getSender();
        if (context.getArgs().length > 0 && context.getArgs()[0] != null) osoba = (User) context.getArgs()[0];
        EmbedBuilder eb = new EmbedBuilder();
        eb.setImage(osoba.getEffectiveAvatarUrl().replace(".webp", ".png") + "?size=2048");
        eb.setAuthor(UserUtil.formatDiscrim(osoba));
        eb.setTitle(context.getTranslated("avatar.link"), osoba.getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(UserUtil.getPrimColor(osoba));
        context.send(eb.build());
        return true;
    }
}
