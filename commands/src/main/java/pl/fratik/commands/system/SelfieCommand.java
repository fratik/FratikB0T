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

import net.dv8tion.jda.api.Permission;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.SilentExecutionFail;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.HashMap;

public class SelfieCommand extends Command {
    public SelfieCommand() {
        name = "selfie";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        aliases = new String[] {"ryjfratika"};
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
    }

    @SubCommand(name="pc")
    public boolean pc(CommandContext context) {
        String ipUrlPc = Ustawienia.instance.apiUrls.get("networkIpPc");
        if (ipUrlPc == null) throw new SilentExecutionFail();
        try {
            HashMap<String, String> xd = new HashMap<>();
            xd.put("Requester", context.getSender().getAsTag());
            Response res = NetworkUtil.downloadResponse(ipUrlPc,
                    Ustawienia.instance.apiKeys.get("networkIpPc"), xd);
            if (res.code() == 401) {
                context.send(context.getTranslated("selfie.invalidpass"));
                return false;
            }
            if (res.code() == 403) {
                context.send(context.getTranslated("selfie.rejected"));
                return false;
            }
            if (res.body() == null) throw new IOException();
            context.getChannel().sendFile(res.body().bytes(), "ryjfratika.jpg").queue();
            return true;
        } catch (IOException e) {
            context.send(context.getTranslated("image.server.fail"));
            return false;
        }
    }

    @SubCommand(name="mb")
    public boolean mb(CommandContext context) {
        String ipUrlMb = Ustawienia.instance.apiUrls.get("networkIpMb");
        if (ipUrlMb == null) throw new SilentExecutionFail();
        try {
            HashMap<String, String> xd = new HashMap<>();
            xd.put("Requester", context.getSender().getAsTag());
            Response res = NetworkUtil.downloadResponse(ipUrlMb,
                    Ustawienia.instance.apiKeys.get("networkIpMb"), xd);
            if (res.code() == 401) {
                context.send(context.getTranslated("selfie.invalidpass"));
                return false;
            }
            if (res.code() == 403) {
                context.send(context.getTranslated("selfie.rejected"));
                return false;
            }
            if (res.body() == null) throw new IOException();
            context.getChannel().sendFile(res.body().bytes(), "ryjfratika.jpg").queue();
            return true;
        } catch (IOException e) {
            context.send(context.getTranslated("image.server.fail"));
            return false;
        }
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }
}
