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

package pl.fratik.music.commands;

import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.*;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TekstCommand extends MusicCommand {
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final NowyManagerMuzyki managerMuzyki;

    public TekstCommand(EventWaiter eventWaiter, EventBus eventBus, NowyManagerMuzyki managerMuzyki) {
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.managerMuzyki = managerMuzyki;
        name = "tekst";
        aliases = new String[] {"lyrics"};
        requireConnection = false;
        uzycie = new Uzycie("tytul", "string", false);
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        String q;
        boolean usedTitle = false;
        if (context.getArgs().length >= 1 && context.getArgs()[0] != null) {
            q = (String) context.getArgs()[0];
        } else if (managerMuzyki.getManagerMuzykiSerwera(context.getGuild()).isPlaying()) {
            q = managerMuzyki.getManagerMuzykiSerwera(context.getGuild()).getAktualnaPiosenka().getAudioTrack()
                    .getInfo().title;
            usedTitle = true;
        } else {
            CommonErrors.usage(context);
            return false;
        }
        Message loading = context.send(context.getTranslated("generic.loading"));
        try {
            Piosenka p = requestGenius(q);
            if (p == null) {
                p = requestTekstowo(q);
                if (p == null) {
                    if (!usedTitle) loading.editMessage(context.getTranslated("tekst.not.found")).queue();
                    else loading.editMessage(context.getTranslated("tekst.not.found.usedtitle")).queue();
                    return false;
                }
            }
            if (p.slowa.length() >= 2000) {
                List<EmbedBuilder> pages = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String slowaChunk : p.slowa.split("\n\n")) {
                    if (sb.length() + slowaChunk.length() + "\n\n".length() >= 2000) {
                        pages.add(new EmbedBuilder().setTitle(p.title, p.website)
                                .setThumbnail(p.imageLink).setColor(CommonUtil.getPrimColorFromImageUrl(p.imageLink))
                                .setDescription(sb).appendDescription(context.getTranslated("tekst.next.page")));
                        sb = new StringBuilder(slowaChunk).append("\n\n");
                        continue;
                    }
                    sb.append(slowaChunk).append("\n\n");
                }
                if (sb.length() != 0) {
                    pages.add(new EmbedBuilder().setTitle(p.title, p.website)
                            .setThumbnail(p.imageLink).setColor(CommonUtil.getPrimColorFromImageUrl(p.imageLink))
                            .setDescription(sb));
                }
                new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                        context.getTlumaczenia(), eventBus).create(loading);
                return true;
            }
            loading.editMessage(new EmbedBuilder().setTitle(p.title, p.website)
                    .setThumbnail(p.imageLink).setColor(CommonUtil.getPrimColorFromImageUrl(p.imageLink))
                    .setDescription(p.slowa).build()).override(true).complete();
            return true;
        } catch (Exception e) {
            loading.editMessage(context.getTranslated("tekst.failed")).queue();
            return false;
        }
    }

    private Piosenka requestGenius(String q) throws IOException {
        JSONObject xd = NetworkUtil.getJson("https://api.genius.com/search?q=" + NetworkUtil.encodeURIComponent(q),
                "Bearer " + Ustawienia.instance.apiKeys.get("genius"));
        if (xd == null) throw new IOException("resp == null");
        JSONArray arr = xd.getJSONObject("response").getJSONArray("hits");
        if (arr.isEmpty()) return null;
        JSONObject song = arr.getJSONObject(0).getJSONObject("result");
        String imageLink = song.getString("song_art_image_url");
        String title = song.getString("full_title");
        String lyricsPath = song.getString("path");
        byte[] html = NetworkUtil.download("https://genius.com" + lyricsPath);
        Document doc = Jsoup.parse(new String(html, StandardCharsets.UTF_8));
        Element slowaElement = doc.select(".lyrics p").get(0);
        StringBuilder slowa = new StringBuilder();
        for (Node n : slowaElement.childNodes()) {
            if (n instanceof TextNode) {
                slowa.append(((TextNode) n).getWholeText());
            } else if (n instanceof Element) {
                if (((Element) n).tagName().equals("a")) slowa.append(((Element) n).wholeText());
                if (((Element) n).tagName().equals("i"))
                    slowa.append("_").append(((Element) n).wholeText()).append("_");
                if (((Element) n).tagName().equals("b")) slowa.append(((Element) n).wholeText());
            }
        }
        return new Piosenka(imageLink, title, "https://genius.com" + lyricsPath, slowa.toString());
    }

    private Piosenka requestTekstowo(String q) throws IOException {
        byte[] xd = NetworkUtil.download("https://www.tekstowo.pl/wyszukaj.html" +
                "?search-artist=Podaj+wykonawc%C4%99&search-title=" + NetworkUtil.encodeURIComponent(q));
        Document wyszukiwarka = Jsoup.parse(new String(xd, StandardCharsets.UTF_8));
        Elements przeboje = wyszukiwarka.select(".content>.box-przeboje>a.title");
        if (przeboje.isEmpty()) return null;
        Element przeboj = przeboje.get(0);
        String title = przeboj.attr("title");
        byte[] hrefPobrane = NetworkUtil.download("https://www.tekstowo.pl" + przeboj.attr("href"));
        Document tekst = Jsoup.parse(new String(hrefPobrane, StandardCharsets.UTF_8));
        Element slowaElement = tekst.select(".song-text").get(0);
        StringBuilder slowa = new StringBuilder();
        for (Node n : slowaElement.childNodes()) {
            if (n instanceof TextNode) {
                slowa.append(((TextNode) n).getWholeText());
            }
        }
        return new Piosenka(null, title, "https://www.tekstowo.pl" + przeboj.attr("href"),
                slowa.toString());
    }

    @AllArgsConstructor
    private static class Piosenka {
        private final String imageLink;
        private final String title;
        private final String website;
        private final String slowa;
    }
}
