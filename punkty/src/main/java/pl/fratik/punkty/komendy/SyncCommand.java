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

package pl.fratik.punkty.komendy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncCommand extends NewCommand {

    private final GuildDao guildDao;

    public SyncCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "synchronizuj";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        InteractionHook hook = context.reply(context.getTranslated("sync.synchronizing"));
        GuildConfig gc = guildDao.get(context.getGuild());
        AtomicInteger udaloSieDla = new AtomicInteger();
        ArrayList<User> nieUdaloSie = new ArrayList<>();
        for (Member member : context.getGuild().loadMembers().get()) {
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
        if (nieUdaloSie.isEmpty()) hook.editOriginal(context.getTranslated("sync.success", udaloSieDla.toString())).queue();
        else {
            String uzytkownicy = UserUtil.getPoPrzecinku(nieUdaloSie);
            if (uzytkownicy.length() < 1000) hook.editOriginal(context.getTranslated("sync.failed", uzytkownicy)).queue();
            else hook.editOriginal(context.getTranslated("sync.failed.toolong", nieUdaloSie.size())).queue();
        }
    }
}
