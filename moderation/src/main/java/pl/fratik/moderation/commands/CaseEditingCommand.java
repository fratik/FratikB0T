/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.util.function.Consumer;

public abstract class CaseEditingCommand extends ModerationCommand {
    protected final CasesDao casesDao;
    protected final ShardManager shardManager;
    protected final ManagerKomend managerKomend;

    protected CaseEditingCommand(CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
    }

    protected boolean updateCase(CommandContext context, String successMessage, String failMessage, CaseRow caseRow, TextChannel modLogChannel, Case aCase, GuildConfig gc) {
        if (modLogChannel == null || !context.getGuild().getSelfMember().hasPermission(modLogChannel,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            context.send(successMessage);
            casesDao.save(caseRow);
            return true;
        }
        Consumer<Throwable> throwableConsumer = err -> context.send(failMessage);
        if (aCase.getDmMsgId() != null) {
            try {
                shardManager.retrieveUserById(aCase.getUserId()).flatMap(User::openPrivateChannel)
                        .flatMap(c -> c.retrieveMessageById(aCase.getDmMsgId()))
                        .flatMap(m -> {
                            StringBuilder content = new StringBuilder(m.getContentRaw());
                            Language lang = context.getTlumaczenia().getLanguage(m.getPrivateChannel().getUser());
                            if (!aCase.getDowody().isEmpty()) {
                                content.append("\n\n");
                                content.append(context.getTlumaczenia().get(lang, "modlog.dm.attached.proof" +
                                        (aCase.getDowody().size() > 1 ? ".multi" : "")));
                                content.append("\n\n");
                                for (int i = 0; i < aCase.getDowody().size(); i++) {
                                    if (content.length() >= 1499) {
                                        content.append(context.getTlumaczenia().get(lang, "modlog.dm.proof.more",
                                                aCase.getDowody().size() - i));
                                    }
                                    content.append(aCase.getDowody().get(i).getContent());
                                    content.append("\n\n");
                                }
                                content.setLength(content.length() - 1);
                            }
                            return m.editMessage(content.toString()).override(true)
                                    .embed(ModLogBuilder.generate(aCase, context.getGuild(), shardManager,
                                            lang, managerKomend, false));
                        }).complete();
            } catch (Exception ignored) {}
        }
        if (aCase.getMessageId() == null) {
            aCase.setIssuerId(context.getSender().getId());
            if (!aCase.getFlagi().contains(Case.Flaga.SILENT)) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend, true);
                modLogChannel.sendMessage(embed).queue(m -> {
                    context.send(successMessage, c -> {});
                    aCase.setMessageId(m.getId());
                    casesDao.save(caseRow);
                }, throwableConsumer);
            }
            context.send(successMessage);
            casesDao.save(caseRow);
            return true;
        }
        modLogChannel.retrieveMessageById(aCase.getMessageId()).queue(msg -> {
            aCase.setIssuerId(context.getSender().getId());
            msg.editMessage(ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend, true))
                    .override(true).queue(m -> {
                context.send(successMessage, c -> {});
                casesDao.save(caseRow);
            }, throwableConsumer);
        }, error -> {
            aCase.setIssuerId(context.getSender().getId());
            if (!aCase.getFlagi().contains(Case.Flaga.SILENT)) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager, gc.getLanguage(), managerKomend, true);
                modLogChannel.sendMessage(embed).queue(m -> {
                    aCase.setMessageId(m.getId());
                    context.send(successMessage, c -> {});
                    casesDao.save(caseRow);
                }, throwableConsumer);
            }
        });
        return true;
    }
}
