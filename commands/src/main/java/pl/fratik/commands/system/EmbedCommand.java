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

import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import pl.fratik.commands.util.CustomEmbedManager;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.webhook.WebhookManager;

public class EmbedCommand extends Command {

    private final CustomEmbedManager cem;
    private final WebhookManager webhookManager;

    public EmbedCommand(CustomEmbedManager cem, WebhookManager webhookManager) {
        name = "embed";
        uzycieDelim = " ";
        uzycie = new Uzycie("code", "string", false);
        permLevel = PermLevel.GADMIN; // TODO
        cooldown = 5;
        this.cem = cem;
        this.webhookManager = webhookManager;
    }

    @Override
    public boolean execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            context.send(context.getTranslated("embed.info", Ustawienia.instance.botUrl));
            return false;
        }
        EmbedBuilder eb = cem.getEmbed((String) context.getArgs()[0]);
        if (eb == null) {
            context.send(context.getTranslated("embed.notfound"));
            return false;
        }
//        webhookManager.send(WebhookEmbedBuilder.fromJDA(eb.build()).build(), context.getTextChannel(), context.getMember());
        //FIXME z rebase: nie uzywac tu webhook managera, normalnie odpowiedziec w systemie slashow
        return true;
    }

}
