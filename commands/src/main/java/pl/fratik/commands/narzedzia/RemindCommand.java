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

package pl.fratik.commands.narzedzia;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.Akcja;
import pl.fratik.core.entity.Schedule;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class RemindCommand extends Command {
    private final ScheduleDao scheduleDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public RemindCommand(ScheduleDao scheduleDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.scheduleDao = scheduleDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "remind";
        category = CommandCategory.UTILITY;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("tekstICzas", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        uzycieDelim = " ";
        allowInDMs = true;
        aliases = new String[] {"remindme", "przypomnij", "todo", "reminder", "przypomnienie", "rappel", "przypomniszmi"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String content;
        if (context.getArgs().length > 0 && context.getArgs()[0] != null)
            content = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 0, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else throw new IllegalStateException("brak argumentów");
        DurationUtil.Response res = DurationUtil.parseDuration(content);
        if (res.getDoKiedy() == null) {
            context.send(context.getTranslated("remind.failed"));
            return false;
        }
        if (res.getTekst().isEmpty()) {
            context.send(context.getTranslated("remind.empty"));
            return false;
        }
        if (res.getTekst().length() > 1000) {
            context.send(context.getTranslated("remind.char.limit"));
            return false;
        }
        scheduleDao.save(scheduleDao.createNew(res.getDoKiedy().toEpochMilli(), context.getSender().getId(),
                Akcja.REMIND, new Schedule.Przypomnienie(context.getSender().getId(), res.getTekst(),
                        Collections.singletonList(context.getMessage().getJumpUrl()))));
        context.send(context.getTranslated("remind.success"));
        return true;
    }

    @SubCommand(name = "list", emptyUsage = true)
    public boolean list(@NotNull CommandContext context) {
        Message msg = context.send(context.getTranslated("generic.loading"));
        List<Schedule> schedules = scheduleDao.getAll();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
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
                    eb.addField(context.getTranslated("remind.list.embed.id"), String.valueOf(sch.getId()),
                            false);
                    return eb;
                }));
            }
        }
        if (pages.isEmpty()) {
            eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" + msg.getId()));
            msg.editMessage(context.getTranslated("remind.list.empty")).queue();
            return false;
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(msg);
        return true;
    }

    @SubCommand(name = "delete")
    public boolean delete(@NotNull CommandContext context) {
        Schedule sch = scheduleDao.get((String) context.getArgs()[0]);
        if (sch == null || !(sch.getContent() instanceof Schedule.Przypomnienie)) {
            context.send(context.getTranslated("remind.delete.not.found"));
            return false;
        }
        if (!((Schedule.Przypomnienie) sch.getContent()).getOsoba().equals(context.getSender().getId())) {
            context.send(context.getTranslated("remind.delete.not.yours"));
            return false;
        }
        scheduleDao.delete(String.valueOf(sch.getId()));
        context.send(context.getTranslated("remind.delete.success"));
        return true;
    }
}
