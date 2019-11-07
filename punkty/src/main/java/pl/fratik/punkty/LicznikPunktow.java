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

package pl.fratik.punkty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.event.LvlupEvent;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.MapUtil;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.TimeUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.entity.PunktyDao;
import pl.fratik.punkty.entity.PunktyRow;

import javax.naming.Reference;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicznikPunktow {
    private static LicznikPunktow instance;
    private final Logger log = LoggerFactory.getLogger(LicznikPunktow.class);
    private final ScheduledExecutorService threadPool;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final PunktyDao punktyDao;
    private final Tlumaczenia tlumaczenia;
    private final EventBus eventBus;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final ArrayList<String> cooldowns = new ArrayList<>();
    private boolean lock;
    private boolean lockedBySetter;
    private static final Pattern URLPATTERN = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
            "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})");
    private final Random random = new Random();
    private static final Cache<String, ConcurrentHashMap<String, Integer>> cache = Caffeine.newBuilder().build();
    private static final Cache<String, Boolean> punktyWlaczoneCache = Caffeine.newBuilder().build();
    LicznikPunktow(GuildDao guildDao, UserDao userDao, PunktyDao punktyDao, ManagerKomend managerKomend, EventBus eventBus, Tlumaczenia tlumaczenia, ShardManager shardManager) {
        this.guildDao = guildDao;
        this.userDao = userDao;
        this.punktyDao = punktyDao;
        this.eventBus = eventBus;
        this.threadPool = Executors.newScheduledThreadPool(4);
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        instance = this; //NOSONAR
        this.tlumaczenia = tlumaczenia;
        threadPool.scheduleWithFixedDelay(this::emptyCache, 5, 5, TimeUnit.MINUTES);
    }

    public static int getPunkty(Member member) {
        ConcurrentHashMap<String, Integer> mapa = LicznikPunktow.cache.getIfPresent(member.getGuild().getId());
        if (mapa == null || mapa.get(member.getUser().getId()) == null) {
            PunktyRow pkt = LicznikPunktow.instance.punktyDao.get(member);
            return pkt.getPunkty();
        }
        return mapa.get(member.getUser().getId());
    }

    private static int getPunkty(String userId, String guildId) {
        ConcurrentHashMap<String, Integer> mapa = LicznikPunktow.cache.getIfPresent(guildId);
        if (mapa == null || mapa.get(userId) == null) {
            PunktyRow pkt = LicznikPunktow.instance.punktyDao.get(userId + "-" + guildId);
            return pkt.getPunkty();
        }
        return mapa.get(userId);
    }

    public static int getLvl(Member member) {
        int punkty = getPunkty(member);
        return (int) Math.floor(0.1 * Math.sqrt((double) punkty * 4));
    }

    public static int calculateLvl(int punkty) {
        return calculateLvl(punkty, 0);
    }

    private static int calculateLvl(int punkty, int przyrost) {
        return (int) Math.floor(0.1 * Math.sqrt((double) (punkty + przyrost) * 4));
    }

    public static Map<String, Integer> getTotalPoints(User user) {
        Map<String, Integer> dbDane = LicznikPunktow.instance.punktyDao.getTotalPoints(user);
        Map<String, Integer> sumaKoncowa = new HashMap<>();
        dbDane.forEach((idS, pkt) -> {
            ConcurrentHashMap<String, Integer> daneZcache = LicznikPunktow.cache.getIfPresent(idS);
            if (daneZcache == null || daneZcache.get(user.getId()) == null) {
                sumaKoncowa.put(idS, pkt);
                return;
            }
            sumaKoncowa.put(idS, daneZcache.get(user.getId()));
        });
        return sumaKoncowa;
    }

    public static Map<String, Integer> getAllUserPunkty() {
        Map<String, Integer> dbDane = MapUtil.sortByValue(LicznikPunktow.instance.punktyDao.getAllUserPunkty());
        Map<String, Integer> fajnal = new HashMap<>();
        dbDane.forEach((id, pkt) -> {
            if (fajnal.size() != 10) fajnal.put(id, pkt);
        });
        return fajnal;
    }

    public static Map<String, Integer> getAllGuildPunkty() {
        Map<String, Integer> dbDane = MapUtil.sortByValue(LicznikPunktow.instance.punktyDao.getAllGuildPunkty());
        Map<String, Integer> fajnal = new HashMap<>();
        dbDane.forEach((id, pkt) -> {
            if (fajnal.size() != 10) fajnal.put(id, pkt);
        });
        return fajnal;
    }

    @Subscribe
    public void onMessage(MessageReceivedEvent event) {
        if (lock) {
            log.debug("Lock włączony, nie podliczam punktów dla {} na {}", event.getAuthor(), event.getGuild());
            return;
        }
        if (event.getAuthor().isBot() || event.getChannel().getType() != ChannelType.TEXT || !event.isFromGuild() ||
                UserUtil.isGbanned(event.getAuthor()) || getCooldown(event.getMember()) ||
                !punktyWlaczone(event.getGuild())) {
            if (UserUtil.isGbanned(event.getAuthor())) log.debug("{} jest zgbanowany, nie liczę punktu",
                    event.getAuthor());
            else if (event.getAuthor().isBot()) log.debug("{} jest botem, nie liczę punktu", event.getAuthor());
            else if (event.getChannel().getType() != ChannelType.TEXT || !event.isFromGuild())
                log.debug("Kanał gdzie {} napisał nie jest kanałem tekstowym, nie liczę punktu", event.getAuthor());
            else if (getCooldown(event.getMember()))
                log.debug("{} ({}) jest na cooldownie!", event.getAuthor(), event.getGuild());
            else if (!punktyWlaczone(event.getGuild()))
                log.debug("Punkty na serwerze {} są wyłączone", event.getGuild());
            return;
        }
        ConcurrentHashMap<String, Integer> mapa = cache.getIfPresent(event.getGuild().getId());
        if (mapa == null) {
            log.debug("Nie znaleziono HashMapy dla {} w cache, biorę z DB", event.getAuthor());
            PunktyRow pkt = punktyDao.get(event.getMember());
            int punkty = pkt.getPunkty();
            ConcurrentHashMap<String, Integer> hmap = new ConcurrentHashMap<>();
            hmap.put(event.getAuthor().getId(), punkty);
            log.debug("Wstawiam do cache: {} -> {} -> {}", event.getGuild(), event.getAuthor(), punkty);
            cache.put(event.getGuild().getId(), hmap);
            mapa = hmap;
        }
        Integer punktyRaw = mapa.get(event.getAuthor().getId());
        int punkty = 0;
        if (punktyRaw == null) {
            log.debug("Nie znaleziono w hashmapie danych dla {}, biorę z DB", event.getAuthor());
            PunktyRow pkt = punktyDao.get(event.getMember());
            punkty = pkt.getPunkty();
            mapa.put(event.getAuthor().getId(), punkty);
            log.debug("Wstawiam do cache: {} -> {} -> {}", event.getGuild(), event.getAuthor(), punkty);
            cache.put(event.getGuild().getId(), mapa);
        }
        if (punktyRaw != null) punkty = punktyRaw;
        int lvlOld = calculateLvl(punkty, 0);
        int przyrost = 1;
        if (!event.getMessage().getAttachments().isEmpty())
            przyrost = getPktFromFileSize(event.getMessage().getAttachments().get(0).getSize());
        Matcher matcher = URLPATTERN.matcher(event.getMessage().getContentRaw());
        if (matcher.find()) {
            String url = matcher.group();
            try {
                String rawHeader;
                if (url.startsWith("http")) rawHeader = NetworkUtil.headRequest(url).header("Content-Length");
                else {
                    log.debug("{} ({}): znaleziono url {}, ignoruje przez brak protokołu", event.getAuthor(), event.getGuild(), url);
                    rawHeader = null;
                }
                if (rawHeader == null) {
                    log.debug("{} ({}): znaleziono url {}, content-length nieznany", event.getAuthor(), event.getGuild(), url);
                } else {
                    log.debug("{} ({}): znaleziono url {}, content-length: {}", event.getAuthor(), event.getGuild(), url, rawHeader);
                    int byteLength = Integer.parseInt(rawHeader);
                    przyrost = getPktFromFileSize(byteLength);
                }
            } catch (IOException e) {
                log.debug("{} ({}): znaleziono url {}, nie udało się połączyć", event.getAuthor(), event.getGuild(), url);
            }
        }
        log.debug("{} na serwerze {} ma {} + {} punktów (lvl {}), zapisuje do cache",
                event.getAuthor(), event.getGuild(), punkty, przyrost, calculateLvl(punkty, przyrost));
        mapa.put(event.getAuthor().getId(), punkty + przyrost);
        cache.put(event.getGuild().getId(), mapa);
        if (lvlOld != calculateLvl(punkty, przyrost))
            eventBus.post(new LvlupEvent(event.getMember(), punkty + przyrost, lvlOld, calculateLvl(punkty, przyrost),
                    event.getChannel()));
        setCooldown(event.getMember(), true);
        threadPool.schedule(() -> setCooldown(event.getMember(), false), 5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean punktyWlaczone(Guild guild) {
        Boolean chuj = punktyWlaczoneCache.get(guild.getId(), id -> guildDao.get(guild).getPunktyWlaczone());
        if (chuj == null) return true;
        return chuj;
    }

    @Subscribe
    private void onDatabaseUpdateEvent(DatabaseUpdateEvent e) {
        if (!(e.getEntity() instanceof GuildConfig)) return;
        punktyWlaczoneCache.invalidate(((GuildConfig) e.getEntity()).getGuildId());
    }

    private int getPktFromFileSize(double sizeInBytes) {
        int[] pkt = new int[0];
        int sizeInMB = (int) Math.floor(sizeInBytes / 1024 / 1024);
        if (sizeInMB <= 1) pkt = new int[] {5, 6, 7};
        if (sizeInMB == 2) pkt = new int[] {6, 7, 8};
        if (sizeInMB == 3) pkt = new int[] {7, 8, 9};
        if (sizeInMB == 4) pkt = new int[] {8, 9, 10};
        if (sizeInMB == 5) pkt = new int[] {9, 10, 11};
        if (sizeInMB == 6) pkt = new int[] {10, 11, 12};
        if (sizeInMB == 7) pkt = new int[] {11, 12, 13};
        if (sizeInMB >= 8) pkt = new int[] {14, 15};
        int i = random.nextInt(pkt.length);
        return pkt[i];
    }

    @Subscribe
    public void handleLvlup(LvlupEvent event) {
        log.debug("{} na serwerze {} osiągnął poziom {}!",
                event.getMember().getUser(), event.getMember().getGuild(), event.getLevel());
        GuildConfig gc = guildDao.get(event.getMember().getGuild());
        UserConfig uc = userDao.get(event.getMember().getUser());
        List<String> prefixesRaw = managerKomend.getPrefixes(event.getMember().getGuild());
        String prefix;
        if (!prefixesRaw.isEmpty()) prefix = prefixesRaw.get(0);
        else prefix = Ustawienia.instance.prefix;
        String rolaStr = gc.getRoleZaPoziomy().get(event.getLevel());
        Role rola;
        if (rolaStr != null) rola = event.getMember().getGuild().getRoleById(rolaStr);
        else rola = null;
        if (rola == null) {

            Language l = tlumaczenia.getLanguage(event.getMember());
            if (uc.isLvlUpOnDM() == false) {
                try {
                    String channelId = gc.getLvlupMessagesCustomChannel();
                    MessageChannel ch = null;
                    if (channelId != null && !channelId.isEmpty()) ch = shardManager.getTextChannelById(channelId);
                    if (ch == null) ch = event.getChannel();
                    if (event.getChannel().equals(ch) && !uc.isLvlupMessages()) return;
                    ch.sendMessage(tlumaczenia.get(l,
                            "generic.lvlup.channel", event.getMember().getUser().getName(), event.getLevel(), prefix))
                            .queue(null, kurwa -> {});
                } catch (Exception e) {
                    //brak permów
                }
                return;
            }
            try {
                event.getMember().getUser().openPrivateChannel().queue(e -> {
                    e.sendMessage(tlumaczenia.get(l, "generic.lvlup.dm",
                            event.getLevel(), event.getMember().getGuild().getName(), prefix)).complete();
                });
                return;
            } catch (ErrorResponseException e) {
                /*lul*/
            }
        }
        try {
            event.getMember().getGuild()
                    .addRoleToMember(event.getMember(), rola)
                    .queue(ignored -> {
                        Language l = tlumaczenia.getLanguage(event.getMember());
                        if (uc.isLvlupMessages()) event.getChannel().sendMessage(tlumaczenia.get(l,
                                "generic.lvlup.withrole", event.getMember().getUser().getName(),
                                rola.getName(), event.getLevel())).queue();
                    }, throwable -> {
                        Language l = tlumaczenia.getLanguage(event.getMember());
                        if (uc.isLvlupMessages()) event.getChannel().sendMessage(tlumaczenia.get(l,
                                "generic.lvlup.withrole.failed", event.getMember().getUser().getName(),
                                rola.getName(), event.getLevel())).queue();
                    });
        } catch (Exception e) {
            Language l = tlumaczenia.getLanguage(event.getMember());
            if (uc.isLvlupMessages()) event.getChannel().sendMessage(tlumaczenia.get(l,
                    "generic.lvlup.withrole.failed", event.getMember().getUser().getName(),
                    rola.getName(), event.getLevel())).queue();
        }
    }

    public synchronized void emptyCache() {
        try {
            long start = System.nanoTime();
            if (lockedBySetter) {
                log.debug("Zostałem zablokowany przez setLocked()!");
                return;
            }
            lock = true;
            HashMap<User, Integer> punktyUzytkownika = new HashMap<>();
            HashMap<Guild, Integer> punktySerwera = new HashMap<>();
            if (cache.asMap().size() == 0) {
                lock = false;
                return;
            }
            log.debug("Zrzucam cache do DB, {} członków do zrzucenia...", cache.asMap().size());
            cache.asMap().forEach((tmpGuild, map) -> {
                Guild guild = shardManager.getGuildById(tmpGuild);
                if (guild == null) return;
                AtomicInteger pktSerwera = new AtomicInteger();
                log.debug("Zrzucam {} danych z serwera {}...", map.size(), guild);
                map.forEach((id, pkt) -> {
                    pktSerwera.addAndGet(pkt);
                    if (guild.getMemberById(id) == null) return;
                    PunktyRow punktyRow = punktyDao.get(id + "-" + guild.getId());
                    punktySerwera.merge(guild, pkt - punktyRow.getPunkty(), Integer::sum);
                    punktyUzytkownika.merge(shardManager.getUserById(id), pkt - punktyRow.getPunkty(), Integer::sum);
                    punktyRow.setPunkty(pkt);
                    punktyDao.save(punktyRow);
                });
            });
            log.debug("Zrzucam informacje o punktach użytkowników: {} użytkownik(ów) do zrzucenia...", punktyUzytkownika.size());
            punktyUzytkownika.forEach((user, pkt) -> {
                PunktyRow punktyRow = punktyDao.get(user);
                punktyRow.setPunkty(punktyRow.getPunkty() + pkt);
                punktyDao.save(punktyRow);
            });
            log.debug("Zrzucam informacje o punktach serwerów: {} serwer(ów) do zrzucenia...", punktySerwera.size());
            punktySerwera.forEach((guild, pkt) -> {
                PunktyRow punktyRow = punktyDao.get(guild);
                punktyRow.setPunkty(punktyRow.getPunkty() + pkt);
                punktyDao.save(punktyRow);
            });
            cache.invalidateAll();
            log.debug("Gotowe! Zajęło {}.", TimeUtil.getDurationBreakdown(
                    TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS), true));
        } catch (Exception e) {
            log.error("Wystąpił błąd przy zrzucaniu punktów", e);
            Sentry.capture(e);
        }
        lock = false;
    }

    private void setCooldown(Member member, boolean set) {
        if (set) cooldowns.add(member.getUser().getId() + "." + member.getGuild().getId());
        else cooldowns.remove(member.getUser().getId() + "." + member.getGuild().getId());
    }

    private boolean getCooldown(Member member) {
        if (member == null) return false;
        return cooldowns.contains(member.getUser().getId() + "." + member.getGuild().getId());
    }

    void shutdown() {
        this.emptyCache();
        threadPool.shutdown();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTo().equals("punkty")) return;
        LoggerFactory.getLogger(getClass()).info("Wiadomość od {}: {}", e.getFrom(), e.getMessage());
        if (e.getMessage().equals("punktyDao-getAllGuildPunkty:")) {
            e.setResponse(punktyDao.getAllGuildPunkty());
            return;
        }
        if (e.getMessage().equals("punktyDao-getAllUserPunkty:")) {
            e.setResponse(punktyDao.getAllUserPunkty());
            return;
        }
        if (e.getMessage().startsWith("punktyDao-getPunkty:")) {
            String id = e.getMessage().replace("punktyDao-getPunkty:", "");
            e.setResponse(punktyDao.get(id).getPunkty());
            return;
        }
        if (e.getMessage().startsWith("Module-getPunkty:")) {
            String[] id = e.getMessage().replace("Module-getPunkty:", "").split("-");
            if (id.length != 2) {
                LoggerFactory.getLogger(getClass()).warn("Wiadomość niezrozumiała!");
                return;
            }
            e.setResponse(getPunkty(id[0], id[1]));
            return;
        }
        LoggerFactory.getLogger(getClass()).warn("Wiadomość niezrozumiała!");
    }

    public void setLock(boolean lock) {
        this.lockedBySetter = lock;
        this.lock = lock;
    }
}
