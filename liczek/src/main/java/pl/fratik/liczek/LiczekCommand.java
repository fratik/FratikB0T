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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;

public class LiczekCommand extends NewCommand {

    private final GuildDao guildDao;

    public LiczekCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "liczek";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @SubCommand(name = "info")
    public void info(NewCommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
            context.reply(context.getTranslated("liczek.not.configured"));
            return;
        }
        TextChannel txt = context.getGuild().getTextChannelById(gc.getLiczekKanal());
        if (txt == null) {
            context.reply(context.getTranslated("liczek.channel.removed"));
            return;
        }
        context.reply(context.getTranslated("liczek.info", txt.getAsMention()));
    }

    @SubCommand(name = "usun")
    public void remove(NewCommandContext context) {
        context.deferAsync(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) {
            context.sendMessage(context.getTranslated("liczek.not.channel.found"));
            return;
        }
        gc.setLiczekKanal("");
        guildDao.save(gc);
        context.sendMessage(context.getTranslated("liczek.success.remove"));
    }

    @SubCommand(name = "ustaw", usage = "<kanal:textchannel>")
    public void set(NewCommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        TextChannel txt = context.getArguments().get("kanal").getAsChannel().asTextChannel();
        gc.setLiczekKanal(txt.getId());
        guildDao.save(gc);
        context.reply(context.getTranslated("liczek.success.save", txt.getAsMention()));
    }

}
