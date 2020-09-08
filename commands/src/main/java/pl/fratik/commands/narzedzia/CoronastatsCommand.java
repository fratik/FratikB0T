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
import org.json.JSONException;
import org.json.JSONObject;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.JSONResponse;
import pl.fratik.core.util.NetworkUtil;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;

public class CoronastatsCommand extends Command {
    public CoronastatsCommand() {
        name = "coronastats";
        aliases = new String[] {"coronavirus", "koronawirus", "covid"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        cooldown = 5;
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("kraj", "string", false);
        allowPermLevelChange = false;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            try {
                JSONObject staty = NetworkUtil.getJson("https://corona.lmao.ninja/v2/all");
                if (staty == null) throw new IOException("tak");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(context.getTranslated("coronastats.basic.header"));
                eb.addField(context.getTranslated("coronastats.cases"), String.valueOf(staty.getInt("cases")),
                        true);
                eb.addField(context.getTranslated("coronastats.deaths"), String.valueOf(staty.getInt("deaths")),
                        true);
                eb.addField(context.getTranslated("coronastats.recovered"), String.valueOf(staty.getInt("recovered")),
                        true);
                eb.addField(context.getTranslated("coronastats.active"), String.valueOf(staty.getInt("active")),
                        true);
                eb.setFooter(context.getTranslated("coronastats.updated"));
                eb.setColor(getColor(staty.getInt("active")));
                eb.setTimestamp(Instant.ofEpochMilli(staty.getLong("updated")));
                context.send(eb.build());
            } catch (IOException e) {
                context.send(context.getTranslated("coronastats.api.error"));
                return false;
            }
        } else {
            try {
                JSONResponse staty;
                try {
                    staty = NetworkUtil.getJson("https://corona.lmao.ninja/v2/countries/" +
                            NetworkUtil.encodeURIComponent((String) context.getArgs()[0]));
                    if (staty == null) throw new IOException("tak");
                    if (staty.getCode() != 200) throw new JSONException("tak");
                } catch (JSONException e) {
                    context.send(context.getTranslated("coronastats.unknown.country"));
                    return false;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(context.getTranslated("coronastats.country.header", staty.get("country")));
                eb.setThumbnail(staty.getJSONObject("countryInfo").getString("flag"));
                eb.addField(context.getTranslated("coronastats.cases"), String.valueOf(staty.getInt("cases")),
                        true);
                eb.addField(context.getTranslated("coronastats.deaths"), String.valueOf(staty.getInt("deaths")),
                        true);
                eb.addField(context.getTranslated("coronastats.recovered"), String.valueOf(staty.getInt("recovered")),
                        true);
                eb.addField(context.getTranslated("coronastats.cases.today"), String.valueOf(staty.getInt("todayCases")),
                        true);
                eb.addField(context.getTranslated("coronastats.deaths.today"), String.valueOf(staty.getInt("todayDeaths")),
                        true);
                eb.addField(context.getTranslated("coronastats.critical"), String.valueOf(staty.getInt("critical")),
                        true);
                eb.addField(context.getTranslated("coronastats.active"), String.valueOf(staty.getInt("active")),
                        true);
                if (staty.getString("country").equals("Poland")) {
                    eb.setImage("https://cdn.discordapp.com/attachments/424887765478539264/694101067461427281/ezgif.com-optimize.gif");
                }
                eb.setColor(getColor(staty.getInt("active")));
                eb.setFooter(context.getTranslated("coronastats.updated"));
                eb.setTimestamp(Instant.ofEpochMilli(staty.getLong("updated")));
                context.send(eb.build());
            } catch (IOException e) {
                context.send(context.getTranslated("coronastats.api.error"));
                return false;
            }
        }
        return true;
    }
    private Color getColor(int cases) {
        if (cases < 10000) return new Color(0xB0FF00);
        else if (cases < 25000) return new Color(0xFFFF00);
        else if (cases < 50000) return new Color(0xFF7900);
        else if (cases < 75000) return new Color(0xAE5500);
        else if (cases < 100000) return new Color(0xFF0000);
        else return new Color(0x000000);
    }
}
