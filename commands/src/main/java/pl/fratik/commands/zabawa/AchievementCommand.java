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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.Random;

public class AchievementCommand extends Command {
    private static final Random RANDOM = new Random();

    public AchievementCommand() {
        name = "achievement";
        category = CommandCategory.FUN;
        aliases = new String[] {"mca", "osiagniecie"};
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
        uzycie = new Uzycie("tresc", "string", true);
        cooldown = 5;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        int rnd = RANDOM.nextInt(39) + 1;

        if (((String) context.getArgs()[0]).length() > 22) {
            context.send(context.getTranslated("achievement.maxsize"));
            return false;
        }
        String url ="https://www.minecraftskinstealer.com/achievement/a.php?i=" + rnd + "&h=" + NetworkUtil.encodeURIComponent(context.getTranslated("achievement.msg")) + "&t=" + NetworkUtil.encodeURIComponent((String) context.getArgs()[0]);
        try {
            context.getChannel().sendFile(NetworkUtil.download(url), "achievement.png").queue();
        } catch (IOException e) {
            context.send(context.getTranslated("image.server.fail"));
        }
        return true;
    }
}
