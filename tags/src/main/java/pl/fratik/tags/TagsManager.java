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

package pl.fratik.tags;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.event.CommandSyncEvent;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.tags.entity.Tag;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TagsManager {
    private final TagsDao tagsDao;
    private final NewManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final Cache<Tags> tagsCache;
    private final Logger logger;

    TagsManager(TagsDao tagsDao, NewManagerKomend managerKomend, ShardManager shardManager, Tlumaczenia tlumaczenia, RedisCacheManager redisCacheManager) {
        this.tagsDao = tagsDao;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        logger = LoggerFactory.getLogger(getClass());
        tagsCache = redisCacheManager.new CacheRetriever<Tags>(){}.getCache((int) TimeUnit.HOURS.toSeconds(1));
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(SlashCommandInteractionEvent e) {
        if (!e.isFromGuild()) return;
        @NotNull Tags tagi = Objects.requireNonNull(tagsCache.get(e.getGuild().getId(), tagsDao::get));
        Tag tag = getTagByName(e.getName(), tagi);
        if (tag == null) return;
        e.reply(tag.getContent()).complete();
    }

    @Subscribe
    public void onSync(CommandSyncEvent e) {
        logger.debug("Rozpoczynam synchronizację tagów");
        for (Tags tags : tagsDao.getAll()) {
            Guild guild = shardManager.getGuildById(tags.getId());
            if (guild == null) continue;
            syncGuild(guild.getId().equals(Ustawienia.instance.botGuild) ? e.getSupportGuildCommands() : null, tags, guild);
        }
        logger.debug("Synchronizacja tagów ukończona");
    }

    public void syncGuild(Tags tags, Guild guild) {
        if (guild.getId().equals(Ustawienia.instance.botGuild)) {
            managerKomend.sync();
            return;
        }
        syncGuild(null, tags, guild);
    }

    private void syncGuild(Set<CommandData> existingCommands, Tags tags, Guild guild) {
        Language language = tlumaczenia.getLanguage(guild);
        CommandListUpdateAction action = guild.updateCommands();
        if (existingCommands != null) action = action.addCommands(existingCommands);
        Set<CommandData> commands = new HashSet<>();
        for (Iterator<Tag> iter = tags.getTagi().stream().sorted(Comparator.comparing(t -> t.getName().toLowerCase())).iterator(); iter.hasNext();) {
            Tag tag = iter.next();
            String createdBy = tag.getCreatedBy();
            if (createdBy == null) createdBy = "???";
            else {
                User user = shardManager.retrieveUserById(createdBy)
                        .onErrorMap(ErrorResponse.UNKNOWN_USER::test, x -> null).complete();
                if (user != null) createdBy = user.getAsTag();
            }
            try {
                commands.add(Commands.slash(tag.getName(), tlumaczenia.get(language, "tag.command.description", createdBy)));
            } catch (IllegalArgumentException ex) {
                // ignoruj, nieprawidłowe tagi zostaną wylistowane w /tag list
            }
            if (commands.size() == 100) break;
        }
        action.addCommands(commands).complete();
    }

    @Nullable
    private Tag getTagByName(String name, Tags tags) {
        return tags.getTagi().stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
