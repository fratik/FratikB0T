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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointUserBests;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;

import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;

public class OsuCommand extends Command {
    private final Osu osu;
    private final ShardManager shardManager;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public OsuCommand(ShardManager shardManager, EventWaiter eventWaiter, EventBus eventBus) {
        this.shardManager = shardManager;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "osu";
        category = CommandCategory.UTILITY;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("nick", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true});
        uzycieDelim = " ";
        allowInDMs = true;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        osu = Osu.getAPI(Ustawienia.instance.apiKeys.get("osu"));
        cooldown = 15;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name = "topPlay")
    public boolean topPlay(@NotNull CommandContext context) {
        try {
            List<OsuScore> wyniki = osu.userBests
                    .query(new EndpointUserBests.ArgumentsBuilder((String) context.getArgs()[0]).setLimit(15).build());
            Message mes = context.send(context.getTranslated("generic.loading"));
            List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
            for (OsuScore w : wyniki) {
                pages.add(new FutureTask<>(() -> {
                    OsuUser u = w.getUser().get();
                    OsuBeatmap m = w.getBeatmap().get();
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setAuthor(u.getUsername(), u.getURL().toString(), "https://a.ppy.sh/" + u.getID());
                    eb.addField(context.getTranslated("osu.topplay.beatmap"), generateBeatmapString(m),
                            false);
                    eb.addField(context.getTranslated("osu.topplay.score"),
                            generateScore(w), true);
                    eb.addField("", generateScoreSecLine(w), true);
                    eb.addField("", generateScoreThirdLine(w), true);
                    eb.addField(context.getTranslated("osu.topplay.pp"),
                            String.valueOf(CommonUtil.round(w.getPp(), 2, RoundingMode.HALF_UP)), true);
                    eb.addField(context.getTranslated("osu.topplay.combo"), w.getMaxCombo() + "x", true);
                    eb.addField(context.getTranslated("osu.topplay.fc"), w.isPerfect() ?
                            context.getTranslated("generic.yes") : context.getTranslated("generic.no"), true);
                    eb.addField(context.getTranslated("osu.topplay.mods"), getMods(w), true);
                    eb.setTimestamp(w.getDate());
                    String imgUrl = "https://assets.ppy.sh/beatmaps/" + m.getBeatmapSetID() + "/covers/cover.jpg";
                    eb.setImage(imgUrl);
                    eb.setColor(CommonUtil.getPrimColorFromImageUrl(imgUrl));
                    return eb;
                }));
            }
            new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                    context.getTlumaczenia(), eventBus).create(mes);
        } catch (OsuAPIException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String getMods(OsuScore w) {
        if (w.getEnabledMods().length == 0) return "No Mods";
        StringBuilder sb = new StringBuilder();
        for (GameMod m : w.getEnabledMods()) {
            sb.append(m.getName()).append("\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String generateScore(OsuScore w) {
        return String.format("**%s**\n%s", w.getScore(), getRank(w));
    }

    private String getRank(OsuScore w) {
        String rank = w.getRank();
        switch (rank) {
            case "XH":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuSSH) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuSSH)).getAsMention() : "XH";
            case "X":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuSS) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuSS)).getAsMention() : "SS";
            case "SH":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuSH) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuSH)).getAsMention() : "SH";
            case "S":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuS) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuS)).getAsMention() : "S";
            case "A":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuA) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuA)).getAsMention() : "A";
            case "B":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuB) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuB)).getAsMention() : "B";
            case "C":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuC) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuC)).getAsMention() : "C";
            case "D":
                return checkEmotkiStr(Ustawienia.instance.emotki.osuD) ? Objects.requireNonNull(shardManager
                        .getEmoteById(Ustawienia.instance.emotki.osuD)).getAsMention() : "D";
            default:
                return "";
        }
    }

    private String generateScoreSecLine(OsuScore w) {
        if (checkEmotki(Ustawienia.instance.emotki)) {
            return String.format("%s %s\n%s %s\n%s %s",
                    getEmotka(Ustawienia.instance.emotki.osu300), w.getHit300(),
                    getEmotka(Ustawienia.instance.emotki.osu100), w.getHit100(),
                    getEmotka(Ustawienia.instance.emotki.osu50), w.getHit50());
        } else {
            return String.format("%s - %s\n%s - %s\n%s - %s",
                    "300", w.getHit300(), "100", w.getHit100(), "50", w.getHit50());
        }
    }

    private String generateScoreThirdLine(OsuScore w) {
        if (checkEmotki(Ustawienia.instance.emotki)) {
            return String.format("%s %s\n%s %s\n%s %s", getEmotka(Ustawienia.instance.emotki.osugeki), w.getGekis(),
                    getEmotka(Ustawienia.instance.emotki.osukatu), w.getKatus(),
                    getEmotka(Ustawienia.instance.emotki.osumiss), w.getMisses());
        } else {
            return String.format("%s %s\n%s %s\n%s %s", "\u6fc0", w.getGekis(), "\u559d", w.getKatus(), "X",
                    w.getMisses());
        }
    }

    private boolean checkEmotki(Ustawienia.Emotki emotki) {
        return checkEmotkiStr(emotki.osu300) && checkEmotkiStr(emotki.osu100) && checkEmotkiStr(emotki.osu50) &&
                checkEmotkiStr(emotki.osuA) && checkEmotkiStr(emotki.osuB) && checkEmotkiStr(emotki.osuC) &&
                checkEmotkiStr(emotki.osuD) && checkEmotkiStr(emotki.osugeki) && checkEmotkiStr(emotki.osukatu) &&
                checkEmotkiStr(emotki.osumiss) && checkEmotkiStr(emotki.osuS) && checkEmotkiStr(emotki.osuSH) &&
                checkEmotkiStr(emotki.osuSS) && checkEmotkiStr(emotki.osuSSH);
    }
    
    private boolean checkEmotkiStr(String uwuOwo) {
        return uwuOwo != null && !uwuOwo.isEmpty() && shardManager.getEmoteById(uwuOwo) != null;
    }

    private Emote getEmotka(String id) {
        if (id == null || id.isEmpty()) return null;
        return shardManager.getEmoteById(id);
    }

    private String generateBeatmapString(OsuBeatmap m) throws MalformedURLException {
        return m.getTitle() +"\n" + m.getArtist() + " // " + m.getCreatorName() + "\n" + "**" + m.getVersion() + "**" +
                "\n[Link](" + m.getURL().toString() + ")";
    }
}
