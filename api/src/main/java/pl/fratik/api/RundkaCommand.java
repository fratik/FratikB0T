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
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.api.entity.Rundka;
import pl.fratik.api.entity.RundkaDao;
import pl.fratik.api.event.RundkaEndEvent;
import pl.fratik.api.event.RundkaStartEvent;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;

import java.util.LinkedHashMap;

public class RundkaCommand extends Command {
    private final EventBus eventBus;
    private final RundkaDao rundkaDao;

    public RundkaCommand(EventBus eventBus, RundkaDao rundkaDao) {
        this.eventBus = eventBus;
        this.rundkaDao = rundkaDao;
        name = "rundka";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("rundka", "integer");
        hmap.put("voteChannel", "channel");
        hmap.put("talkChannel", "channel");
        uzycieDelim = " ";
        uzycie = new Uzycie(hmap, new boolean[] {true, true, true});
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
    }

    @Getter @Setter private static boolean rundkaOn = false;
    @Getter @Setter private static int numerRundy = 0;

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!rundkaOn && (context.getArgs().length == 0 || context.getArgs()[0] == null)) {
            context.send("Nie podano numeru rundki");
            return false;
        }
        rundkaOn = !rundkaOn;
        Rundka rundka = rundkaDao.get(numerRundy);
        numerRundy = rundkaOn ? (int) context.getArgs()[0] : numerRundy;
        if (rundkaOn) {
            if (rundka == null) rundka = new Rundka(numerRundy, true);
        } else rundka.setTrwa(false);
        if (rundkaOn) {
            TextChannel vch = (TextChannel) context.getArgs()[1];
            TextChannel tch = (TextChannel) context.getArgs()[2];
            context.send("Rundka nr " + (numerRundy == 0 ? context.getArgs()[0] : numerRundy) + " została rozpoczęta!\n" +
                    String.format("Ustawiono <#%s> jako kanał dyskusji administracyjnej/głosów i <#%s> jako kanał dyskusji",
                            vch.getId(), tch.getId()) + " po napisaniu podania!");
            rundka.setVoteChannel(vch.getId());
            rundka.setNormalChannel(tch.getId());
        } else {
            context.send("Rundka nr " + (numerRundy == 0 ? context.getArgs()[0] : numerRundy) + " została zakończona!");
        }
        numerRundy = rundkaOn ? (int) context.getArgs()[0] : 0;
        rundkaDao.save(rundka);
        if (rundkaOn) eventBus.post(new RundkaStartEvent());
        else eventBus.post(new RundkaEndEvent());
        return true;
    }
}
