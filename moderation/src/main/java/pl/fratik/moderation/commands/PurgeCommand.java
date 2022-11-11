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

import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.ExpiringHashMap;
import pl.fratik.moderation.listeners.LogListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PurgeCommand extends ModerationCommand {

    private final LogListener logListener;

    @Getter private static HashMap<String, String> znanePurge;

    public PurgeCommand(LogListener logListener) {
        super(false);
        this.logListener = logListener;
        name = "purge";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE);
        usage = "<ilosc:int>";
        znanePurge = new ExpiringHashMap<>(30, TimeUnit.SECONDS); // NOSONAR
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!(context.getChannel() instanceof GuildMessageChannel)) {
            context.replyEphemeral(context.getTranslated("generic.text.only"));
            return false;
        }
        return super.permissionCheck(context);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        int ilosc = context.getArguments().get("ilosc").getAsInt();
        InteractionHook hook = context.defer(true);
        CompletableFuture<MessageHistory> historia = context.getChannel().getHistoryBefore(hook.getInteraction().getIdLong(), ilosc).submit();
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
        if (staraWiadomosc) {
            context.sendMessage(context.getTranslated("purge.deleting.too.old", wiadomosciWszystkie.size(),
                    wiadomosci.size() - wiadomosciWszystkie.size()));
            if (wiadomosciWszystkie.isEmpty()) return;
            else if (wiadomosciWszystkie.size() == 1) {
                wiadomosciWszystkie.get(0).delete().queue();
                return;
            }
        } else if (!wiadomosciWszystkie.isEmpty()) context.sendMessage(context.getTranslated("purge.deleting", ilosc));
        znanePurge.put(context.getGuild().getId(), context.getSender().getId());
        if (wiadomosciWszystkie.isEmpty()) {
            context.sendMessage(context.getTranslated("purge.deleting.empty"));
            return;
        }
        if (wiadomosciWszystkie.size() == 1) {
            wiadomosciWszystkie.get(0).delete().queue();
            return;
        }
        context.getChannel().asGuildMessageChannel().deleteMessages(wiadomosciWszystkie).queue();
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("ilosc")) option.setRequiredRange(2, 100);
    }
}
