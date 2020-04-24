/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;

import java.util.Calendar;
import java.util.Date;

public class DailyCommand extends CoinCommand {

    public DailyCommand(MemberDao memberDao, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        super(memberDao, guildDao, redisCacheManager);
        name = "daily";
        aliases = new String[] {"dzienna", "dziennazaplata", "zaplatadzienna", "kasazadarmo", "kieszonkowe", "getfc", "wyplata"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildConfig gc = gcCache.get(context.getGuild().getId(), guildDao::get);
        if (gc.getMoneta() == null) gc.setMoneta(new GuildConfig.Moneta(context.getShardManager()));
        GuildConfig.Moneta m = gc.getMoneta();
        MemberConfig mc = memberDao.get(context.getMember());
        Date dailyDate = mc.getDailyDate();
        Date teraz = new Date();
        if (mc.getDailyDate() != null) {
            long dist = dailyDate.getTime() - teraz.getTime();
            if (dist >= 0) {
                context.send(context.getTranslated("daily.cooldown"));
                return false;
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(teraz);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        dailyDate = Date.from(cal.toInstant());
        long kasa = isHoliday() ? mc.getKasa() + 500 : mc.getKasa() + 250;
        if (kasa == Long.MAX_VALUE) {
            context.send(context.getTranslated("daily.too.many.coins"));
            return false;
        }
        String msg = isHoliday() ? "daily.success.holiday" : "daily.success";
        mc.setKasa(kasa);
        mc.setDailyDate(dailyDate);
        memberDao.save(mc);
        context.send(context.getTranslated(msg, m.getShort(context)));
        return true;
    }

    private static Boolean isHoliday() { // mozna to pozniej gdzies przeniesc
        Date teraz = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(teraz);
        if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER && calendar.get(Calendar.DAY_OF_MONTH) == 24) return true;
        // TODO: dodac inne swieta
        return false;
    }
}

