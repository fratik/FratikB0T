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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.Uzycie;

public class KasaCommand extends Command {

    private final MemberDao memberDao;

    public KasaCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "kasa";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        aliases = new String[] {"fc", "stan", "konto", "stankonta"};
        uzycie = new Uzycie("czlonek", "member", false);
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member m = context.getMember();
        if (context.getArgs().length > 0 && context.getArgs()[0] != null) {
            m = (Member) context.getArgs()[0];
        }
        MemberConfig mc = memberDao.get(m);
        Emote e = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.fratikCoin);
        if (e == null) throw new IllegalStateException("eMoTkA jEsT nUlL");
        if (m.equals(context.getMember()))
            context.reply(context.getTranslated("kasa.success.self", String.valueOf(mc.getFratikCoiny()), e.getAsMention()));
        else
            context.reply(context.getTranslated("kasa.success.other", m.getUser().getAsTag(),
                    String.valueOf(mc.getFratikCoiny()), e.getAsMention()));
        return true;
    }
}
