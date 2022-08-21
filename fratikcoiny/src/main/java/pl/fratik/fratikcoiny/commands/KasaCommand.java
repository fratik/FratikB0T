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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

public class KasaCommand extends MoneyCommand {

    private final MemberDao memberDao;

    public KasaCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "kasa";
        usage = "[osoba:user]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member m = context.getArgumentOr("osoba", context.getMember(), OptionMapping::getAsMember);
        Emoji e = getFratikCoin(context);
        MemberConfig mc = memberDao.get(m);
        if (m.equals(context.getMember()))
            context.reply(context.getTranslated("kasa.success.self", String.valueOf(mc.getFratikCoiny()), e.getFormatted()));
        else
            context.reply(context.getTranslated("kasa.success.other", m.getUser().getAsTag(),
                    String.valueOf(mc.getFratikCoiny()), e.getFormatted()));
    }
}
