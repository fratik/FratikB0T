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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;

public class InviteCommand extends NewCommand {

    public InviteCommand() {
        name = "invite";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String invite = generateInviteLink(context.getSender().getJDA().getSelfUser().getId());
        String dc = Ustawienia.instance.botGuildInvite;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(context.getSender().getJDA().getSelfUser()), null, context.getSender().getJDA().getSelfUser().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
        eb.setDescription(context.getTranslated("invite.embed.description"));
        eb.addField(context.getTranslated("invite.addbot"), context.getTranslated("generic.click", invite), true);
        eb.addField(context.getTranslated("invite.joinfdev"), context.getTranslated("generic.click", dc), true);
        context.reply(eb.build());
    }

    private String generateInviteLink(String id) {
        return "https://discord.com/oauth2/authorize?client_id=" +
                id + "&permissions=" +
                Globals.permissions + "&scope=bot+applications.commands";
    }
}
