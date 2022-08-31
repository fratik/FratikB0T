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

package pl.fratik.invite.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MapUtil;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvitesAdminCommand extends SharedInvitesCommand {

    public InvitesAdminCommand(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, EventWaiter eventWaiter, EventBus eventBus) {
        super(inviteDao, invitesCache, guildDao, eventWaiter, eventBus);
        name = "zaproszenia_admin";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES, Permission.MANAGE_SERVER);
        cooldown = 5;
    }

    @SubCommand(name = "ustaw", usage = "<ilosc_zaproszen:int> <rola:role>")
    public void set(@NotNull NewCommandContext context) {
        if (!checkEnabled(context)) return;
        int zaprszenie = context.getArguments().get("ilosc_zaproszen").getAsInt();
        Role rola = context.getArguments().get("rola").getAsRole();
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getRoleZaZaproszenia() == null) gc.setRoleZaZaproszenia(new HashMap<>());

        if (!context.getGuild().getSelfMember().getRoles().get(0).canInteract(rola)) {
            context.reply(context.getTranslated("invites.badrole"));
            return;
        }

        gc.getRoleZaZaproszenia().put(zaprszenie, rola.getId());
        guildDao.save(gc);
        context.reply(context.getTranslated("invites.set.success"));
    }

    @SubCommand(name = "lista")
    public void list(@NotNull NewCommandContext context) {
        if (!checkEnabled(context)) return;
        InteractionHook hook = context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getRoleZaZaproszenia() == null || gc.getRoleZaZaproszenia().isEmpty()) {
            context.reply(context.getTranslated("invites.list.empty"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        List<EmbedBuilder> pages = new ArrayList<>();
        Map<Role, Integer> sorted = new HashMap<>();

        for (Map.Entry<Integer, String> entry : gc.getRoleZaZaproszenia().entrySet()) {
            Role r = context.getGuild().getRoleById(entry.getValue());
            if (r == null) continue;
            sorted.put(r, entry.getKey());
        }
        int i = 0;
        Instant now = Instant.now();
        for (Map.Entry<Role, Integer> entry : MapUtil.sortByValueDesc(sorted).entrySet()) {
            sb.append(context.getTranslated("invites.list.entry", entry.getKey().getAsMention(), entry.getValue())).append("\n");
            if (i != 0 && (i + 1) % 10 == 0) {
                pages.add(renderEmbed(context, sb, sorted, now));
                sb.setLength(0);
            }
            i++;
        }
        if (sb.length() != 0) pages.add(renderEmbed(context, sb, sorted, now));
        if (pages.isEmpty()) {
            context.reply(context.getTranslated("invites.list.empty"));
            return;
        }
        if (pages.size() == 1) context.reply(pages.get(0).build());
        else new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(hook);
    }

    @SubCommand(name = "usun", usage = "<rola:role>")
    public boolean remove(@NotNull NewCommandContext context) {
        if (!checkEnabled(context)) return false;
        Role rola = context.getArguments().get("rola").getAsRole();
        GuildConfig gc = guildDao.get(context.getGuild().getId());
        if (gc.getRoleZaZaproszenia() == null || gc.getRoleZaZaproszenia().isEmpty() || !gc.getRoleZaZaproszenia().containsValue(rola.getId())) {
            context.reply(context.getTranslated("invites.cannotdeleterole"));
            return false;
        }
        gc.getRoleZaZaproszenia().entrySet().removeIf(a -> a.getValue().equals(rola.getId()));
        guildDao.save(gc);
        context.reply(context.getTranslated("invites.successdelete"));
        return true;
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("ilosc_zaproszen")) {
            option.setRequiredRange(1, 1000);
        }
    }
}
