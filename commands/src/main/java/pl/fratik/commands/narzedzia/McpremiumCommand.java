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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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
        try {
            JSONObject jOb = NetworkUtil.getJson("https://api.mojang.com/users/profiles/minecraft/" + NetworkUtil.encodeURIComponent((String) context.getArgs()[0]));
            uuid = Objects.requireNonNull(jOb).getString("id");
            name = Objects.requireNonNull(jOb).getString("name");
            JSONArray lista = NetworkUtil.getJsonArray("https://api.mojang.com/user/profiles/" + uuid + "/names");
            for (Object tfu : Objects.requireNonNull(lista)) {
                JSONObject obj = (JSONObject) tfu;
                StringBuilder sb = new StringBuilder();
                sb.append(obj.getString("name"));
                if (obj.has("changedToAt")) {
                    long timestamp = obj.getLong("changedToAt");
                    Date zmienioneO = new Date(timestamp);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", context.getLanguage().getLocale());
                    sb.append(" ").append(context.getTranslated("mcpremium.changed.to.at", sdf.format(zmienioneO)));
                }
                listaNazw.add(sb.toString());
            }
            Collections.reverse(listaNazw);
            String xd = listaNazw.remove(0);
            StringBuilder tekstPierw = new StringBuilder();
            StringBuilder tekstDalej = new StringBuilder();
            for (int i = 0; i < xd.split(" ").length; i++) {
                if (i == 0)
                    tekstPierw.append(xd.split(" ")[i]);
                else {
                    tekstDalej.append(xd.split(" ")[i]);
                    if (i + 1 < xd.split(" ").length) tekstDalej.append(" ");
                }
            }
            listaNazw.add(0, "**" + tekstPierw.toString() + "** " + tekstDalej.toString());
        } catch (Exception e) {
            context.send(context.getTranslated("mcpremium.failed", (String) context.getArgs()[0]));
            return false;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.addField(context.getTranslated("mcpremium.nazwa"), name, false);
        eb.addField("UUID", formatUuid(uuid), false);
        eb.addField(context.getTranslated("mcpremium.namemc"), "[namemc.com](https://namemc.com/profile/" + uuid + ")", false);
        eb.setFooter(context.getTranslated("mcpremium.infonick", name), null);
        if (listaNazw.size() > 1)
            eb.addField(context.getTranslated("mcpremium.namehistory"), String.join("\n", listaNazw),
                    false);
        eb.setThumbnail("https://minotar.net/helm/" + name + "/2048.png");
        eb.setImage("https://minotar.net/armor/body/" + name + "/124.png");
        context.send(eb.build());
        return true;
    }

    private static String formatUuid(String uuid) {
        String regex = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";
        return UUID.fromString(uuid.replaceFirst(regex, "$1-$2-$3-$4-$5")).toString();
    }

}
