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

import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

public class ShipCommand extends Command {

    @Getter private static HashMap<Character, Builder> znaki = new HashMap<>();

    private static final String HEART1 = "❤️️";
    private static final String HEART2 = "\uD83D\uDC9F️";
    private static final String BLOCK = "▉️";

    private ManagerArgumentow managerArgumentow;

    public ShipCommand(ManagerArgumentow managerArgumentow) {
        name = "ship";
        cooldown = 5;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        uzycieDelim = " ";
        hmap.put("rzecz1", "string");
        hmap.put("rzecz2", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        this.managerArgumentow = managerArgumentow;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (getZnaki().isEmpty()) loadZnaki();
        String arg0 = (String) context.getArgs()[0];

        // sprawdzamy czy arg0 jest userem
        User user = (User) managerArgumentow.getArguments().get("user").execute(arg0, context.getTlumaczenia(), context.getLanguage());
        if (user != null) {
            if (context.getArgs().length == 1) { // shipujemy arg0 i sendera
                if (user.getId().equals(context.getMember().getId())) {
                    context.send("Nie możesz shipować samego siebie!");
                    return false;
                }
                return ship(UserUtil.formatDiscrim(context.getSender()), UserUtil.formatDiscrim(user), context,
                        context.getSender().getAsMention(), user.getAsMention());
            } else { // sprawdzamy czy mamy shipowac user1 i user2
                User user2 = (User) managerArgumentow.getArguments().get("user").execute((String) context.getArgs()[1], context.getTlumaczenia(), context.getLanguage());
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
        CommonErrors.usage(context);
        return false;
    }

    private boolean ship(String rzecz1, String rzecz2, CommandContext context) {
        return ship(rzecz1, rzecz2, context, "", "");
    }

    private boolean ship(String rzecz1, String rzecz2, CommandContext context, String wyswietlana1, String wyswietlana2) {
        EmbedBuilder eb = context.getBaseEmbed();
        eb.setTitle("Ship");
        eb.setColor(Color.pink);
        StringBuilder desc = new StringBuilder();
        String shipFormat = HEART1 + " %s x %s " + HEART2 + "\n";

        shipFormat = String.format(shipFormat,
                MarkdownSanitizer.escape(wyswietlana1.isEmpty() ? rzecz1 : wyswietlana1),
                MarkdownSanitizer.escape(wyswietlana2.isEmpty() ? rzecz2 : wyswietlana2));

        int procent = calc(rzecz1, 10);
        procent = calc(rzecz2, procent);

        desc.append(shipFormat);

        String loadingScreen = "[%s](%s)%s %s%%";
        desc.append(String.format(loadingScreen,
                append(BLOCK, 5),
                Ustawienia.instance.botUrl,
                append(BLOCK, 5), procent));
        eb.setDescription(desc);
        context.send(eb.build());
        return false;
    }

    private String append(String s, int iloraz) {
        for (int i = 1; i < iloraz+1; i++) { s += s; }
        return s;
    }

    private int calc(String s, int c) {
        for (char ch : s.toCharArray()) {
            Builder b = getZnaki().get(ch);
            if (b == null) continue;
            if (b.getDodawanie()) c += b.getProcenty();
            else c -= b.getProcenty();
        }
        return c;
    }

    private void loadZnaki() {
        String znaki = "qwertyuiopasdfghjklzxcvbnm1234567890-=~!@#$%^&*()_+[];',./{}:?";
        for (char c : znaki.toCharArray()) {
            Builder build = new Builder();
            int random = new Random().nextInt(100);
            build.setProcenty(new Random().nextInt(20));
            if (random < 40) build.setDodawanie(false);
            getZnaki().put(c, build);
        }
    }

    @Data
    private static class Builder {
        public Builder() { }

        private Boolean dodawanie = true;
        private Integer procenty = 0;
    }
    
}
