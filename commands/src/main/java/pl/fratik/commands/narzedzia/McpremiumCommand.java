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

package pl.fratik.commands.narzedzia;

import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;

import java.text.SimpleDateFormat;
import java.util.*;

public class McpremiumCommand extends NewCommand {

    public McpremiumCommand() {
        name = "mcpremium";
        usage = "<nick:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String uuid;
        String name;
        String nick = context.getArguments().get("nick").getAsString();
        context.deferAsync(false);
        try {
            JSONObject jOb = NetworkUtil.getJson("https://api.mojang.com/users/profiles/minecraft/" + NetworkUtil.encodeURIComponent(nick));
            uuid = Objects.requireNonNull(jOb).getString("id");
            name = Objects.requireNonNull(jOb).getString("name");
            JSONArray lista = NetworkUtil.getJsonArray("https://api.mojang.com/user/profiles/" + uuid + "/names");
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("mcpremium.failed", nick));
            Sentry.capture(new EventBuilder().withMessage(e.getMessage())
                    .withSentryInterface(new ExceptionInterface(e)).withExtra("nick", nick));
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.addField(context.getTranslated("mcpremium.nazwa"), name, false);
        eb.addField("UUID", formatUuid(uuid), false);
        eb.addField(context.getTranslated("mcpremium.namemc"), "[namemc.com](https://namemc.com/profile/" + uuid + ")", false);
        eb.setFooter(context.getTranslated("mcpremium.infonick", name), null);
        eb.setThumbnail("https://minotar.net/helm/" + name + "/2048.png");
        eb.setImage("https://minotar.net/armor/body/" + name + "/124.png");
        context.reply(eb.build());
        return;
    }

    private static String formatUuid(String uuid) {
        String regex = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";
        return UUID.fromString(uuid.replaceFirst(regex, "$1-$2-$3-$4-$5")).toString();
    }

}
