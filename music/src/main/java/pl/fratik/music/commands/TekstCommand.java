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

package pl.fratik.music.commands;

import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.NetworkUtil;
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
        requireConnection = false;
        usage = "[tytul:string]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String q = context.getArgumentOr("tytul",
            managerMuzyki.getManagerMuzykiSerwera(context.getGuild()).isPlaying() ? managerMuzyki.getManagerMuzykiSerwera(context.getGuild()).getAktualnaPiosenka().getAudioTrack().getInfo().title
                : null,
            OptionMapping::getAsString);

        if (q == null) {
            context.replyEphemeral(context.getTranslated("tekst.empty.args"));
            return;
        }

        InteractionHook hook = context.defer(false);
        try {
            Piosenka p = requestGenius(q);
            if (p == null) {
                p = requestTekstowo(q);
                if (p == null) {
                    context.sendMessage(context.getTranslated("tekst.not.found.usedtitle"));
                    return;
                }
            }
            if (p.slowa.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                List<EmbedBuilder> pages = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String slowaChunk : p.slowa.split("\n\n")) {
                    String nextPage = context.getTranslated("tekst.next.page");
                    if (sb.length() + slowaChunk.length() + "\n\n".length() + nextPage.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                        pages.add(new EmbedBuilder().setTitle(p.title, p.website)
                                .setThumbnail(p.imageLink).setColor(CommonUtil.getPrimColorFromImageUrl(p.imageLink))
                                .setDescription(sb).appendDescription(nextPage));
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
                        context.getTlumaczenia(), eventBus).create(hook);
                return;
            }
            hook.editOriginalEmbeds(new EmbedBuilder().setTitle(p.title, p.website)
                    .setThumbnail(p.imageLink).setColor(CommonUtil.getPrimColorFromImageUrl(p.imageLink))
                    .setDescription(p.slowa).build()).setContent("").complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("tekst.failed"));
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
        List<Element> elements = doc.select("div[data-lyrics-container]");
        StringBuilder slowa = new StringBuilder();
        for (Element element : elements) {
            NodeTraversor.traverse(new NodeVisitor() {
                @Override
                public void head(Node n, int depth) {
                    if (n instanceof TextNode) {
                        slowa.append(((TextNode) n).text());
                    } else if (n instanceof Element) {
                        Element e = (Element) n;
                        switch (e.tagName()) {
                            case "br":
                                slowa.append("\n");
                                break;
                            case "a":
                                slowa.append(e.ownText());
                                break;
                            case "i":
                                slowa.append("_");
                                break;
                            case "b":
                                slowa.append("**");
                                break;
                            default:
                                break;
                        }
                    }
                }

                @Override
                public void tail(Node n, int depth) {
                    if (n instanceof Element) {
                        Element e = (Element) n;
                        switch (e.tagName()) {
                            case "i":
                                slowa.append("_");
                                break;
                            case "b":
                                slowa.append("**");
                                break;
                            default:
                                break;
                        }
                    }
                }
            }, element);
            slowa.append("\n");
        }
        return new Piosenka(imageLink, title, "https://genius.com" + lyricsPath, slowa.substring(0, slowa.length() - 1));
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
