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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegulaminCommand extends ModerationCommand {

    public RegulaminCommand() {
        name = "regulamin";
        category = CommandCategory.MODERATION;
        uzycie = new Uzycie("zasada", "integer", true);
        aliases = new String[] {"zasady", "reg", "zasada"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        TextChannel kanal;
        try {
            kanal = context.getGuild().getTextChannelsByName("regulamin", true).get(0);
            if (kanal == null) throw new IllegalStateException();
        } catch (Exception e) {
           try {
               kanal = context.getGuild().getTextChannelsByName("zasady", true).get(0);
               if (kanal == null) throw new IllegalStateException();
           } catch (Exception e2) {
                kanal = context.getGuild().getTextChannels().get(0);
           }
        }
        if (kanal == null) {
            context.send(context.getTranslated("regulamin.channel.doesnt.exist"));
            return false;
        }
        if (!context.getGuild().getSelfMember().hasPermission(kanal, Permission.MESSAGE_HISTORY)) {
            context.send(context.getTranslated("regulamin.channel.no.perms"));
            return false;
        }
        List<Message> wiadomosci = kanal.getHistory().retrievePast(100).complete();
        Collections.reverse(wiadomosci);
        Map<Integer, String> punkty = new HashMap<>();
        for (Message wiadomosc : wiadomosci) {
            List<String> punktyTmp = new ArrayList<>();
            Collections.addAll(punktyTmp, wiadomosc.getContentRaw().split("(\\r\\n|\\r|\\n)"));
            for (String punkt : punktyTmp) {
                Matcher matcher = Pattern.compile("(^\\d+)", Pattern.MULTILINE | Pattern.DOTALL).matcher(punkt);
                if (!matcher.find()) continue;
                try { punkty.put(Integer.valueOf(matcher.group(1)), punkt); } catch (Exception ignored) {/*lul*/}
            }
        }
        if ((int) context.getArgs()[0] > punkty.size()) {
            context.send(context.getTranslated("regulamin.no.rule"));
            return false;
        }
        //noinspection SuspiciousMethodCalls (faktycznie jest to int)
        String tekst = punkty.get(context.getArgs()[0]);
        if (tekst == null) {
            context.send(context.getTranslated("regulamin.no.rule"));
            return false;
        }
        context.send(tekst);
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.EVERYONE;
    }

}
