/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GuildUtil;

import java.util.ArrayList;
import java.util.List;

public class UstawieniaCommand extends NewCommand {
    private static final String PRIV_TOGGLE = "PRIV_TOGGLE";
    private static final String LVLUP_MSG_TOGGLE = "LVLUP_MSG_TOGGLE";
    private static final String LVLUP_MSG_DM_TOGGLE = "LVLUP_MSG_DM_TOGGLE";
    private final UserDao userDao;
    private final EventWaiter eventWaiter;

    public UstawieniaCommand(UserDao userDao, EventWaiter eventWaiter) {
        this.userDao = userDao;
        this.eventWaiter = eventWaiter;
        name = "ustawienia";
        allowInDMs = true;
    }

    @SubCommand(name = "uzytkownik")
    public void user(NewCommandContext context) { //todo
        InteractionHook hook = context.defer(true);
        UserConfig userConfig = userDao.get(context.getSender());
        List<Button> buttons = new ArrayList<>();
        if (userConfig.isPrivWlaczone())
            buttons.add(Button.of(ButtonStyle.DANGER, PRIV_TOGGLE, "Wyłącz priv"));
        else
            buttons.add(Button.of(ButtonStyle.SUCCESS, PRIV_TOGGLE, "Włącz priv"));
        if (userConfig.isLvlupMessages())
            buttons.add(Button.of(ButtonStyle.DANGER, LVLUP_MSG_TOGGLE, "Wyłącz wiad. o wyższym poziomie"));
        else
            buttons.add(Button.of(ButtonStyle.SUCCESS, LVLUP_MSG_TOGGLE, "Włącz wiad. o wyższym poziomie"));
        if (userConfig.isLvlUpOnDM())
            buttons.add(Button.of(ButtonStyle.DANGER, LVLUP_MSG_DM_TOGGLE, "Wyłącz wiad. o wyższym poziomie w DM"));
        else
            buttons.add(Button.of(ButtonStyle.SUCCESS, LVLUP_MSG_DM_TOGGLE, "Włącz wiad. o wyższym poziomie w DM"));
        ButtonWaiter bw = new ButtonWaiter(eventWaiter, context, hook.getInteraction(), ButtonWaiter.ResponseType.REPLY_EPHEMERAL);
        bw.setButtonHandler(e -> {
            UserConfig uc = userDao.get(context.getSender());
            switch (e.getComponentId()) {
                case PRIV_TOGGLE: {
                    uc.setPrivWlaczone(!uc.isPrivWlaczone());
                    if (uc.isPrivWlaczone()) e.editMessage("Pomyślnie włączono wiadomości prywatne /priv").queue();
                    else e.editMessage("Pomyślnie wyłączono wiadomości prywatne /priv").queue();
                    break;
                }
                case LVLUP_MSG_TOGGLE: {
                    uc.setLvlupMessages(!uc.isLvlupMessages());
                    if (uc.isLvlupMessages()) e.editMessage("Pomyślnie włączono wiadomości o zdobyciu wyższego poziomu").queue();
                    else e.editMessage("Pomyślnie wyłączono wiadomości o zdobyciu wyższego poziomu").queue();
                    break;
                }
                case LVLUP_MSG_DM_TOGGLE: {
                    uc.setLvlUpOnDM(!uc.isLvlUpOnDM());
                    if (uc.isLvlUpOnDM()) e.editMessage("Pomyślnie włączono wiadomości o zdobyciu wyższego poziomu w DM").queue();
                    else e.editMessage("Pomyślnie wyłączono wiadomości o zdobyciu wyższego poziomu w DM").queue();
                    break;
                }
            }
            userDao.save(uc);
        });
        context.sendMessage(new MessageBuilder("Witamy w nowym, niekompletnym systemie konfiguracji użytkownika. " +
                "Kiedyś to będzie działać dobrze, ale na razie ~~pobaw się tymi paroma guzikami~~ możesz zmienić tylko te ustawienia.")
                .setActionRows(ActionRow.of(buttons)).build());
    }

    @SubCommand(name = "serwer")
    public void guild(NewCommandContext context) {
        if (context.getGuild() == null) {
            context.replyEphemeral(context.getTranslated("ustawienia.no.guild"));
            return;
        }
        context.replyEphemeral(context.getTranslated("ustawienia.dashboard", GuildUtil.getManageLink(context.getGuild())));
    }
}
