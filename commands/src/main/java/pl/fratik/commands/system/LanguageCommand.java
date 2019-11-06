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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.tlumaczenia.Language;

import java.util.ArrayList;

public class LanguageCommand extends Command {

    private final UserDao userDao;

    public LanguageCommand(UserDao userDao) {
        name = "language";
        uzycie = new Uzycie("język", "language");
        category = CommandCategory.BASIC;
        permLevel = PermLevel.EVERYONE;
        aliases = new String[] {"jezyk"};
        this.userDao = userDao;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            EmbedBuilder eb = context.getBaseEmbed(context.getTranslated("language.embed.author"));
            ArrayList<String> tekst = new ArrayList<>();
            tekst.add(context.getTranslated("language.embed.header", context.getPrefix()));
            tekst.add("");
            for (Language l : Language.values()) {
                if (l.equals(Language.DEFAULT)) continue;
                tekst.add(String.format("%s %s", l.getEmoji(), l.getLocalized()));
            }
            eb.setDescription(String.join("\n", tekst));
            context.send(eb.build());
            return false;
        }
        UserConfig uc = userDao.get(context.getSender());
        uc.setLanguage((Language) context.getArgs()[0]);
        userDao.save(uc);
        context.send(context.getTranslated("language.change.success", ((Language) context.getArgs()[0]).getLocalized()));
        return true;
    }
}
