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
import pl.fratik.core.command.Command;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;

public class EatCommand extends Command {
    public EatCommand() {
        name = "eat";
        category = CommandCategory.FUN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("użytkownik", "user");
        hmap.put("tekst", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        uzycieDelim = " ";
        permLevel = PermLevel.EVERYONE;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        cooldown = 5;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = context.getSender();
        if (context.getArgs().length != 0 && context.getArgs()[0] != null) user = (User) context.getArgs()[0];
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/eat?avatarURL=" + URLEncoder.encode(user.getEffectiveAvatarUrl()
                            .replace(".webp", ".png") + "?size=2048", "UTF-8"),
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.send(context.getTranslated("image.server.fail"));
                return false;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.getChannel().sendFile(img, "eat.png").queue();
        } catch (IOException | NullPointerException e) {
            context.send(context.getTranslated("image.server.fail"));
        }

        return true;
    }
}
