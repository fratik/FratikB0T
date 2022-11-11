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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegulaminCommand extends ModerationCommand {

    public RegulaminCommand() {
        super(false);
        name = "regulamin";
        usage = "<zasada:int>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        int zasada = context.getArguments().get("zasada").getAsInt();
        TextChannel kanal;
        try {
            kanal = context.getGuild().getRulesChannel();
            if (kanal == null) throw new IllegalStateException();
        } catch (Exception e) {
            try {
                kanal = context.getGuild().getTextChannelsByName("regulamin", true).get(0);
                if (kanal == null) throw new IllegalStateException();
            } catch (Exception e1) {
                try {
                    kanal = context.getGuild().getTextChannelsByName("zasady", true).get(0);
                    if (kanal == null) throw new IllegalStateException();
                } catch (Exception e2) {
                    try {
                        kanal = context.getGuild().getTextChannelsByName(context.getTlumaczenia()
                                        .get(context.getTlumaczenia().getLanguage(context.getGuild()), "regulamin.channel.name"),
                                true).get(0);
                        if (kanal == null) throw new IllegalStateException();
                    } catch (Exception e3) {
                        kanal = context.getGuild().getTextChannels().get(0);
                    }
                }
            }
        }
        if (kanal == null) {
            context.reply(context.getTranslated("regulamin.channel.doesnt.exist"));
            return;
        }
        if (!context.getGuild().getSelfMember().hasPermission(kanal, Permission.MESSAGE_HISTORY)) {
            context.reply(context.getTranslated("regulamin.channel.no.perms"));
            return;
        }
        List<Message> wiadomosci = kanal.getHistory().retrievePast(100).complete();
        Collections.reverse(wiadomosci);
        Map<Integer, String> punkty = new HashMap<>();
        for (Message wiadomosc : wiadomosci) {
            List<String> punktyTmp = new ArrayList<>();
            Collections.addAll(punktyTmp, wiadomosc.getContentRaw().split("(\\r\\n|\\r|\\n)"));
            for (String punktR : punktyTmp) {
                String punkt = MarkdownSanitizer.sanitize(punktR);
                Matcher matcher = Pattern.compile("(^\\d+)", Pattern.MULTILINE | Pattern.DOTALL).matcher(punkt);
                if (!matcher.find()) continue;
                try { punkty.put(Integer.valueOf(matcher.group(1)), punktR); } catch (Exception ignored) {/*lul*/}
            }
        }
        if (zasada > punkty.size()) {
            context.reply(context.getTranslated("regulamin.no.rule"));
            return;
        }
        //noinspection SuspiciousMethodCalls (faktycznie jest to int)
        String tekst = punkty.get(zasada);
        if (tekst == null) {
            context.reply(context.getTranslated("regulamin.no.rule"));
            return;
        }
        context.reply(tekst);
    }
}
