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

package pl.fratik.fratikcoiny.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

import java.util.Calendar;
import java.util.Date;

public class DailyCommand extends Command {

    private final MemberDao memberDao;

    public DailyCommand(MemberDao memberDao) {
        this.memberDao = memberDao;
        name = "daily";
        category = CommandCategory.MONEY;
        permissions.add(Permission.MESSAGE_EXT_EMOJI);
        aliases = new String[] {"dzienna", "dziennazaplata", "zaplatadzienna", "kasazadarmo", "kieszonkowe", "getfc", "wyplata"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Emote emotkaFc = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.fratikCoin);
        if (emotkaFc == null) throw new IllegalStateException("emotka null");
        MemberConfig mc = memberDao.get(context.getMember());
        Date dailyDate = mc.getDailyDate();
        Date teraz = new Date();
        long dist;
        if (mc.getDailyDate() != null) dist = dailyDate.getTime() - teraz.getTime();
        else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(teraz);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            dailyDate = Date.from(cal.toInstant());
            if (mc.getFratikCoiny() + 250 == Long.MAX_VALUE) {
                context.send(context.getTranslated("daily.too.many.coins"));
                return false;
            }
            mc.setFratikCoiny(mc.getFratikCoiny() + 250);
            mc.setDailyDate(dailyDate);
            memberDao.save(mc);
            context.send(context.getTranslated("daily.success", emotkaFc.getAsMention()));
            return true;
        }
        if (dist >= 0) {
            context.send(context.getTranslated("daily.cooldown"));
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(teraz);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        dailyDate = Date.from(cal.toInstant());
        if (mc.getFratikCoiny() + 250 == Long.MAX_VALUE) {
            context.send(context.getTranslated("daily.too.many.coins"));
            return false;
        }
        mc.setFratikCoiny(mc.getFratikCoiny() + 250);
        mc.setDailyDate(dailyDate);
        memberDao.save(mc);
        context.send(context.getTranslated("daily.success", emotkaFc.getAsMention()));
        return true;
    }
}
