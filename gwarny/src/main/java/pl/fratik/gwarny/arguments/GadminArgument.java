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

package pl.fratik.gwarny.arguments;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.arguments.UserArgument;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

public class GadminArgument extends UserArgument {

    public GadminArgument(ShardManager shardManager) {
        super(shardManager);
        name = "gadmin";
    }

    @Override
    public User execute(@NotNull ArgumentContext context) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (!Globals.inFratikDev || fdev == null) return null;
        User u = super.execute(context);
        if (u == null) return null;
        Member mem = fdev.getMember(u);
        if (mem == null) return null;
        if (UserUtil.isGadm(mem, shardManager)) return u;
        return null;
    }

    @Override
    public User execute(String argument, Tlumaczenia tlumaczenia, Language language) {
        Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (!Globals.inFratikDev || fdev == null) return null;
        User u = super.execute(argument, tlumaczenia, language);
        if (u == null) return null;
        Member mem = fdev.getMember(u);
        if (mem == null) return null;
        if (UserUtil.isGadm(mem, shardManager)) return u;
        return null;
    }
}
