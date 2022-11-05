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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.Random;
public class AchievementCommand extends NewCommand {
    private static final Random RANDOM = new Random();

    private static final String URL = "https://skinmc.net/en/achievement/%s/%s/%s";

    public AchievementCommand() {
        name = "achievement";
        usage = "[górny_tekst:string] <dolny_tekst:string> [ikona:string]";
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String tekst = context.getArguments().get("tekst").getAsString();

        String yellowText = context.getArgumentOr("górny_tekst", context.getTranslated("achievement.msg"), OptionMapping::getAsString);
        String whiteText = context.getArguments().get("dolny_tekst").getAsString();
        int icon = context.getArgumentOr("ikona", RANDOM.nextInt(25) + 1, OptionMapping::getAsInt);

        if (tekst.length() > 22) {
            context.replyEphemeral(context.getTranslated("achievement.maxsize"));
            return;
        }

        context.deferAsync(false);

        try {
            context.sendMessage("achievement.png", NetworkUtil.download(String.format(URL, yellowText, whiteText, icon)));
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (!option.getName().equals("ikona")) return;

        for (Material value : Material.values()) {
            if (option.getChoices().size() == OptionData.MAX_CHOICES) break;
            option.addChoice(value.getName(), String.valueOf(value.getId()));
        }
    }

    @RequiredArgsConstructor
    @Getter
    enum Material {
        STONE(1, "Kamień"),
        GRASS(2, "Ziemia"),
        WOODEN_PLANK(3, "Drewniana deska"),
        CRAFINT_TABLE(4, "Crafting"),
        FURNACE(5, "Piecyk"),
        CHEST(6, "Piec"),
        BED(7, "Łóżko"),
        COAL(8, "Węgiel"),
        IRON(9, "Żelazo"),
        GOLD(10, "Złoto"),
        DIAMOND(11, "Diament"),
        SIGN(12, "Tabliczka"),
        BOOK(13, "Książka"),
        WOODEN_DOOR(14, "Drzwi"),
        IRON_DOOR(15, "Żelazne Drzwi"),
        REDSTONE(16, "Redstone"),
        RAIL(17, "Szyny"),
        BOW(18, "Łuk"),
        ARROW(19, "Strzała"),
        IRON_SWORD(20, "Żelazny miecz"),
        DIAMOND_SWORD(21, "Diamentowy miecz"),
        IRON_CHESTPLATE(22, "Żelazny napiersnik"),
        DIAMOND_CHESTPLATE(23, "Diamentowy napiersnik"),
        TNT(24, "TNT"),
        FLINT_AND_STEEL(25, "Zapalniczka");

        private final int id;
        private final String name;
    }

}
