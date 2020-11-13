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

package pl.fratik.core.manager.implementation;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.RateLimiter;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.CommandDispatchEvent;
import pl.fratik.core.event.CommandDispatchedEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.*;

import java.awt.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ManagerKomendImpl implements ManagerKomend {
    @Getter
    private Set<Command> registered;
    @Getter
    private Map<String, Command> commands;
    @Getter
    private final Map<String, Integer> registeredPerModule;
    private final Logger logger;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final EventBus eventBus;
    private final com.github.benmanes.caffeine.cache.Cache<String, RateLimiter> rateLimits =
            Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private final Map<String, Instant> cooldowns = new HashMap<>();
    private final Cache<GuildConfig> gcCache;
    private final Cache<UserConfig> ucCache;
    @Setter
    private static String loadingModule;

    public ManagerKomendImpl(ShardManager shardManager, GuildDao guildDao, UserDao userDao, Tlumaczenia tlumaczenia,
                             EventBus eventBus, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.userDao = userDao;
        logger = LoggerFactory.getLogger(getClass());
        this.registered = new HashSet<>();
        this.registeredPerModule = new HashMap<>();
        this.commands = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(32);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.tlumaczenia = tlumaczenia;
        this.eventBus = eventBus;
        this.shardManager = shardManager;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
        ucCache = redisCacheManager.new CacheRetriever<UserConfig>(){}.getCache();
        scheduledExecutor.scheduleWithFixedDelay(this::clearCooldowns, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void registerCommand(Command command) {
        if (command == null) return;

        List<String> aliases = Arrays.asList(command.getAliases());

        if (commands.containsKey(command.getName()) || (!aliases.isEmpty() && commands.keySet().containsAll(aliases)))
            throw new IllegalArgumentException(String.format("Alias lub nazwa już zarejestrowana! [%s]", command.getName()));

        for (Method method : command.getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(SubCommand.class) && method.getParameterCount() == 1) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    String name = subCommand.name().isEmpty() ? method.getName() : subCommand.name();
                    command.getSubCommands().put(name.toLowerCase(), method);
                    logger.debug("Zarejestrowano subkomendę: {} -> {}", name, method);
                    for (String alias : subCommand.aliases()) {
                        command.getSubCommands().put(alias.toLowerCase(), method);
                        logger.debug("Zarejestrowano alias: {} -> {} -> {}", alias, name, method);
                    }
                }
            } catch (Exception e) {
                logger.error("Nie udało się zarejestrować subkomendy!", e);
                Sentry.capture(e);
            }
        }

        if (loadingModule != null) registeredPerModule.put(loadingModule, registeredPerModule
                .getOrDefault(loadingModule, 0) + 1);
        command.onRegister();
        registered.add(command);
        commands.put(command.getName(), command);

        aliases.forEach(alias -> commands.put(alias, command));

        for (Language lang : Language.values()) {
            if (lang == Language.DEFAULT) continue;
            Properties props = tlumaczenia.getLanguages().get(lang);
            if (!props.containsKey(command.getName() + ".help.description")) {
                logger.warn("Komenda {} nie zawiera opisu w helpie w języku {}!", command.getName(), lang.getLocalized());
            }
            if (!props.containsKey(command.getName() + ".help.uzycie")) {
                logger.warn("Komenda {} nie zawiera użycia w helpie w języku {}!", command.getName(), lang.getLocalized());
            }
            String langAliases = tlumaczenia.get(lang, command.getName() + ".help.name");
            if (!langAliases.isEmpty()) {
                for (String alias : langAliases.split("\\|")) {
                    if (alias.isEmpty()) continue;
                    alias = alias.toLowerCase();
                    if (!alias.matches("^[^ \\u200b ]+$")) {
                        logger.warn("Alias {} ({}) nie może zostać zarejestrowany dla {}: alias zawiera spacje",
                                alias, lang.getLocalized(), command.getName());
                        continue;
                    }
                    if (commands.containsKey(alias)) {
                        logger.warn("Alias {} ({}) nie może zostać zarejestrowany dla {}: komenda/alias już zarejestrowane",
                                alias, lang.getLocalized(), command.getName());
                        continue;
                    }
                    commands.put(alias, command);
                }
            }
        }

        logger.debug("Zarejestrowano komendę: {} -> {}", command.getName(), command);

        long perms = Globals.permissions;
        EnumSet<Permission> permList = Permission.getPermissions(perms);
        for (Permission perm : command.getPermissions()) {
            if (!permList.contains(perm)) permList.add(perm);
        }
        if (Permission.getRaw(permList) != Globals.permissions) {
            logger.debug("Zmieniam long uprawnień: {} -> {}", perms, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
    }

    @Override
    public void unregisterCommand(Command command) {
        if (command == null) return;
        command.onUnregister();
        commands.values().removeIf(cmd -> command == cmd);
        registered.removeIf(cmd -> command == cmd);
        commands.values().removeIf(cmd -> cmd.getName().equals(command.getName()));
        registered.removeIf(cmd -> cmd.getName().equals(command.getName()));

        if (loadingModule != null) {
            registeredPerModule.put(loadingModule, registeredPerModule.getOrDefault(loadingModule, 0) - 1);
        }

        logger.debug("Wyrejestrowano komendę: {} -> {}", command.getName(), command);
    }

    private void handlePrivate(MessageReceivedEvent event) {
        handlePrefix(event, true);
    }

    private void handleGuild(MessageReceivedEvent event) {
        handlePrefix(event, false);
    }

    private void handlePrefix(MessageReceivedEvent event, boolean direct) {
        String content = event.getMessage().getContentRaw();

        List<String> prefixes = !direct ? getPrefixes(event.getGuild()) : new ArrayList<>();
        if (prefixes.isEmpty()) prefixes.add(Ustawienia.instance.prefix);

        for (String prefix : prefixes) {
            if (content.toLowerCase().startsWith(prefix.toLowerCase())) {
                content = content.trim().substring(prefix.length()).trim();
                handleNormal(event, prefix, content, direct);
                return;
            }
        }

        if (content.startsWith("<@" + event.getJDA().getSelfUser().getId() + ">")) {
            content = content.trim().substring(("<@" + event.getJDA().getSelfUser().getId() + ">").length()).trim();
            if (content.isEmpty()) {
                if (!direct) prefixReminder(event, prefixes);
                return;
            }
            handleNormal(event, prefixes.get(0), content, direct);
            return;
        }
        if (content.startsWith("<@!" + event.getJDA().getSelfUser().getId() + ">")) {
            content = content.trim().substring(("<@!" + event.getJDA().getSelfUser().getId() + ">").length()).trim();
            if (content.isEmpty()) {
                if (!direct) prefixReminder(event, prefixes);
                return;
            }
            handleNormal(event, prefixes.get(0), content, direct);
            return;
        }
        if (!direct && content.toLowerCase().startsWith(Ustawienia.instance.prefix.toLowerCase()) &&
                CommonUtil.isPomoc(shardManager, event.getGuild()) && UserUtil.isStaff(event.getAuthor(), shardManager)) {
            content = content.trim().substring(Ustawienia.instance.prefix.length()).trim();
            handleNormal(event, Ustawienia.instance.prefix, content, false);
        }

    }

    private void prefixReminder(MessageReceivedEvent event, List<String> prefixes) {
        if (!event.getTextChannel().canTalk()) {
            event.getMessage().addReaction("\u274c").queue();
            return;
        }
        if (prefixes.size() == 1) event.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(event.getMember()),
                "generic.prefix.reminder", prefixes.get(0).replaceAll("`", "`\u200b"))).queue();
        else event.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(event.getMember()),
                "generic.prefix.reminder.multiple", prefixes.stream().map(p->p.replaceAll("`",
                        "\u200b`\u200b")).collect(Collectors.joining("\n")))).queue();
    }

    private void handleNormal(MessageReceivedEvent event, String prefix, String content, boolean direct) {
        String[] parts = content.split(" ");
        if (parts.length != 0) {
            Command c = commands.get(parts[0].toLowerCase());
            if (c != null) {
                if (!c.isAllowInDMs() && direct) return;
                if (event.isFromType(ChannelType.TEXT) && !event.getTextChannel().canTalk()) return;
                if (event.isFromGuild() && isRateLimited(event.getGuild())) {
                    logger.debug("Serwer {} jest na ratelimicie!", event.getGuild());
                    return;
                }
                Language l = !direct ? tlumaczenia.getLanguage(event.getMember()) : tlumaczenia.getLanguage(event.getAuthor());

                int cooldown = isOnCooldown(event.getAuthor(), c);
                if (cooldown > 0) {
                    event.getChannel().sendMessage(tlumaczenia.get(l, "generic.cooldown", String.valueOf(cooldown)))
                            .queue();
                    return;
                }

                PermLevel plvl;

                if (!direct) {
                    plvl = UserUtil.getPermlevel(event.getMember(), guildDao, shardManager);
                    if (c.isIgnoreGaPerm() && plvl != PermLevel.BOTOWNER && plvl.getNum() >= PermLevel.GADMIN.getNum()) {
                        plvl = UserUtil.getPermlevel(event.getMember(), guildDao, shardManager, PermLevel.OWNER);
                    }
                } else {
                    plvl = UserUtil.getPermlevel(event.getAuthor(), shardManager);
                }

                PermLevel customPlvl = null;
                if (event.isFromGuild()) {
                    GuildConfig gc = gcCache.get(event.getGuild().getId(), guildDao::get);
                    customPlvl = getPermLevelOverride(c, gc);
                }
                final PermLevel permLevel = customPlvl == null ? c.getPermLevel() : customPlvl;
                if (permLevel.getNum() > plvl.getNum()) {
                    event.getChannel().sendMessage(tlumaczenia.get(l, "generic.permlevel.too.small",
                            plvl.getNum(), permLevel.getNum())).queue();
                    return;
                }

                String[] argsNotDelimed = new String[parts.length - 1];
                System.arraycopy(parts, 1, argsNotDelimed, 0, argsNotDelimed.length);

                String[] args = !c.getUzycieDelim().equals("") ?
                        String.join(" ", argsNotDelimed).split(c.getUzycieDelim()) :
                        Collections.singletonList(String.join(" ", argsNotDelimed)).toArray(new String[]{});
                CommandContext context;
                try {
                    context = new CommandContext(shardManager, tlumaczenia, c, event, prefix, parts[0], args, customPlvl, direct);
                } catch (ArgsMissingException e) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.decode("#bef7c3"))
                            .setFooter("© " + event.getJDA().getSelfUser().getName(),
                                    event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                                            .replace(".webp", ".png"));
                    CommonErrors.usage(eb, tlumaczenia, l, prefix, c, event.getChannel(), customPlvl);
                    return;
                }

                if (!direct && gcCache.get(event.getGuild().getId(), guildDao::get).getDisabledCommands()
                        .contains(c.getName())) {
                    context.send(context.getTranslated("generic.disabled"));
                    zareaguj(context, false);
                    return;
                }

                if (UserUtil.isGbanned(context.getSender())) {
                    GbanData gdata = UserUtil.getGbanData(context.getSender());
                    User issuer;
                    if (gdata.getIssuerId() == null) {
                        issuer = null;
                    } else {
                        issuer = context.getShardManager().retrieveUserById(gdata.getIssuerId()).complete();
                        if (issuer == null)
                            issuer = context.getShardManager().retrieveUserById(gdata.getIssuerId()).complete();
                    }
                    String issuerString = issuer == null ? "N/a???" : UserUtil.formatDiscrim(issuer);
                    if (!issuerString.equals(gdata.getName())) issuerString = context.getTranslated("gbanlist.different.name",
                            issuer == null ? "N/a???" : UserUtil.formatDiscrim(issuer), gdata.getIssuer());
                    context.send(context.getTranslated("generic.gban", issuerString, gdata.getReason()));
                    zareaguj(context, false);
                    return;
                }

                if (!direct && GuildUtil.isGbanned(context.getGuild())) {
                    GbanData gdata = GuildUtil.getGbanData(context.getGuild());
                    context.send(context.getTranslated("generic.gban.guild", gdata.getIssuer(), gdata.getReason()));
                    zareaguj(context, false);
                    return;
                }

                Runnable runnable = () -> {
                    final String name = Thread.currentThread().getName();
                    Thread.currentThread().setName(context.getCommand().getName() + "-" + context.getSender().getId() + "-" +
                            (context.getGuild() != null ? context.getGuild().getId() : "direct"));
                    logger.info("Użytkownik " + StringUtil.formatDiscrim(event.getAuthor()) + "(" +
                            event.getAuthor().getId() + ") " + (!direct ? "na serwerze " + event.getGuild().getName() + "(" +
                            event.getGuild().getId() + ") " : "") + "wykonał komendę " + c.getName() +
                            " (" + String.join(" ", args) + ")");
                    long millis = System.currentTimeMillis();
                    try {
                        CommandDispatchEvent dispatchEvent = new CommandDispatchEvent(context);
                        eventBus.post(dispatchEvent);
                        if (!dispatchEvent.isCancelled()) {
                            setCooldown(event.getAuthor(), c);
                            millis = System.currentTimeMillis();
                            boolean reakcja = c.preExecute(context);
                            eventBus.post(new CommandDispatchedEvent(context, reakcja, System.currentTimeMillis() - millis));
                            zareaguj(context, reakcja);
                        }
                    } catch (SilentExecutionFail e) {
                        eventBus.post(new CommandDispatchedEvent(context, false, System.currentTimeMillis() - millis));
                        //teraz nic nie robimy: nie reagujemy
                    } catch (Exception e) {
                        Sentry.getContext().setUser(new io.sentry.event.User(event.getAuthor().getId(),
                                UserUtil.formatDiscrim(event.getAuthor()), null, null));
                        Sentry.capture(e);
                        Sentry.clearContext();
                        logger.error("Błąd w komendzie:", e);
                        CommonErrors.exception(context, e);
                    }
                    Thread.currentThread().setName(name);
                };
                executor.submit(runnable);
            }
        }
    }

    public static PermLevel getPermLevelOverride(Command c, GuildConfig gc) {
        if (gc.getCmdPermLevelOverrides() == null)
            gc.setCmdPermLevelOverrides(new HashMap<>());
        PermLevel override = gc.getCmdPermLevelOverrides().get(c.getName());
        if (!checkPermLevelOverride(c, gc, override)) return null;
        return override;
    }

    public static boolean checkPermLevelOverride(Command c, GuildConfig gc, PermLevel override) {
        return gc.getCmdPermLevelOverrides().containsKey(c.getName()) && // https://simulator.io/board/N4iB1fbHf1/1
                ((c.isAllowPermLevelChange() && override != PermLevel.EVERYONE) ||
                ((c.isAllowPermLevelChange() && c.isAllowPermLevelEveryone()) && override == PermLevel.EVERYONE));
    }

    private void zareaguj(CommandContext context, boolean success) {
        try {
            Emoji reakcja = getReakcja(context.getSender(), success);
            if (reakcja.isUnicode()) context.getMessage().addReaction(reakcja.getName()).queue();
            else if (shardManager.getEmoteById(reakcja.getId()) != null)
                context.getMessage().addReaction(reakcja).queue(null, a -> {});
            else {
                Emote zielonyPtak = shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick);
                if (zielonyPtak != null) context.getMessage().addReaction(zielonyPtak).queue(null, a -> {});
            }
        } catch (Exception ignored) {
            try {
                Emote zielonyPtak = shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick);
                if (zielonyPtak != null) context.getMessage().addReaction(zielonyPtak).queue(null, a -> {});
            } catch (Exception ignored1) {
                //teraz to juz nic
            }
        }
    }

    private boolean isRateLimited(Guild guild) {
        RateLimiter r = rateLimits.get(guild.getId(), g -> RateLimiter.create(3, 5, TimeUnit.SECONDS));
        if (r == null) return false; //intellij przestań się pluć plz
        return !r.tryAcquire();
    }

    private int isOnCooldown(User user, Command command) {
        if (command.getCooldown() == 0) return 0;
        if (Ustawienia.instance.devs.contains(user.getId())) return 0;
        Instant cooldown = cooldowns.getOrDefault(user.getId() + command.getName(), Instant.now());
        long teraz = Instant.now().toEpochMilli();
        if (cooldown.toEpochMilli() - teraz > 0) {
            return (int) TimeUnit.SECONDS.convert(cooldown.toEpochMilli() - teraz, TimeUnit.MILLISECONDS);
        }
        return 0;
    }


    private void setCooldown(User user, Command command) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, command.getCooldown());
        if (Ustawienia.instance.devs.contains(user.getId())) return;
        cooldowns.put(user.getId() + command.getName(), cal.toInstant());
    }

    @SuppressWarnings("squid:S2864")
    private void clearCooldowns() {
        for (String key : cooldowns.keySet()) {
            Instant instant = cooldowns.get(key);
            long teraz = Instant.now().toEpochMilli();
            if (instant.toEpochMilli() - teraz <= 0)
                cooldowns.remove(key);
        }
    }

    @Override
    public void unregisterAll() {
        for (Command cmd : registered) {
            cmd.onUnregister();
            logger.debug("Wyrejestrowano komendę: {} -> {}", cmd.getName(), cmd);
        }

        registered = new HashSet<>();
        commands = new HashMap<>();
    }

    @Override
    public Emoji getReakcja(User user, boolean success) {
        UserConfig uc = ucCache.get(user.getId(), userDao::get);
        String r = success ? uc.getReakcja() : uc.getReakcjaBlad();
        return Emoji.resolve(r, shardManager);
    }

    @Override
    public List<String> getPrefixes(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        List<String> p = gc.getPrefixes();
        if (p == null || p.isEmpty()) p = Collections.singletonList(Ustawienia.instance.prefix);
        return p;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        scheduledExecutor.shutdown();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannelType() == ChannelType.TEXT) {
            this.handleGuild(event);
        } else if (event.getChannelType() == ChannelType.PRIVATE) {
            this.handlePrivate(event);
        }
    }
}
