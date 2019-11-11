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

package pl.fratik.liczek.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;
import pl.fratik.liczek.entity.Liczek;
import pl.fratik.liczek.entity.LiczekDao;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;

public class LiczekCommand extends Command {

    private final GuildDao guildDao;
    private final LiczekDao liczekDao;

    public LiczekCommand(GuildDao guildDao, LiczekDao liczekDao) {
        this.guildDao = guildDao;
        this.liczekDao = liczekDao;
        name = "liczek";
        category = CommandCategory.BASIC;
        permLevel = PermLevel.ADMIN;
        aliases = new String[] {"liczydlo"};
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("typ", "string");
        hmap.put("kanal", "channel");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        Liczek licz = liczekDao.get(context.getGuild());

        if (context.getArgs()[0].equals("info")) {
            String id = gc.getLiczekKanal();
            if (id == null) {
                context.send(context.getTranslated("liczek.notset"));
                return false;
            }
            TextChannel txt = context.getGuild().getTextChannelById(id);
            if (txt == null) {
                context.send(context.getTranslated("liczek.badchannel"));
                return false;
            }
            String trans = context.getTranslated(hasPermission(context, txt));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
            eb.setTitle(context.getTranslated("liczek.embed.title"));

            eb.addField(context.getTranslated("liczek.embed.channel"),
                    context.getTranslated("liczek.embed.channel.value",
                    txt.getAsMention(), "#" + txt.getName(), txt.getId()), false);

            eb.addField(context.getTranslated("liczek.embed.number"),
                    String.valueOf(licz.getLiczekLiczba()), false);

            eb.addField(context.getTranslated("liczek.embed.permission"), trans, false);
            context.send(eb.build());
            return true;
        }
        if (context.getArgs()[0].equals("set")) {
            TextChannel cha;
            try {
                cha = (TextChannel) context.getArgs()[1];
            } catch (Exception xd) {
                CommonErrors.usage(context);
                return false;
            }

            if (cha == null) {
                context.send(context.getTranslated("liczek.badchannel"));
                return false;
            }

            if (cha.getId().equals(gc.getLiczekKanal())) {
                context.send(context.getTranslated("liczek.alreadyset"));
                return false;
            }

            context.send(context.getTranslated("liczek.successful", cha.getAsMention()));
            cha.sendMessage(context.getTranslated("liczek.start")).complete();
            gc.setLiczekKanal(cha.getId());
            guildDao.save(gc);
            return true;
        }
        if (context.getArgs()[0].equals("reset")) {
            gc.setLiczekKanal("0");
            liczekDao.delete(context.getGuild());
            guildDao.save(gc);
            context.send(context.getTranslated("liczek.submitreset"));
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

    private String hasPermission(CommandContext ctx, GuildChannel channel) {
        if (ctx.getGuild().getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE,
                Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE, Permission.MESSAGE_READ)) return "generic.yes";
        return "liczek.perms.no";
    }

}
