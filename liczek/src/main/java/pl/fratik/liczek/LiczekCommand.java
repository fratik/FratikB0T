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

package pl.fratik.liczek;

import net.dv8tion.jda.api.entities.TextChannel;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;

public class LiczekCommand extends Command {

    private final GuildDao guildDao;

    public LiczekCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "liczek";
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.FUN;
        uzycieDelim = " ";
        uzycie = new Uzycie("kanal", "channel", false);
    }

    @SubCommand(name = "info", emptyUsage = true)
    public boolean info(CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
            context.reply(context.getTranslated("liczek.not.configured"));
            return false;
        }
        TextChannel txt = context.getGuild().getTextChannelById(gc.getLiczekKanal());
        if (txt == null) {
            context.reply(context.getTranslated("liczek.channel.removed"));
            return false;
        }
        context.reply(context.getTranslated("liczek.info", txt.getAsMention()));
        return true;
    }

    @SubCommand(name = "remove", emptyUsage = true)
    public boolean remove(CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
            context.reply(context.getTranslated("liczek.not.channel.found"));
            return false;
        }
        gc.setLiczekKanal("");
        guildDao.save(gc);
        context.reply(context.getTranslated("liczek.success.remove"));
        return true;
    }

    @SubCommand(name = "set")
    public boolean set(CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        TextChannel txt;
        try {
            txt = (TextChannel) context.getArgs()[0];
        } catch (Exception e) {
            CommonErrors.usage(context);
            return false;
        }
        if (txt == null) {
            context.reply(context.getTranslated("liczek.not.channel.found"));
            return false;
        }
        gc.setLiczekKanal(txt.getId());
        guildDao.save(gc);
        context.reply(context.getTranslated("liczek.success.save", txt.getAsMention()));
        return true;
    }

    @Override
    public boolean execute(CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

}
