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
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.moderation.entity.Case;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.moderation.entity.CaseBuilder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class KickCommand extends ModerationCommand {

    public KickCommand() {
        name = "kick";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        permissions.add(Permission.KICK_MEMBERS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"wyrzuc", "wywalgo", "kickmen", "kopnij", "aidzpan", "wykoptypa", "wykop.pl"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("kick.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("kick.cant.kick.yourself"));
            return false;
        }
        if (uzytkownik.isOwner()) {
            context.send(context.getTranslated("kick.cant.kick.owner"));
            return false;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.send(context.getTranslated("kick.cant.interact"));
            return false;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.send(context.getTranslated("kick.bot.cant.interact"));
            return false;
        }
        Case aCase = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild())
                .setCaseId(Case.getNextCaseId(context.getGuild())).setTimestamp(Instant.now()).setMessageId(null)
                .setKara(Kara.KICK).createCase();
        aCase.setIssuerId(context.getSender());
        aCase.setReason(powod);
        List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(context.getGuild(), new ArrayList<>());
        caseList.add(aCase);
        ModLogListener.getKnownCases().put(context.getGuild(), caseList);
        try {
            context.getGuild().kick(uzytkownik).reason(powod).complete();
            context.send(context.getTranslated("kick.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (HierarchyException e) {
            caseList.remove(aCase);
            ModLogListener.getKnownCases().put(context.getGuild(), caseList);
            context.send(context.getTranslated("kick.failed.hierarchy"));
        } catch (Exception e) {
            caseList.remove(aCase);
            ModLogListener.getKnownCases().put(context.getGuild(), caseList);
            context.send(context.getTranslated("kick.failed"));
        }
        return true;
    }
}
