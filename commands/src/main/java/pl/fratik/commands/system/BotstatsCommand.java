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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Statyczne;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.implementation.ManagerModulowImpl;
import pl.fratik.core.util.DurationUtil;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BotstatsCommand extends Command {

    private final ShardManager shardManager;
    private final ManagerModulow managerModulow;

    public BotstatsCommand(ShardManager shardManager, ManagerModulow managerModulow) {
        this.managerModulow = managerModulow;
        name = "botstats";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.EVERYONE;
        this.shardManager = shardManager;
        aliases = new String[] {"cosobocie", "statyfratikb0ta"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        double used = round((double) (total - free) / 1024 / 1024);
        EmbedBuilder eb = context.getBaseEmbed(context.getTranslated("botstats.header"));
        eb.addField(context.getTranslated("botstats.ram"), String.format("%s/%s MB", used,
                round((double) total / 1024 / 1024)), true);
        eb.addField(context.getTranslated("botstats.uptime"),
                DurationUtil.humanReadableFormat(ManagementFactory.getRuntimeMXBean().getUptime(), false), true);
        eb.addField(context.getTranslated("botstats.users"), String.valueOf(shardManager.getUsers().size()), true);
        eb.addField(context.getTranslated("botstats.guilds"), String.valueOf(shardManager.getGuilds().size()), true);
        eb.addField(context.getTranslated("botstats.channels"), String.valueOf(shardManager.getTextChannels().size()), true);
        eb.addField(context.getTranslated("botstats.shard"), context.getEvent().getJDA().getShardInfo().getShardString(), true);
        eb.addField(context.getTranslated("botstats.version"), Statyczne.WERSJA, true);
        eb.addField(context.getTranslated("botstats.jda"), JDAInfo.VERSION, true);
        if (managerModulow.getModules().get("music") != null) {
            try {
                Class<?> klasa;
                try {
                    klasa = ManagerModulowImpl.moduleClassLoader.loadClass("pl.fratik.music.API");
                } catch (Exception e) {
                    klasa = Class.forName("pl.fratik.music.API");
                }
                int pol = (int) klasa.getDeclaredMethod("getConnections").invoke(null);
                eb.addField(context.getTranslated("botstats.audio"), String.valueOf(pol), true);
            } catch (Exception e) {
                // nic XD
            }
        }
        context.send(eb.build());
        return true;
    }

    private static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
