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
import pl.fratik.commands.util.CustomEmbedManager;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.webhook.WebhookManager;

public class EmbedCommand extends NewCommand {

    private final CustomEmbedManager cem;
    private final WebhookManager webhookManager;

    public EmbedCommand(CustomEmbedManager cem, WebhookManager webhookManager) {
        name = "embed";
        type = CommandType.SUPPORT_SERVER;
        usage = "<code:string>";
        cooldown = 5;
        this.cem = cem;
        this.webhookManager = webhookManager;
    }

    @Override
    public void execute(NewCommandContext context) {
        EmbedBuilder eb = cem.getEmbed(context.getArguments().get("code").getAsString());
        if (eb == null) {
            context.sendMessage(context.getTranslated("embed.notfound"));
            return;
        }
        try {
//            webhookManager.send(WebhookEmbedBuilder.fromJDA(eb.build()).build(), context.getTextChannel(), context.getMember());
            //FIXME z rebase: nie uzywac tu webhook managera, normalnie odpowiedziec w systemie slashow

            //FIXME: Dobrze fratik
            context.sendMessage(eb.build());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("embed.error", e.getMessage()));
        }
    }

}
