/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class UstawPoziomCommand extends Command {

    private final GuildDao guildDao;
    private final ManagerKomend managerKomend;

    public UstawPoziomCommand(GuildDao guildDao, ManagerKomend managerKomend) {
        name = "ustawpoziom";
        cooldown = 5;
        permLevel = PermLevel.OWNER;
        category = CommandCategory.SYSTEM;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("list|set|remove", "string");
        hmap.put("komenda", "string");
        hmap.put("permlevel", "integer");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        uzycieDelim = " ";
        this.guildDao = guildDao;
        this.managerKomend = managerKomend;
    }

    @Override
    public boolean execute(CommandContext context) {
        String typ = ((String) context.getArgs()[0]).toLowerCase();
        GuildConfig gc = guildDao.get(context.getGuild());
        boolean setuj = false;
        if (gc.getPermLevel() == null) {
            gc.setPermLevel(new HashMap<>());
            setuj = true;
        }
        if (typ.equals("list")) {
            if (gc.getPermLevel().isEmpty()) {
                context.send(context.getTranslated("ustawpoziom.list.isempty"));
                return false;
            }
            ArrayList<String> list = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            EmbedBuilder embed = context.getBaseEmbed();
            embed.setColor(UserUtil.getPrimColor(context.getSender()));

            for (Map.Entry<String, Integer> entry : gc.getPermLevel().entrySet()) {
                PermLevel pl = PermLevel.getPermLevel(entry.getValue());
                if (pl == null) {
                    gc.getPermLevel().remove(entry.getKey());
                    setuj = true;
                } else {
                    String append = context.getTranslated("ustawpoziom.list.desc",
                            entry.getValue(),
                            context.getTranslated(pl.getLanguageKey()),
                            entry.getValue());
                    if (sb.toString().length() + append.length() >= 900) {
                        list.add(sb.toString());
                        sb = new StringBuilder();
                    } else sb.append(append);
                }
            }
            if (list.isEmpty()) embed.setDescription(sb.toString());
            else list.forEach(s -> embed.addField(" ", s, false));
            context.send(embed.build());
            if (setuj) guildDao.save(gc);
            return true;
        }
        if (typ.equals("remove")) {
            String cmd;
            try {
                cmd = ((String) context.getArgs()[1]).toLowerCase();
            } catch (Exception e) {
                CommonErrors.usage(context);
                return false;
            }
            if (!gc.getPermLevel().containsKey(cmd)) {
                context.send(context.getTranslated("ustawpoziom.remove.doesnt.exist"));
                return false;
            }
            gc.getPermLevel().remove(cmd);
            guildDao.save(gc);
            context.send(context.getTranslated("ustawpoziom.remove.succes"));
            return true;
        }
        if (typ.equals("set")) {
            String cmd;
            Integer lvl;
            PermLevel plvl;
            Command ccmd = null;
            try {
                cmd = ((String) context.getArgs()[1]).toLowerCase();
                lvl = (Integer) context.getArgs()[2];
            } catch (Exception e) {
                CommonErrors.usage(context);
                return false;
            }

            try {
                plvl = PermLevel.getPermLevel(lvl);
            } catch (Exception e) {
                context.send(context.getTranslated("ustawpoziom.set.badlvl"));
                return false;
            }
            if (plvl == null) {
                context.send(context.getTranslated("ustawpoziom.set.badlvl"));
                return false;
            }
            for (Map.Entry<String, Command> entry : managerKomend.getCommands().entrySet()) {
                if (!entry.getKey().equals(cmd)) continue;
                ccmd = entry.getValue();
                break;
            }
            if (ccmd == null) {
                context.send(context.getTranslated("ustawpoziom.set.badcmd"));
                return false;
            }

            PermLevel jegolvl = UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager());
            if (ccmd.getPermLevel().getNum() > jegolvl.getNum() || plvl.getNum() > jegolvl.getNum()) {
                context.send(context.getTranslated("ustawpoziom.set.nopermissions"));
                return false;
            }

            if (ccmd.getCategory() == CommandCategory.MODERATION && plvl.getNum() == 0) {
                context.send(context.getTranslated("ustawpoziom.set.moderationcmd"));
                return false;
            }

            if (ccmd.getPermLevel().getNum() > 4) {
                context.send(context.getTranslated("ustawpoziom.set.gacmd"));
                return false;
            }

            if (ccmd.getName().equals("ustawpoziom")) {
                context.send(context.getTranslated("ustawpoziom.set.poziomcmd"));
                return false;
            }

            gc.getPermLevel().remove(ccmd.getName());
            gc.getPermLevel().put(ccmd.getName(), plvl.getNum());
            guildDao.save(gc);
            context.send(context.getTranslated("ustawpoziom.set.succes",
                    ccmd.getName(), plvl.getNum(),
                    context.getTranslated(plvl.getLanguageKey())));
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

}
