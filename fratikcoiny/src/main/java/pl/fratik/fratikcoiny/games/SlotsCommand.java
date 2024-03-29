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

package pl.fratik.fratikcoiny.games;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.fratikcoiny.libs.slots.Results;
import pl.fratik.fratikcoiny.libs.slots.SlotMachine;
import pl.fratik.fratikcoiny.libs.slots.SlotSymbol;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SlotsCommand extends NewCommand {
    private final MemberDao memberDao;
    private final SlotMachine maszynaLosujaca;

    public SlotsCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "slots";
        cooldown = 10;
        List<SlotSymbol> symbole = new ArrayList<>();
        symbole.add(new SlotSymbol("lemon", "\uD83C\uDF4B", 1, 100));
        symbole.add(new SlotSymbol("cherry", "\uD83C\uDF52", 1, 100));
        symbole.add(new SlotSymbol("wild", "\u2754", 1, 40, true));
        symbole.add(new SlotSymbol("bell", "\uD83D\uDD14", 2, 40));
        symbole.add(new SlotSymbol("clover", "\uD83C\uDF40", 3, 35));
        symbole.add(new SlotSymbol("heart", "\u2764", 4, 30));
        symbole.add(new SlotSymbol("money", "\uD83D\uDCB0", 5, 25));
        symbole.add(new SlotSymbol("diamond", "\uD83D\uDC8E", 100, 20));
        symbole.add(new SlotSymbol("jackpot", "\uD83D\uDD05", 500, 10));
        maszynaLosujaca = new SlotMachine(3, symbole);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        MemberConfig mc = memberDao.get(context.getMember());
        long zaklad = 10;
        if (mc.getFratikCoiny() < zaklad) {
            context.reply(context.getTranslated("slots.no.money",
                    Objects.requireNonNull(context.getShardManager().getEmojiById(Ustawienia.instance.emotki.fratikCoin)).getAsMention()));
            return;
        }
        Results results = maszynaLosujaca.play();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(context.getTranslated("slots.embed.title"));
        eb.setDescription(results.visualize());
        eb.appendDescription("\n\n");
        mc.setFratikCoiny(mc.getFratikCoiny() - zaklad + results.totalPoints() * 10L);
        String emote = Objects.requireNonNull(context.getShardManager().getEmojiById(Ustawienia.instance.emotki.fratikCoin)).getAsMention();
        if (results.winCount() == 0) {
            eb.appendDescription(context.getTranslated("slots.embed.desc.lost", context.getSender().getAsTag()));
            eb.setColor(Color.red);
        } else {
            eb.appendDescription(context.getTranslated("slots.embed.desc.won", context.getSender().getAsTag(),
                    results.totalPoints() * 10, emote,
                    mc.getFratikCoiny(), emote));
            eb.setColor(Color.green);
        }
        context.reply(eb.build());
        memberDao.save(mc);
    }

}
