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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvatarCommand extends NewCommand {

    private static final Pattern URL_EX = Pattern.compile("(\\.(gif|jpe?g|tiff?|png))");

    public AvatarCommand() {
        name = "avatar";
        usage = "[osoba:user]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        User osoba = context.getArgumentOr("osoba", context.getSender(), OptionMapping::getAsUser);

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
            hook.sendFiles(FileUpload.fromData(NetworkUtil.download(url), name))
                .addEmbeds(eb.setImage("attachment://" + name).build())
                .queue();
        } catch (IOException e) {
            context.sendMessage(eb.setImage(url).build());
        }

    }
}
