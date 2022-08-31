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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.requests.ErrorResponse;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.entity.Dowod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class DowodCommand extends ModerationCommand {

    private final GuildDao guildDao;
    private final CaseDao caseDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public DowodCommand(GuildDao guildDao,
                        CaseDao caseDao,
                        EventWaiter eventWaiter,
                        EventBus eventBus) {
        super(true);
        this.guildDao = guildDao;
        this.caseDao = caseDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "dowod";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS);
    }

    @SubCommand(name = "dodaj", usage = "<numer_sprawy:int> <dowod:string>")
    public void dodaj(NewCommandContext context) {
        context.deferAsync(false);
        String content = context.getArguments().get("dowod").getAsString();
        String caseId;
        try {
            caseId = CaseDao.getId(context.getGuild(), context.getArguments().get("numer_sprawy").getAsLong());
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            context.sendMessage(context.getTranslated("dowod.invalid.case"));
            return;
        }
        Case aCase = caseDao.getLocked(caseId);
        if (aCase == null) {
            context.sendMessage(context.getTranslated("dowod.invalid.case"));
            return;
        }
        try {
            content = content.replace("\u200b", ""); // nie dla zws
            StringBuilder contentBld = new StringBuilder();
            for (String splat : content.split("\n")) { // podwójne linie będą się gryźć z wyświetlaniem tego w DM - wymuszamy pojedyncze
                if (!splat.trim().isEmpty()) { // nie umiałem wpaść na lepszy sposób, \n+ nie wykrywa spacji
                    contentBld.append(splat);
                    contentBld.append('\n');
                }
            }
            content = contentBld.toString();
            if (content.length() > 500) {
                context.sendMessage(context.getTranslated("dowod.char.limits"));
                return;
            }
            aCase.getDowody().add(new Dowod(Dowod.getNextId(aCase.getDowody()), context.getSender().getIdLong(), content.trim()));
            caseDao.save(aCase);
            context.sendMessage(context.getTranslated("dowod.success"));
        } finally {
            caseDao.unlock(aCase);
        }
    }

    @SubCommand(name = "wyswietl", usage = "<numer_sprawy:int>")
    public void wyswietl(NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        Case aCase;
        try {
            aCase = caseDao.get(CaseDao.getId(context.getGuild(), context.getArguments().get("numer_sprawy").getAsLong()));
            if (aCase == null) throw new NullPointerException("e");
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
            context.sendMessage(context.getTranslated("dowod.invalid.case"));
            return;
        }
        for (Dowod dowod : aCase.getDowody()) {
            pages.add(new FutureTask<>(() -> {
                User user = dowod.retrieveAttachedBy(context.getShardManager()).complete();
                return new EmbedBuilder()
                        .setImage(CommonUtil.getImageUrl(dowod.getContent()))
                        .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                        .setColor(UserUtil.getPrimColor(context.getSender()))
                        .setDescription(dowod.getContent())
                        .setFooter("ID: " + aCase.getCaseNumber() + "-" + dowod.getId() + " | %s/%s");
            }));
        }
        if (pages.isEmpty()) {
            context.sendMessage(context.getTranslated("dowod.empty"));
            return;
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(hook);
    }

    @SubCommand(name = "usun", usage = "<numer_sprawy:int> <numer_dowodu:int>")
    public void usun(NewCommandContext context) {
        String caseId;
        try {
            caseId = CaseDao.getId(context.getGuild(), context.getArguments().get("numer_sprawy").getAsLong());
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            context.reply(context.getTranslated("dowod.invalid.case"));
            return;
        }
        Case aCase = caseDao.getLocked(caseId);
        if (aCase == null) {
            context.reply(context.getTranslated("dowod.invalid.case"));
            return;
        }
        try {
            Dowod dowod;
            try {
                dowod = Dowod.getDowodById(context.getArguments().get("numer_dowodu").getAsInt(), aCase.getDowody());
                if (dowod == null) throw new NullPointerException("e");
            } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
                context.reply(context.getTranslated("dowod.invalid.proof.id"));
                return;
            }
            User user = dowod.retrieveAttachedBy(context.getShardManager()).complete();
            Member member = context.getGuild().retrieveMember(user)
                    .onErrorMap(ErrorResponse.UNKNOWN_MEMBER::test, x -> null).complete();
            if (!(member == null || context.getMember().canInteract(member))) {
                context.reply(context.getTranslated("dowod.usun.lower.permlevel"));
                return;
            }
            if (!aCase.getDowody().remove(dowod)) throw new IllegalStateException("nie udało się usunąć!");
            context.reply(context.getTranslated("dowod.usun.success"));
            caseDao.save(aCase);
        } finally {
            caseDao.unlock(aCase);
        }
    }

}
