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

import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BigemojiCommand extends NewCommand {
    public BigemojiCommand() {
        name = "bigemoji";
        usage = "<emotki:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (context.getArguments().get("emotki").getMentions().getCustomEmojis().isEmpty()) {
            context.replyEphemeral(context.getTranslated("bigemoji.no.emojis"));
            return;
        }
        context.deferAsync(false);
        try {
            StringBuilder sb = new StringBuilder(Ustawienia.instance.apiUrls.get("image-server"));
            sb.append("/api/polacz?");
            for (CustomEmoji emotka : context.getArguments().get("emotki").getMentions().getCustomEmojis()) {
                sb.append("zdjecie[]=");
                sb.append(URLEncoder.encode(emotka.getImageUrl(), StandardCharsets.UTF_8));
                sb.append("&");
            }
            sb.setLength(sb.length() - 1);
            JSONObject zdjecie = NetworkUtil.getJson(sb.toString(), Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.sendMessage(context.getTranslated("image.server.fail"));
                return;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.sendMessage("bigemoji.png", img);
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }
    }
}
