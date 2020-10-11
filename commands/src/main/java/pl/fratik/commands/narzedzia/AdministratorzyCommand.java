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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;

import java.util.ArrayList;
import java.util.List;

import static net.dv8tion.jda.api.entities.MessageEmbed.VALUE_MAX_LENGTH;

public class AdministratorzyCommand extends Command {

    private final GuildDao guildDao;
    private final Cache<GuildConfig> gcCache;

    public AdministratorzyCommand(GuildDao guildDao, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        name = "administratorzy";
        aliases = new String[] {"admini"};
        category = CommandCategory.SYSTEM;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
        this.gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Override
    public boolean execute(CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed(context.getTranslated("administratorzy.embed.header"), null);
        Member owner;
        try {
            owner = context.getGuild().retrieveOwner().complete();
        } catch (ErrorResponseException e) {
            owner = null;
        }
        GuildConfig gc = gcCache.get(context.getGuild().getId(), guildDao::get);
        List<Role> manageServerRoles = new ArrayList<>();
        List<Role> adminRoles = new ArrayList<>();
        List<Role> modRoles = new ArrayList<>();
        for (Role r : context.getGuild().getRoles()) {
            if (r.hasPermission(Permission.MANAGE_SERVER)) manageServerRoles.add(r);
            else if (gc.getAdminRole().equals(r.getId())) adminRoles.add(r);
            else if (gc.getModRole().equals(r.getId())) modRoles.add(r);
        }
        eb.addField(formatFieldTitle(PermLevel.OWNER, context),
                owner == null ? context.getTranslated("administratorzy.owner.null") : owner.getAsMention(), false);
        eb.addField(formatFieldTitle(PermLevel.MANAGESERVERPERMS, context), formatFieldContent(manageServerRoles), false);
        eb.addField(formatFieldTitle(PermLevel.ADMIN, context), formatFieldContent(adminRoles), false);
        eb.addField(formatFieldTitle(PermLevel.MOD, context), formatFieldContent(modRoles), false);
        context.send(eb.build());
        return true;
    }

    private String formatFieldTitle(PermLevel plvl, CommandContext context) {
        return String.format("%s (%s)", context.getTranslated(plvl.getLanguageKey()), plvl.getNum());
    }

    private String formatFieldContent(List<Role> roles) {
        StringBuilder sb = new StringBuilder();
        for (Role r : roles) {
            if (sb.length() + r.getAsMention().length() + 2 >= VALUE_MAX_LENGTH - 3) {
                sb.setLength(sb.length() - 2); // wywalenie ,
                sb.append("..., ");
                break;
            }
            sb.append(r.getAsMention()).append(", ");
        }
        return sb.substring(0, sb.length() - 2); // wywalenie ,
    }

}
