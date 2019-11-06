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
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.*;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.moderation.entity.CaseBuilder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MuteCommand extends ModerationCommand {

    private final GuildDao guildDao;

    public MuteCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "mute";
        uzycieDelim = " ";
        permissions.add(Permission.MANAGE_ROLES);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"wycisz", "zamknijtenryjek", "cicho", "niemownic", "badzcicho", "cśś", "cichajgupiababo", "cichaj", "izolatka"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = guildDao.get(context.getGuild());
        Role rola;
        String powod;
        Instant muteDo;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("mute.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("mute.cant.mute.yourself"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.send(context.getTranslated("mute.no.bot"));
            return false;
        }
        Member mem = context.getGuild().getMemberById(uzytkownik.getUser().getId());
        if (mem != null) {
            if (mem.isOwner()) {
                context.send(context.getTranslated("mute.cant.mute.owner"));
                return false;
            }
            if (!context.getMember().canInteract(mem)) {
                context.send(context.getTranslated("mute.cant.interact"));
                return false;
            }
        }
        //#region ustawianie roli
        try {
            rola = context.getGuild().getRoleById(gc.getWyciszony());
            if (rola == null) throw new RuntimeException("nie znaleziono roli, ustaw ją");
        } catch (Exception ignored) {
            List<Role> aktualneRoleWyciszony = context.getGuild().getRolesByName("Wyciszony", false);
            if (aktualneRoleWyciszony.size() == 1) { //migracja z v2
                rola = aktualneRoleWyciszony.get(0);
                gc.setWyciszony(rola.getId());
                guildDao.save(gc);
            } else {
                rola = createMuteRole(context, gc);
                if (rola == null) return false;
            }
        }
        //#endregion ustawianie roli
        if (uzytkownik.getRoles().contains(rola)) {
            context.send(context.getTranslated("mute.already.muted"));
            return false;
        }
        DurationUtil.Response durationResp = DurationUtil.parseDuration(powod);
        powod = durationResp.getTekst();
        muteDo = durationResp.getDoKiedy();
        Case aCase = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild())
                .setCaseId(Case.getNextCaseId(context.getGuild())).setTimestamp(Instant.now()).setMessageId(null)
                .setKara(Kara.MUTE).createCase();
        aCase.setIssuerId(context.getSender());
        aCase.setReason(powod);
        aCase.setValidTo(muteDo);
        List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(context.getGuild(), new ArrayList<>());
        caseList.add(aCase);
        ModLogListener.getKnownCases().put(context.getGuild(), caseList);
        try {
            context.getGuild().addRoleToMember(uzytkownik, rola).complete();
            context.send(context.getTranslated("mute.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (Exception ignored) {
            caseList.remove(aCase);
            ModLogListener.getKnownCases().put(context.getGuild(), caseList);
            context.send(context.getTranslated("mute.fail"));
        }
        return true;
    }

    private Role createMuteRole(CommandContext context, GuildConfig gc) {
        Message msg = context.getChannel().sendMessage(context.getTranslated("mute.no.mute.role")).complete();
        try {
            Role rola = context.getGuild().createRole().setName("Wyciszony").complete();
            context.getGuild().modifyRolePositions(true).selectPosition(rola)
                    .moveTo(context.getGuild().getSelfMember().getRoles().get(0).getPosition() - 1);
            gc.setWyciszony(rola.getId());
            msg.editMessage(context.getTranslated("mute.no.mute.role.updating.perms")).complete();
            Thread.sleep(5000);
            List<GuildChannel> failed = new ArrayList<>();
            for (GuildChannel channel : context.getGuild().getChannels(true)) {
                if (!context.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
                    failed.add(channel);
                    continue;
                }
                try { //NOSONAR
                    channel.putPermissionOverride(rola)
                            .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).complete();
                } catch (Exception e) {
                    failed.add(channel);
                }
            }
            String wiad = context.getTranslated("mute.no.mute.role.updated.perms.with.errors",
                    failed.size(), failed.stream().map(c -> "<#" + c.getId() + ">")
                            .collect(Collectors.joining(", ")));
            if (!failed.isEmpty()) {
                if (wiad.length() <= 2000) {
                    msg.editMessage(wiad).queue();
                } else {
                    msg.editMessage(context.getTranslated("mute.no.mute.role.updated.perms.with.errors",
                            failed.size(), "[too long]")).queue();
                }
            } else {
                msg.editMessage(context.getTranslated("mute.no.mute.role.updated.perms")).queue();
            }
            guildDao.save(gc);
            return rola;
        } catch (Exception ignored1) {
            msg.editMessage(context.getTranslated("mute.no.mute.role.cant.create")).queue();
            return null;
        }
    }
}
