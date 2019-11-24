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
                    .query(new EndpointUserBests.ArgumentsBuilder((String) context.getArgs()[0]).setLimit(100).build());
            Message mes = context.send(context.getTranslated("generic.loading"));
            List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
            for (OsuScore w : wyniki) {
                pages.add(new FutureTask<>(() -> renderScore(context, w)));
            }
            new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                    context.getTlumaczenia(), eventBus, false).setEnableShuffle(true).create(mes);
        } catch (OsuAPIException e) {
            e.printStackTrace();
        }
        return true;
    }

    @NotNull
    private EmbedBuilder renderScore(@NotNull CommandContext context, OsuScore w) throws OsuAPIException, MalformedURLException {
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
        eb.addField(context.getTranslated("osu.topplay.acc"),
                CommonUtil.round(calcAcc(w) * 100, 2, RoundingMode.HALF_UP) + "%", true);
        eb.setTimestamp(w.getDate());
        String imgUrl = "https://assets.ppy.sh/beatmaps/" + m.getBeatmapSetID() + "/covers/cover.jpg";
        eb.setImage(imgUrl);
        eb.setColor(CommonUtil.getPrimColorFromImageUrl(imgUrl));
        return eb;
    }

    private double calcAcc(OsuScore w) {
        return (double) (50 * w.getHit50() + 100 * w.getHit100() + 300 * w.getHit300()) /
                (300 * (w.getMisses() + w.getHit50() + w.getHit100() + w.getHit300()));
    }

    private String getMods(OsuScore w) {
        if (w.getEnabledMods().length == 0) return "No Mods";
        StringBuilder sb = new StringBuilder();
        for (GameMod m : w.getEnabledMods()) {
            if (!checkEmotki(Ustawienia.instance.emotki)) {
                sb.append(m.getName()).append("\n");
            } else {
                try {
                    sb.append(getEmotka((String) Ustawienia.instance.emotki.getClass().getDeclaredField("osu" +
                            getOsuShortMod(m)).get(Ustawienia.instance.emotki))).append("\n");
                } catch (NoSuchFieldException | IllegalAccessException | ClassCastException | OsuAPIException e) {
                    sb.append(m.getName()).append("\n");
                }
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String getOsuShortMod(GameMod m) throws OsuAPIException {
        switch (m) {
            case EASY:
                return "EZ";
            case KEY_1:
                return "1K";
            case KEY_2:
                return "2K";
            case KEY_3:
                return "3K";
            case KEY_4:
                return "4K";
            case KEY_5:
                return "5K";
            case KEY_6:
                return "6K";
            case KEY_7:
                return "7K";
            case KEY_8:
                return "8K";
            case KEY_9:
                return "9K";
            case RELAX:
                return "RX";
            case CINEMA:
                return "CN";
            case HIDDEN:
                return "HD";
            case RANDOM:
                return "RD";
            case TARGET:
                return "TP";
            case FADE_IN:
                return "FI";
            case NO_FAIL:
                return "NF";
            case PERFECT:
                return "PF";
            case SPUNOUT:
                return "SO";
            case AUTOPLAY:
                return "AO";
            case KEY_COOP:
                return "COOPK";
            case LAST_MOD:
                return "LM";
            case SCORE_V2:
                return "V2";
            case AUTOPILOT:
                return "AP";
            case HALF_TIME:
                return "HT";
            case HARD_ROCK:
                return "HR";
            case NIGHTCORE:
                return "NC";
            case FLASHLIGHT:
                return "FL";
            case DOUBLE_TIME:
                return "DT";
            case SUDDEN_DEATH:
                return "SD";
            case TOUCH_DEVICE:
                return "TD";
        }
        throw new OsuAPIException("kurwa co to za mod");
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
                        .getEmoteById(Ustawienia.instance.emotki.osuSS)).getAsMention() : "X";
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
                checkEmotkiStr(emotki.osuSS) && checkEmotkiStr(emotki.osuSSH) && checkEmotkiStr(emotki.osu1K) &&
                checkEmotkiStr(emotki.osu2K) && checkEmotkiStr(emotki.osu3K) && checkEmotkiStr(emotki.osu4K) &&
                checkEmotkiStr(emotki.osu5K) && checkEmotkiStr(emotki.osu6K) && checkEmotkiStr(emotki.osu7K) &&
                checkEmotkiStr(emotki.osu8K) && checkEmotkiStr(emotki.osu9K) && checkEmotkiStr(emotki.osuNF) &&
                checkEmotkiStr(emotki.osuEZ) && checkEmotkiStr(emotki.osuTD) && checkEmotkiStr(emotki.osuHD) &&
                checkEmotkiStr(emotki.osuHR) && checkEmotkiStr(emotki.osuSD) && checkEmotkiStr(emotki.osuDT) &&
                checkEmotkiStr(emotki.osuRX) && checkEmotkiStr(emotki.osuHT) && checkEmotkiStr(emotki.osuNC) &&
                checkEmotkiStr(emotki.osuFL) && checkEmotkiStr(emotki.osuAO) && checkEmotkiStr(emotki.osuSO) &&
                checkEmotkiStr(emotki.osuAP) && checkEmotkiStr(emotki.osuPF) && checkEmotkiStr(emotki.osuFI) &&
                checkEmotkiStr(emotki.osuRD) && checkEmotkiStr(emotki.osuCN) && checkEmotkiStr(emotki.osuTP) &&
                checkEmotkiStr(emotki.osuCOOPK) && checkEmotkiStr(emotki.osuV2) && checkEmotkiStr(emotki.osuLM);
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
