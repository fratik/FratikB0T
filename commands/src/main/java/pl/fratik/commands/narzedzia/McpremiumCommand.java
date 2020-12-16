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

package pl.fratik.commands.narzedzia;

import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;

import java.text.SimpleDateFormat;
import java.util.*;

public class McpremiumCommand extends Command {

    public McpremiumCommand() {
        name = "mcpremium";
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("nick", "string", true);
        allowInDMs = true;
        aliases = new String[] {"minecraftpremium", "premka", "mcpremka", "premiummc"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String uuid;
        String name;
        List<String> listaNazw = new ArrayList<>();
        String nick = (String) context.getArgs()[0];
        try {
            JSONObject jOb = NetworkUtil.getJson("https://api.mojang.com/users/profiles/minecraft/" + NetworkUtil.encodeURIComponent(nick));
            uuid = Objects.requireNonNull(jOb).getString("id");
            name = Objects.requireNonNull(jOb).getString("name");
            JSONArray lista = NetworkUtil.getJsonArray("https://api.mojang.com/user/profiles/" + uuid + "/names");
            boolean first = true;
            if (lista != null) {
                List<JSONObject> tak = new ArrayList<>();
                for (Object xd : lista) tak.add((JSONObject) xd);
                Collections.reverse(tak);
                for (JSONObject obj : tak) {
                    StringBuilder sb = new StringBuilder();
                    if (first) sb.append("**");
                    sb.append(MarkdownSanitizer.escape(obj.getString("name")));
                    if (first) sb.append("**");
                    first = false;
                    if (obj.has("changedToAt")) {
                        long timestamp = obj.getLong("changedToAt");
                        Date zmienioneO = new Date(timestamp);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", context.getLanguage().getLocale());
                        sb.append(" ").append(context.getTranslated("mcpremium.changed.to.at", sdf.format(zmienioneO)));
                    }
                    listaNazw.add(sb.toString());
                    if (listaNazw.size() >= 10) {
                        listaNazw.add(context.getTranslated("mcstatus.embed.players.more", tak.size() - listaNazw.size()));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            context.reply(context.getTranslated("mcpremium.failed", nick));
            Sentry.capture(new EventBuilder().withMessage(e.getMessage())
                    .withSentryInterface(new ExceptionInterface(e)).withExtra("nick", nick));
            return false;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.addField(context.getTranslated("mcpremium.nazwa"), name, false);
        eb.addField("UUID", formatUuid(uuid), false);
        eb.addField(context.getTranslated("mcpremium.namemc"), "[namemc.com](https://namemc.com/profile/" + uuid + ")", false);
        eb.setFooter(context.getTranslated("mcpremium.infonick", name), null);
        if (listaNazw.size() > 1) {
            eb.addField(context.getTranslated("mcpremium.namehistory"), String.join("\n", listaNazw),
                    false);
        }
        eb.setThumbnail("https://minotar.net/helm/" + name + "/2048.png");
        eb.setImage("https://minotar.net/armor/body/" + name + "/124.png");
        context.reply(eb.build());
        return true;
    }

    private static String formatUuid(String uuid) {
        String regex = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";
        return UUID.fromString(uuid.replaceFirst(regex, "$1-$2-$3-$4-$5")).toString();
    }

}
