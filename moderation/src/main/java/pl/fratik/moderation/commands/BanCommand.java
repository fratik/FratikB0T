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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.listeners.ModLogListener;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class BanCommand extends ModerationCommand {

    public BanCommand() {
        name = "ban";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        permissions.add(Permission.BAN_MEMBERS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "user");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"b", "dzban", "BiletWJednąStronę", "syberia", "banujetypa", "zbanuj", "del", "delett", "b&"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        User uzytkownik = (User) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("ban.reason.default");
        if (uzytkownik.equals(context.getSender())) {
            context.send(context.getTranslated("ban.cant.ban.yourself"));
            return false;
        }
        Member uzMem = context.getGuild().getMemberById(uzytkownik.getId());
        if (uzMem != null) {
            if (uzMem.isOwner()) {
                context.send(context.getTranslated("ban.cant.ban.owner"));
                return false;
            }
            if (!context.getMember().canInteract(uzMem)) {
                context.send(context.getTranslated("ban.cant.interact"));
                return false;
            }
        }
        DurationUtil.Response durationResp = DurationUtil.parseDuration(powod);
        powod = durationResp.getTekst();
        Instant banDo = durationResp.getDoKiedy();
        CaseBuilder cb = new CaseBuilder().setUser(uzytkownik).setGuild(context.getGuild())
                .setCaseId(Case.getNextCaseId(context.getGuild())).setTimestamp(Instant.now())
                .setMessageId(null);
        if (banDo != null) cb.setKara(Kara.TIMEDBAN);
        else cb.setKara(Kara.BAN);
        Case aCase = cb.createCase();
        aCase.setIssuerId(context.getSender());
        aCase.setReason(powod);
        aCase.setValidTo(banDo);
        List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(context.getGuild(), new ArrayList<>());
        caseList.add(aCase);
        ModLogListener.getKnownCases().put(context.getGuild(), caseList);
        try {
            context.getGuild().ban(uzytkownik, 0, powod).reason(powod).complete();
            context.send(context.getTranslated("ban.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (HierarchyException e) {
            caseList.remove(aCase);
            ModLogListener.getKnownCases().put(context.getGuild(), caseList);
            context.send(context.getTranslated("ban.failed.hierarchy"));
        } catch (Exception e) {
            caseList.remove(aCase);
            ModLogListener.getKnownCases().put(context.getGuild(), caseList);
            context.send(context.getTranslated("ban.failed"));
        }
        return true;
    }
}
