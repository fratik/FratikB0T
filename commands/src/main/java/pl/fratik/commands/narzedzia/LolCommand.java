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
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

public class LolCommand extends Command {

    private final ApiConfig config;

    public LolCommand() {
        name = "lol";
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("nick", "string", true);
        aliases = new String[] {"lolstats", "leagueoflegends", "leagueoflegendsstats"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        cooldown = 5;
        allowInDMs = true;
        config = new ApiConfig().setKey(Ustawienia.instance.apiKeys.get("lolToken"));
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        RiotApi api = new RiotApi(config);
        Summoner gracz;
        try {
            gracz = api.getSummonerByName(Platform.NA, String.valueOf(context.getArgs()[0]));
        } catch (RiotApiException e) {
            context.send(context.getTranslated("lol.baduser"));
            return false;
        }

        if (gracz == null) {
            context.send(context.getTranslated("lol.baduser"));
            return false;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.setFooter("© " + context.getEvent().getJDA().getSelfUser().getName());

        eb.setTitle(gracz.getName());
        eb.addField(context.getTranslated("lol.embed.profileid"), gracz.getAccountId(), false);
        eb.addField(context.getTranslated("lol.embed.summonerlevel"), String.valueOf(gracz.getSummonerLevel()), false);
        eb.addField(context.getTranslated("lol.embed.revisiondate"), String.valueOf(gracz.getRevisionDate()), false);
        context.send(eb.build());
        return true;
    }

}
