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

package pl.fratik.api;

import com.google.common.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import pl.fratik.api.entity.*;
import pl.fratik.api.event.RundkaAnswerVoteEvent;
import pl.fratik.api.event.RundkaNewAnswerEvent;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;

public class TestCommand extends Command {
    private final EventBus eventBus;

    public TestCommand(EventBus eventBus) {
        this.eventBus = eventBus;
        name = "test";
        category = CommandCategory.SYSTEM;
//        permLevel = PermLevel.BOTOWNER;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        try {
            RundkaOdpowiedzFull odp = new RundkaOdpowiedzFull(context.getSender().getId(), 1, "jakies na pewno",
                    Plec.MEZCZYZNA, "fb!conf", "fb!ustawPowitanie/ustawPozegnanie",
                    "fb!usunPowitanie/usunPozegnanie", PermLevel.GADMIN.getNum(),
                    CzestotliwoscPrzebywania.CalyDzien, true, "bo tak",
                    "bo musicie", null, new Oceny());
            eventBus.post(new RundkaNewAnswerEvent(odp));
            context.send("Wysłano fałszywe podanie");
            Thread.sleep(5000);
            odp.getOceny().getTak().add("1");
            odp.getOceny().getTak().add("2");
            eventBus.post(new RundkaAnswerVoteEvent(odp));
            context.send("Dodano dwa fałszywe głosy na tak");
            Thread.sleep(5000);
            odp.getOceny().getNie().add("3");
            odp.getOceny().getNie().add("4");
            eventBus.post(new RundkaAnswerVoteEvent(odp));
            context.send("Dodano dwa fałszywe głosy na nie");
            Thread.sleep(5000);
            odp.getOceny().getNie().remove("3");
            odp.getOceny().getNie().remove("4");
            eventBus.post(new RundkaAnswerVoteEvent(odp));
            context.send("Usunięto dwa fałszywe głosy na nie");
            Thread.sleep(5000);
            odp.getOceny().getTak().remove("1");
            odp.getOceny().getTak().remove("2");
            eventBus.post(new RundkaAnswerVoteEvent(odp));
            context.send("Usunięto dwa fałszywe głosy na tak");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
