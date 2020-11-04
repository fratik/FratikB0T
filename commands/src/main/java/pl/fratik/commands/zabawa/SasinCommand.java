/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.commands.zabawa;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SasinCommand extends Command {
    public SasinCommand() {
        name = "sasin";
        category = CommandCategory.FUN;
        uzycie = new Uzycie("liczba", "integer", true);
        allowPermLevelChange = false;
        allowInDMs = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            CommonErrors.usage(context);
            return false;
        }
        Integer liczba = ((Integer) context.getArgs()[0]);
        BigDecimal sasiny = new BigDecimal(liczba / 70_000_000d).setScale(9, RoundingMode.HALF_UP);
        String sasinyStr;
        if (sasiny.intValue() == sasiny.doubleValue()) sasinyStr = String.valueOf(sasiny.intValue());
        else sasinyStr = sasiny.toPlainString();
        context.send(context.getTranslated("sasin.result", liczba, sasinyStr));
        return true;
    }
}
