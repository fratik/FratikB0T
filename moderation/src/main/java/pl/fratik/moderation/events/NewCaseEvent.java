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

package pl.fratik.moderation.events;

import lombok.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.moderation.entity.Case;

@AllArgsConstructor
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class NewCaseEvent implements CaseEvent {
    private final Case aCase;
    @Getter private final boolean sendDm;
    @Getter private MessageChannel channel;
    @Getter private Language language;

    @Override
    public Case getCase() {
        return aCase;
    }
}
