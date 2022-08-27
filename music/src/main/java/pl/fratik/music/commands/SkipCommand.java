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

package pl.fratik.music.commands;

import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.music.entity.RepeatMode;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.util.List;

public class SkipCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final GuildDao guildDao;
    private final Cache<GuildConfig> gcCache;

    public SkipCommand(NowyManagerMuzyki managerMuzyki, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        this.managerMuzyki = managerMuzyki;
        this.guildDao = guildDao;
        name = "skip";
        requireConnection = true;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>() {}.getCache();
    }

    @SubCommand(name = "normal")
    public void normal(@NotNull NewCommandContext context) {
        if (check(context)) return;

        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.getRepeatMode() != RepeatMode.OFF) {
            context.reply(context.getTranslated("skip.on.repeat"));
            return;
        }
        synchronized (context.getGuild()) {
            if (mms.getChannel().getMembers().size() > 4) {
                List<String> skips = mms.getSkips();
                if (skips.contains(context.getSender().getId())) {
                    context.reply(context.getTranslated("skip.already.voted"));
                    return;
                }
                skips.add(context.getSender().getId());
                int total = mms.getChannel().getMembers().size() - 1;
                int size = skips.size();
                if (size < total * 0.4) {
                    context.reply("\uD83D\uDD38 | " + context.getTranslated("skip.votes", size, (int) Math.ceil(total * 0.4)));
                    return;
                }
            } else if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
                context.reply(context.getTranslated("skip.dj"));
                return;
            }
            context.reply(context.getTranslated("skip.success"));
            mms.skip();
        }
    }

    @SubCommand(name = "admin")
    public void admin(@NotNull NewCommandContext context) {
        GuildConfig gc = gcCache.get(context.getGuild().getId(), guildDao::get);
        if ((isDjConfigured(gc) && !hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) ||
            (!isDjConfigured(gc) && context.getMember().hasPermission(context.getMember().getVoiceState().getChannel(), Permission.MANAGE_CHANNEL))) {
            context.reply(context.getTranslated("skip.forced.error"));
            return;
        }
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.getRepeatMode() != RepeatMode.OFF) {
            context.reply(context.getTranslated("skip.on.repeat"));
            return;
        }
        context.reply(context.getTranslated("skip.success.forced"));
        mms.skip();
    }
}
