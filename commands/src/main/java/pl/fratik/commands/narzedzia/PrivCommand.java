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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.entity.Priv;
import pl.fratik.commands.entity.PrivDao;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.StringUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class PrivCommand extends Command {
    private final PrivDao privDao;
    private final UserDao userDao;

    public PrivCommand(PrivDao privDao, UserDao userDao) {
        this.privDao = privDao;
        this.userDao = userDao;
        name = "priv";
        category = CommandCategory.UTILITY;
        cooldown = 15;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("osoba", "user");
        hmap.put("treść", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        allowInDMs = true;
        uzycieDelim = " ";
        aliases = new String[] {"pw", "dm", "msg", "pv", "privee"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User doKogo = (User) context.getArgs()[0];
        User sender = context.getSender();
        String tresc = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        if (sender.equals(doKogo)) {
            context.send(context.getTranslated("priv.same.recipient"));
            return false;
        }
        if (privDao.isZgloszone(sender.getId())) {
            context.send(context.getTranslated("priv.reported"));
            return false;
        }
        UserConfig uc = userDao.get(sender);
        if (!uc.isPrivWlaczone()) {
            context.send(context.getTranslated("priv.off"));
            return false;
        }
        if (uc.isPrivBlacklist()) {
            context.send(context.getTranslated("priv.blacklist"));
            return false;
        }
        if (uc.getPrivIgnored().contains(doKogo.getId())) {
            context.send(context.getTranslated("priv.ignored"));
            return false;
        }
        try {
            sender.openPrivateChannel().complete();
        } catch (Exception e) {
            context.send(context.getTranslated("priv.cant.receive.dm"));
            return false;
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
            context.send(context.getTranslated("priv.cant.send.dm"));
            return false;
        }
        String id = StringUtil.generateId();
        try {
            ch.sendMessage(context.getTlumaczenia().get(context.getTlumaczenia().getLanguage(doKogo),
                    "priv.message", sender.getAsTag(), sender.getId(), tresc,
                    Ustawienia.instance.prefix, sender.getId(), Ustawienia.instance.prefix, id)).complete();
            privDao.save(new Priv(id, doKogo.getId(), sender.getId(), tresc, false));
            context.send(context.getTranslated("priv.success"));
        } catch (Exception e) {
            context.send(context.getTranslated("priv.cant.send.dm"));
        }
        return true;
    }

    private static class KurwaException extends Exception { //sonarlint weź się przymknij
    }
}
