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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.NetworkUtil;

import java.awt.*;
import java.util.Objects;

public class PogodaCommand extends Command {

    private final UserDao userDao;

    public PogodaCommand(UserDao userDao) {
        this.userDao = userDao;
        name = "pogoda";
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("miejsce", "string");
        aliases = new String[] {"weather", "w", "pg", "prognoza", "pogodynka", "meteo", "meteostp", "warunkiatmosferyczne"};
        allowInDMs = true;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String nibyLokacja = userDao.get(context.getSender()).getLocation();
        if (context.getArgs().length == 0 && (nibyLokacja == null || !Objects.equals(nibyLokacja, ""))) {
            context.send(context.getTranslated("pogoda.no.place", context.getPrefix()));
            return false;
        }
        String lokacja = nibyLokacja;
        if (context.getArgs().length != 0 && context.getArgs()[0] != null && !((String) context.getArgs()[0]).isEmpty())
            lokacja = (String) context.getArgs()[0];
        if (lokacja == null) {
            context.send(context.getTranslated("pogoda.no.place", context.getPrefix()));
            return false;
        }
        try {
            String downloaded = new String(NetworkUtil.download("http://pl.wttr.in/" + NetworkUtil.encodeURIComponent(lokacja) + "?T0m"));
            downloaded = Jsoup.parse(downloaded).getElementsByTag("body").text();
            if (downloaded.startsWith("ERROR:")) {
                context.send(context.getTranslated("pogoda.failed"));
                return false;
                // TODO: 01.12.18 3:42 osobny failed dla braku lokacji
            }
            context.send(context.getBaseEmbed(context.getTranslated("pogoda.embed.header", lokacja), null)
                    .setImage("http://pl.wttr.in/" + NetworkUtil.encodeURIComponent(lokacja) + ".png?0m")
                    .setColor(Color.GREEN).build());
        } catch (Exception e) {
            context.send(context.getTranslated("pogoda.failed"));
            return false;
        }
        return true;
    }
}
