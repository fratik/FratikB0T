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

package pl.fratik.core.manager.implementation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.RateLimiter;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.CommandDispatchEvent;
import pl.fratik.core.event.CommandDispatchedEvent;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ManagerKomendImpl implements ManagerKomend, ThreadFactory {
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
    private final Cache<String, RateLimiter> rateLimits = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private final Map<String, Instant> cooldowns = new HashMap<>();
    private final Cache<String, List<String>> prefixCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private final Cache<String, List<String>> disabledCommandsCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private final Cache<String, String> reakcjaSuccessCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private final Cache<String, String> reakcjaFailCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    @Setter
    private static String loadingModule;
    private Map<Runnable, CommandContext> runnables = new ConcurrentHashMap<>();

    public ManagerKomendImpl(ShardManager shardManager, GuildDao guildDao, UserDao userDao, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this.guildDao = guildDao;
        this.userDao = userDao;
        logger = LoggerFactory.getLogger(getClass());
        this.registered = new HashSet<>();
        this.registeredPerModule = new HashMap<>();
        this.commands = new HashMap<>();
        this.executor = new ManagerKomendImplExecutor(32, this);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.tlumaczenia = tlumaczenia;
        this.eventBus = eventBus;
        this.shardManager = shardManager;
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
            if (!tlumaczenia.getLanguages().get(lang).containsKey(command.getName() + ".help.description")) {
                logger.warn("Komenda {} nie zawiera opisu w helpie w języku {}!", command.getName(), lang.getLocalized());
            }
            if (!tlumaczenia.getLanguages().get(lang).containsKey(command.getName() + ".help.uzycie")) {
                logger.warn("Komenda {} nie zawiera użycia w helpie w języku {}!", command.getName(), lang.getLocalized());
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
            }
        }

        if (content.startsWith("<@" + event.getJDA().getSelfUser().getId() + ">")) {
            content = content.trim().substring(("<@" + event.getJDA().getSelfUser().getId() + ">").length()).trim();
            if (content.isEmpty()) {
                if (!direct) prefixReminder(event, prefixes);
                return;
            }
            handleNormal(event, prefixes.get(0), content, direct);
        }
        if (content.startsWith("<@!" + event.getJDA().getSelfUser().getId() + ">")) {
            content = content.trim().substring(("<@!" + event.getJDA().getSelfUser().getId() + ">").length()).trim();
            if (content.isEmpty()) {
                if (!direct) prefixReminder(event, prefixes);
                return;
            }
            handleNormal(event, prefixes.get(0), content, direct);
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

                int cooldown = isOnCooldown(event.getAuthor(), c);
                if (cooldown > 0) {
                    Language l = tlumaczenia.getLanguage(event.getMember());
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

                if (c.getPermLevel().getNum() > plvl.getNum()) {
                    Language l = tlumaczenia.getLanguage(event.getMember());
                    event.getChannel().sendMessage(tlumaczenia.get(l, "generic.permlevel.too.small",
                            UserUtil.getPermlevel(event.getMember(), guildDao, shardManager).getNum(), c.getPermLevel().getNum()))
                            .queue();
                    return;
                }

                String[] argsNotDelimed = new String[parts.length - 1];
                System.arraycopy(parts, 1, argsNotDelimed, 0, argsNotDelimed.length);

                String[] args = !c.getUzycieDelim().equals("") ?
                        String.join(" ", argsNotDelimed).split(c.getUzycieDelim()) :
                        Collections.singletonList(String.join(" ", argsNotDelimed)).toArray(new String[]{});
                CommandContext context;
                try {
                    context = new CommandContext(shardManager, tlumaczenia, c, event, prefix, parts[0], args);
                } catch (ArgsMissingException e) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.decode("#bef7c3"))
                            .setFooter("© " + event.getJDA().getSelfUser().getName(),
                                    event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                                            .replace(".webp", ".png"));
                    CommonErrors.usage(eb, tlumaczenia, tlumaczenia.getLanguage(event.getMember()), prefix, c, event.getChannel());
                    return;
                }

                //noinspection ConstantConditions - nie może być null
                if (!direct && disabledCommandsCache.get(event.getGuild().getId(), id -> guildDao.get(id).getDisabledCommands())
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

                if (GuildUtil.isGbanned(context.getGuild())) {
                    GbanData gdata = GuildUtil.getGbanData(context.getGuild());
                    context.send(context.getTranslated("generic.gban.guild", gdata.getIssuer(), gdata.getReason()));
                    zareaguj(context, false);
                    return;
                }

                Runnable runnable = () -> {
                    logger.info("Użytkownik " + StringUtil.formatDiscrim(event.getAuthor()) + "(" +
                            event.getAuthor().getId() + ") na serwerze " + event.getGuild().getName() + "(" +
                            event.getGuild().getId() + ") wykonał komendę " + c.getName() +
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
                        Sentry.getContext().setUser(new io.sentry.event.User(event.getAuthor().getId(), UserUtil.formatDiscrim(event.getAuthor()), null, null));
                        Sentry.capture(e);
                        Sentry.clearContext();
                        logger.error("Błąd w komendzie:", e);
                        CommonErrors.exception(context, e);
                    }
                };
                runnables.put(runnable, context);
                executor.submit(runnable);
            }
        }
    }

    private void zareaguj(CommandContext context, boolean success) {
        try {
            Emoji reakcja = getReakcja(context.getSender(), success);
            if (reakcja.isUnicode()) context.getMessage().addReaction(reakcja.getName()).queue();
            else if (shardManager.getEmoteById(reakcja.getId()) != null)
                context.getMessage().addReaction(reakcja).queue();
            else {
                Emote zielonyPtak = shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick);
                if (zielonyPtak != null) context.getMessage().addReaction(zielonyPtak).queue();
            }
        } catch (Exception ignored) {
            try {
                Emote zielonyPtak = shardManager.getEmoteById(Ustawienia.instance.emotki.greenTick);
                if (zielonyPtak != null) context.getMessage().addReaction(zielonyPtak).queue();
            } catch (Exception ignored1) {
                //teraz to juz nic
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
        String r = success ? reakcjaSuccessCache.getIfPresent(user.getId()) : reakcjaFailCache.getIfPresent(user.getId());
        if (r == null) {
            UserConfig config = userDao.get(user);
            reakcjaSuccessCache.put(user.getId(), config.getReakcja());
            reakcjaFailCache.put(user.getId(), config.getReakcjaBlad());
            if (success) {
                return Emoji.resolve(config.getReakcja(), shardManager);
            } else {
                return Emoji.resolve(config.getReakcjaBlad(), shardManager);
            }
        } else return Emoji.resolve(r, shardManager);
    }

    @Override
    public List<String> getPrefixes(Guild guild) {
        List<String> p = prefixCache.getIfPresent(guild.getId());
        if (p == null) {
            try {
                GuildConfig config = guildDao.get(guild);

                if (config.getPrefixes() == null) {
                    prefixCache.put(guild.getId(), Collections.emptyList());
                    p = Collections.singletonList(Ustawienia.instance.prefix);
                } else {
                    prefixCache.put(guild.getId(), config.getPrefixes());
                    p = config.getPrefixes();
                }
            } catch (Exception e) {
                p = Collections.singletonList(Ustawienia.instance.prefix);
            }
        }
        if (p.isEmpty()) {
            p = Collections.singletonList(Ustawienia.instance.prefix);
        }
        return p;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        scheduledExecutor.shutdown();
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof GuildConfig) {
            for (String guildId : prefixCache.asMap().keySet()) {
                if (((GuildConfig) event.getEntity()).getGuildId().equals(guildId)) {
                    prefixCache.invalidate(guildId);
                    disabledCommandsCache.invalidate(guildId);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof UserConfig) {
            for (String userId : reakcjaSuccessCache.asMap().keySet()) {
                if (((UserConfig) event.getEntity()).getId().equals(userId)) {
                    reakcjaSuccessCache.invalidate(userId);
                    return;
                }
            }
        }
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

    @Override
    public Thread newThread(@NotNull Runnable r) {
        if (r instanceof ManagerKomendImplExecutor.WorkerXD) {
            if (runnables.containsKey(((ManagerKomendImplExecutor.WorkerXD) r).parent.runnable)) {
                CommandContext ctx = runnables.remove(((ManagerKomendImplExecutor.WorkerXD) r).parent.runnable);
                String tName = ctx.getCommand().getName() + "-" + ctx.getSender().getId() + "-" +
                        (ctx.getEvent().getGuild() != null ? ctx.getGuild().getId() : "direct");
                return new Thread(r, tName);
            }
        }
        return new Thread(r, "ManagerKomendImpl-executor-unknown");
    }

    public static class ManagerKomendImplExecutor extends AbstractExecutorService {

        private final int threadLimit;
        private final ThreadFactory factory;
        private Map<WorkerXD, Thread> thready = new ConcurrentHashMap<>();
        private Queue<JebanyTask> czekajaceTaski = new ConcurrentLinkedQueue<>();
        @Getter private boolean shutdown;

        public ManagerKomendImplExecutor(int threadLimit, ThreadFactory factory) {
            this.threadLimit = threadLimit;
            this.factory = factory;
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new JebanyTask<>(callable);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new JebanyTask<>(runnable, value);
        }

        @Override
        public void shutdown() {
            shutdown = true;
            try {
                awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @NotNull
        @Override
        public List<Runnable> shutdownNow() {
            List<Runnable> anulowane = new ArrayList<>();
            thready.forEach((w, t) -> {
                if (!t.isInterrupted()) {
                    t.interrupt();
                    anulowane.add(w.parent);
                }
            });
            return anulowane;
        }

        @Override
        public boolean isTerminated() {
            return isShutdown() && thready.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
            long prob = TimeUnit.SECONDS.convert(timeout, unit);
            long proba = 0;
            while (!thready.isEmpty()) {
                Thread.sleep(100);
                proba++;
                if (proba >= prob * 10) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void execute(@NotNull Runnable command) {
            if (thready.size() >= threadLimit) {
                czekajaceTaski.add((JebanyTask) command);
                return;
            }
            WorkerXD w = new WorkerXD((JebanyTask) command, this::removeFromQueue);
            Thread t = factory.newThread(w);
            thready.put(w, t);
            t.start();
        }

        private void removeFromQueue(WorkerXD x) {
            thready.remove(x);
            while (thready.size() < threadLimit) {
                Runnable r = czekajaceTaski.poll();
                if (r == null) break;
                execute(r);
            }
        }

        static class WorkerXD implements Runnable {
            private final JebanyTask parent;
            private final Consumer<WorkerXD> callback;

            WorkerXD(JebanyTask r, Consumer<WorkerXD> callback) {
                parent = r;
                this.callback = callback;
            }

            @Override
            public void run() {
                try {
                    parent.run();
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Thread wywalił", e);
                    Sentry.capture(e);
                }
                callback.accept(this);
            }
        }

        static class JebanyTask<T> extends FutureTask<T> {
            public Callable<T> callable;
            public Runnable runnable;

            public JebanyTask(@NotNull Callable<T> callable) {
                super(callable);
                this.callable = callable;
            }

            public JebanyTask(@NotNull Runnable runnable, T result) {
                super(runnable, result);
                this.runnable = runnable;
            }
        }
    }
}
