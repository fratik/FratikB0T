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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OsiemBallCommand extends NewCommand {
    private static final Random RANDOM = new Random();

    public OsiemBallCommand() {
        name = "8ball";
        usage = "<pytanie:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String[] odpowiedzi = context.getTranslated("8ball.responses").split(";");
        String odp = odpowiedzi[RANDOM.nextInt(odpowiedzi.length)];
        if (!context.getArguments().get("pytanie").getAsString().endsWith("?")) {
            context.replyEphemeral(context.getTranslated("8ball.not.a.question"));
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("\uD83E\uDD14\uD83E\uDD14\uD83E\uDD14");
        eb.setAuthor(context.getSender().getAsTag(), null, context.getSender().getEffectiveAvatarUrl());

        InteractionHook hook = context.reply(eb.build());

        eb.setTitle(null);
        eb.setDescription(context.getArgumentOr("pytanie", "", OptionMapping::toString));
        eb.addField("Odpowiedź", odp, false);

        hook.editOriginalEmbeds(eb.build()).queueAfter(3, TimeUnit.SECONDS);
    }
}
