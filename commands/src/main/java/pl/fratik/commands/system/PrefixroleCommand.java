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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("RedundantCollectionOperation")
public class PrefixroleCommand extends Command {

    private static final int PREFIX_LENGTH = 8;

    private GuildDao guildDao;

    public PrefixroleCommand(GuildDao guildDao) {
        name = "prefixrole";
        aliases = new String[] {"roleprefix"};
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("set|remove|list", "string");
        hmap.put("rola", "role");
        hmap.put("prefix", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.SYSTEM;
        cooldown = 5;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);

        this.guildDao = guildDao;
    }

    @Override
    public boolean execute(CommandContext context) {
        String typ = ((String) context.getArgs()[0]).toLowerCase();
        if (!typ.equals("set") && !typ.equals("remove") && !typ.equals("list")) {
            CommonErrors.usage(context);
            return false;
        }
        GuildConfig gc = guildDao.get(context.getGuild().getId());
        if (gc.getRolePrefix() == null) {
            gc.setRolePrefix(new HashMap<>());
            guildDao.save(gc);
        }
        if (typ.equals("list")) {
            if (gc.getRolePrefix() == null || gc.getRolePrefix().isEmpty()) {
                context.send(context.getTranslated("prefixrole.list.isempty"));
                return false;
            }

            ArrayList<String> strArray = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            boolean setuj = false;
            for (Map.Entry<String, String> entry : gc.getRolePrefix().entrySet()) {
                Role r = context.getGuild().getRoleById(entry.getValue());
                if (r == null) {
                    gc.getRolePrefix().remove(entry.getKey());
                    setuj = true;
                } else {
                    String s = r.getAsMention() + " :: " + entry.getValue() + "\n";
                    if (sb.length() + s.length() >= 1996) {
                        strArray.add(sb.toString());
                        sb = new StringBuilder().append(s);
                    } else sb.append(s);
                }
            }

            EmbedBuilder eb = context.getBaseEmbed();
            eb.setColor(UserUtil.getPrimColor(context.getSender()));
            for (String str : strArray) { eb.addField("", str, false); }
            context.send(eb.build());
            if (setuj) guildDao.save(gc);
            return true;
        }
        if (typ.equals("remove")) {
            Role r;
            try {
                r = (Role) context.getArgs()[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                CommonErrors.usage(context);
                return false;
            }
            if (r == null || !gc.getRolePrefix().containsKey(r.getId())) {
                context.send(context.getTranslated("prefixrole.doesnt.set"));
                return false;
            }
            context.send(context.getTranslated("prefixrole.remove.succes", r.getName()));
            gc.getRolePrefix().remove(r.getId());
            guildDao.save(gc);
        }
        if (typ.equals("set")) {
            Role     role;
            String prefix;

            try {
                role = (Role) context.getArgs()[1];
                prefix = (String) context.getArgs()[2];
            } catch (ArrayIndexOutOfBoundsException e) {
                CommonErrors.usage(context);
                return false;
            }

            if (role == null) {
                context.send(context.getTranslated("prefixrole.badrole"));
                return false;
            }

            if (prefix.length() > PREFIX_LENGTH) {
                context.send(context.getTranslated("prefixrole.length", PREFIX_LENGTH));
                return false;
            }
            context.send(context.getTranslated("prefixrole.set.succes", role.getName()));

            gc.getRolePrefix().remove(role.getId());
            gc.getRolePrefix().put(role.getId(), prefix);
            guildDao.save(gc);
            return true;
        }
        CommonErrors.usage(context); // tak na wszelki wypadek
        return false;
    }

}
