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

package pl.fratik.moderation.listeners;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.OldCasesDao;

public class PublishListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final OldCasesDao casesDao;
    private final Cache<GuildConfig> gcCache;

    private static final String EMOTE = "\uD83D\uDCE3";

    public PublishListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ShardManager shardManager, OldCasesDao casesDao, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.casesDao = casesDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>() {}.getCache();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onReact(MessageReactionAddEvent e) {
        return; //todo usuń
//        TextChannel kanal;
//        if (e.isFromGuild()) kanal = e.getTextChannel();
//        else kanal = null;
//        if (!e.isFromGuild() || kanal == null || !kanal.isNews() ||
//                !e.getGuild().getSelfMember().hasPermission(kanal, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY) ||
//                !e.getReactionEmote().isEmoji() || // jeżeli nie emotka
//                (e.getReactionEmote().isEmoji() && !e.getReactionEmote().getEmoji().equals(EMOTE))) // jeżeli nie EMOTE
//            return;
//        GuildConfig gc = gcCache.get(e.getGuild().getId(), guildDao::get);
//        if (!gc.isPublikujReakcja()) return;
//        Message message = e.retrieveMessage().complete();
//        if (message.getFlags().contains(Message.MessageFlag.CROSSPOSTED)) return;
//        Member member = e.retrieveMember().complete();
//        if (message.getAuthor().getIdLong() == member.getIdLong()) {
//            if (!(member.hasPermission(Permission.MESSAGE_WRITE) || member.hasPermission(Permission.MESSAGE_MANAGE))) return;
//        } else {
//            if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;
//        }
//        try {
//            Emoji success = managerKomend.getReakcja(member.getUser(), true);
//            message.crosspost().flatMap(m -> {
//                RestAction<Void> ra;
//                if (success.isUnicode()) ra = m.addReaction(success.getName());
//                else ra = m.addReaction(success);
//                return ra.delay(5, TimeUnit.SECONDS).flatMap(v -> m.removeReaction(success));
//            }).complete();
//        } catch (Exception ignored) {}
    }
}
