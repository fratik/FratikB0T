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

package pl.fratik.commands.system;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.StringUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

public class ChooseCommand extends Command {
    private static final Random random = new Random();
    private static final String STRINGARGTYPE = "string";

    public ChooseCommand() {
        name = "choose";
        category = CommandCategory.FUN;
        uzycieDelim = ";";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("opcja1", STRINGARGTYPE);
        hmap.put("opcja2", STRINGARGTYPE);
        hmap.put("[...]", STRINGARGTYPE);
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        aliases = new String[] {"chose", "wybierz", "choisi", "choisis", "losuj", "wylosuj", "losowanie", "pomozwybrac"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Object[] odpowiedzi = Arrays.stream(context.getArgs()).filter(s -> !((String) s).isEmpty()).toArray();
        if (odpowiedzi.length < 2) {
            CommonErrors.usage(context);
            return false;
        }
        String odp = (String) odpowiedzi[random.nextInt(odpowiedzi.length)];
        context.send(context.getTranslated("choose.choosing", "\uD83E\uDD14", StringUtil.escapeMarkdown(odp.trim())));
        return true;
    }
}
