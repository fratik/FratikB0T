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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.OldCase;
import pl.fratik.moderation.entity.OldCaseRow;
import pl.fratik.moderation.entity.OldCasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.util.function.Consumer;

public abstract class CaseEditingCommand extends ModerationCommand {
    protected final OldCasesDao casesDao;
    protected final ShardManager shardManager;
    protected final ManagerKomend managerKomend;

    protected CaseEditingCommand(OldCasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
    }

    protected boolean updateCase(CommandContext context, String successMessage, String failMessage, OldCaseRow caseRow, TextChannel modLogChannel, OldCase aCase, GuildConfig gc) {
        if (modLogChannel == null || !context.getGuild().getSelfMember().hasPermission(modLogChannel,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            context.reply(successMessage);
            casesDao.save(caseRow);
            return true;
        }
        Consumer<Throwable> throwableConsumer = err -> context.reply(failMessage, c -> {});
        User issuer = null;
        if (aCase.getIssuerId() != null && !aCase.getIssuerId().isEmpty()) {
            try {
                issuer = shardManager.retrieveUserById(aCase.getIssuerId()).complete();
            } catch (ErrorResponseException er) {
                if (er.getErrorResponse() != ErrorResponse.UNKNOWN_USER) throw er;
                // else ignore
            }
        }
        User finalIssuer = issuer;
        User user = shardManager.retrieveUserById(aCase.getUserId()).complete();
        if (aCase.getDmMsgId() != null) {
            try {
                shardManager.retrieveUserById(aCase.getUserId()).flatMap(User::openPrivateChannel)
                        .flatMap(c -> c.retrieveMessageById(aCase.getDmMsgId()))
                        .flatMap(m -> {
                            Tlumaczenia tlum = context.getTlumaczenia();
                            Language lang = tlum.getLanguage(m.getPrivateChannel().getUser());
                            StringBuilder content = new StringBuilder(tlum.get(lang, "modlog.dm.msg",
                                    context.getGuild().getName()));
                            if (!aCase.getDowody().isEmpty()) {
                                content.append("\n\n");
                                content.append(tlum.get(lang, "modlog.dm.attached.proof" +
                                        (aCase.getDowody().size() > 1 ? ".multi" : "")));
                                content.append("\n\n");
                                for (int i = 0; i < aCase.getDowody().size(); i++) {
                                    String moreStr = tlum.get(lang, "modlog.dm.proof.more",
                                            aCase.getDowody().size() - i);
                                    if (content.length() + moreStr.length() >= 1499) {
                                        content.append(moreStr);
                                        content.append("\n"); // setLength -1 wywali tego \n
                                        break;
                                    }
                                    content.append(aCase.getDowody().get(i).getContent());
                                    content.append("\n\n");
                                }
                                content.setLength(content.length() - 1);
                            }
                            return m.editMessage(content.toString()).override(true)
                                    .embed(ModLogBuilder.generate(aCase, context.getGuild(), lang, managerKomend,
                                            false, false, finalIssuer, user));
                        }).complete();
            } catch (Exception ignored) {}
        }
        if (aCase.getMessageId() == null) {
            aCase.setIssuerId(context.getSender().getId());
            if (!aCase.getFlagi().contains(OldCase.Flaga.SILENT)) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(),
                        gc.getLanguage(), managerKomend, true, false, finalIssuer, user);
                modLogChannel.sendMessage(embed).queue(m -> {
                    context.reply(successMessage, c -> {});
                    aCase.setMessageId(m.getId());
                    casesDao.save(caseRow);
                }, throwableConsumer);
            }
            context.reply(successMessage);
            casesDao.save(caseRow);
            return true;
        }
        modLogChannel.retrieveMessageById(aCase.getMessageId()).queue(msg -> {
            aCase.setIssuerId(context.getSender().getId());
            msg.editMessage(ModLogBuilder.generate(aCase, context.getGuild(), gc.getLanguage(), managerKomend,
                    true, false, finalIssuer, user))
                    .override(true).queue(m -> {
                context.reply(successMessage, c -> {});
                casesDao.save(caseRow);
            }, throwableConsumer);
        }, error -> {
            aCase.setIssuerId(context.getSender().getId());
            if (!aCase.getFlagi().contains(OldCase.Flaga.SILENT)) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), gc.getLanguage(), managerKomend,
                        true, false, finalIssuer, user);
                modLogChannel.sendMessage(embed).queue(m -> {
                    aCase.setMessageId(m.getId());
                    context.reply(successMessage, c -> {});
                    casesDao.save(caseRow);
                }, throwableConsumer);
            }
        });
        return true;
    }
}
