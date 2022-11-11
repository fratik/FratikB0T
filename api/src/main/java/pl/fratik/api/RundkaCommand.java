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

package pl.fratik.api;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.api.entity.Rundka;
import pl.fratik.api.entity.RundkaDao;
import pl.fratik.api.event.RundkaEndEvent;
import pl.fratik.api.event.RundkaStartEvent;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.util.UserUtil;

public class RundkaCommand extends NewCommand {
    private final EventBus eventBus;
    private final RundkaDao rundkaDao;

    public RundkaCommand(EventBus eventBus, RundkaDao rundkaDao) {
        this.eventBus = eventBus;
        this.rundkaDao = rundkaDao;
        name = "rundka";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Getter @Setter private static boolean rundkaOn = false;
    @Getter @Setter private static int numerRundy = 0;

    @SubCommand(name = "rozpocznij", usage = "<numer_rundki:integer> <vote_channel:textchannel> <talk_channel:textchannel>")
    public void start(@NotNull NewCommandContext context) {
        if (rundkaOn) {
            context.replyEphemeral("Rundka już jest aktywna.");
            return;
        }
        rundkaOn = true;
        numerRundy = context.getArguments().get("numer_rundki").getAsInt();
        Rundka rundka = rundkaDao.get(numerRundy);
        if (rundka == null) rundka = new Rundka(numerRundy, true);
        TextChannel vch = context.getArguments().get("vote_channel").getAsChannel().asTextChannel();
        TextChannel tch = context.getArguments().get("talk_channel").getAsChannel().asTextChannel();
        context.reply("Rundka nr " + numerRundy + " została rozpoczęta!\n" +
                String.format("Ustawiono <#%s> jako kanał dyskusji administracyjnej/głosów i <#%s> jako kanał dyskusji",
                        vch.getId(), tch.getId()) + " po napisaniu podania!");
        rundka.setVoteChannel(vch.getId());
        rundka.setNormalChannel(tch.getId());
        rundkaDao.save(rundka);
        eventBus.post(new RundkaStartEvent());
    }

    @SubCommand(name = "zakoncz")
    public void stop(@NotNull NewCommandContext context) {
        rundkaOn = false;
        Rundka rundka = rundkaDao.get(numerRundy);
        rundka.setTrwa(false);
        context.reply("Rundka" + (numerRundy == 0 ? "" : " nr " + numerRundy) + " została zakończona!");
        numerRundy = 0;
        rundkaDao.save(rundka);
        eventBus.post(new RundkaEndEvent());
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("numer_rundki")) {
            option.setMinValue(1);
            option.setMaxValue(Integer.MAX_VALUE);
        }
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return false;
        }
        return true;
    }
}
