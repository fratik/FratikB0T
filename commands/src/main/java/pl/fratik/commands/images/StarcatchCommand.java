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

package pl.fratik.commands.images;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.net.URLEncoder;

public class StarcatchCommand extends NewCommand {
    public StarcatchCommand() {
        name = "starcatch";
        usage = "[osoba:user] [tryb_przycinania:string] [rozszerzone:bool]";
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        User user = context.getArgumentOr("osoba", context.getSender(), OptionMapping::getAsUser);
        String cutMode = context.getArgumentOr("tryb_przycinania", "cut", OptionMapping::getAsString);
        boolean extended = context.getArgumentOr("rozszerzone", false, OptionMapping::getAsBoolean);
        context.deferAsync(false);
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/starcatch?avatarURL=" + URLEncoder.encode(user.getEffectiveAvatarUrl()
                            .replace(".webp", ".png") + "?size=2048", "UTF-8") +
                            "&cutMode=" + cutMode + "&extended=" + extended,
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null || !zdjecie.getBoolean("success")) {
                context.reply(context.getTranslated("image.server.fail"));
                return;
            }
            byte[] img = NetworkUtil.getBytesFromBufferArray(zdjecie.getJSONObject("image").getJSONArray("data"));
            context.sendMessage("starcatch.png", img);
        } catch (IOException | NullPointerException e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }

    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("tryb_przycinania")) {
            option.addChoice("przyciete", "cut");
            option.addChoice("rozciagniete", "uncut");
        }
    }
}
