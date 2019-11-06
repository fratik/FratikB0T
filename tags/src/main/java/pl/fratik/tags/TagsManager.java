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

package pl.fratik.tags;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.Emoji;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.tags.entity.Tag;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class TagsManager {
    private final Cache<String, Tags> tagsCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).build();
    private final TagsDao tagsDao;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;

    TagsManager(TagsDao tagsDao, ManagerKomend managerKomend, ShardManager shardManager, Tlumaczenia tlumaczenia) {
        this.tagsDao = tagsDao;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    public void onMessage(MessageReceivedEvent e) {
        if (!e.isFromType(ChannelType.TEXT)) return;
        @NotNull Tags tagi = Objects.requireNonNull(tagsCache.get(e.getGuild().getId(), tagsDao::get));
        Tag tag = getTagByName(getFirstWord(e.getMessage()), tagi);
        if (tag == null) return;
        if (managerKomend.getRegistered().stream().anyMatch(c -> c.getName().equals(tag.getName()))) return;
        e.getChannel().sendMessage(generateEmbed(tag, tlumaczenia.getLanguage(e.getMember()), e.getGuild())).queue();
        try {
            Emoji reakcja = managerKomend.getReakcja(e.getMessage().getAuthor(), true);
            if (reakcja.isUnicode()) e.getMessage().addReaction(reakcja.getName()).queue();
            else if (shardManager.getEmoteById(reakcja.getIdLong()) != null)
                e.getMessage().addReaction(reakcja).queue();
            else {
                Emote emotka = shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick);
                if (emotka != null) e.getMessage().addReaction(emotka).queue();
            }
        } catch (Exception ignored) {
            /*lul*/
        }
    }

    private MessageEmbed generateEmbed(Tag tag, Language lang, Guild guild) {
        EmbedBuilder eb = new EmbedBuilder();
        if (tag.getCreatedBy() != null) {
            User createdBy = shardManager.getUserById(tag.getCreatedBy());
            if (createdBy == null) createdBy = shardManager.retrieveUserById(tag.getCreatedBy()).complete();
            eb.setFooter(createdBy.getAsTag(), createdBy.getEffectiveAvatarUrl());
            Member mem = guild.getMember(createdBy);
            eb.setColor(mem == null || mem.getColor() == null ? UserUtil.getPrimColor(createdBy) : mem.getColor());
        } else {
            eb.setColor(Color.decode("#bef7c3"));
            eb.addField(tlumaczenia.get(lang, "tag.warning"), tlumaczenia.get(lang, "tag.warning.content",
                    Ustawienia.instance.botUrl), false);
            List<String> prefixes = managerKomend.getPrefixes(guild);
            if (prefixes.isEmpty()) prefixes.add(Ustawienia.instance.prefix);
            eb.setFooter(tlumaczenia.get(lang, "tag.creator.unknown", prefixes.get(0),
                    tag.getName()), null);
        }
        eb.setAuthor(tag.getName());
        eb.setDescription(tag.getContent());
        eb.setImage(CommonUtil.getImageUrl(tag.getContent()));
        return eb.build();
    }

    private String getFirstWord(Message message) {
        String content = message.getContentRaw().toLowerCase();
        for (String prefix : managerKomend.getPrefixes(message.getGuild())) {
            if (!content.startsWith(prefix)) continue;
            return content.replaceFirst(Pattern.quote(prefix), "").split(" ")[0];
        }
        return null;
    }

    @Nullable
    private Tag getTagByName(String name, Tags tags) {
        return tags.getTagi().stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }

    @Subscribe
    private void onDatabaseUpdate(DatabaseUpdateEvent e) {
        if (!(e.getEntity() instanceof Tags)) return;
        tagsCache.invalidate(((Tags) e.getEntity()).getId());
    }
}
