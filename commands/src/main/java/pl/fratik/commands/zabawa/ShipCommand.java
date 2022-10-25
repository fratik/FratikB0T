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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.awt.*;
import java.time.Instant;
import java.util.Random;

import static pl.fratik.core.util.CommonUtil.generateProgressBar;

public class ShipCommand extends NewCommand {

    private static final String HEART1 = "\u2764\uFE0F";
    private static final String HEART2 = "\uD83D\uDC9F\uFE0F";
    private static final long SEED_DATE = Instant.now().toEpochMilli();

    public ShipCommand() {
        name = "ship";
        usage = "<co:string> [z_czym:string]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String wyswietlana1 = null;
        String wyswietlana2 = null;

        OptionMapping coMapping = context.getArguments().get("co");
        OptionMapping zCzymMapping = context.getArguments().getOrDefault("z_czym", null);

        String rzecz1 = coMapping.getAsString();
        String rzecz2 = zCzymMapping != null ? zCzymMapping.getAsString() : context.getSender().getAsMention();

        if (rzecz1.startsWith("<@") && rzecz1.endsWith(">") && coMapping.getMentions().getUsers().size() == 1) {
            rzecz1 = coMapping.getMentions().getUsers().get(0).getAsTag();
            wyswietlana1 = coMapping.getMentions().getUsers().get(0).getAsMention();
        }

        if (zCzymMapping != null) {
            if (rzecz2.startsWith("<@") && rzecz1.endsWith(">") && zCzymMapping.getMentions().getUsers().size() == 1) {
                rzecz2 = zCzymMapping.getMentions().getUsers().get(0).getAsTag();
                wyswietlana2 = zCzymMapping.getMentions().getUsers().get(0).getAsMention();
            }
        } else {
            rzecz2 = context.getSender().getAsTag();
            wyswietlana2 = context.getSender().getAsMention();
        }

        ship(rzecz1, rzecz2, context, wyswietlana1, wyswietlana2);
    }

    private void ship(String rzecz1,
                      String rzecz2,
                      NewCommandContext context,
                      String wyswietlana1,
                      String wyswietlana2) {
        EmbedBuilder eb = context.getBaseEmbed(null, null);
        eb.setTitle("Ship");
        eb.setColor(Color.pink);
        StringBuilder desc = new StringBuilder();
        String shipFormat = HEART1 + " %s %s %s " + HEART1 + "\n";

        if (rzecz1.equalsIgnoreCase(rzecz2)) {
            context.replyEphemeral(context.getTranslated("ship.same"));
            return;
        }

        shipFormat = String.format(shipFormat,
                MarkdownSanitizer.escape(wyswietlana1 == null ? rzecz1 : wyswietlana1),
                HEART2,
                MarkdownSanitizer.escape(wyswietlana2 == null ? rzecz2 : wyswietlana2));

        int procent = calc(rzecz1 + "x" + rzecz2);

        String text = getText(procent / 10, context.getTlumaczenia(), context.getLanguage());
        if (procent == 69) text = context.getTranslated("ship.percent.69");

        desc.append(shipFormat);

        String loadingScreen = "%s\n%s";
        desc.append(String.format(loadingScreen, generateProgressBar(procent, true), text));
        eb.setDescription(desc);
        context.reply(eb.build());
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
