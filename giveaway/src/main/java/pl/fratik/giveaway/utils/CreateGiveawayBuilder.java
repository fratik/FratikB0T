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

package pl.fratik.giveaway.utils;

import com.google.gson.Gson;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.GiveawayConfig;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.giveaway.listener.GiveawayListener;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class CreateGiveawayBuilder {

    private final Logger logger = LoggerFactory.getLogger(getClass());;

    private final EventWaiter eventWaiter;
    private final Tlumaczenia tlumaczenia;
    private final Language language;
    private final ManagerArgumentow managerArgumentow;
    private final GiveawayListener giveawayListener;

    private GiveawayConfig config = new GiveawayConfig("0");

    String userId;

    TextChannel channel;
    Message lastMessage;

    BlockingQueue<Progress> progress = new LinkedBlockingDeque<>();

    public CreateGiveawayBuilder(String userId, TextChannel channel, EventWaiter eventWaiter, Tlumaczenia tlumaczenia, ManagerArgumentow managerArgumentow, GiveawayListener giveawayListener) {
        this.userId = userId;
        this.channel = channel;
        this.eventWaiter = eventWaiter;
        this.tlumaczenia = tlumaczenia;
        this.language = tlumaczenia.getLanguage(channel.getGuild());
        this.managerArgumentow = managerArgumentow;
        this.giveawayListener = giveawayListener;
        progress.addAll(Arrays.asList(Progress.values()));

        config.setOrganizator(userId);
    }

    public void create() {
        if (lastMessage != null) throw new UnsupportedOperationException("Próbowano 2 raz stworzyć buildera");
        waitForMessage();
    }

    private void waitForMessage() {
        Progress nextStep = progress.poll();
        if (nextStep == null) {
            logger.debug(new Gson().toJson(config));
            giveawayListener.create(config, channel.getGuild());
            return;
        }
        lastMessage = channel.sendMessage(nextStep.getMsg()).complete();
        eventWaiter.waitForEvent(MessageReceivedEvent.class, this::check,
                (MessageReceivedEvent e) -> handle(e, nextStep),
                2, TimeUnit.MINUTES,
                () -> clear(true));
    }

    private boolean check(MessageReceivedEvent e) {
        return e.getChannel().getId().equals(channel.getId()) && e.getAuthor().getId().equals(userId);
    }

    private void handle(MessageReceivedEvent e, Progress nextStep) {
        String msg = e.getMessage().getContentRaw();
        if (msg.isEmpty() || nextStep == null) {
            clear(false);
            return;
        }
        switch (nextStep) {
            case CHANNEL:
                TextChannel txt = (TextChannel) managerArgumentow.getArguments().get("channel").execute(msg, tlumaczenia, language, channel.getGuild());
                if (txt == null) {
                    clear(false);
                    return;
                }
                config.setChannelId(txt.getId());
                break;
            case PRIZE:
                if (msg.length() > 1000) {
                    clear(false);
                    return;
                }
                config.setPrize(msg);
                break;
            case TIME:
                try {
                    DurationUtil.Response durationResp = DurationUtil.parseDuration(msg);
                    config.setEnd(durationResp.getDoKiedy().toEpochMilli());
                } catch (Exception ex) {
                    clear(false);
                    return;
                }
                break;
            case MEMBERS:
                try {
                    int i = Integer.parseInt(msg);
                    if (i <= 0 || i > 50) throw new NumberFormatException();
                    config.setWygranychOsob(i);
                } catch (NumberFormatException ne) {
                    clear(false);
                    return;
                }
        }
        try {
            lastMessage.delete().complete();
        } catch (Exception ignored) { }
        waitForMessage();
    }

    private void clear(boolean time) {
        try {
            if (time) channel.sendMessage("Czas minął").queue();
            else channel.sendMessage("Podałeś zły argument!").queue();
            lastMessage.delete().complete();
        } catch (Exception e) {
            channel.sendMessage("Operacja zakończona.").queue();
        }
    }

    @Getter
    private enum Progress {
        CHANNEL("giveaway.builder.channel"),
        MEMBERS("giveaway.builder.members"),
        PRIZE("giveaway.builder.prize"),
        TIME("giveaway.builder.time");

        private final String msg;

        Progress(String msg) {
            this.msg = msg;
        }

    }

}
