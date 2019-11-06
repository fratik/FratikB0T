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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Emoji;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;

import java.net.URLEncoder;
import java.util.LinkedHashMap;

public class BigemojiCommand extends Command {
    public BigemojiCommand() {
        name = "bigemoji";
        category = CommandCategory.FUN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("emotka", "emote");
        hmap.put("[...]", "emote");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        uzycieDelim = " ";
        aliases = new String[] {"bigmoji"};
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        try {
            StringBuilder sb = new StringBuilder(Ustawienia.instance.apiUrls.get("image-server"));
            sb.append("/api/polacz?");
            for (Object obj : context.getArgs()) {
                Emoji emotka = (Emoji) obj;
                sb.append("zdjecie[]=");
                sb.append(URLEncoder.encode(emotka.getImageUrl(), "UTF-8"));
                sb.append("&");
            }
            sb.setLength(sb.length() - 1);
            JSONObject zdjecie = NetworkUtil.getJson(sb.toString(), Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.send(context.getTranslated("image.server.fail"));
                return false;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.getChannel().sendFile(img, "bigemoji.png").queue();
            return true;
        } catch (Exception e) {
            context.send(context.getTranslated("image.server.fail"));
            return false;
        }
    }
}
