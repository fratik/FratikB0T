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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.UserUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ZglosCommand extends ModerationCommand {

    private final GuildDao guildDao;

    public ZglosCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "zglos";
        permLevel = PermLevel.EVERYONE;
        permissions.add(Permission.BAN_MEMBERS);
        permissions.add(Permission.KICK_MEMBERS);
        category = CommandCategory.MODERATION;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        uzycieDelim = " ";
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"report"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member uzytkownik = (Member) context.getArgs()[0];
        String powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        if (powod.isEmpty()) {
            context.send(context.getTranslated("zglos.no.reason"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.send(context.getTranslated("zglos.no.bot"));
            return false;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("zglos.cant.report.yourself"));
            return false;
        }
        if (uzytkownik.isOwner()) {
            context.send(context.getTranslated("zglos.cant.report.owner"));
            return false;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.send(context.getTranslated("zglos.self.cant.interact"));
            return false;
        }
        powod = powod.replaceAll("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
                "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
                "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})", context.getTranslated("generic.link.removed"));
        String tmpAdmk = guildDao.get(context.getGuild()).getKanalAdministracji();
        TextChannel channel = null;
        if (tmpAdmk != null && !tmpAdmk.isEmpty()) channel = context.getGuild().getTextChannelById(tmpAdmk);
        if (tmpAdmk == null || tmpAdmk.isEmpty() || channel == null || !channel.canTalk()) {
            context.send(context.getTranslated("zglos.invalid.admin.channel"));
            return false;
        }
        context.getMessage().delete().queue();
        try {
            channel.sendMessage(context.getTranslated("zglos.admin.message",
                    UserUtil.formatDiscrim(context.getSender()), context.getSender().getId(), powod,
                    UserUtil.formatDiscrim(uzytkownik), uzytkownik.getUser().getId(),
                    context.getPrefix(), uzytkownik.getUser().getId())).complete();
        } catch (Exception e) {
            context.getChannel().sendMessage(context.getTranslated("zglos.failure"))
                    .queueAfter(5, TimeUnit.SECONDS);
            return false;
        }
        context.getChannel().sendMessage(context.getTranslated("zglos.confirmation"))
                .queueAfter(5, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.EVERYONE;
    }
}
