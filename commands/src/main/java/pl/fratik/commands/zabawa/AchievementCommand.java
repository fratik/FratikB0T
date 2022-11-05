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
import java.net.URI;
import java.util.Random;
public class AchievementCommand extends NewCommand {
    private static final Random RANDOM = new Random();

    private static final String URL = "https://skinmc.net/en/achievement/%s/%s/%s";

    public AchievementCommand() {
        name = "achievement";
        usage = "<ikona:string> <dolny_tekst:string> [górny_tekst:string]";
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String yellowText = context.getArgumentOr("górny_tekst", context.getTranslated("achievement.msg"), OptionMapping::getAsString);
        String whiteText = context.getArguments().get("dolny_tekst").getAsString();
        int icon = context.getArgumentOr("ikona", RANDOM.nextInt(25) + 1, OptionMapping::getAsInt);

        if (whiteText.length() > 22 || yellowText.length() > 22) {
            context.replyEphemeral(context.getTranslated("achievement.maxsize"));
            return;
        }

        context.deferAsync(false);

        try {
            context.sendMessage("achievement.png", NetworkUtil.download(String.format(URL, icon,
                NetworkUtil.encodeURIComponent(yellowText),
                NetworkUtil.encodeURIComponent(whiteText))));
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
        STONE(20, "Kamień"),
        GRASS(1, "Ziemia"),
        WOODEN_PLANK(21, "Drewniana deska"),
        CRAFINT_TABLE(13, "Crafting"),
        FURNACE(18, "Piecyk"),
        CHEST(17, "Skrzynia"),
        BED(9, "Łóżko"),
        COAL(31, "Węgiel"),
        IRON(22, "Żelazo"),
        GOLD(23, "Złoto"),
        DIAMOND(2, "Diament"),
        SIGN(11, "Tabliczka"),
        BOOK(19, "Książka"),
        WOODEN_DOOR(24, "Drzwi"),
        IRON_DOOR(25, "Żelazne Drzwi"),
        REDSTONE(14, "Redstone"),
        RAIL(12, "Szyny"),
        BOW(33, "Łuk"),
        ARROW(34, "Strzała"),
        IRON_SWORD(32, "Żelazny miecz"),
        DIAMOND_SWORD(3, "Diamentowy miecz"),
        IRON_CHESTPLATE(35, "Żelazny napiersnik"),
        DIAMOND_CHESTPLATE(26, "Diamentowy napiersnik"),
        TNT(6, "TNT"),
        FLINT_AND_STEEL(27, "Zapalniczka");

        private final int id;
        private final String name;
    }

}
