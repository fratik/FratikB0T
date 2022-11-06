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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Priv;
import pl.fratik.commands.entity.PrivDao;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.StringUtil;

public class PrivCommand extends NewCommand {
    private final PrivDao privDao;
    private final UserDao userDao;

    public PrivCommand(PrivDao privDao, UserDao userDao) {
        this.privDao = privDao;
        this.userDao = userDao;
        name = "priv";
        cooldown = 15;
        usage = "<osoba:user> <tresc:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        User sender = context.getSender();
        User doKogo = context.getArguments().get("osoba").getAsUser();
        String tresc = context.getArguments().get("tresc").getAsString();
        if (sender.equals(doKogo)) {
            context.replyEphemeral(context.getTranslated("priv.same.recipient"));
            return;
        }
        context.defer(true);
        if (privDao.isZgloszone(sender.getId())) {
            context.sendMessage(context.getTranslated("priv.reported"));
            return;
        }
        UserConfig uc = userDao.get(sender);
        if (!uc.isPrivWlaczone()) {
            context.sendMessage(context.getTranslated("priv.off"));
            return;
        }
        if (uc.isPrivBlacklist()) {
            context.sendMessage(context.getTranslated("priv.blacklist"));
            return;
        }
        if (uc.getPrivIgnored().contains(doKogo.getId())) {
            context.sendMessage(context.getTranslated("priv.ignored"));
            return;
        }
        try {
            sender.openPrivateChannel().complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("priv.cant.receive.dm"));
            return;
        }
        PrivateChannel ch;
        try {
            ch = doKogo.openPrivateChannel().complete();
            if (privDao.isZgloszone(doKogo.getId())) throw new KurwaException();
            UserConfig uc2 = userDao.get(doKogo);
            if (uc2.getPrivIgnored().contains(sender.getId())) throw new KurwaException();
            if (!uc2.isPrivWlaczone()) throw new KurwaException();
            if (uc2.isPrivBlacklist()) throw new KurwaException();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("priv.cant.send.dm"));
            return;
        }
        String id = StringUtil.generateId();
        try {
            ch.sendMessage(context.getTlumaczenia().get(context.getTlumaczenia().getLanguage(doKogo),
                    "priv.message", sender.getAsTag(), sender.getId(), tresc, sender.getId(), id)).complete();
            privDao.save(new Priv(id, sender.getId(), doKogo.getId(), tresc, null));
            context.sendMessage(context.getTranslated("priv.success"));
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("priv.cant.send.dm"));
        }
    }

    private static class KurwaException extends Exception { //sonarlint weź się przymknij
    }
}
