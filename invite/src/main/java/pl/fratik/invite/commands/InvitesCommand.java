/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.invite.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.entity.InviteConfig;
import pl.fratik.invite.entity.InviteDao;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class InvitesCommand extends Command {

    private final InviteDao inviteDao;
    private final GuildDao guildDao;
    private final ManagerArgumentow managerArgumentow;

    public InvitesCommand(InviteDao inviteDao, GuildDao guildDao, ManagerArgumentow managerArgumentow) {
        name = "invites";
        category = CommandCategory.INVITES;
        aliases = new String[] {"zaproszenia"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("user|integer", "string");
        hmap.put("rola", "role");
        uzycie = new Uzycie(hmap, new boolean[] {false, false});
        uzycieDelim = " ";
        cooldown = 5;
        this.inviteDao = inviteDao;
        this.guildDao = guildDao;
        this.managerArgumentow = managerArgumentow;
        allowPermLevelChange = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name = "info")
    public boolean info(@NotNull CommandContext context) {
        User osoba = null;
        if (context.getArgs().length != 0) osoba = (User) managerArgumentow.getArguments().get("user")
                .execute(context.getRawArgs()[0], context.getTlumaczenia(), context.getLanguage(), context.getGuild());
        if (osoba == null) osoba = context.getSender();

        InviteConfig dao = inviteDao.get(osoba.getId(), context.getGuild().getId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(osoba));
        eb.setThumbnail(UserUtil.getAvatarUrl(osoba));
        eb.setTitle(UserUtil.formatDiscrim(osoba));
        eb.setTimestamp(Instant.now());
        if (!context.getGuild().getSelfMember().hasPermission(Permission.MANAGE_SERVER)) {
            eb.setDescription(context.getTranslated("invites.maybie.doesnt.work"));
        }
        eb.addField(context.getTranslated("invites.stats"),
                context.getTranslated("invites.fieldvalue", 2137,
                        dao.getTotalInvites() - dao.getLeaveInvites(),
                        dao.getLeaveInvites(), dao.getTotalInvites()
                ), false);


        context.send(eb.build());
        return true;
    }

    @SubCommand(name = "set")
    public boolean set(@NotNull CommandContext context) {
        try {
            int zaprszenie = Integer.parseInt(context.getRawArgs()[0]);
            Role rola = (Role) context.getArgs()[1];
            if (rola == null || zaprszenie <= 0) throw new NumberFormatException();
            GuildConfig gc = guildDao.get(context.getGuild());
            if (gc.getRoleZaZaproszenia() == null) gc.setRoleZaZaproszenia(new HashMap<>());

            if (!rola.canInteract(context.getGuild().getSelfMember().getRoles().get(0))) {
                context.send(context.getTranslated("topinvites.badrole"));
                return false;
            }

            gc.getRoleZaZaproszenia().put(zaprszenie, rola.getId());
            guildDao.save(gc);
            context.send(context.getTranslated("topinvites.set.succes"));
            return true;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            CommonErrors.usage(context);
        }
        return false;
    }

}
