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
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.implementation.ManagerKomendImpl;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
        allowPermLevelChange = false;
        this.guildDao = guildDao;
        this.managerKomend = managerKomend;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String typ = ((String) context.getArgs()[0]).toLowerCase();
        GuildConfig gc = guildDao.get(context.getGuild());
        final AtomicBoolean setuj = new AtomicBoolean(false);
        if (gc.getCmdPermLevelOverrides() == null) {
            gc.setCmdPermLevelOverrides(new HashMap<>());
            setuj.set(true);
        }
        if (typ.equals("list") || typ.equals("lista")) {
            if (gc.getCmdPermLevelOverrides().isEmpty()) {
                context.reply(context.getTranslated("ustawpoziom.list.isempty"));
                return false;
            }
            ArrayList<String> list = new ArrayList<>();
            final StringBuilder[] sb = {new StringBuilder()};
            EmbedBuilder embed = context.getBaseEmbed();
            embed.setColor(UserUtil.getPrimColor(context.getSender()));

            gc.getCmdPermLevelOverrides().entrySet().stream().sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey())).forEach(entry -> {
                Command cmd = managerKomend.getCommands().get(entry.getKey());
                if (cmd == null || !ManagerKomendImpl.checkPermLevelOverride(cmd, gc, entry.getValue())) {
                    gc.getCmdPermLevelOverrides().remove(entry.getKey());
                    setuj.set(true);
                } else {
                    String append = context.getTranslated("ustawpoziom.list.desc",
                            entry.getKey(),
                            entry.getValue().getNum(),
                            context.getTranslated(entry.getValue().getLanguageKey())) + "\n";
                    if (sb[0].toString().length() + append.length() >= 900) {
                        list.add(sb[0].toString());
                        sb[0] = new StringBuilder();
                    } else sb[0].append(append);
                }
            });
            if (list.isEmpty()) embed.setDescription(sb[0].toString());
            else list.forEach(s -> embed.addField(" ", s, false));
            context.reply(embed.build());
            if (setuj.get()) guildDao.save(gc);
            return true;
        }
        if (typ.equals("remove") || typ.equals("reset") || typ.equals("clear")) {
            String cmd;
            try {
                cmd = ((String) context.getArgs()[1]).toLowerCase();
            } catch (Exception e) {
                CommonErrors.usage(context);
                return false;
            }
            if (!managerKomend.getCommands().containsKey(cmd)) {
                context.reply(context.getTranslated("ustawpoziom.set.unknowncmd"));
                return false;
            }
            if (!gc.getCmdPermLevelOverrides().containsKey(cmd)) {
                context.reply(context.getTranslated("ustawpoziom.remove.doesnt.exist"));
                return false;
            }
            gc.getCmdPermLevelOverrides().remove(cmd);
            guildDao.save(gc);
            context.reply(context.getTranslated("ustawpoziom.remove.succes"));
            return true;
        }
        if (typ.equals("set") || typ.equals("add")) {
            String cmd;
            Integer lvl;
            PermLevel plvl;
            Command ccmd;
            try {
                cmd = ((String) context.getArgs()[1]).toLowerCase();
                lvl = (Integer) context.getArgs()[2];
            } catch (Exception e) {
                CommonErrors.usage(context);
                return false;
            }

            try {
                plvl = PermLevel.getPermLevel(lvl);
                if (plvl == null) throw new Exception("jeśli to czytasz, idź na wybory");
            } catch (Exception e) {
                context.reply(context.getTranslated("ustawpoziom.set.unknownplvl"));
                return false;
            }
            ccmd = managerKomend.getCommands().get(cmd);
            if (ccmd == null) {
                context.reply(context.getTranslated("ustawpoziom.set.unknowncmd"));
                return false;
            }

            if (plvl.getNum() > PermLevel.OWNER.getNum()) {
                context.reply(context.getTranslated("ustawpoziom.set.gaplvl", PermLevel.OWNER.getNum()));
                return false;
            }

            PermLevel jegolvl = UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager());
            if (ccmd.getPermLevel().getNum() > jegolvl.getNum() || plvl.getNum() > jegolvl.getNum()) {
                context.reply(context.getTranslated("ustawpoziom.set.nopermissions"));
                return false;
            }

            if (!ccmd.isAllowPermLevelChange()) {
                context.reply(context.getTranslated("ustawpoziom.set.edit.not.allowed", context.getPrefix()));
                return false;
            }

            if (!ccmd.isAllowPermLevelEveryone() && plvl.getNum() == PermLevel.EVERYONE.getNum()) {
                context.reply(context.getTranslated("ustawpoziom.set.everyone.not.allowed", plvl.getNum()));
                return false;
            }

            if (ccmd.getPermLevel().equals(plvl)) {
                if (gc.getCmdPermLevelOverrides().remove(cmd) != null) {
                    context.reply(context.getTranslated("ustawpoziom.set.default.value", plvl.getNum()));
                    guildDao.save(gc);
                    return true;
                } else {
                    context.reply(context.getTranslated("ustawpoziom.set.not.changed", plvl.getNum(), ccmd.getPermLevel().getNum()));
                    return false;
                }
            }

            gc.getCmdPermLevelOverrides().put(ccmd.getName(), plvl);
            guildDao.save(gc);
            context.reply(context.getTranslated("ustawpoziom.set.success",
                    ccmd.getName(), plvl.getNum(), context.getTranslated(plvl.getLanguageKey())));
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

}
