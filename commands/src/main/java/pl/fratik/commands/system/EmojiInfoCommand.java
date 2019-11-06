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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Emoji;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EmojiInfoCommand extends Command {

    public EmojiInfoCommand() {
        name = "emojiinfo";
        category = CommandCategory.BASIC;
        permLevel = PermLevel.EVERYONE;
        aliases = new String[] {"infoemotka", "infoemoji"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        uzycie = new Uzycie("emotka", "emote", true);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Emoji em = (Emoji) context.getArgs()[0];
        if (em.isUnicode()) {
            context.send(context.getTranslated("emojiinfo.unicode"));
            return false;
        }
        EmbedBuilder eb = new EmbedBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", context.getLanguage().getLocale());
        String time = sdf.format(new Date(em.getTimeCreated().toInstant().toEpochMilli()));
        eb.setTitle(context.getTranslated("emojiinfo.embed.title", em.getName()));
        eb.addField(context.getTranslated("emojiinfo.embed.emoteid"), em.getId(), false);
        Guild g = em.getGuild();
        if (g != null) {
            eb.addField(context.getTranslated("emojiinfo.embed.emoteguild.title"),
                    context.getTranslated("emojiinfo.embed.emoteguild.value", em.getGuild().getName(),
                            em.getGuild().getId()), false);
        } else {
            eb.addField(context.getTranslated("emojiinfo.embed.emoteguild.title"),
                    context.getTranslated("emojiinfo.embed.emoteguild.value.empty"), false);
        }
        eb.addField(context.getTranslated("emojiinfo.embed.emoteurl"),
                context.getTranslated("emojiinfo.embed.emoteurl.value", em.getImageUrl()), false);
        eb.addField(context.getTranslated("emojiinfo.embed.createdat"), time, false);
        eb.addField(context.getTranslated("emojiinfo.embed.isanimated"), context.getTranslated(em.isAnimated() ?
                "generic.yes" : "generic.no"), false);
        eb.setThumbnail(em.getImageUrl());
        eb.setColor(CommonUtil.getPrimColorFromImageUrl(em.getImageUrl()));
        context.send(eb.build());
        return true;
    }

}
