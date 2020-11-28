/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlurpleCommand extends Command {
    public BlurpleCommand() {
        name = "blurple";
        category = CommandCategory.FUN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("osoba", "user");
        hmap.put("flagi", "string");
        hmap.put("[...]", "string");
        uzycieDelim = " ";
        uzycie = new Uzycie(hmap, new boolean[] {false, false, false});
        permLevel = PermLevel.EVERYONE;
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = (User) context.getArgs()[0];
        if (user == null) user = context.getSender();
        boolean reverse = false;
        boolean classic = false;
        if (context.getArgs().length > 1 && context.getArgs()[1] != null) {
            String arg = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim)).toLowerCase();
            if (arg.contains("-r") || arg.contains("--reverse") || arg.contains("—reverse")) {
                reverse = true;
            }
            if (arg.contains("-c") || arg.contains("--classic") || arg.contains("—classic")) {
                classic = true;
            }
        }
        try {
            JSONObject zdjecie = NetworkUtil.getJson(String.format("%s/api/image/blurple?avatarURL=%s&reverse=%s&classic=%s",
                    Ustawienia.instance.apiUrls.get("image-server"),
                    URLEncoder.encode(user.getEffectiveAvatarUrl().replace(".webp", ".png")
                            + "?size=2048", "UTF-8"), reverse, classic), Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.reply("Wystąpił błąd ze zdobyciem zdjęcia!");
                return false;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.getMessageChannel().sendFile(img, "blurple.png").reference(context.getMessage()).queue();
        } catch (IOException | NullPointerException e) {
            context.reply(context.getTranslated("image.server.fail"));
        }

        return true;
    }
}
