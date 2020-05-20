/*
 * Copyright (C) 2020 FratikB0T Contributors
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

import net.dv8tion.jda.api.entities.TextChannel;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;

import java.util.LinkedHashMap;

public class LiczekCommand extends Command {

    GuildDao guildDao;

    public LiczekCommand(GuildDao guildDao) {
        name = "liczek";
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.FUN;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("remove|info|set", "string");
        hmap.put("kanal", "channel");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});

        this.guildDao = guildDao;
    }

    @Override
    public boolean execute(CommandContext context) {
        String typ = ((String) context.getArgs()[0]).toLowerCase();
        GuildConfig gc = guildDao.get(context.getGuild());
        if (typ.equals("info")) {
            if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
                context.send(context.getTranslated("liczek.not.channel.found"));
                return false;
            }
            TextChannel txt = context.getGuild().getTextChannelById(gc.getLiczekKanal());
            if (txt == null) {
                context.send(context.getTranslated("liczek.not.channel.found"));
                return false;
            }
            context.send(context.getTranslated("liczek.info", txt.getAsMention()));
            return true;
        }
        if (typ.equals("remove")) {
            if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
                context.send(context.getTranslated("liczek.not.channel.found"));
                return false;
            }
            gc.setLiczekKanal("");
            guildDao.save(gc);
            context.send(context.getTranslated("liczek.succes.remove"));
            return true;
        }
        if (typ.equals("set")) {
            TextChannel txt;
            try {
                txt = (TextChannel) context.getArgs()[1];
            } catch (Exception e) {
                CommonErrors.usage(context);
                return false;
            }
            if (txt == null) {
                context.send(context.getTranslated("liczek.not.channel.found"));
                return false;
            }
            gc.setLiczekKanal(txt.getId());
            guildDao.save(gc);
            context.send(context.getTranslated("liczek.succes.save", txt.getAsMention()));
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

}
