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

package pl.fratik.fratikcoiny.games;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.fratikcoiny.commands.CoinCommand;
import pl.fratik.fratikcoiny.libs.blackjack.BlackjackResult;
import pl.fratik.fratikcoiny.libs.blackjack.DiscordBlackJack;

import java.util.HashSet;
import java.util.Set;

public class BlackjackCommand extends CoinCommand {
    private final EventWaiter eventWaiter;
    private final Set<String> locki = new HashSet<>();

    public BlackjackCommand(MemberDao memberDao, GuildDao guildDao, RedisCacheManager redisCacheManager, EventWaiter eventWaiter) {
        super(memberDao, guildDao, redisCacheManager);
        this.eventWaiter = eventWaiter;
        name = "blackjack";
        category = CommandCategory.MONEY;
        cooldown = 10;
        uzycie = new Uzycie("zakład", "long", true);
        aliases = new String[] {"bj", "zagrajwblackjack", "zagrajwbj"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (locki.contains(context.getSender().getId())) {
            context.send(context.getTranslated("blackjack.in.progress"));
            return false;
        }
        MemberConfig mc = memberDao.get(context.getMember());
        long zaklad = (Long) context.getArgs()[0];
        if (zaklad == 0 || mc.getKasa() < zaklad) {
            context.send(context.getTranslated("blackjack.no.money", resolveMoneta(context).getShort(context)));
            return false;
        }
        locki.add(context.getSender().getId());
        DiscordBlackJack bj = new DiscordBlackJack(context, mc.getKasa(), eventWaiter, resolveMoneta(context));
        BlackjackResult bjres = bj.startPlay(zaklad);
        mc.setKasa(bjres.getMoney());
        memberDao.save(mc);
        locki.remove(context.getSender().getId());
        return true;
    }

}
