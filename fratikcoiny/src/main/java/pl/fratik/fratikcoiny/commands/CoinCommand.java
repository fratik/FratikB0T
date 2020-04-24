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

import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;

public abstract class CoinCommand extends Command {
    protected final MemberDao memberDao;
    protected final GuildDao guildDao;
    protected final Cache<GuildConfig> gcCache;

    public CoinCommand(MemberDao memberDao, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        this.memberDao = memberDao;
        this.guildDao = guildDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
        category = CommandCategory.MONEY;
    }

    protected GuildConfig.Moneta resolveMoneta(CommandContext context) {
        GuildConfig gc = gcCache.get(context.getGuild().getId(), guildDao::get);
        if (gc.getMoneta() == null) gc.setMoneta(new GuildConfig.Moneta(context.getShardManager()));
        return gc.getMoneta();
    }
}
