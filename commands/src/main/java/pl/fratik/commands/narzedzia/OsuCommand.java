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
import com.oopsjpeg.osu4j.backend.EndpointUserRecents;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
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
import pl.fratik.core.util.*;

import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;

import static pl.fratik.core.Ustawienia.instance;
import static pl.fratik.core.util.CommonUtil.round;

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
        osu = Osu.getAPI(instance.apiKeys.get("osu"));
        cooldown = 15;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name = "user")
    public boolean user(@NotNull CommandContext context) {
        NumberFormat nf = NumberFormat.getInstance(context.getLanguage().getLocale());
        Message mes = context.send(context.getTranslated("generic.loading"));
        try {
            OsuUser u = osu.users.query(new EndpointUsers.ArgumentsBuilder((String) context.getArgs()[0]).build());
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(u.getUsername(), u.getURL().toString(), "https://a.ppy.sh/" + u.getID());
            eb.addField(context.getTranslated("osu.user.accuracy"),
                    round(u.getAccuracy(), 2, RoundingMode.HALF_UP) + "%", true);
            eb.addField(context.getTranslated("osu.user.ranks"), String.format("%s %s\n%s %s\n%s %s\n%s %s\n%s %s",
                    getEmotka(instance.emotki.osuSSH), u.getCountRankSSH(), getEmotka(instance.emotki.osuSS),
                    u.getCountRankSS(), getEmotka(instance.emotki.osuSH), u.getCountRankSH(),
                    getEmotka(instance.emotki.osuS), u.getCountRankS(), getEmotka(instance.emotki.osuA),
                    u.getCountRankA()), true);
            eb.addField(context.getTranslated("osu.user.country"),
                    ":flag_" + u.getCountry().name().toLowerCase() + ":", true);
            eb.addField(context.getTranslated("osu.user.rank"), context.getTranslated("osu.user.rank.text",
                    u.getRank(), u.getCountryRank()), true);
            eb.addField(context.getTranslated("osu.user.total.hits"), nf.format(u.getTotalHits()), true);
            eb.addField(context.getTranslated("osu.user.total.score"), nf.format(u.getTotalScore()), true);
            eb.addField(context.getTranslated("osu.user.total.ranked.score"), nf.format(u.getRankedScore()), true);
            eb.addField(context.getTranslated("osu.user.pp"), nf.format(u.getPPRaw()), true);
            eb.addField(context.getTranslated("osu.user.level"), nf.format(round(u.getLevel(), 2,
                    RoundingMode.HALF_UP)), true);
            eb.addField(context.getTranslated("osu.user.play.count"), nf.format(u.getPlayCount()), true);
            eb.addField(context.getTranslated("osu.user.played"),
                    DurationUtil.humanReadableFormat(u.getTotalSecondsPlayed() * 1000, false),
                    true);
            mes.editMessage(eb.build()).override(true).complete();
        } catch (OsuAPIException | MalformedURLException e) {
            mes.editMessage(context.getTranslated("osu.error")).queue();
            return false;
        }
        return true;
    }

    @SubCommand(name = "topPlay")
    public boolean topPlay(@NotNull CommandContext context) {
        Message mes = context.send(context.getTranslated("generic.loading"));
        try {
            List<OsuScore> wyniki = osu.userBests
                    .query(new EndpointUserBests.ArgumentsBuilder((String) context.getArgs()[0]).setLimit(100).build());
            if (wyniki.isEmpty()) {
                mes.editMessage(context.getTranslated("osu.topplay.empty")).queue();
                return false;
            }
            renderScores(context, mes, wyniki);
        } catch (OsuAPIException e) {
            mes.editMessage(context.getTranslated("osu.error")).queue();
            return false;
        }
        return true;
    }

    @SubCommand(name = "recentPlay")
    public boolean recentPlay(@NotNull CommandContext context) {
        Message mes = context.send(context.getTranslated("generic.loading"));
        try {
            List<OsuScore> wyniki = osu.userRecents
                    .query(new EndpointUserRecents.ArgumentsBuilder((String) context.getArgs()[0]).setLimit(50).build());
            if (wyniki.isEmpty()) {
                mes.editMessage(context.getTranslated("osu.recentplay.empty")).queue();
                return false;
            }
            renderScores(context, mes, wyniki);
        } catch (OsuAPIException e) {
            mes.editMessage(context.getTranslated("osu.error")).queue();
            return false;
        }
        return true;
    }

    private void renderScores(@NotNull CommandContext context, Message mes, List<OsuScore> wyniki) {
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        for (OsuScore w : wyniki) {
            pages.add(new FutureTask<>(() -> renderScore(context, w)));
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus, false).setEnableShuffle(true).create(mes);
    }

    @NotNull
    private EmbedBuilder renderScore(@NotNull CommandContext context, OsuScore w) throws OsuAPIException, MalformedURLException {
        NumberFormat nf = NumberFormat.getInstance(context.getLanguage().getLocale());
        OsuUser u = w.getUser().get();
        OsuBeatmap m = w.getBeatmap().get();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(u.getUsername(), u.getURL().toString(), "https://a.ppy.sh/" + u.getID());
        eb.addField(context.getTranslated("osu.score.beatmap"), generateBeatmapString(m),
                false);
        eb.addField(context.getTranslated("osu.score.score"),
                generateScore(w), true);
        eb.addField("", generateScoreSecLine(w), true);
        eb.addField("", generateScoreThirdLine(w), true);
        if (isPass(w) && w.getPp() != 0) {
            eb.addField(context.getTranslated("osu.score.pp"),
                    nf.format(round(w.getPp(), 2, RoundingMode.HALF_UP)), true);
        }
        eb.addField(context.getTranslated("osu.score.combo"), w.getMaxCombo() + "x", true);
        if (isPass(w)) {
            eb.addField(context.getTranslated("osu.score.fc"), w.isPerfect() ?
                    context.getTranslated("generic.yes") : context.getTranslated("generic.no"), true);
        }
        eb.addField(context.getTranslated("osu.score.mods"), getMods(w), true);
        eb.addField(context.getTranslated("osu.score.acc"),
                round(calcAcc(w) * 100, 2, RoundingMode.HALF_UP) + "%", true);
        eb.addField(context.getTranslated("osu.score.replay"), w.isReplayAvailable() ?
                context.getTranslated("osu.score.replay.download", "https://osu.ppy.sh/scores/osu/" +
                        w.getScoreID() + "/download") : context.getTranslated("osu.score.replay.unavailable"),
                true);
        eb.setTimestamp(w.getDate());
        String imgUrl = "https://assets.ppy.sh/beatmaps/" + m.getBeatmapSetID() + "/covers/cover.jpg";
        eb.setImage(imgUrl);
        eb.setColor(CommonUtil.getPrimColorFromImageUrl(imgUrl));
        return eb;
    }

    private boolean isPass(OsuScore w) {
        return !w.getRank().equals("F");
    }

    private double calcAcc(OsuScore w) {
        return (double) (50 * w.getHit50() + 100 * w.getHit100() + 300 * w.getHit300()) /
                (300 * (w.getMisses() + w.getHit50() + w.getHit100() + w.getHit300()));
    }

    private String getMods(OsuScore w) {
        if (w.getEnabledMods().length == 0) return "No Mods";
        StringBuilder sb = new StringBuilder();
        for (GameMod m : w.getEnabledMods()) {
            if (!checkEmotki(instance.emotki)) {
                sb.append(m.getName()).append("\n");
            } else {
                try {
                    sb.append(getEmotka((String) instance.emotki.getClass().getDeclaredField("osu" +
                            getOsuShortMod(m)).get(instance.emotki)).getAsMention()).append("\n");
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
                return checkEmotkiStr(instance.emotki.osuSSH) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuSSH)).getAsMention() : "XH";
            case "X":
                return checkEmotkiStr(instance.emotki.osuSS) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuSS)).getAsMention() : "X";
            case "SH":
                return checkEmotkiStr(instance.emotki.osuSH) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuSH)).getAsMention() : "SH";
            case "S":
                return checkEmotkiStr(instance.emotki.osuS) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuS)).getAsMention() : "S";
            case "A":
                return checkEmotkiStr(instance.emotki.osuA) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuA)).getAsMention() : "A";
            case "B":
                return checkEmotkiStr(instance.emotki.osuB) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuB)).getAsMention() : "B";
            case "C":
                return checkEmotkiStr(instance.emotki.osuC) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuC)).getAsMention() : "C";
            case "D":
                return checkEmotkiStr(instance.emotki.osuD) ? Objects.requireNonNull(shardManager
                        .getEmoteById(instance.emotki.osuD)).getAsMention() : "D";
            case "F":
                return "FAIL";
            default:
                return "";
        }
    }

    private String generateScoreSecLine(OsuScore w) {
        if (checkEmotki(instance.emotki)) {
            return String.format("%s %s\n%s %s\n%s %s",
                    getEmotka(instance.emotki.osu300), w.getHit300(),
                    getEmotka(instance.emotki.osu100), w.getHit100(),
                    getEmotka(instance.emotki.osu50), w.getHit50());
        } else {
            return String.format("%s - %s\n%s - %s\n%s - %s",
                    "300", w.getHit300(), "100", w.getHit100(), "50", w.getHit50());
        }
    }

    private String generateScoreThirdLine(OsuScore w) {
        if (checkEmotki(instance.emotki)) {
            return String.format("%s %s\n%s %s\n%s %s", getEmotka(instance.emotki.osugeki), w.getGekis(),
                    getEmotka(instance.emotki.osukatu), w.getKatus(),
                    getEmotka(instance.emotki.osumiss), w.getMisses());
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
