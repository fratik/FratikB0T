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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.net.URLEncoder;

public class GraficznaCommand extends Command {

    private final String endpoint;
    private final String param;
    private final boolean preventOnSender;

    { //NOSONAR
        cooldown = 5;
        uzycie = new Uzycie("osoba", "user");
    }

    public GraficznaCommand(String name, String endpoint, boolean preventOnSender) {
        this(name, endpoint, "imageURL", preventOnSender);
    }

    public GraficznaCommand(String name, String endpoint, String param, boolean preventOnSender) {
        this.name = name;
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);

        this.endpoint = endpoint;
        this.param = param;
        this.preventOnSender = preventOnSender;
    }

    public GraficznaCommand(String name, String endpoint, String[] aliases, boolean preventOnSender) {
        this(name, endpoint, aliases, "imageURL", preventOnSender);
    }

    private GraficznaCommand(String name, String endpoint, String[] aliases, String param, boolean preventOnSender) {
        this.name = name;
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        this.aliases = aliases;

        this.endpoint = endpoint;
        this.param = param;
        this.preventOnSender = preventOnSender;
    }

    public GraficznaCommand(String name, String endpoint, PermLevel permLvl, boolean preventOnSender) {
        this(name, endpoint, permLvl, "imageURL", preventOnSender);
    }

    private GraficznaCommand(String name, String endpoint, PermLevel permLvl, String param, boolean preventOnSender) {
        this.name = name;
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        permLevel = permLvl;

        this.endpoint = endpoint;
        this.param = param;
        this.preventOnSender = preventOnSender;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = context.getSender();
        if (context.getArgs().length != 0 && context.getArgs()[0] != null) user = (User) context.getArgs()[0];
        if (user.equals(context.getSender()) && preventOnSender) {
            context.send(context.getTranslated(name + ".prevent.on.sender"));
            return false;
        }
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            endpoint + "?" + param + "=" + URLEncoder.encode(user.getEffectiveAvatarUrl()
                            .replace(".webp", ".png") + "?size=2048", "UTF-8"),
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.send(context.getTranslated("image.server.fail"));
                return false;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.getChannel().sendFile(img, name + ".png").queue();
        } catch (IOException | NullPointerException e) {
            context.send(context.getTranslated("image.server.fail"));
        }
        return true;
    }
}
