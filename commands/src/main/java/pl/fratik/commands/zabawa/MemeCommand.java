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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;

public class MemeCommand extends Command {
    private static final Random random = new Random();

    public MemeCommand() {
        name = "meme";
        category = CommandCategory.FUN;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        aliases = new String[] {"memes", "pokazmiswojetowary", "memez"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        JSONObject json;
        Object[] posts;
        try {
            json = NetworkUtil.getJson("https://www.reddit.com/r/dankmemes/top/.json?sort=top&t=day&limit=500");
            if (json == null) throw new IOException("kasia to pizda");
            posts = json.getJSONObject("data").getJSONArray("children").toList()
                    .stream().filter(a -> new JSONObject(writeValueAsString(a)).getJSONObject("data")
                            .getString("post_hint").equals("image")).toArray();
        } catch (Exception e) {
            context.send(context.getTranslated("meme.failed"));
            return false;
        }
        JSONObject post = new JSONObject(writeValueAsString(posts[random.nextInt(posts.length)]));
        Color color;
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/primColor?imageURL=" +
                            URLEncoder.encode(post.getJSONObject("data").getString("url"), "UTF-8"),
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null) {
                color = new Color(114, 137, 2108);
            } else {
                int r = -1;
                int g = -1;
                int b = -1;
                for (Object _color : zdjecie.getJSONArray("color")) {
                    if (r == -1) r = (int) _color;
                    if (g == -1) g = (int) _color;
                    if (b == -1) b = (int) _color;
                }
                color = new Color(r, g, b);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(MemeCommand.class).error("Błąd w uzyskiwaniu koloru!", e);
            color = new Color(114, 137, 218);
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(color);
        eb.setImage(post.getJSONObject("data").getString("url"));
        eb.setTitle(post.getJSONObject("data").getString("title"), "https://reddit.com" +
                post.getJSONObject("data").getString("permalink"));
        eb.setFooter("\uD83D\uDC4D " + post.getJSONObject("data").getInt("ups") + " | \uD83D\uDCAC " +
                post.getJSONObject("data").getInt("num_comments"), null);
        context.send(eb.build());
        return true;
    }

    private String writeValueAsString(Object a) {
        try {
            return new ObjectMapper().writeValueAsString(a);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
