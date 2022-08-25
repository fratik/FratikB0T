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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;

public class UnbanCommand extends ModerationCommand {
    private final ModLogListener modLogListener;

    public UnbanCommand(ModLogListener modLogListener) {
        super(true);
        this.modLogListener = modLogListener;
        name = "unban";
        usage = "<osoba:user> [powod:string]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String powod = context.getArgumentOr("powod", context.getTranslated("unban.reason.default"), OptionMapping::getAsString);
        User uzytkownik = context.getArguments().get("osoba").getAsUser();
        Guild.Ban ban = context.getGuild().retrieveBan(uzytkownik).onErrorMap(ErrorResponse.UNKNOWN_BAN::test, x -> null).complete();
        if (ban == null) {
            context.replyEphemeral(context.getTranslated("unban.not.banned"));
            return;
        }
        context.defer(false);
        Case aCase = new Case.Builder(context.getGuild(), uzytkownik, Instant.now(), Kara.UNBAN)
                .setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        modLogListener.getKnownCases().put(ModLogListener.generateKey(uzytkownik, context.getGuild()), aCase);
        try {
            context.getGuild().unban(uzytkownik).reason(powod).complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("unban.failed"));
            return;
        }
        context.sendMessage(context.getTranslated("unban.success", UserUtil.formatDiscrim(uzytkownik)));
    }
}
