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
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.Objects;

public class ClassicEmbedPaginator extends EmbedPaginator {
    private final List<EmbedBuilder> pages;

    public ClassicEmbedPaginator(EventWaiter eventWaiter, List<EmbedBuilder> pages, User user, Language language, Tlumaczenia tlumaczenia, EventBus eventBus) {
        super(eventBus, eventWaiter, user.getIdLong(), language, tlumaczenia);
        this.pages = pages;
        if (pages.isEmpty()) throw new IllegalArgumentException("brak stron");
    }

    @Override
    @NotNull
    protected MessageEmbed render(int page) {
        EmbedBuilder pageEmbed = pages.get(page - 1);
        if (!customFooter) pageEmbed.setFooter(String.format("%s/%s", page, pages.size()), null);
        else pageEmbed.setFooter(String.format(Objects.requireNonNull(Objects.requireNonNull(pageEmbed.build().getFooter(),
                "stopka jest null mimo customFooter").getText(), "text jest null mimo customFooter"),
                page, pages.size()), null);
        return pageEmbed.build();
    }

    @Override
    protected int getPageCount() {
        return pages.size();
    }
}
