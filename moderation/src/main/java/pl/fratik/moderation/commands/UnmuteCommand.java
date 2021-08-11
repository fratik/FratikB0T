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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnmuteCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final ModLogListener modLogListener;

    public UnmuteCommand(GuildDao guildDao, ModLogListener modLogListener) {
        this.guildDao = guildDao;
        this.modLogListener = modLogListener;
        name = "unmute";
        uzycieDelim = " ";
        permissions.add(Permission.MANAGE_ROLES);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"usunmute", "niemutuj", "niemute", "usunmuta", "usunmumute"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        Role rola;
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("unmute.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.reply(context.getTranslated("unmute.cant.unmute.yourself"));
            return false;
        }
        if (uzytkownik.isOwner()) {
            context.reply(context.getTranslated("unmute.cant.unmute.owner"));
            return false;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.reply(context.getTranslated("unmute.cant.interact"));
            return false;
        }
        try {
            rola = context.getGuild().getRoleById(gc.getWyciszony());
        } catch (Exception ignored) {
            rola = null;
            List<Role> aktualneRoleWyciszony = context.getGuild().getRolesByName("Wyciszony", false);
            if (aktualneRoleWyciszony.size() == 1) { //migracja z v2
                rola = aktualneRoleWyciszony.get(0);
                gc.setWyciszony(rola.getId());
                guildDao.save(gc);
            }
        }
        if (rola == null) {
            context.reply(context.getTranslated("unmute.no.mute.role"));
            return false;
        }
        if (!uzytkownik.getRoles().contains(rola)) {
            context.reply(context.getTranslated("unmute.not.muted"));
            return false;
        }
        Case aCase = new Case.Builder(uzytkownik, Instant.now(), Kara.UNMUTE)
                .setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        modLogListener.getKnownCases().put(ModLogListener.generateKey(uzytkownik), aCase);
        try {
            context.getGuild().removeRoleFromMember(uzytkownik, rola).complete();
            context.reply(context.getTranslated("unmute.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (Exception ignored) {
            context.reply(context.getTranslated("unmute.fail"));
        }
        return true;
    }
}
