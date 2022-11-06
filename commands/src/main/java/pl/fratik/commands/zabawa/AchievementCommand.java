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
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.Random;
public class AchievementCommand extends NewCommand {
    private static final Random RANDOM = new Random();

    private static final String URL = "https://skinmc.net/en/achievement/%s/%s/%s";

    private final Tlumaczenia tlumaczenia;

    public AchievementCommand(Tlumaczenia tlumaczenia) {
        name = "achievement";
        usage = "<ikona:string> <dolny_tekst:string> [górny_tekst:string]";
        cooldown = 5;
        allowInDMs = true;
        this.tlumaczenia = tlumaczenia;
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
            option.addChoice(tlumaczenia.get(Language.DEFAULT, String.format("achievement.ikona.%s.name", value.name().toLowerCase())), String.valueOf(value.getId()));
        }
    }

    @RequiredArgsConstructor
    @Getter
    enum Material {
        STONE(20),
        GRASS(1),
        WOODEN_PLANK(21),
        CRAFTING_TABLE(13),
        FURNACE(18),
        CHEST(17),
        BED(9),
        COAL(31),
        IRON(22),
        GOLD(23),
        DIAMOND(2),
        SIGN(11),
        BOOK(19),
        WOODEN_DOOR(24),
        IRON_DOOR(25),
        REDSTONE(14),
        RAIL(12),
        BOW(33),
        ARROW(34),
        IRON_SWORD(32),
        DIAMOND_SWORD(3),
        IRON_CHESTPLATE(35),
        DIAMOND_CHESTPLATE(26),
        TNT(6),
        FLINT_AND_STEEL(27);

        private final int id;
    }

}
