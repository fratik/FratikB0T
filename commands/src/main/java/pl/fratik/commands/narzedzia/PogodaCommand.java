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

package pl.fratik.commands.narzedzia;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.NetworkUtil;

import java.awt.*;

public class PogodaCommand extends NewCommand {

    private final UserDao userDao;

    public PogodaCommand(UserDao userDao) {
        this.userDao = userDao;
        name = "pogoda";
        usage = "[miasto:string]";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String lokacja;
        if (context.getArguments().containsKey("miasto")) lokacja = context.getArguments().get("miasto").getAsString();
        else lokacja = userDao.get(context.getSender()).getLocation();
        if (lokacja == null || lokacja.isEmpty()) {
            context.replyEphemeral(context.getTranslated("pogoda.no.place"));
            return;
        }
        context.deferAsync(false);
        try {
            String downloaded = new String(NetworkUtil.download("http://en.wttr.in/" +
                    NetworkUtil.encodeURIComponent(lokacja) + "?T"));
            downloaded = Jsoup.parse(downloaded).getElementsByTag("body").text();
            if (downloaded.startsWith("ERROR:")) {
                context.sendMessage(context.getTranslated("pogoda.failed"));
                return;
            }
            if (downloaded.contains("We were unable to find your location")) {
                context.sendMessage(context.getTranslated("pogoda.unknown.location"));
                return;
            }
            context.sendMessage(context.getBaseEmbed(context.getTranslated("pogoda.embed.header", lokacja), null)
                    .setImage("http://" + context.getLanguage().getLocale()
                            .getLanguage().toLowerCase().split("_")[0] + ".wttr.in/" +
                            NetworkUtil.encodeURIComponent(lokacja) + ".png?0m")
                    .setColor(Color.GREEN).build());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("pogoda.failed"));
        }

    }
}
