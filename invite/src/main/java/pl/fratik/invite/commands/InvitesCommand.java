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
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.*;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;
import pl.fratik.invite.entity.InviteData;

import java.time.Instant;
import java.util.*;

public class InvitesCommand extends AbstractInvitesCommand {

    private final GuildDao guildDao;
    private final ManagerArgumentow managerArgumentow;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public InvitesCommand(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, ManagerArgumentow managerArgumentow, EventWaiter eventWaiter, EventBus eventBus) {
        super(inviteDao, invitesCache);
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "invites";
        category = CommandCategory.INVITES;
        allowPermLevelChange = false;
        cooldown = 5;
        aliases = new String[] {"zaproszenia"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("user|integer", "string");
        hmap.put("rola", "role");
        uzycie = new Uzycie(hmap, new boolean[] {false, false});
        uzycieDelim = " ";
        this.guildDao = guildDao;
        this.managerArgumentow = managerArgumentow;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!checkEnabled(context)) return false;
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name = "info")
    public boolean info(@NotNull CommandContext context) {
        if (!checkEnabled(context)) return false;
        User osoba = null;
        if (context.getRawArgs().length != 0) osoba = (User) managerArgumentow.getArguments().get("user")
                .execute(context.getRawArgs()[0], context.getTlumaczenia(), context.getLanguage());
        if (osoba == null) osoba = context.getSender();

        InviteData dao = inviteDao.get(osoba.getId(), context.getGuild().getId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(osoba));
        eb.setThumbnail(UserUtil.getAvatarUrl(osoba));
        eb.setTitle(UserUtil.formatDiscrim(osoba));
        eb.setTimestamp(Instant.now());
        if (!context.getGuild().getSelfMember().hasPermission(Permission.MANAGE_SERVER)) {
            eb.setDescription(context.getTranslated("invites.maybe.doesnt.work"));
        }
        eb.addField(context.getTranslated("invites.stats"),
                context.getTranslated("invites.fieldvalue", dao.getTotalInvites() - dao.getLeaveInvites(),
                        dao.getLeaveInvites(), dao.getTotalInvites()
                ), false);

        context.reply(eb.build());
        return true;
    }

    @SubCommand(name = "set")
    public boolean set(@NotNull CommandContext context) {
        if (!checkAdmin(context)) return false;
        if (!checkEnabled(context)) return false;
        try {
            int zaprszenie = Integer.parseInt(context.getRawArgs()[0]);
            if (zaprszenie > 1000 || zaprszenie <= 0) {
                context.reply(context.getTranslated("invites.badnumber"));
                return false;
            }
            Role rola = (Role) context.getArgs()[1];
            if (rola == null) throw new NumberFormatException();
            GuildConfig gc = guildDao.get(context.getGuild());
            if (gc.getRoleZaZaproszenia() == null) gc.setRoleZaZaproszenia(new HashMap<>());

            if (!context.getGuild().getSelfMember().getRoles().get(0).canInteract(rola)) {
                context.reply(context.getTranslated("invites.badrole"));
                return false;
            }

            gc.getRoleZaZaproszenia().put(zaprszenie, rola.getId());
            guildDao.save(gc);
            context.reply(context.getTranslated("invites.set.success"));
            return true;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            CommonErrors.usage(context);
        }
        return false;
    }

    @SubCommand(name = "list")
    public boolean list(@NotNull CommandContext context) {
        if (!checkAdmin(context)) return false;
        if (!checkEnabled(context)) return false;
        GuildConfig gc = guildDao.get(context.getGuild());
        if (gc.getRoleZaZaproszenia() == null || gc.getRoleZaZaproszenia().isEmpty()) {
            context.reply(context.getTranslated("invites.list.empty"));
            return false;
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
            return false;
        }
        if (pages.size() == 1) context.reply(pages.get(0).build());
        else new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(), context.getTlumaczenia(), eventBus).create(context.getTextChannel());
        return true;
    }

    @NotNull
    private EmbedBuilder renderEmbed(@NotNull CommandContext context, StringBuilder sb, Map<Role, Integer> sorted, Instant now) {
        return new EmbedBuilder()
                .setAuthor(context.getTranslated("invites.roles", sorted.size()))
                .setColor(UserUtil.getPrimColor(context.getSender()))
                .setDescription(sb.toString())
                .setTimestamp(now);
    }

    @SubCommand(name = "remove")
    public boolean remove(@NotNull CommandContext context) {
        if (!checkAdmin(context)) return false;
        if (!checkEnabled(context)) return false;
        if (context.getArgs().length == 0) {
            CommonErrors.usage(context);
            return false;
        }
        Role rola = (Role) managerArgumentow.getArguments().get("role").execute((String) context.getArgs()[0],
                context.getTlumaczenia(), context.getLanguage(), context.getGuild());
        if (rola == null) {
            context.reply(context.getTranslated("sklep.sprzedaj.invalidrole"));
            return false;
        }
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

    private boolean checkAdmin(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < PermLevel.ADMIN.getNum()) {
            context.send(context.getTranslated("invites.no.perms", PermLevel.ADMIN.getNum(), context.getTranslated(PermLevel.ADMIN.getLanguageKey())));
            return false;
        }
        return true;
    }

}
