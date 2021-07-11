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

package pl.fratik.core.util;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class DynamicEmbedPaginator extends EmbedPaginator {
    private static final ExecutorService mainExecutor = Executors.newFixedThreadPool(4);
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEmbedPaginator.class);

    private final List<FutureTask<EmbedBuilder>> pages;
    private final boolean preload;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(mainExecutor::shutdown));
    }

    private boolean loading;

    public DynamicEmbedPaginator(EventWaiter eventWaiter, List<FutureTask<EmbedBuilder>> pages, User user, Language language, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this(eventWaiter, pages, user, language, tlumaczenia, eventBus, true);
    }

    public DynamicEmbedPaginator(EventWaiter eventWaiter, List<FutureTask<EmbedBuilder>> pages, User user, Language language, Tlumaczenia tlumaczenia, EventBus eventBus, boolean preload) {
        this(eventWaiter, pages, user, language, tlumaczenia, eventBus, preload, 1);
    }

    public DynamicEmbedPaginator(EventWaiter eventWaiter, List<FutureTask<EmbedBuilder>> pages, User user, Language language, Tlumaczenia tlumaczenia, EventBus eventBus, boolean preload, int startPage) {
        super(eventBus, eventWaiter, user.getIdLong(), language, tlumaczenia, startPage);
        this.preload = preload;
        this.pages = pages;
        if (pages.isEmpty()) throw new IllegalArgumentException("brak stron");
        if (this.preload) {
            mainExecutor.submit(() -> {
                LOGGER.debug("Zaczynam pobieranie stron...");
                ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("PageLoader-" +
                        userId + "-" + messageId + "-" + pages.size() + "-pages"));
                pages.forEach(executor::execute);
                while (!pages.stream().allMatch(FutureTask::isDone)) {
                    try {
                        if (ended) {
                            pages.forEach(f -> f.cancel(true));
                            break;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                loaded();
                executor.shutdownNow();
                LOGGER.debug("Gotowe!");
            });
        } else loading = false;
    }


    @Override
    @NotNull
    protected MessageEmbed render(int page) throws LoadingException {
        FutureTask<EmbedBuilder> pageEmbed = pages.get(page - 1);
        EmbedBuilder eb;
        if (!pageEmbed.isDone()) mainExecutor.submit(pageEmbed);
        try {
            if (page == 1) {
                if (pageEmbed.get() == null) throw new IllegalStateException("pEmbed == null");
                eb = new EmbedBuilder(pageEmbed.get().build());
            }
            else {
                EmbedBuilder pEmbed = pageEmbed.get(5, TimeUnit.SECONDS);
                if (pEmbed == null) throw new IllegalStateException("pEmbed == null");
                eb = new EmbedBuilder(pEmbed.build());
            }
        } catch (TimeoutException e) {
            throw new LoadingException(false, true, null);
        } catch (ExecutionException e) {
            throw new LoadingException(page == startPage, false, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LoadingException(false, true, null);
        }
        if (!customFooter) {
            eb.setFooter(String.format("%s/%s", page, pages.size()), null);
            if (loading) eb.setFooter(String.format("%s/%s", page, pages.size())
                    + " ⌛", null);
        }
        else {
            String stopka = Objects.requireNonNull(eb.build().getFooter(),
                    "stopka jest null mimo customFooter").getText();
            if (stopka == null) throw new NullPointerException("tekst stopki jest null mimo customFooter");
            eb.setFooter(String.format(stopka, page, pages.size()), null);
            //noinspection ConstantConditions (ustawiamy ją wyżej)
            stopka = eb.build().getFooter().getText();
            if (loading) //noinspection ConstantConditions (ustawiamy tekst wyżej)
                eb.setFooter(stopka.endsWith(" ⌛") ? stopka : stopka + " ⌛", null);
            else //noinspection ConstantConditions (ustawiamy tekst wyżej)
                eb.setFooter(stopka.endsWith(" ⌛") ? stopka.substring(0, stopka.length() - 2): stopka, null);
        }
        return eb.build();
    }

    @Override
    protected int getPageCount() {
        return pages.size();
    }

    private void loaded() {
        this.loading = false;
        long waitUntil = System.currentTimeMillis() + 5000;
        //noinspection StatementWithEmptyBody
        while (message == null && System.currentTimeMillis() < waitUntil); // czekamy aż będzie wiadomość, max 5s
        try {
            rerender();
        } catch (LoadingException ignored) {
            // nie w tej implementacji
        }
    }
}
