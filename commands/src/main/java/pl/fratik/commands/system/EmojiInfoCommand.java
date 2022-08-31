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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.CommonUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EmojiInfoCommand extends NewCommand {

    public EmojiInfoCommand() {
        name = "emojiinfo";
        usage = "<emotka:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (context.getArguments().get("emotka").getMentions().getCustomEmojis().size() != 1) {
            context.reply(context.getTranslated("emojiinfo.not.detected"));
            return;
        }
        CustomEmoji em = context.getArguments().get("emotka").getMentions().getCustomEmojis().get(0);
        EmbedBuilder eb = new EmbedBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy',' HH:mm:ss z", context.getLanguage().getLocale());
        String time = sdf.format(new Date(em.getTimeCreated().toInstant().toEpochMilli()));
        eb.setTitle(context.getTranslated("emojiinfo.embed.title", em.getName()));
        eb.addField(context.getTranslated("emojiinfo.embed.emoteid"), em.getId(), false);
        RichCustomEmoji richEm = context.getShardManager().getEmojiById(em.getId());
        Guild g = richEm == null ? null : richEm.getGuild();
        if (g != null) {
            eb.addField(context.getTranslated("emojiinfo.embed.emoteguild.title"),
                    context.getTranslated("emojiinfo.embed.emoteguild.value", g.getName(), g.getId()), false);
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
        context.reply(eb.build());
    }

}
