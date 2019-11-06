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

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class WarnCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final CasesDao casesDao;
    private final ShardManager shardManager;
    private final ManagerKomend managerKomend;

    public WarnCommand(GuildDao guildDao, CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.guildDao = guildDao;
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
        name = "warn";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        permissions.add(Permission.BAN_MEMBERS);
        permissions.add(Permission.KICK_MEMBERS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"ostrzez", "dajostrzezenie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("warn.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("warn.cant.warn.yourself"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.send(context.getTranslated("warn.no.bot"));
            return false;
        }
        if (context.getGuild().getMemberById(uzytkownik.getUser().getId()) != null) {
            if (uzytkownik.isOwner()) {
                context.send(context.getTranslated("warn.cant.warn.owner"));
                return false;
            }
            if (!context.getMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("warn.user.cant.interact"));
                return false;
            }
            if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("warn.bot.cant.interact"));
                return false;
            }
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        CaseRow caseRow = casesDao.get(context.getGuild());
        int caseId = Case.getNextCaseId(caseRow);
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild()).setCaseId(caseId)
                .setTimestamp(timestamp).setMessageId(null).setKara(Kara.WARN).createCase();
        aCase.setReason(powod);
        aCase.setIssuerId(context.getSender().getId());
        TextChannel mlog = null;
        if (gc.getModLog() != null && !gc.getModLog().equals("")) mlog = shardManager.getTextChannelById(gc.getModLog());
        if (mlog == null || !context.getGuild().getSelfMember().hasPermission(mlog,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            context.send(context.getTranslated("warn.success", UserUtil.formatDiscrim(uzytkownik)));
            context.send(context.getTranslated("warn.nomodlogs", context.getPrefix()));
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
            WarnUtil.takeAction(guildDao, casesDao, uzytkownik, context.getChannel(), context.getLanguage(),
                    context.getTlumaczenia(), managerKomend);
            return true;
        }
        MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager,
                gc.getLanguage(), managerKomend);
        Message message = mlog.sendMessage(embed).complete();
        context.send(context.getTranslated("warn.success", UserUtil.formatDiscrim(uzytkownik)));
        aCase.setMessageId(message.getId());
        caseRow.getCases().add(aCase);
        casesDao.save(caseRow);
        WarnUtil.takeAction(guildDao, casesDao, uzytkownik, context.getChannel(), context.getLanguage(),
                context.getTlumaczenia(), managerKomend);
        return true;
    }
}
