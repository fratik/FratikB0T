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

package pl.fratik.core.util;

import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import pl.fratik.core.Globals;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class CommonErrors {
    private CommonErrors() {}

    public static void noPermissionBot(CommandContext context, Throwable e) {
        Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(), UserUtil.formatDiscrim(context.getSender()), null, null));
        Sentry.capture(e);
        Sentry.clearContext();
        context.reply("Bot nie ma wystarczających uprawnień do użycia tej komendy!");
    }

    public static void exception(CommandContext context, Throwable err) {
        if (Globals.production) {
            context.reply("Wystąpił błąd!");
        } else {
            context.reply("Wystąpił błąd! ```\n" + err + "```");
        }
    }

    public static void cooldown(CommandContext context) {
        context.reply("Poczekaj chwilę zanim użyjesz ponownie tej komendy!");
    }

    public static void usage(CommandContext context) {
        usage(context.getBaseEmbed(null), context.getTlumaczenia(), context.getLanguage(), context.getPrefix(),
                context.getCommand(), context.getMessageChannel(), context.getCustomPermLevel(), context.getMessage());
    }

    public static void usage(EmbedBuilder baseEmbed, Tlumaczenia tlumaczenia, Language language, String prefix,
                             Command command, MessageChannel channel, PermLevel customPermLevel, Message refMessage) {
        baseEmbed.setDescription(tlumaczenia.get(language, "generic.usage") + "\n" + prefix +
                CommonUtil.resolveName(command, tlumaczenia, language) + " " +
                tlumaczenia.get(language,command.getName().toLowerCase() + ".help.uzycie") + "");
        baseEmbed.addField(tlumaczenia.get(language, "generic.command.desc"),
                tlumaczenia.get(language,command.getName().toLowerCase() + ".help.description"), false);
        String[] aliases = command.getAliases(tlumaczenia);
        if (aliases.length != 0) baseEmbed.addField(tlumaczenia.get(language, "generic.command.aliases"),
                String.join(", ", aliases).toLowerCase(), false);
        
        String eldo = tlumaczenia.get(language,command.getName().toLowerCase() + ".help.extended");
        if (!eldo.isEmpty() && !eldo.equals("!<pusto>!"))
            baseEmbed.addField(tlumaczenia.get(language, "generic.command.extended"), eldo.replaceAll("\\{\\{PREFIX}}", prefix), false);
        PermLevel plvl = customPermLevel == null ? command.getPermLevel() : customPermLevel;
        String plvlval;
        if (customPermLevel == null) plvlval = tlumaczenia.get(language, "generic.command.permlevel.value",
                plvl.getNum(), tlumaczenia.get(language, plvl.getLanguageKey()));
        else plvlval = tlumaczenia.get(language, "generic.command.permlevel.value.overwritten",
                plvl.getNum(), tlumaczenia.get(language, plvl.getLanguageKey()));
        baseEmbed.addField(tlumaczenia.get(language, "generic.command.permlevel"), plvlval, false);
        try {
            channel.sendMessageEmbeds(baseEmbed.build()).reference(refMessage).queue();
        } catch (InsufficientPermissionException e) {
            channel.sendMessage(tlumaczenia.get(language, "generic.usage") + "\n" + prefix + command.getName() +
                    " " + tlumaczenia.get(language,command.getName().toLowerCase() + ".help.uzycie") + "")
                    .reference(refMessage).queue();
        }
    }
}
