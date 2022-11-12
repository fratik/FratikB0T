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

package pl.fratik.fratikcoiny.games;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.EventWaiter;

import java.util.HashSet;
import java.util.Set;

public class BlackjackCommand extends NewCommand {
    private final MemberDao memberDao;
    private final EventWaiter eventWaiter;
    private final Set<String> locki = new HashSet<>();

    public BlackjackCommand(MemberDao memberDao, EventWaiter eventWaiter) {
        this.memberDao = memberDao;
        this.eventWaiter = eventWaiter;
        name = "blackjack";
        usage = "<zaklad:int>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        context.replyEphemeral(context.getTranslated("generic.intent.temp.off"));
    }

}
