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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class RolementionCommand extends ModerationCommand {
    public RolementionCommand() {
        name = "rolemention";
        permLevel = PermLevel.ADMIN;
        cooldown = 10;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MANAGE_ROLES);
        permissions.add(Permission.MESSAGE_MANAGE);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("rola", "role");
        hmap.put("tekst", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        uzycieDelim = " ";
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        Role rola = (Role) context.getArgs()[0];
        String tekst;
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            tekst = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else tekst = null;
        if (!context.getGuild().getSelfMember().canInteract(rola)) {
            context.send(context.getTranslated("rolemention.hierarchy"));
            return false;
        }
        context.getMessage().delete().queue();
        if (tekst != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(UserUtil.formatDiscrim(context.getSender()), null, context.getSender()
                    .getEffectiveAvatarUrl());
            eb.setDescription(tekst);
            eb.setTimestamp(context.getMessage().getTimeCreated());
            if (context.getMember().getColor() != null) eb.setColor(context.getMember().getColor());
            else eb.setColor(UserUtil.getPrimColor(context.getSender()));
            boolean state = rola.isMentionable();
            rola.getManager().setMentionable(true).complete();
            context.getChannel().sendMessage(rola.getAsMention()).embed(eb.build()).complete();
            rola.getManager().setMentionable(state).complete();
            return true;
        }
        boolean state = rola.isMentionable();
        rola.getManager().setMentionable(true).complete();
        context.getChannel().sendMessage(rola.getAsMention()).complete();
        rola.getManager().setMentionable(state).complete();
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.ADMIN;
    }
}
