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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.UserUtil;

public class ZglosCommand extends ModerationCommand {

    private final GuildDao guildDao;

    public ZglosCommand(GuildDao guildDao) {
        super(false);
        this.guildDao = guildDao;
        name = "zglos";
        usage = "<osoba:user> <powod:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        String powod = context.getArguments().get("powod").getAsString();
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.getUser().isBot()) {
            context.replyEphemeral(context.getTranslated("zglos.no.bot"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("zglos.cant.report.yourself"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("zglos.cant.report.owner"));
            return;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("zglos.self.cant.interact"));
            return;
        }
        context.defer(true);
        powod = powod.replaceAll("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
                "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
                "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})", context.getTranslated("generic.link.removed"));
        String tmpAdmk = guildDao.get(context.getGuild()).getKanalAdministracji();
        TextChannel channel = null;
        if (tmpAdmk != null && !tmpAdmk.isEmpty()) channel = context.getGuild().getTextChannelById(tmpAdmk);
        if (tmpAdmk == null || tmpAdmk.isEmpty() || channel == null || !channel.canTalk()) {
            context.sendMessage(context.getTranslated("zglos.invalid.admin.channel"));
            return;
        }
        try {
            channel.sendMessage(context.getTranslated("zglos.admin.message",
                    UserUtil.formatDiscrim(context.getSender()), context.getSender().getId(), powod,
                    UserUtil.formatDiscrim(uzytkownik), uzytkownik.getUser().getId(), uzytkownik.getUser().getId())).complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("zglos.failure"));
            return;
        }
        context.sendMessage(context.getTranslated("zglos.confirmation"));
    }
}
