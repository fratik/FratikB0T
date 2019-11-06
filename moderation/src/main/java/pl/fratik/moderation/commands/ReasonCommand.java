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

import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReasonCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final CasesDao casesDao;
    private final ShardManager shardManager;
    private final ManagerKomend managerKomend;
    private static final String RESU = "reason.success";

    public ReasonCommand(GuildDao guildDao, CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.guildDao = guildDao;
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
        name = "reason";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("caseid", "integer");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"powod", "powód"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Integer caseId = (Integer) context.getArgs()[0];
        String reason = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        GuildConfig gc = guildDao.get(context.getGuild());
        CaseRow caseRow = casesDao.get(context.getGuild());
        if (caseRow.getCases().size() < caseId || (caseRow.getCases().size() >= caseId - 1 &&
                caseRow.getCases().get(caseId - 1) == null)) {
            context.send(context.getTranslated("reason.invalid.case"));
            return false;
        }
        if (reason.equals("")) {
            context.send(context.getTranslated("reason.reason.empty"));
            return false;
        }
        Case aCase = caseRow.getCases().get(caseId - 1);
        @Nullable TextChannel modLogChannel = gc.getModLog() != null && !Objects.equals(gc.getModLog(), "") ?
                context.getGuild().getTextChannelById(gc.getModLog()) : null;
        Consumer<Throwable> throwableConsumer = err -> context.send(context.getTranslated("reason.failed"));
        if (modLogChannel == null || !context.getGuild().getSelfMember().hasPermission(modLogChannel,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            aCase.setReason(reason);
            aCase.setIssuerId(context.getSender().getId());
            context.send(context.getTranslated(RESU));
            casesDao.save(caseRow);
            return true;
        }
        if (aCase.getMessageId() == null) {
            aCase.setReason(reason);
            aCase.setIssuerId(context.getSender().getId());
            MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend);
            modLogChannel.sendMessage(embed).queue(m -> {
                context.send(context.getTranslated(RESU), c -> {});
                casesDao.save(caseRow);
            }, throwableConsumer);
            context.send(context.getTranslated(RESU));
            casesDao.save(caseRow);
            return true;
        }
        modLogChannel.retrieveMessageById(aCase.getMessageId()).queue(msg -> {
            aCase.setReason(reason);
            aCase.setIssuerId(context.getSender().getId());
            msg.editMessage(ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend))
                    .override(true).queue(m -> {
                        context.send(context.getTranslated(RESU), c -> {});
                        casesDao.save(caseRow);
                    }, throwableConsumer);
        }, error -> {
            aCase.setReason(reason);
            aCase.setIssuerId(context.getSender().getId());
            MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend);
            modLogChannel.sendMessage(embed).queue(m -> {
                aCase.setMessageId(m.getId());
                context.send(context.getTranslated(RESU), c -> {});
                casesDao.save(caseRow);
            }, throwableConsumer);
        });
        return true;
    }
}
