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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SprawdzuprawnieniaCommand extends NewCommand {
    public SprawdzuprawnieniaCommand() {
        name = "sprawdzuprawnienia";
        usage = "[kanal:textchannel]";
    }

    private static final List<Permission> perms;

    static {
        List<Permission> permy = new ArrayList<>();
        Collections.addAll(permy, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_MANAGE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI, Permission.KICK_MEMBERS,
                Permission.BAN_MEMBERS, Permission.MESSAGE_MENTION_EVERYONE);
        perms = Collections.unmodifiableList(permy);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        TextChannel kanal = context.getArgumentOr("kanal", context.getChannel().asTextChannel(), o -> o.getAsChannel().asTextChannel());
        Member sm = context.getGuild().getSelfMember();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(context.getTranslated("sprawdzuprawnienia.embed.author"), null,
                sm.getUser().getAvatarUrl());
        eb.setDescription(context.getTranslated("sprawdzuprawnienia.embed.description"));
        for (Permission perm : perms) {
            eb.addField(context.getTranslated("sprawdzuprawnienia.perm." + perm.name().toLowerCase()
                    .replaceAll("_", ".")), getReaction(sm.hasPermission(kanal, perm)), false);
        }
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.view.channel"), getReaction(sm
//                .hasPermission(kanal, Permission.VIEW_CHANNEL)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.write"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_WRITE)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.add.reaction"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_ADD_REACTION)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.manage"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_MANAGE)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.embed.links"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_EMBED_LINKS)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.attach.files"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_ATTACH_FILES)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.history"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_HISTORY)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.ext.emoji"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_EXT_EMOJI)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.kick.members"), getReaction(sm
//                .hasPermission(kanal, Permission.KICK_MEMBERS)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.ban.members"), getReaction(sm
//                .hasPermission(kanal, Permission.BAN_MEMBERS)), true);
//        eb.addField(context.getTranslated("sprawdzuprawnienia.perm.message.mention.everyone"), getReaction(sm
//                .hasPermission(kanal, Permission.MESSAGE_MENTION_EVERYONE)), true);
        int procent = calculatePrecent(sm, kanal);
        eb.setFooter(procent + "%", null);
        eb.setColor(Color.decode(procent >= 50 ? "#00ff00" : procent >= 25 ? "#ffff00" : "#ff0000")); // NOSONAR
        context.reply(eb.build());
    }

    private int calculatePrecent(Member sm, TextChannel kanal) {
        List<Permission> permy = sm.getPermissions(kanal).stream().filter(perms::contains)
                .collect(Collectors.toList());
        return (int) Math.floor(((double) permy.size() / perms.size()) * 100);
    }

    private String getReaction(boolean tru) {
        return tru ? "\u2705" : "\u274c";
    }
}
