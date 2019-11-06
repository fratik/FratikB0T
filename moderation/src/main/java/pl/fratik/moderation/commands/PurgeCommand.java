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

import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.moderation.listeners.LogListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PurgeCommand extends ModerationCommand { //TODO: 1000 wiadomosci dla premium

    private final LogListener logListener;

    @Getter private static HashMap<String, String> znanePurge = new HashMap<>();

    public PurgeCommand(LogListener logListener) {
        this.logListener = logListener;
        name = "purge";
        category = CommandCategory.MODERATION;
        uzycie = new Uzycie("ilość", "integer", true);
        permLevel = PermLevel.MOD;
        permissions.add(Permission.MESSAGE_HISTORY);
        permissions.add(Permission.MESSAGE_MANAGE);
        znanePurge = new HashMap<>(); //NOSONAR
        aliases = new String[] {"usunwiad", "clear", "usunwiadomosci", "usun", "usunwiadomosciztegokanalu", "usuwam", "clearpruge", "czysc", "backspace", "delete"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        logListener.getZnaneAkcje().add(context.getMessage().getId());
        context.getMessage().delete().queue();
        int ilosc = (int) context.getArgs()[0];
        if (ilosc < 2 || ilosc > 100) {
            context.send(context.getTranslated("purge.no.limit"));
            return false;
        }
        Message message = context.getChannel().sendMessage(context.getTranslated("purge.retrieving")).complete();
        CompletableFuture<MessageHistory> historia = context.getChannel().getHistoryBefore(context.getMessage(), ilosc).submit();
        boolean staraWiadomosc = false;
        long dwaTygodnieTemu = (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14) - TimeUtil.DISCORD_EPOCH)
                << TimeUtil.TIMESTAMP_OFFSET;
        List<Message> wiadomosciWszystkie = new ArrayList<>();
        List<Message> wiadomosci = historia.join().getRetrievedHistory();
        for (Message msg : wiadomosci) logListener.pushMessage(msg);
        for (Message msg : wiadomosci) {
            if (wiadomosciWszystkie.size() != ilosc) {
                if (msg.getIdLong() < dwaTygodnieTemu) {
                    staraWiadomosc = true;
                    break;
                }
                wiadomosciWszystkie.add(msg);
            }
        }
        logListener.getZnaneAkcje().add(message.getId());
        if (staraWiadomosc) {
            message.editMessage(context.getTranslated("purge.deleting.too.old", wiadomosciWszystkie.size(),
                    wiadomosci.size() - wiadomosciWszystkie.size())).complete();
            if (wiadomosciWszystkie.isEmpty()) return false;
            else if (wiadomosciWszystkie.size() == 1) {
                wiadomosciWszystkie.get(0).delete().queue();
                return true;
            }
        } else message.editMessage(context.getTranslated("purge.deleting", ilosc)).complete();
        znanePurge.put(context.getGuild().getId(), context.getSender().getId());
        if (wiadomosciWszystkie.isEmpty()) {
            message.editMessage(context.getTranslated("purge.deleting.empty")).complete();
            return false;
        }
        if (wiadomosciWszystkie.size() == 1) {
            wiadomosciWszystkie.get(0).delete().queue();
            return true;
        }
        context.getChannel().deleteMessages(wiadomosciWszystkie).queue();
        logListener.getZnaneAkcje().add(message.getId());
        message.delete().queueAfter(5, TimeUnit.SECONDS);
        return true;
    }
}
