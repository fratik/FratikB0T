/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Random;

import static pl.fratik.core.util.CommonUtil.generateProgressBar;

public class ShipCommand extends Command {

    private final ManagerArgumentow managerArgumentow;

    private static final String HEART1 = "\u2764\uFE0F";
    private static final String HEART2 = "\uD83D\uDC9F\uFE0F";
    private static final long SEED_DATE = Instant.now().toEpochMilli();

    public ShipCommand(ManagerArgumentow managerArgumentow) {
        name = "ship";
        cooldown = 5;
        category = CommandCategory.FUN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        uzycieDelim = " ";
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        hmap.put("rzecz1", "string");
        hmap.put("rzecz2", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        this.managerArgumentow = managerArgumentow;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String arg0 = (String) context.getArgs()[0];

        // sprawdzamy czy arg0 jest userem
        User user = (User) managerArgumentow.getArguments().get("user").execute(arg0,
                context.getTlumaczenia(), context.getLanguage());
        if (user != null) {
            if (context.getArgs().length == 1 || (context.getArgs().length > 1 && context.getArgs()[1] == null)) {
                // shipujemy arg0 i sendera
                if (user.getId().equals(context.getMember().getId())) {
                    context.send(context.getTranslated("ship.urself"));
                    return false;
                }
                return ship(UserUtil.formatDiscrim(context.getSender()), UserUtil.formatDiscrim(user), context,
                        context.getSender().getAsMention(), user.getAsMention());
            } else { // sprawdzamy czy mamy shipowac user1 i user2
                User user2 = (User) managerArgumentow.getArguments().get("user").execute((String) context.getArgs()[1],
                        context.getTlumaczenia(), context.getLanguage());
                if (user2 != null) { // shipujemy user1 i user2
                    return ship(UserUtil.formatDiscrim(user), UserUtil.formatDiscrim(user2), context,
                            user.getAsMention(), user2.getAsMention());
                } else { // shipujemy user1 i rzecz2
                    return ship(UserUtil.formatDiscrim(user), (String) context.getArgs()[1], context,
                            user.getAsMention(), "");
                }
            }
        }
        if (context.getArgs().length > 1) { // shipujemy rzecz1 i rzecz2
            return ship(arg0, (String) context.getArgs()[1], context);
        }
        // shipujemy sendera i rzecz1
        return ship(UserUtil.formatDiscrim(context.getSender()), (String) context.getArgs()[0], context,
                context.getSender().getAsMention(), "");
    }

    private boolean ship(String rzecz1, String rzecz2, CommandContext context) {
        return ship(rzecz1, rzecz2, context, "", "");
    }

    private boolean ship(String rzecz1,
                         String rzecz2,
                         CommandContext context,
                         @NotNull String wyswietlana1,
                         @NotNull String wyswietlana2) {
        EmbedBuilder eb = context.getBaseEmbed(null, null);
        eb.setTitle("Ship");
        eb.setColor(Color.pink);
        StringBuilder desc = new StringBuilder();
        String shipFormat = HEART1 + " %s %s %s " + HEART1 + "\n";

        if (rzecz1.equalsIgnoreCase(rzecz2)) {
            context.send(context.getTranslated("ship.same"));
            return false;
        }

        shipFormat = String.format(shipFormat,
                MarkdownSanitizer.escape(wyswietlana1.isEmpty() ? rzecz1 : wyswietlana1),
                HEART2,
                MarkdownSanitizer.escape(wyswietlana2.isEmpty() ? rzecz2 : wyswietlana2));

        int procent = calc(rzecz1 + "x" + rzecz2);

        String text = getText(procent / 10, context.getTlumaczenia(), context.getLanguage());
        if (procent == 69) text = context.getTranslated("ship.percent.69");

        desc.append(shipFormat);

        String loadingScreen = "%s\n%s";
        desc.append(String.format(loadingScreen, generateProgressBar(procent, true), text));
        eb.setDescription(desc);
        context.send(eb.build());
        return true;
    }

    private String getText(int procent, Tlumaczenia tlumaczenia, Language lang) {
        if (procent >= 0 && procent <= 10) { // zabezpieczenie przeciwko `<key> nie jest przetłumaczone`
            return tlumaczenia.get(lang, "ship.percent." + procent);
        }
        return "???";
    }

    private int calc(String s) {
        Random rand = new Random((s.toUpperCase().trim().hashCode()) + SEED_DATE);
        return rand.nextInt(101);
    }
    
}
