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

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import okhttp3.Response;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.SilentExecutionFail;
import pl.fratik.core.util.NetworkUtil;

import java.io.IOException;
import java.util.HashMap;

public class SelfieCommand extends NewCommand {
    public SelfieCommand() {
        name = "selfie";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
        allowInDMs = true;
    }

    @SubCommand(name="pc")
    public void pc(NewCommandContext context) {
        String ipUrlPc = Ustawienia.instance.apiUrls.get("networkIpPc");
        if (ipUrlPc == null) throw new SilentExecutionFail();
        HashMap<String, String> xd = new HashMap<>();
        xd.put("Requester", context.getSender().getAsTag());
        context.deferAsync(false);
        try (Response res = NetworkUtil.downloadResponse(ipUrlPc,
                Ustawienia.instance.apiKeys.get("networkIpPc"), xd)) {
            if (res.code() == 401) {
                context.sendMessage(context.getTranslated("selfie.invalidpass"));
                return;
            }
            if (res.code() == 403) {
                context.sendMessage(context.getTranslated("selfie.rejected"));
                return;
            }
            if (res.body() == null) throw new IOException();
            context.sendMessage("ryjfratika.jpg", res.body().bytes());
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }
    }

    @SubCommand(name="mb")
    public void mb(NewCommandContext context) {
        String ipUrlMb = Ustawienia.instance.apiUrls.get("networkIpMb");
        if (ipUrlMb == null) throw new SilentExecutionFail();
        HashMap<String, String> xd = new HashMap<>();
        xd.put("Requester", context.getSender().getAsTag());
        context.deferAsync(false);
        try (Response res = NetworkUtil.downloadResponse(ipUrlMb,
                Ustawienia.instance.apiKeys.get("networkIpMb"), xd)) {
            if (res.code() == 401) {
                context.reply(context.getTranslated("selfie.invalidpass"));
                return;
            }
            if (res.code() == 403) {
                context.reply(context.getTranslated("selfie.rejected"));
                return;
            }
            if (res.body() == null) throw new IOException();
            context.sendMessage("ryjfratika.jpg", res.body().bytes());
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("image.server.fail"));
        }
    }
}
