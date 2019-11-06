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

import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.UserUtil;
import pl.fratik.core.entity.Uzycie;
import javax.annotation.Nonnull;

public class PoziomCommand extends Command {

    private final GuildDao guildDao;
    private final ShardManager shardManager;

    public PoziomCommand(GuildDao guildDao, ShardManager shardManager) {
        name = "poziom";
        aliases = new String[] {"permissionlevel", "plevel", "uprawnienia", "permLevel", "poziomwbocie", "niveau", "lvl", "level"};
        category = CommandCategory.SYSTEM;
        uzycie = new Uzycie("osoba", "member");
        this.guildDao = guildDao;
        this.shardManager = shardManager;
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        Member mem = (Member) context.getArgs()[0];
        if (mem == null) {
            PermLevel pmLvl = UserUtil.getPermlevel(context.getMember(), guildDao, shardManager);
            context.send(context.getTranslated("poziom.response", context.getSender().getAsMention(),
                    String.valueOf(pmLvl.getNum()), context.getTranslated(pmLvl.getLanguageKey())));
            return true;
        } else {
            PermLevel pmLvl = UserUtil.getPermlevel(mem, guildDao, shardManager);
            context.send(context.getTranslated("poziom.response.someone", UserUtil.formatDiscrim(mem),
                    String.valueOf(pmLvl.getNum()), context.getTranslated(pmLvl.getLanguageKey())));
            return true;
        }
    }
}
