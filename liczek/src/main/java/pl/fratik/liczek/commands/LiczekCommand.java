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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.liczek.LiczekListener;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;

public class LiczekCommand extends Command {

    private final GuildDao guildDao;
    private final ManagerArgumentow managerArgumentow;
    private final LiczekListener liczekListener;

    public LiczekCommand(GuildDao guildDao, ManagerArgumentow managerArgumentow, LiczekListener liczekListener) {
        this.guildDao = guildDao;
        this.managerArgumentow = managerArgumentow;
        this.liczekListener = liczekListener;
        name = "liczek";
        category = CommandCategory.BASIC;
        permLevel = PermLevel.ADMIN;
        aliases = new String[] {"liczydlo"};
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("typ", "string");
        hmap.put("kanal", "channel");
        uzycieDelim = " ";
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());

        Integer liczba = gc.getLiczekLiczba();

        if (context.getArgs()[0].equals("info")) {
            TextChannel cha = null;
            if (gc.getLiczekKanal() != null) {
                cha = (TextChannel) context.getGuild().getGuildChannelById(gc.getLiczekKanal());
            }

            if (cha == null) {
                context.send(context.getTranslated("liczek.notset"));
                return false;
            }
            context.send(context.getTranslated("liczek.info", cha.getId(), cha.getName(), liczba));
            return true;
        }

        if (context.getArgs()[0].equals("reset")) {
            gc.setLiczekKanal("0");
            gc.setLiczekLiczba(0);
            context.send(context.getTranslated("liczek.submitreset"));
            return true;
        }

        if (context.getArgs()[0].equals("set")) {
            Member botMember = context.getGuild().getMemberById(context.getEvent().getJDA().getSelfUser().getId());
            assert botMember != null;

            TextChannel channel = null;
            channel = (TextChannel) context.getArgs()[1];
            if (channel == null) {
                context.send("channel==null");
                return false;
            }

//            if (!liczekListener.hasPermission(botMember, channel)) {
//                context.send(context.getTranslated("liczek.badchannel"));
//                return false;
//            }

//            liczekListener.setNumer(context.getGuild(), 0);
//            liczekListener.setChannel(context.getGuild(), channel);
//            liczekListener.refreshDescription(context.getGuild(), botMember.getUser());

            context.send(context.getTranslated("liczek.successful", channel.getId()));
            channel.sendMessage(context.getTranslated("liczek.start")).queue();
            return true;

        }
        CommonErrors.usage(context);
        return false;
    }

}
