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

import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.liczek.LiczekListener;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;

public class LiczekCommand extends Command {

    private final GuildDao guildDao;
    private final LiczekListener liczekListener;

    public LiczekCommand(GuildDao guildDao, LiczekListener liczekListener) {
        this.guildDao = guildDao;
        this.liczekListener = liczekListener;

        name = "liczek";
        category = CommandCategory.BASIC;
        permLevel = PermLevel.ADMIN;
        aliases = new String[] {"liczydlo"};
        uzycieDelim = " ";

        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("typ", "string");
        hmap.put("kanal", "channel");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());

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
            context.send(context.getTranslated("liczek.info", txt.getAsMention(), txt.getId(), gc.getLiczekLiczba()));
            return true;
        }
        if (context.getArgs()[0].equals("set")) {
            if (context.getArgs()[1] == null)  {
                CommonErrors.usage(context);
                return false;
            }
            TextChannel cha = null;
            cha = (TextChannel) context.getArgs()[1];

            if (cha == null) {
                context.send(context.getTranslated("liczek.badchannel"));
                return false;
            }

            if (cha.getId().equals(gc.getLiczekKanal())) {
                context.send(context.getTranslated("liczek.alreadyset"));
                return false;
            }

            context.send(context.getTranslated("liczek.successful", cha.getAsMention()));
            cha.sendMessage(context.getTranslated("liczek.start")).queue();
            gc.setLiczekKanal(cha.getId());
            guildDao.save(gc);
            return true;
        }
        if (context.getArgs()[0].equals("reset")) {
            gc.setLiczekKanal("0");
            gc.setLiczekLiczba(0);
            guildDao.save(gc);
            context.send(context.getTranslated("liczek.submitreset"));
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

}
