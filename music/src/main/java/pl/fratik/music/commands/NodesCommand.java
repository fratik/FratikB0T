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

package pl.fratik.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;

import java.util.LinkedHashMap;

public class NodesCommand extends Command {
    public NodesCommand() {
        name = "nodes";
        permLevel = PermLevel.BOTOWNER;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("node", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        uzycieDelim = " ";
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name = "add")
    public boolean add(@NotNull CommandContext context) {
        String ip;
        String pass;
        int port;
        try {
            String[] args = ((String) context.getArgs()[0]).split(":");
            ip = args[0];
            pass = args[1];
            port = Integer.parseInt(args[2]);
        } catch (Exception e) {
            context.send(context.getTranslated("nodes.add.failed.parse"));
            return false;
        }
        if (pass.equals("default")) {
            pass = Ustawienia.instance.lavalink.defaultPass;
        }
        Ustawienia.instance.lavalink.nodes.add(new Ustawienia.Lavalink.LavalinkNode(ip, pass, port, port));
        context.send(context.getTranslated("nodes.add.success"));
        return true;
    }

    @SubCommand(name = "delete", aliases = "del")
    public boolean delete(@NotNull CommandContext context) {
        int index = Integer.parseInt((String) context.getArgs()[0]);
        try {
            Ustawienia.instance.lavalink.nodes.remove(index);
        } catch (IndexOutOfBoundsException e) {
            context.send(context.getTranslated("nodes.delete.unknown"));
            return false;
        }
        context.send(context.getTranslated("nodes.delete.success"));
        return true;
    }

    @SubCommand(name = "list", emptyUsage = true)
    public boolean list(@NotNull CommandContext context) {
        StringBuilder builderTresci = new StringBuilder();
        builderTresci.append("```\n");
        int i = 0;
        for (Ustawienia.Lavalink.LavalinkNode n : Ustawienia.instance.lavalink.nodes) {
            builderTresci.append(context.getTranslated("nodes.list.entry", n.address, n.wsPort, i));
            i++;
        }
        builderTresci.append("```");
        context.send(builderTresci);
        return true;
    }
}
