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

package pl.fratik.fratikcoiny.commands;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

public abstract class MoneyCommand extends NewCommand {

    @NotNull
    protected Emoji getFratikCoin(NewCommandContext context) {
        Emoji emotkaFc = context.getShardManager().getEmojiById(Ustawienia.instance.emotki.fratikCoin);
        if (emotkaFc == null) throw new IllegalStateException("emotka null");
        return emotkaFc;
    }

    protected boolean checkTooMuch(long fc, NewCommandContext context) {
        if (fc >= Long.MAX_VALUE) {
            if (context != null) context.reply(context.getTranslated("daily.too.many.coins"));
            return true;
        }
        return false;
    }
}
