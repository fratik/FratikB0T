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

import lavalink.client.io.LavalinkSocket;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.util.Objects;

public abstract class MusicCommand extends NewCommand {

    @Getter protected boolean requireConnection = false;
    @Setter private static NowyManagerMuzyki managerMuzyki;

    /**
     * @param context Kontekst
     * @return Czy wyjebało error? True - tak, false -nie
     */
    public boolean check(NewCommandContext context) {
        if (managerMuzyki.getLavaClient().getNodes().stream().noneMatch(LavalinkSocket::isAvailable)) {
            context.reply(context.getTranslated("music.nodes.unavailble"));
            return true;
        }

        if (!requireConnection) return false;

        if (context.getMember().getVoiceState() == null || !context.getMember().getVoiceState().inAudioChannel()) {
            context.reply(context.getTranslated("music.notconnected"));
            return true;
        }

        if (context.getGuild().getSelfMember().getVoiceState() == null || !context.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            context.reply(context.getTranslated("music.self.notconnected"));
            return true;
        }

        if (!Objects.equals(context.getMember().getVoiceState().getChannel(),
                context.getGuild().getSelfMember().getVoiceState().getChannel())) {
            context.reply(context.getTranslated("music.different.channels"));
            return true;
        }

        return false;
    }

    protected boolean hasFullDjPerms(Member member, ShardManager shardManager, GuildDao guildDao) {
        return hasFullDjPerms(member, shardManager, guildDao, guildDao.get(member.getGuild()));
    }

    protected boolean hasFullDjPerms(Member member, ShardManager shardManager, GuildDao guildDao, GuildConfig gc) {
        if (!isDjConfigured(gc)) return true;
        else return UserUtil.getPermlevel(member, guildDao, shardManager).getNum() >= PermLevel.MOD
                .getNum() || (isDj(member, gc) && isDjConfigured(gc));
    }

    protected boolean isDj(Member member, GuildDao guildDao) {
        return isDj(member, guildDao.get(member.getGuild()));
    }

    protected boolean isDj(Member member, GuildConfig gc) {
        return gc.getDjRole() != null && !gc.getDjRole().isEmpty() && member.getRoles()
                .contains(member.getGuild().getRoleById(gc.getDjRole()));
    }

    protected boolean isDjConfigured(Guild g, GuildDao guildDao) {
        return isDjConfigured(guildDao.get(g));
    }

    protected boolean isDjConfigured(GuildConfig gc) {
        return gc.getTylkoDjWGoreMozeDodawacPiosenki() != null && gc.getTylkoDjWGoreMozeDodawacPiosenki();
    }

}
