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

package pl.fratik.commands.zabawa;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BoomCommand extends Command {
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private static final Cache<String, Boolean> boomWlaczoneCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private static UserDao userDao;
    private ExecutorService executor;
    private boolean noMore;

    public BoomCommand(EventWaiter eventWaiter, EventBus eventBus, UserDao userDao) {
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        initUserDao(userDao);
        name = "boom";
        category = CommandCategory.FUN;
        cooldown = 60;
        noMore = false;
        aliases = new String[] {"bomba", "llahakbar", "bom", "bum"};
    }

    private static void initUserDao(UserDao ud) {
        BoomCommand.userDao = ud;
    }

    @Override
    public void onRegister() {
        eventBus.register(this);
        executor = Executors.newFixedThreadPool(4);
        BoomRundka.rundki = new ArrayList<>(); // NOSONAR
        noMore = false;
    }

    @Override
    public void onUnregister() {
        try {
            eventBus.unregister(this);
        } catch (Exception e) {
            // nic
        }
        noMore = true;
        for (BoomRundka rundka : BoomRundka.rundki) rundka.end = true;
        long initialBlew = BoomRundka.rundki.stream().filter(r -> r.blew).count();
        long rundki = BoomRundka.rundki.stream().filter(r -> !r.blew).count() - initialBlew;
        logger.info("Oczekiwanie na zakończenie {} rundek boom, powinno to zająć max 35s!", rundki);
        long ostatnioSprawdzone = 0;
        while (BoomRundka.rundki.stream().anyMatch(r -> !r.blew)) {
            try {
                Thread.sleep(500);
                long tmp = BoomRundka.rundki.stream().filter(r -> r.blew).count() - initialBlew;
                if (tmp != ostatnioSprawdzone) logger.info("Zamknięto {}/{} rundek", tmp, rundki);
                ostatnioSprawdzone = tmp;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Wszystkie rundki pozamykane, unloaduje komendę!");
        executor.shutdown();
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (noMore) {
            context.send(context.getTranslated("boom.unloading"));
            return false;
        }
        if (context.getGuild().getMembers().size() >= 1000) {
            context.send(context.getTranslated("boom.too.many.members"));
            return false;
        }
        long online = context.getChannel().getMembers().stream()
                .filter(member -> BoomRundka.filtr(member) && !member.equals(context.getMember())).count();
        if (online < 2) {
            context.send(context.getTranslated("boom.no.people.online", String.valueOf(online)));
            return false;
        }
        if (!BoomRundka.filtr(context.getMember())) {
            context.send(context.getTranslated("boom.off"));
            return false;
        }
        for (BoomRundka rundka : BoomRundka.rundki) {
            if (rundka.context.getGuild().equals(context.getGuild())) {
                context.send(context.getTranslated("boom.in.progress"));
                return false;
            }
        }
        context.send(context.getTranslated("boom.warning"));
        executor.submit(new BoomRundka(context, eventWaiter)::execute);
        return true;
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent e) {
        if (e.getEntity() instanceof UserConfig)
            boomWlaczoneCache.invalidate(((UserConfig) e.getEntity()).getId());
    }

    private static class BoomRundka {

        private final CommandContext context;
        private final List<Member> byli;
        private final EventWaiter eventWaiter;
        private boolean end;
        private boolean blew = false;
        private int runs = 0;
        private int doKiedy;
        private static ArrayList<BoomRundka> rundki;

        private static final Random random = new Random();

        private BoomRundka(CommandContext context, EventWaiter eventWaiter) {
            this.context = context;
            this.eventWaiter = eventWaiter;
            byli = new ArrayList<>();
            doKiedy = random.nextInt(6) + 3;
            rundki.add(this);
        }

        private void execute() {
            List<Member> online;
            if (runs == 0) {
                online = context.getChannel().getMembers().stream().filter(BoomRundka::filtr)
                        .filter(m -> !m.equals(context.getMember())).collect(Collectors.toList());
            } else {
                online = context.getChannel().getMembers().stream().filter(m -> filtr(m)
                        && !byli.contains(m) && !m.equals(context.getMember())).collect(Collectors.toList());
            }
            Collections.shuffle(online);
            if (online.isEmpty() || end) doKiedy = 0;
            if (doKiedy == 0) {
                context.send(String.format(getTranslated(byli.get(byli.size() - 1), "boom.end"),
                        "\uD83D\uDCA5", "\uD83D\uDCA5",
                        byli.get(byli.size() - 1).getAsMention()));
                blew = true;
                rundki.remove(this);
                return;
            }
            Member randomMember = online.get(random.nextInt(online.size()));
            if (runs == 0) context.send(String.format(getTranslated(randomMember, "boom.first.run"),
                    "\uD83D\uDCA3",
                    context.getSender().getAsMention(), randomMember.getUser().getAsMention()));
            else context.send(String.format(getTranslated(randomMember, "boom.next.run"), "\uD83D\uDCA3",
                    byli.get(byli.size() - 1).getAsMention(), randomMember.getUser().getAsMention()));
            MessageWaiter waiter = new MessageWaiter(eventWaiter, context) {
                @Override
                protected boolean checkMessage(MessageReceivedEvent event) {
                    return event.isFromGuild() && event.getTextChannel().equals(context.getChannel())
                            && event.getAuthor().equals(randomMember.getUser()) && event.getMessage().getContentRaw()
                            .equalsIgnoreCase(getTranslated(randomMember, "boom.pass"));
                }
            };
            waiter.setMessageHandler(e -> {
                runs++;
                doKiedy--;
                byli.add(e.getMember());
                execute();
            });
            waiter.setTimeoutHandler(() -> {
                context.send(String.format(getTranslated(randomMember, "boom.end.time"),
                        randomMember.getAsMention()));
                blew = true;
                rundki.remove(this);
            });
            waiter.create();
        }

        private String getTranslated(Member member, String key) {
            return context.getTlumaczenia().get(context.getTlumaczenia().getLanguage(member), key);
        }

        private static boolean filtr(Member member) {
            return member.getOnlineStatus() == OnlineStatus.ONLINE && !member.getUser().isBot() && isBoomWlaczone(member.getUser());
        }

        private static boolean isBoomWlaczone(User user) {
            return Objects.requireNonNull(boomWlaczoneCache.get(user.getId(), u -> userDao.get(u).isBoomWlaczone()));
        }
    }
}
