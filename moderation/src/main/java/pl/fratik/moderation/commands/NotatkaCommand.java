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
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotatkaCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final CasesDao casesDao;
    private final ShardManager shardManager;
    private final ManagerKomend managerKomend;

    public NotatkaCommand(GuildDao guildDao, CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.guildDao = guildDao;
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
        name = "notatka";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        cooldown = 5;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("notatka", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"notka", "note"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else throw new IllegalStateException("nie ma powodu, wtf");
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("notatka.cant.note.yourself"));
            return false;
        }
        if (context.getGuild().getMemberById(uzytkownik.getUser().getId()) != null) {
            if (uzytkownik.isOwner()) {
                context.send(context.getTranslated("notatka.cant.note.owner"));
                return false;
            }
            if (!context.getMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("notatka.user.cant.interact"));
                return false;
            }
            if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("notatka.bot.cant.interact"));
                return false;
            }
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        CaseRow caseRow = casesDao.get(context.getGuild());
        int caseId = Case.getNextCaseId(caseRow);
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild()).setCaseId(caseId)
                .setTimestamp(timestamp).setMessageId(null).setKara(Kara.NOTATKA).createCase();
        aCase.setReason(powod);
        aCase.setIssuerId(context.getSender().getId());
        String mlogchanStr = gc.getModLog();
        if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
        TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
        if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            context.send(context.getTranslated("notatka.success", UserUtil.formatDiscrim(uzytkownik)));
            context.send(context.getTranslated("notatka.nomodlogs", context.getPrefix()));
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
            return false;
        }
        MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager,
                gc.getLanguage(), managerKomend);
        mlogchan.sendMessage(embed).queue(message -> {
            context.send(context.getTranslated("notatka.success", UserUtil.formatDiscrim(uzytkownik)), m -> {});
            aCase.setMessageId(message.getId());
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        });
        return true;
    }
}
