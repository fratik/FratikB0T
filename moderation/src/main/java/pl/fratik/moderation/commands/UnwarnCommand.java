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

import io.sentry.Sentry;
import io.sentry.event.User;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
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

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnwarnCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final CasesDao casesDao;
    private final ShardManager shardManager;
    private final ManagerKomend managerKomend;

    public UnwarnCommand(GuildDao guildDao, CasesDao casesDao, ShardManager shardManager, ManagerKomend managerKomend) {
        this.guildDao = guildDao;
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.managerKomend = managerKomend;
        name = "unwarn";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"usunwarna", "uniwarn", "odwarnuj", "odwajnowywuj", "odwarnowany", "odostrzezenie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("unwarn.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.send(context.getTranslated("unwarn.cant.unwarn.yourself"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.send(context.getTranslated("unwarn.no.bot"));
            return false;
        }
        if (context.getGuild().getMemberById(uzytkownik.getUser().getId()) != null) {
            if (uzytkownik.isOwner()) {
                context.send(context.getTranslated("unwarn.cant.unwarn.owner"));
                return false;
            }
            if (!context.getMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("unwarn.user.cant.interact"));
                return false;
            }
            if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
                context.send(context.getTranslated("unwarn.bot.cant.interact"));
                return false;
            }
        }
        GuildConfig gc = guildDao.get(context.getGuild());
        CaseRow caseRow = casesDao.get(context.getGuild());
        int caseId = Case.getNextCaseId(caseRow);
        List<Case> mcases;
        mcases = caseRow.getCases().stream().filter(c -> c.getUserId().equals(uzytkownik.getId())).collect(Collectors.toList());
        List<Case> warnCases = mcases.stream().filter(c -> c.getType().equals(Kara.WARN)).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType().equals(Kara.UNWARN)).collect(Collectors.toList());
        // powody sie nie liczą, ważne by była dobra liczba
        TextChannel mlog = null;
        if (gc.getModLog() != null && !gc.getModLog().isEmpty()) mlog = shardManager.getTextChannelById(gc.getModLog());
        try {
            if (!unwarnCases.isEmpty()) {
                warnCases.subList(0, unwarnCases.size()).clear();
            }
        } catch (IndexOutOfBoundsException e) {
            context.send(context.getTranslated("unwarn.too.many.unwarns.fixing"));
            try {
                int warns = 0;
                for (Case ignored : warnCases) warns++; //NOSONAR
                for (Case ignored : unwarnCases) warns--; //NOSONAR
                if (warns >= 0) throw new IllegalStateException("outofbounds ale warnów >=0");
                for (int i = 0; i >= warns; i--) {
                    TemporalAccessor timestamp = Instant.now();
                    Case c = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild())
                            .setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.WARN).createCase();
                    c.setIssuerId(context.getGuild().getSelfMember().getUser());
                    c.setReason(context.getTlumaczenia()
                            .get(context.getTlumaczenia().getLanguage(context.getGuild()),
                                    "unwarn.too.many.unwarns.fix.reason"));
                    MessageEmbed embed = ModLogBuilder.generate(c, context.getGuild(), context.getShardManager(),
                            context.getLanguage(), null);
                    if (mlog == null || !context.getGuild().getSelfMember().hasPermission(mlog,
                            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                        caseRow.getCases().add(c);
                    } else {
                        Message message = mlog.sendMessage(embed).complete();
                        c.setMessageId(message.getId());
                        caseRow.getCases().add(c);
                    }
                    caseId++;
                }
            } catch (Exception e1) {
                context.send(context.getTranslated("unwarn.too.many.unwarns.cant.fix"));
                return false;
            }
            context.send(context.getTranslated("unwarn.too.many.unwarns.fixed"));
            mcases = caseRow.getCases().stream().filter(c -> c.getUserId().equals(uzytkownik.getId())).collect(Collectors.toList());
            warnCases = mcases.stream().filter(c -> c.getType().equals(Kara.WARN)).collect(Collectors.toList());
            unwarnCases = mcases.stream().filter(c -> c.getType().equals(Kara.UNWARN)).collect(Collectors.toList());
            warnCases.subList(0, unwarnCases.size()).clear();
        } catch (Exception e) {
            Sentry.getContext().setUser(new User(context.getSender().getId(), context.getSender().getAsTag(), null, null));
            Sentry.getContext().addExtra("warnCases", warnCases);
            Sentry.getContext().addExtra("unwarnCases", unwarnCases);
            Sentry.capture(e);
            Sentry.clearContext();
            String prefix = context.getPrefix();
            context.send(context.getTranslated("unwarn.unexpected.error", prefix, prefix));
            return false;
        }
        if (warnCases.size() <= 0) {
            context.send(context.getTranslated("unwarn.no.warns"));
            return false;
        }
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new CaseBuilder().setUser(uzytkownik.getUser()).setGuild(context.getGuild()).setCaseId(caseId)
                .setTimestamp(timestamp).setMessageId(null).setKara(Kara.UNWARN).createCase();
        aCase.setReason(powod);
        aCase.setIssuerId(context.getSender().getId());
        if (mlog == null || !context.getGuild().getSelfMember().hasPermission(mlog,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
            context.send(context.getTranslated("unwarn.success", UserUtil.formatDiscrim(uzytkownik)));
            context.send(context.getTranslated("unwarn.nomodlogs"));
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
            return true;
        }
        MessageEmbed embed = ModLogBuilder.generate(aCase, context.getGuild(), shardManager,
                gc.getLanguage(), managerKomend);
        mlog.sendMessage(embed).queue(message -> {
            context.send(context.getTranslated("unwarn.success", UserUtil.formatDiscrim(uzytkownik)), m -> {});
            aCase.setMessageId(message.getId());
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        });
        return true;
    }
}
