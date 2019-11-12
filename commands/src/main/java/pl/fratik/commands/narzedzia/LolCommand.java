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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.constant.LeagueQueue;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import java.util.LinkedHashMap;
import java.util.Set;

public class LolCommand extends Command {

    private final ApiConfig config;
    private final Tlumaczenia tlumaczenia;

    public LolCommand(Tlumaczenia tlumaczenia) {
        this.tlumaczenia = tlumaczenia;
        name = "lol";
        category = CommandCategory.UTILITY;
        aliases = new String[] {"lolstats", "leagueoflegends", "leagueoflegendsstats"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        cooldown = 5;
        allowInDMs = true;
        config = new ApiConfig().setKey(Ustawienia.instance.apiKeys.get("lolToken"));
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        uzycie = new Uzycie("nick", "string", false);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        RiotApi api = new RiotApi(config);
        Summoner gracz;
        MatchList matchList;
        Platform platform = Platform.EUNE;
        Set<LeagueEntry> leagues;

        try {
            gracz = api.getSummonerByName(Platform.EUNE, String.valueOf(context.getArgs()[0]));
            matchList = api.getMatchListByAccountId(Platform.EUNE, gracz.getAccountId());
            leagues = api.getLeagueEntriesBySummonerId(platform, gracz.getId());
        } catch (RiotApiException e) {
            context.send(context.getTranslated("lol.baduser", platform.getName()));
            return false;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.setFooter("© " + context.getEvent().getJDA().getSelfUser().getName());
        eb.setTitle(gracz.getName());
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));

        String flextt = getInfo(leagues, LeagueQueue.RANKED_FLEX_TT.name(), context.getLanguage());
        String flexsr = getInfo(leagues, LeagueQueue.RANKED_FLEX_SR.name(), context.getLanguage());
        String solo5x5 = getInfo(leagues, LeagueQueue.RANKED_SOLO_5x5.name(), context.getLanguage());

        eb.addField(context.getTranslated("lol.embed.summonerlevel"), String.valueOf(gracz.getSummonerLevel()), false);
        eb.addField(context.getTranslated("lol.embed.matchlist"), String.valueOf(matchList.getTotalGames()), false);
        eb.addField(context.getTranslated("lol.info.flextt"), flextt, false);
        eb.addField(context.getTranslated("lol.info.flexsr"), flexsr, false);
        eb.addField(context.getTranslated("lol.info.solo"), solo5x5, false);
        context.send(eb.build());
        return true;
    }

    private String getInfo(Set<LeagueEntry> leagues, String q, Language l) {
        LinkedHashMap cos = getInfoForQueue(leagues, q);
        return tlumaczenia.get(l, "lol.info",
                cos.get("tier"),
                cos.get("lp"),
                cos.get("wins"),
                cos.get("losses"));
    }

    private LinkedHashMap getInfoForQueue(Set<LeagueEntry> leagues, String q) {
        LinkedHashMap<String, Object> hmap = new LinkedHashMap<>();
        for(LeagueEntry league : leagues) {
            if (league.getQueueType().equals(q)) {
                hmap.put("tier", league.getTier());
                hmap.put("losses", league.getLosses());
                hmap.put("wins", league.getWins());
                hmap.put("lp", league.getLeaguePoints());
            }
        }
        return hmap;
    }
}
