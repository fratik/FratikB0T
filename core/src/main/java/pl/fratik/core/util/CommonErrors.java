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

package pl.fratik.core.util;

import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import pl.fratik.core.Globals;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class CommonErrors {
    private CommonErrors() {}

    public static void noPermissionUser(CommandContext context) {
        context.send("Nie masz uprawnień do użycia tej komendy!");
    }

    public static void noPermissionBot(CommandContext context, Throwable e) {
        Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(), UserUtil.formatDiscrim(context.getSender()), null, null));
        Sentry.capture(e);
        Sentry.clearContext();
        context.send("Bot nie ma wystarczających uprawnień do użycia tej komendy!");
    }

    public static void noUserFound(CommandContext context, String user) {
        context.send(String.format("Użytkownik %s nie znaleziony!", user));
    }

    public static void devOnly(CommandContext context) {
        context.send("Ta komenda jest nie dla Ciebie!");
    }

    public static void exception(CommandContext context, Throwable err) {
        if (Globals.production) {
            context.send("Wystąpił błąd!");
        } else {
            context.send("Wystąpił błąd! ```\n" + err + "```");
        }
    }

    public static void notANumber(CommandContext context) {
        context.send("Argument musi być prawidłową liczbą!");
    }

    public static void noBanFound(CommandContext context, String arg) {
        context.send(String.format("Nie znaleziono bana %s", arg));
    }

    public static void cooldown(CommandContext context) {
        context.send("Poczekaj chwilę zanim użyjesz ponownie tej komendy!");
    }

    public static void owner(CommandContext context) {
        context.send("Ten użytkownik jest właścicielem serwera!");
    }

    public static void usage(CommandContext context) {
        usage(context.getBaseEmbed(null), context.getTlumaczenia(), context.getLanguage(), context.getPrefix(), context.getCommand(), context.getChannel());
    }

    public static void usage(EmbedBuilder baseEmbed, Tlumaczenia tlumaczenia, Language language, String prefix, Command command, MessageChannel channel) {
        baseEmbed.setDescription(tlumaczenia.get(language, "generic.usage") + "\n" + prefix + command.getName() +
                " " + tlumaczenia.get(language,command.getName().toLowerCase() + ".help.uzycie") + "");
        baseEmbed.addField(tlumaczenia.get(language, "generic.command.desc"),
                tlumaczenia.get(language,command.getName().toLowerCase() + ".help.description"), false);
        try {
            channel.sendMessage(baseEmbed.build()).queue();
        } catch (InsufficientPermissionException e) {
            channel.sendMessage(tlumaczenia.get(language, "generic.usage") + "\n" + prefix + command.getName() +
                    " " + tlumaczenia.get(language,command.getName().toLowerCase() + ".help.uzycie") + "").queue();
        }
    }
}
