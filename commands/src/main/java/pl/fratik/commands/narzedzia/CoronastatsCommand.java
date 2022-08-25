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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.JSONResponse;
import pl.fratik.core.util.NetworkUtil;

import java.awt.*;
import java.io.IOException;
import java.sql.Date;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.FutureTask;

@SuppressWarnings("DuplicatedCode")
public class CoronastatsCommand extends NewCommand {
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public CoronastatsCommand(EventWaiter eventWaiter, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "coronastats";
        cooldown = 5;
        usage = "[kraj:string]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        Instant dzisiaj = cal.toInstant();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Instant wczoraj = cal.toInstant();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Instant dwaDniTemu = cal.toInstant();
        if (!context.getArguments().containsKey("kraj")) {
            try {
                JSONObject staty = NetworkUtil.getJson("https://disease.sh/v3/covid-19/all");
                if (staty == null) throw new IOException("tak");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(context.getTranslated("coronastats.basic.header"));
                formatDate(context, eb, dzisiaj.toEpochMilli());
                addFields(eb, context, staty);
                pages.add(new FutureTask<>(() -> eb));
            } catch (IOException e) {
                context.reply(context.getTranslated("coronastats.api.error"));
                return;
            }
            pages.add(new FutureTask<>(() -> {
                JSONObject staty = NetworkUtil.getJson("https://disease.sh/v3/covid-19/all?yesterday=1");
                if (staty == null) throw new IOException("tak");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(context.getTranslated("coronastats.basic.header"));
                formatDate(context, eb, wczoraj.toEpochMilli());
                addFields(eb, context, staty);
                return eb;
            }));
            pages.add(new FutureTask<>(() -> {
                JSONObject staty = NetworkUtil.getJson("https://disease.sh/v3/covid-19/all?twoDaysAgo=1");
                if (staty == null) throw new IOException("tak");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(context.getTranslated("coronastats.basic.header"));
                formatDate(context, eb, dwaDniTemu.toEpochMilli());
                addFields(eb, context, staty);
                return eb;
            }));
        } else {
            try {
                String path = "https://disease.sh/v3/covid-19/countries/" +
                        NetworkUtil.encodeURIComponent(context.getArguments().get("kraj").getAsString());
                try {
                    JSONResponse staty = NetworkUtil.getJson(path);
                    if (staty == null) throw new IOException("tak");
                    if (staty.getCode() != 200) throw new JSONException("tak");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor(context.getTranslated("coronastats.country.header", staty.get("country")));
                    eb.setThumbnail(staty.getJSONObject("countryInfo").getString("flag"));
                    formatDate(context, eb, dzisiaj.toEpochMilli());
                    addFields(eb, context, staty);
                    if (staty.getString("country").equals("Poland")) {
                        eb.setImage("https://cdn.discordapp.com/attachments/424887765478539264/694101067461427281/ezgif.com-optimize.gif");
                    }
                    pages.add(new FutureTask<>(() -> eb));
                } catch (JSONException e) {
                    context.reply(context.getTranslated("coronastats.unknown.country"));
                    return;
                }
                pages.add(new FutureTask<>(() -> {
                    JSONObject staty2 = NetworkUtil.getJson(path + "?yesterday=1");
                    if (staty2 == null) throw new IOException("tak");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor(context.getTranslated("coronastats.country.header", staty2.get("country")));
                    eb.setThumbnail(staty2.getJSONObject("countryInfo").getString("flag"));
                    formatDate(context, eb, wczoraj.toEpochMilli());
                    addFields(eb, context, staty2);
                    if (staty2.getString("country").equals("Poland")) {
                        eb.setImage("https://cdn.discordapp.com/attachments/424887765478539264/694101067461427281/ezgif.com-optimize.gif");
                    }
                    return eb;
                }));
                pages.add(new FutureTask<>(() -> {
                    JSONObject staty2 = NetworkUtil.getJson(path + "?twoDaysAgo=1");
                    if (staty2 == null) throw new IOException("tak");
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor(context.getTranslated("coronastats.country.header", staty2.get("country")));
                    eb.setThumbnail(staty2.getJSONObject("countryInfo").getString("flag"));
                    formatDate(context, eb, dwaDniTemu.toEpochMilli());
                    addFields(eb, context, staty2);
                    if (staty2.getString("country").equals("Poland")) {
                        eb.setImage("https://cdn.discordapp.com/attachments/424887765478539264/694101067461427281/ezgif.com-optimize.gif");
                    }
                    return eb;
                }));
            } catch (IOException e) {
                context.reply(context.getTranslated("coronastats.api.error"));
                return;
            }
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(hook);
    }

    private void addFields(EmbedBuilder eb, NewCommandContext ctx, JSONObject staty) {
        eb.addField(ctx.getTranslated("coronastats.cases"), parseNumber(staty.getLong("cases"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.deaths"), parseNumber(staty.getLong("deaths"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.recovered"), parseNumber(staty.getLong("recovered"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.cases.today"), parseNumber(staty.getLong("todayCases"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.deaths.today"), parseNumber(staty.getLong("todayDeaths"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.active"), parseNumber(staty.getLong("active"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.critical"), parseNumber(staty.getLong("critical"), ctx.getLanguage()), true);
        eb.addField(ctx.getTranslated("coronastats.tests"), parseNumber(staty.getLong("tests"), ctx.getLanguage()), true);
        eb.setFooter(" (%s/%s) | " + ctx.getTranslated("coronastats.updated"));
        eb.setColor(getColor(staty.getInt("active")));
        eb.setTimestamp(Instant.ofEpochMilli(staty.getLong("updated")));
    }

    private String parseNumber(long num, Language language) {
        return NumberFormat.getInstance(language.getLocale()).format(num);
    }

    private void formatDate(@NotNull NewCommandContext context, EmbedBuilder eb, long updated) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", context.getLanguage().getLocale());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        eb.setDescription(context.getTranslated("coronastats.content", sdf.format(new Date(updated))));
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
