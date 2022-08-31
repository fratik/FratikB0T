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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.Akcja;
import pl.fratik.core.entity.Schedule;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.FutureTask;

public class RemindCommand extends NewCommand {
    private final ScheduleDao scheduleDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public RemindCommand(ScheduleDao scheduleDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.scheduleDao = scheduleDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "remind";
        allowInDMs = true;
    }

    @SubCommand(name = "utworz", usage = "<tekst_z_czasem:string>")
    public void create(@NotNull NewCommandContext context) {
        String content = context.getArguments().get("tekst_z_czasem").getAsString();
        DurationUtil.Response res;
        try {
            res = DurationUtil.parseDuration(content);
        } catch (IllegalArgumentException e) {
            context.replyEphemeral(context.getTranslated("remind.max.duration"));
            return;
        }
        if (res.getDoKiedy() == null) {
            context.replyEphemeral(context.getTranslated("remind.failed"));
            return;
        }
        if (res.getTekst().isEmpty()) {
            context.replyEphemeral(context.getTranslated("remind.empty"));
            return;
        }
        if (res.getTekst().length() > 1000) {
            context.replyEphemeral(context.getTranslated("remind.char.limit"));
            return;
        }
        InteractionHook hook = context.defer(false);
        scheduleDao.save(scheduleDao.createNew(res.getDoKiedy().toEpochMilli(), context.getSender().getId(),
                Akcja.REMIND, new Schedule.Przypomnienie(context.getSender().getId(), res.getTekst(),
                        Collections.singletonList(hook.retrieveOriginal().complete().getJumpUrl()))));
        context.sendMessage(context.getTranslated("remind.success"));
    }

    @SubCommand(name = "lista")
    public boolean list(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        List<Schedule> schedules = scheduleDao.getAll();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", context.getLanguage().getLocale());
        for (Schedule sch : schedules) {
            if (!(sch.getContent() instanceof Schedule.Przypomnienie)) continue;
            Schedule.Przypomnienie przyp = ((Schedule.Przypomnienie) sch.getContent());
            if (przyp.getOsoba().equals(context.getSender().getId())) {
                pages.add(new FutureTask<>(() -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(new Color(0x9cdff));
                    String kontent = przyp.getTresc();
                    if (kontent.length() > 1000) kontent = kontent.substring(0, 1000) + "...";
                    eb.addField(context.getTranslated("remind.list.embed.content"), kontent, false);
                    if (sch.getData() - Instant.now().toEpochMilli() > 0) {
                        eb.addField(context.getTranslated("remind.list.embed.remaining"),
                                DurationUtil.humanReadableFormat(sch.getData() - Instant.now().toEpochMilli(),
                                        false), false);
                    } else {
                        eb.addField(context.getTranslated("remind.list.embed.remaining"),
                                context.getTranslated("remind.list.embed.remaining.due"), false);
                    }
                    eb.addField(context.getTranslated("remind.list.embed.remaining.date"),
                            sdf.format(new Date(sch.getData())), false);
                    eb.addField(context.getTranslated("remind.list.embed.id"), String.valueOf(sch.getId()),
                            false);
                    return eb;
                }));
            }
        }
        if (pages.isEmpty()) {
            context.sendMessage(context.getTranslated("remind.list.empty"));
            return false;
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(hook);
        return true;
    }

    @SubCommand(name = "usun", usage = "<id:string>")
    public void delete(@NotNull NewCommandContext context) {
        context.defer(true);
        Schedule sch = scheduleDao.get(context.getArguments().get("id").getAsString());
        if (sch == null || !(sch.getContent() instanceof Schedule.Przypomnienie)) {
            context.reply(context.getTranslated("remind.delete.not.found"));
            return;
        }
        if (!((Schedule.Przypomnienie) sch.getContent()).getOsoba().equals(context.getSender().getId())) {
            context.sendMessage(context.getTranslated("remind.delete.not.yours"));
            return;
        }
        scheduleDao.delete(String.valueOf(sch.getId()));
        context.sendMessage(context.getTranslated("remind.delete.success"));
    }
}
