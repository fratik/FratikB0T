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

import net.dv8tion.jda.api.entities.GuildChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class UstawPowitanieCommand extends Command {
    private final GuildDao guildDao;

    public UstawPowitanieCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "ustawpowitanie";
        category = CommandCategory.SYSTEM;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("kanalDoPowitan", "channel");
        hmap.put("tekst", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        uzycieDelim = " ";
        aliases = new String[] {"ustawpowitanie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String tekst;
        GuildConfig gc = guildDao.get(context.getGuild());
        if (context.getMessage().getContentRaw().contains("--delete")) {
            String usunieto = gc.getPowitania().remove(((GuildChannel) context.getArgs()[0]).getId());
            if (usunieto == null) {
                context.send(context.getTranslated("ustawpowitanie.delete.failure"));
                return false;
            }
            context.send(context.getTranslated("ustawpowitanie.delete.success"));
            guildDao.save(gc);
            return true;
        }
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            tekst = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        else {
            CommonErrors.usage(context);
            return false;
        }
        gc.getPowitania().put(((GuildChannel) context.getArgs()[0]).getId(), tekst);
        guildDao.save(gc);
        context.send(context.getTranslated("ustawpowitanie.response"));
        return true;
    }
}
