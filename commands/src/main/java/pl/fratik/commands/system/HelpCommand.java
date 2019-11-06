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

import com.google.common.collect.Ordering;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.util.UserUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    private final ManagerKomend managerKomend;
    private final GuildDao guildDao;
    private final ShardManager shardManager;

    public HelpCommand(ManagerKomend managerKomend, GuildDao guildDao, ShardManager shardManager) {
        this.managerKomend = managerKomend;
        this.guildDao = guildDao;
        this.shardManager = shardManager;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        name = "help";
        type = CommandType.NORMAL;
        uzycie = new Uzycie("kategoria", "category");
        aliases = new String[] {"commands"};
        category = CommandCategory.BASIC;
        permLevel = PermLevel.EVERYONE;
        allowInDMs = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed();
        TreeMap<CommandCategory, List<String>> kategorieRaw = Arrays.stream(CommandCategory.values())
                .collect(Collectors.toMap(c -> c, c -> new ArrayList<>(), (a, b) -> b, TreeMap::new));
        TreeMap<CommandCategory, List<String>> kategorie = new TreeMap<>();
        managerKomend.getRegistered().stream().filter(command -> command.getCategory() != null)
                .forEach(command -> kategorieRaw.get(command.getCategory()).add(command.getName()));
        kategorieRaw.forEach((cc, listRaw) -> {
            List<String> list = new ArrayList<>();
            for (String cmd : listRaw) {
                if ((managerKomend.getCommands().get(cmd).getPermLevel().getNum() >
                        UserUtil.getPermlevel(context.getMember(), guildDao, shardManager).getNum()) ||
                        (!managerKomend.getCommands().get(cmd).isAllowInDMs() &&
                                context.getChannel().getType() == ChannelType.PRIVATE)) continue;
                list.add(cmd);
            }
            kategorie.put(cc, list);
        });
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            AtomicInteger iloscKomend = new AtomicInteger();
            kategorie.forEach((cat, cmd) -> iloscKomend.getAndAdd(cmd.size()));
            eb.setAuthor(context.getTranslated("help.listall.embed.author", iloscKomend.get()), null,
                    context.getEvent().getJDA().getSelfUser().getEffectiveAvatarUrl()
                        .replace(".webp", ".png"));
            ArrayList<String> opis = new ArrayList<>();
            opis.add(context.getTranslated("help.listall.embed.description.firstline", context.getPrefix()));
            opis.add("");
            kategorie.forEach((category, commands) -> {
                if (!commands.isEmpty()) {
                    commands.sort(Ordering.usingToString());
                    opis.add("**" + context.getTranslated("help.category." + category.name().toLowerCase()) +
                            String.format("** (%s)", commands.size()));
                }
            });
            eb.setDescription(String.join("\n", opis));
            context.send(eb.build());
            return true;
        }
        ArrayList<String> opis = new ArrayList<>();
        CommandCategory kategoria = (CommandCategory) context.getArgs()[0];
        List<String> komendy = kategorie.get(kategoria);
        eb.setAuthor(context.getTranslated("help.listcat.embed.author",
                context.getTranslated("help.category." +
                        ((CommandCategory) context.getArgs()[0]).name().toLowerCase()), komendy.size()), null,
                context.getEvent().getJDA().getSelfUser().getEffectiveAvatarUrl()
                        .replace(".webp", ".png"));
        komendy.sort(String::compareToIgnoreCase);
        komendy.forEach(command -> opis
                .add("**" + command + "**: " + context.getTranslated(command + ".help.description",
                        context.getPrefix().replace("*", "\\*")
                                .replace("|", "\\|")
                                .replace("_", "\\_")
                                .replace("`", "\\`"))));
        eb.setDescription(String.join("\n", opis));
        context.send(eb.build());
        return true;
    }

}
