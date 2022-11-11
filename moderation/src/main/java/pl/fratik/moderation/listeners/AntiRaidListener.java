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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.implementation.ManagerModulowImpl;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;

import javax.crypto.IllegalBlockSizeException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AntiRaidListener {

    private final GuildDao guildDao;
    private final ShardManager shardManager;
    private final NormalizedLevenshtein l = new NormalizedLevenshtein();
    private final EventBus eventBus;
    private final Timer timerN;
    private final Timer timerE;
    private final Tlumaczenia tlumaczenia;
    private static final Pattern PING_REGEX = Pattern.compile("<@[!&]?([0-9]{17,18})>");
    private final Map<String, List<String>> lastContentsNormal = new HashMap<>();
    private final Map<String, List<String>> lastContentsExtreme = new HashMap<>();

    private final Cache<GuildConfig> gcCache;

    public AntiRaidListener(GuildDao guildDao, ShardManager shardManager, EventBus eventBus, Tlumaczenia tlumaczenia, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.tlumaczenia = tlumaczenia;
        timerN = new Timer("antiraidNormalClean");
        timerN.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastContentsNormal.clear();
            }
        }, 10000, 10000);
        timerE = new Timer("antiraidExtremeClean");
        timerE.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastContentsExtreme.clear();
            }
        }, 10000, 10000);
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent e) {
        if (e.isWebhookMessage() || !e.isFromGuild() || e.getMessage().getMember() == null ||
                e.getAuthor().isBot() || e.getMessage().getType() != MessageType.DEFAULT) return; //todo mes-type thready
        if (antiRaidDisabled(e.getMessage().getGuild())) return;
        if (e.getMember().hasPermission(Permission.MESSAGE_MANAGE) || e.getMember().hasPermission(Permission.MANAGE_SERVER)) return;
        if (getAntiRaidChannels(e.getMessage().getGuild()).contains(e.getChannel().getId())) return;
        if (!CommonUtil.canTalk(e.getChannel())) return;
        if (antiRaidExtreme(e.getMessage().getGuild())) extreme(e.getMessage());
        normal(e.getMessage());
    }

    private void normal(Message e) {
        List<String> lastC = lastContentsNormal.get(e.getChannel().getId() + e.getAuthor().getId());
        if (lastC == null) {
            List<String> arr = new ArrayList<>();
            arr.add(null);
            arr.add(null);
            arr.add(null);
            arr.add(null);
            arr.add(e.getContentRaw());
            lastContentsNormal.put(e.getChannel().getId() + e.getAuthor().getId(), arr);
            lastC = arr;
        } else {
            lastC.remove(0);
            lastC.add(e.getContentRaw());
            lastContentsNormal.put(e.getChannel().getId() + e.getAuthor().getId(), lastC);
        }
        List<Double> procentyRoznicy = new ArrayList<>();
        int pingiNaWiadomosc = 0;
        for (int i = 0; i < lastC.size(); i++) {
            try {
                if (lastC.get(i) == null || lastC.get(i + 1) == null || lastC.get(i).isEmpty() || lastC.get(i + 1).isEmpty())
                    throw new IllegalBlockSizeException("fratik jest za gruby");
            } catch (Exception err) {
                continue;
            }
            procentyRoznicy.add(l.similarity(lastC.get(i), lastC.get(i + 1)));
            Matcher supermarketMatch = PING_REGEX.matcher(lastC.get(i));
            if (supermarketMatch.matches()) pingiNaWiadomosc++;
        }
        double czulosc = getAntiRaidCzulosc(e.getGuild()) / 100d;
        List<Double> proc = procentyRoznicy.stream().filter(v -> v >= czulosc).collect(Collectors.toList());
        if (proc.size() >= 3) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) log(e, lastC, "3 wiadomości o podobieństwie " + proc.stream().map(w -> w * 100 + "%")
                    .collect(Collectors.joining(", ")));
        }
        if (pingiNaWiadomosc >= 3) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) log(e, lastC, "3 wiadomości zawierają ping");
        }
        if (e.getMentions().getMembers().size() >= 5 ||
                (e.getMentions().getRoles().stream().filter(Role::isMentionable).count() == e.getGuild()
                        .getRoles().stream().filter(Role::isMentionable).count() && e.getGuild().getRoles().stream()
                        .anyMatch(Role::isMentionable))) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) log(e, lastC, "5 pingów w wiadomości lub oznaczone wszystkie role oznaczalne");
        }
    }

    private void extreme(Message e) {
        List<String> lastC = lastContentsExtreme.get(e.getChannel().getId() + e.getAuthor().getId());
        if (lastC == null) {
            List<String> arr = new ArrayList<>();
            for (int i = 0; i < 14; i++) {
                arr.add(null);
            }
            arr.add(e.getContentRaw());
            lastContentsExtreme.put(e.getChannel().getId() + e.getAuthor().getId(), arr);
            lastC = arr;
        } else {
            lastC.remove(0);
            lastC.add(e.getContentRaw());
            lastContentsExtreme.put(e.getChannel().getId() + e.getAuthor().getId(), lastC);
        }
        List<Double> procentyRoznicy = new ArrayList<>();
        int pingiNaWiadomosc = 0;
        for (int i = 0; i < lastC.size(); i++) {
            try {
                if (lastC.get(i) == null || lastC.get(i + 1) == null || lastC.get(i).isEmpty() || lastC.get(i + 1).isEmpty())
                    throw new IllegalBlockSizeException("fratik jest za gruby");
            } catch (Exception err) {
                continue;
            }
            procentyRoznicy.add(l.similarity(lastC.get(i), lastC.get(i + 1)));
            Matcher supermarketMatch = PING_REGEX.matcher(lastC.get(i));
            if (supermarketMatch.matches()) pingiNaWiadomosc++;
        }
        double czulosc = getAntiRaidCzulosc(e.getGuild()) / 100d;
        List<Double> proc = procentyRoznicy.stream().filter(v -> v >= czulosc).collect(Collectors.toList());
        if (proc.size() >= 2) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) logExtreme(e, lastC, "2 wiadomości o podobieństwie " + proc.stream().map(w -> w * 100 + "%")
                    .collect(Collectors.joining(", ")));
        }
        if (lastC.stream().filter(c -> c != null && c.length() <= 3).count() >= 3) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) logExtreme(e, lastC, "3 wiadomości o długości mniejszej lub równej 3");
        }
        if (pingiNaWiadomosc >= 2) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) logExtreme(e, lastC, "2 wiadomości zawierają ping");
        }
        if (e.getMentions().getMembers().size() >= 4 ||
                ((e.getMentions().getRoles().stream().filter(Role::isMentionable).count() == e.getGuild()
                        .getRoles().stream().filter(Role::isMentionable).count() || e.getMentions().getRoles()
                        .stream().filter(Role::isMentionable).count() >= 4) && e.getGuild().getRoles().stream()
                        .anyMatch(Role::isMentionable))) {
            boolean success;
            try {
                e.getGuild().ban(e.getAuthor(), 0, TimeUnit.MILLISECONDS).reason("Raid").complete();
                success = true;
            } catch (Exception err) {
                success = false;
            }
            if (success) logExtreme(e, lastC, "4 pingów w wiadomości lub oznaczone wszystkie role oznaczalne");
        }
    }

    private List<String> getAntiRaidChannels(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        if (gc == null) //czyli nigdy
            return Collections.emptyList();
        if (gc.getKanalyGdzieAntiRaidNieDziala() == null) return Collections.emptyList();
        return gc.getKanalyGdzieAntiRaidNieDziala();
    }

    private boolean antiRaidDisabled(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        if (gc == null) //czyli nigdy
            return true;
        if (gc.getAntiRaid() == null) return true;
        return !gc.getAntiRaid();
    }

    private boolean antiRaidExtreme(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        if (gc == null) //czyli nigdy
            return false;
        if (gc.getAntiRaidExtreme() == null) return false;
        return gc.getAntiRaidExtreme();
    }

    private int getAntiRaidCzulosc(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        if (gc == null) //czyli nigdy
            return 50;
        if (gc.getAntiRaidCzulosc() == null) return 50;
        return gc.getAntiRaidCzulosc();
    }

    private void log(Message e, List<String> lastC, String powod) {
        e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "antiraid.notification",
                e.getAuthor().getAsTag(), e.getAuthor().getId())).queue();
        String wiad = "Zbanowano " + e.getAuthor().getAsTag() + " (" + e.getAuthor().getId() + ") na serwerze " +
                e.getGuild().getName() + " (" + e.getGuild().getId() + "): " + powod + ".\nWiadomości:\n";
        List<String> lastCostatnie3 = new ArrayList<>();
        for (String el : lastC) {
            if (el == null || el.isEmpty()) continue;
            lastCostatnie3.add(el);
            if (lastCostatnie3.size() == 3) break;
        }
        String tekst = lastCostatnie3.stream().filter(t -> t != null && t.length() > 0).collect(Collectors.joining("\n"))
                .replaceAll("@", "@\u200b");
        if ((wiad + tekst).length() >= 2000) wiad += "za długie";
        else wiad += tekst;
        try {
            Object logEvent = ManagerModulowImpl.moduleClassLoader.loadClass("pl.fratik.logs.GenericLogEvent")
                    .getDeclaredConstructor(String.class, String.class).newInstance("antiraid", wiad);
            eventBus.post(logEvent);
        } catch (Exception err) {
            // nic
        }

    }

    private void logExtreme(Message e, List<String> lastC, String powod) {
        String wiad = "[AR EXTREME] Zbanowano " + e.getAuthor().getAsTag() + " (" + e.getAuthor().getId() + ") na serwerze " +
                e.getGuild().getName() + " (" + e.getGuild().getId() + "): " + powod + ".\nWiadomości:\n";
        List<String> lastCostatnie3 = new ArrayList<>();
        for (String el : lastC) {
            if (el == null || el.isEmpty()) continue;
            lastCostatnie3.add(el);
            if (lastCostatnie3.size() == 3) break;
        }
        String tekst = lastCostatnie3.stream().filter(t -> t != null && t.length() > 0).collect(Collectors.joining("\n"))
                .replaceAll("@", "@\u200b");
        if ((wiad + tekst).length() >= 2000) wiad += "za długie";
        else wiad += tekst;
        try {
            Object logEvent = Class.forName("pl.fratik.logs.GenericLogEvent").getDeclaredConstructor(String.class,
                    String.class).newInstance("antiraid", wiad);
            eventBus.post(logEvent);
        } catch (Exception err) {
            // nic
        }
    }

    @Subscribe
    public void onDatabaseUpdateEvent(DatabaseUpdateEvent e) {
        if (!(e.getEntity() instanceof GuildConfig)) return;
        gcCache.put(((GuildConfig) e.getEntity()).getGuildId(), (GuildConfig) e.getEntity());
    }

    public void shutdown() {
        timerN.cancel();
        timerE.cancel();
    }
}
