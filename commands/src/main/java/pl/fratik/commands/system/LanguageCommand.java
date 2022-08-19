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

package pl.fratik.commands.system;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.ArrayList;
import java.util.List;

public class LanguageCommand extends Command {

    private static final String BUTTON_CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private static final String MENU_CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private final EventBus eventBus;
    private final UserDao userDao;
    private final Tlumaczenia tlumaczenia;

    public LanguageCommand(EventBus eventBus, UserDao userDao, Tlumaczenia tlumaczenia) {
        name = "language";
        uzycie = new Uzycie("język", "language");
        category = CommandCategory.BASIC;
        permLevel = PermLevel.EVERYONE;
        aliases = new String[] {"jezyk", "lang"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
        allowInDMs = true;
        this.eventBus = eventBus;
        this.userDao = userDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Override
    public void onRegister() {
        eventBus.register(this);
    }

    @Override
    public void onUnregister() {
        try {
            eventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            //h
        }
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            EmbedBuilder eb = context.getBaseEmbed(context.getTranslated("language.embed.author"));
            ArrayList<String> tekst = new ArrayList<>();
            tekst.add(context.getTranslated("language.embed.header", context.getPrefix(), Ustawienia.instance.translationUrl));
            tekst.add("");
            for (Language l : Language.values()) {
                if (l.equals(Language.DEFAULT)) continue;
                String str = String.format("%s %s", l.getEmoji().toString(), l.getLocalized());
                if (!l.isChecked()) str += "\\*";
                tekst.add(str);
            }
            eb.setDescription(String.join("\n", tekst));
            context.reply(eb.build(),ActionRow.of(
                    Button.primary(BUTTON_CHANGE_LANGUAGE, context.getTranslated("language.change.button"))));
            return true;
        }
        UserConfig uc = userDao.get(context.getSender());
        uc.setLanguage((Language) context.getArgs()[0]);
        userDao.save(uc);
        context.reply(context.getTranslated("language.change.success", ((Language) context.getArgs()[0]).getLocalized()));
        return true;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onButtonClick(ButtonClickEvent e) {
        if (!e.getComponentId().equals(BUTTON_CHANGE_LANGUAGE)) return;
        Language l = tlumaczenia.getLanguage(e.getUser());
        List<SelectOption> options = new ArrayList<>();
        for (Language lang : Language.values()) {
            if (lang.equals(Language.DEFAULT)) continue;
            options.add(SelectOption.of(lang.getLocalized(), lang.name())
                    .withDefault(lang == l)
                    .withEmoji(Emoji.fromUnicode(lang.getEmoji().toString())));
        }
        SelectionMenu menu = SelectionMenu.create(MENU_CHANGE_LANGUAGE)
                .setPlaceholder(tlumaczenia.get(l, "language.change.placeholder"))
                .setRequiredRange(1, 1)
                .addOptions(options)
                .build();
        e.reply(tlumaczenia.get(l, "language.change.text")).setEphemeral(true).addActionRow(menu).queue();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMenuSelect(SelectionMenuEvent e) {
        if (!e.getComponentId().equals(MENU_CHANGE_LANGUAGE)) return;
        if (e.getValues().size() != 1) return;
        Language l;
        try {
            l = Language.valueOf(e.getValues().get(0));
        } catch (IllegalArgumentException ex) {
            return;
        }
        UserConfig userConfig = userDao.get(e.getUser());
        userConfig.setLanguage(l);
        userDao.save(userConfig);
        e.reply(tlumaczenia.get(l, "language.change.success", l.getLocalized())).setEphemeral(true).complete();
    }
}
