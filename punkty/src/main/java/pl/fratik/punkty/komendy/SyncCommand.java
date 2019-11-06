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

package pl.fratik.punkty.komendy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncCommand extends Command {

    private final GuildDao guildDao;

    public SyncCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "sync";
        aliases = new String[] {"synchronizuj"};
        category = CommandCategory.POINTS;
        permLevel = PermLevel.ADMIN;
        permissions.add(Permission.MANAGE_ROLES);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message message = context.send(context.getTranslated("sync.synchronizing"));
        GuildConfig gc = guildDao.get(context.getGuild());
        AtomicInteger udaloSieDla = new AtomicInteger();
        ArrayList<User> nieUdaloSie = new ArrayList<>();
        for (Member member : context.getGuild().getMembers()) {
            int poziom = LicznikPunktow.calculateLvl(LicznikPunktow.getPunkty(member));
            List<Role> roleList = new ArrayList<>();
            gc.getRoleZaPoziomy().forEach((lvl, id) -> {
                Role role = context.getGuild().getRoleById(id);
                if (role != null && poziom >= lvl && !member.getRoles().contains(role)) roleList.add(role);
            });
            if (roleList.isEmpty()) continue;
            try {
                context.getGuild()
                        .modifyMemberRoles(member, roleList, new ArrayList<>())
                        .complete();
                udaloSieDla.addAndGet(1);
            } catch (Exception e) {
                nieUdaloSie.add(member.getUser());
            }
        }
        if (nieUdaloSie.isEmpty()) message.editMessage(context.getTranslated("sync.success", udaloSieDla.toString())).queue();
        else {
            String uzytkownicy = UserUtil.getPoPrzecinku(nieUdaloSie);
            if (uzytkownicy.length() < 1000) message.editMessage(context.getTranslated("sync.failed", uzytkownicy)).queue();
            else message.editMessage(context.getTranslated("sync.failed.toolong", nieUdaloSie.size())).queue();
        }
        return true;
    }
}
