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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvatarCommand extends Command {

    private static final Pattern URL_EX = Pattern.compile("(\\.(gif|jpe?g|tiff?|png))");

    public AvatarCommand() {
        name = "avatar";
        category = CommandCategory.FUN;
        uzycie = new Uzycie("osoba", "user");
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        aliases = new String[] {"prof", "profilowe", "awatar"};
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User osoba = context.getSender();
        if (context.getArgs().length > 0 && context.getArgs()[0] != null) osoba = (User) context.getArgs()[0];

        String url = osoba.getEffectiveAvatarUrl() + "?size=2048";
        String ex = "png";
        Matcher matcher = URL_EX.matcher(url);
        if (matcher.find()) ex = matcher.group(1);
        String name = osoba.getId() + ex;

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(osoba));
        eb.setTitle(context.getTranslated("avatar.link"), url);
        eb.setColor(UserUtil.getPrimColor(osoba));

        try {
            context.getMessageChannel()
                    .sendFile(NetworkUtil.download(url), name)
                    .setEmbeds(eb.setImage("attachment://" + name).build())
                    .reference(context.getMessage())
                    .queue();
        } catch (IOException e) {
            context.reply(eb.setImage(url).build());
        }

        return true;
    }
}
