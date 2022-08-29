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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.Random;

public class AchievementCommand extends NewCommand {
    private static final Random RANDOM = new Random();

    public AchievementCommand() {
        name = "achievement";
        usage = "<tekst:string>";
        cooldown = 5;
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String tekst = context.getArguments().get("tekst").getAsString();
        int rnd = RANDOM.nextInt(39) + 1;

        if (tekst.length() > 22) {
            context.replyEphemeral(context.getTranslated("achievement.maxsize"));
            return;
        }
        context.deferAsync(false);
        String url = "https://www.minecraftskinstealer.com/achievement/a.php?i=" + rnd + "&h=" +
                NetworkUtil.encodeURIComponent(context.getTranslated("achievement.msg")) + "&t=" +
                NetworkUtil.encodeURIComponent(tekst);
        try {
            context.sendMessage("achievement.png", NetworkUtil.download(url));
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }
    }
}
