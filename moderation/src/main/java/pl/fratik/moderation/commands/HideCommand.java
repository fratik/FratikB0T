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

package pl.fratik.moderation.commands;

import com.google.common.collect.Lists;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.ManagerKomend;

import java.util.List;

public class HideCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final ManagerKomend managerKomend;

    public HideCommand(GuildDao guildDao, ManagerKomend managerKomend) {
        this.guildDao = guildDao;
        this.managerKomend = managerKomend;
        name = "hide";
        aliases = new String[] {"unhide", "ukryj", "schowaj", "zachowajdlaadministracji"};
        category = CommandCategory.MODERATION;
        permLevel = PermLevel.ADMIN;
        permissions.add(Permission.MANAGE_PERMISSIONS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getAdminRole() == null || gc.getAdminRole().equals("") || (gc.getAdminRole() != null &&
                !gc.getAdminRole().equals("") && context.getGuild().getRoleById(gc.getAdminRole()) == null)) {
            context.send(context.getTranslated("hide.no.adminrole", managerKomend.getPrefixes(context.getGuild()).get(0)));
            return false;
        }
        Role adminRole = context.getGuild().getRoleById(gc.getAdminRole());
        if (adminRole == null) {
            context.send(context.getTranslated("hide.no.adminrole", managerKomend.getPrefixes(context.getGuild()).get(0)));
            return false;
        }
        PermissionOverrideAction overrides = context.getChannel().upsertPermissionOverride(context.getGuild().getPublicRole());
        if (overrides.getDeniedPermissions().contains(Permission.VIEW_CHANNEL)) {
            PermissionOverrideAction publicOverrides = context.getChannel().putPermissionOverride(context.getGuild().getPublicRole());
            PermissionOverrideAction adminOverrides = context.getChannel().putPermissionOverride(adminRole);
            List<Permission> publicDeny = Lists.newArrayList(publicOverrides.getDeniedPermissions());
            List<Permission> adminAllow = Lists.newArrayList(adminOverrides.getAllowedPermissions());
            publicDeny.remove(Permission.VIEW_CHANNEL);
            adminAllow.remove(Permission.VIEW_CHANNEL);
            try {
                publicOverrides.setDeny(publicDeny).complete();
                adminOverrides.setAllow(adminAllow).complete();
                context.send(context.getTranslated("hide.unhide.success"));
            } catch (Exception e) {
                context.send(context.getTranslated("hide.unhide.fail"));
            }
        } else {
            PermissionOverrideAction publicOverrides = context.getChannel().putPermissionOverride(context.getGuild().getPublicRole());
            PermissionOverrideAction adminOverrides = context.getChannel().putPermissionOverride(adminRole);
            List<Permission> publicDeny = Lists.newArrayList(publicOverrides.getDeniedPermissions());
            List<Permission> adminAllow = Lists.newArrayList(adminOverrides.getAllowedPermissions());
            publicDeny.add(Permission.VIEW_CHANNEL);
            adminAllow.add(Permission.VIEW_CHANNEL);
            try {
                publicOverrides.setDeny(publicDeny).complete();
                adminOverrides.setAllow(adminAllow).complete();
                context.send(context.getTranslated("hide.hide.success"));
            } catch (Exception e) {
                context.send(context.getTranslated("hide.hide.fail"));
            }
        }
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.ADMIN;
    }
}
