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

package pl.fratik.music.commands;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.*;
import pl.fratik.core.util.UserUtil;


public class NodesCommand extends NewCommand {

    public NodesCommand() {
        name = "nodes";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
    }

    @SubCommand(name = "add", usage = "<node:string>")
    public void add(@NotNull NewCommandContext context) {
        String ip;
        String pass;
        int port;
        try {
            String[] args = context.getArguments().get("node").getAsString().split(":");
            ip = args[0];
            pass = args[1];
            port = Integer.parseInt(args[2]);
        } catch (Exception e) {
            context.replyEphemeral(context.getTranslated("nodes.add.failed.parse"));
            return;
        }
        if (pass.equals("default")) pass = Ustawienia.instance.lavalink.defaultPass;
        Ustawienia.instance.lavalink.nodes.add(new Ustawienia.Lavalink.LavalinkNode(ip, pass, port, port));
        context.replyEphemeral(context.getTranslated("nodes.add.success"));
    }

    @SubCommand(name = "delete", usage = "<index:integer>")
    public void delete(@NotNull NewCommandContext context) {
        int index = context.getArguments().get("index").getAsInt();
        try {
            Ustawienia.instance.lavalink.nodes.remove(index);
        } catch (IndexOutOfBoundsException e) {
            context.replyEphemeral(context.getTranslated("nodes.delete.unknown"));
            return;
        }

        context.replyEphemeral(context.getTranslated("nodes.delete.success"));
    }

    @SubCommand(name = "list")
    public void list(@NotNull NewCommandContext context) {
        StringBuilder builderTresci = new StringBuilder();
        builderTresci.append("```\n");
        int i = 0;
        for (Ustawienia.Lavalink.LavalinkNode n : Ustawienia.instance.lavalink.nodes) {
            builderTresci.append(context.getTranslated("nodes.list.entry", n.address, n.wsPort, i));
            i++;
        }
        builderTresci.append("```");
        context.replyEphemeral(builderTresci.toString());
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
