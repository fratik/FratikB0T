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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import pl.fratik.commands.MemberListener;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.NamedThreadFactory;
import pl.fratik.core.util.UserUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE;
import static net.dv8tion.jda.api.entities.MessageEmbed.VALUE_MAX_LENGTH;

public class PrefixroliCommand extends Command {

    private static final int PREFIX_LENGTH = 8;
    private final ExecutorService executor;

    private final GuildDao guildDao;
    private final MemberListener listener;

    public PrefixroliCommand(GuildDao guildDao, MemberListener listener) {
        this.listener = listener;
        name = "prefixroli";
        aliases = new String[] {"prefiksroli", "prefixrol", "prefixrole"}; //FIXME wydżebać prefixrole i wrzucić do .help.nazwa w eng tłumaczeniach
        uzycieDelim = " ";
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("set|remove|list", "string");
        hmap.put("rola", "role");
        hmap.put("prefix", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false, false});
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.MODERATION;
        cooldown = 5;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);

        this.guildDao = guildDao;
        executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("prefixroli-setter"));
    }

    @Override
    public void onUnregister() {
        executor.shutdown();
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
                context.send(context.getTranslated("prefixroli.list.isempty"));
                return false;
            }
            ArrayList<String> strArray = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            boolean setuj = false;
            for (Map.Entry<String, String> entry : gc.getRolePrefix().entrySet()) {
                Role r = context.getGuild().getRoleById(entry.getKey());
                String s;
                if (r == null) {
                    gc.getRolePrefix().remove(entry.getKey());
                    setuj = true;
                    continue;
                }
                s = r.getAsMention() + " = `" + entry.getValue() + "`\n";
                if (sb.length() + s.length() > VALUE_MAX_LENGTH - 100) {
                    strArray.add(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(s);
            }

            EmbedBuilder eb = context.getBaseEmbed(context.getTranslated("prefixroli.list.header"), null);
            eb.setColor(UserUtil.getPrimColor(context.getSender()));
            if (!strArray.isEmpty()) strArray.forEach(se -> eb.addField(ZERO_WIDTH_SPACE, se, false));
            eb.addField(ZERO_WIDTH_SPACE, sb.toString(), false);
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
                context.send(context.getTranslated("prefixroli.doesnt.set"));
                return false;
            }
            context.send(context.getTranslated("prefixroli.remove.success", r.getName()));
            String prefix = gc.getRolePrefix().remove(r.getId());
            executor.submit(() -> {
                for (Member member : context.getGuild().findMembers(m -> m.getRoles().contains(r)).get()) {
                    try {
                        if (member.getNickname() != null && member.getNickname().startsWith(prefix + " "))
                            member.modifyNickname(member.getNickname().substring((prefix + " ").length())).queue(null, i -> {});
                    } catch (Exception ignored) {}
                }
            });
            guildDao.save(gc);
            return true;
        }
        Role     role;
        String prefix;

        try {
            role = (Role) context.getArgs()[1];
            prefix = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 2, context.getArgs().length))
                    .map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(uzycieDelim));
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            CommonErrors.usage(context);
            return false;
        }

        if (role == null) {
            context.send(context.getTranslated("prefixroli.badrole"));
            return false;
        }

        if (prefix.length() > PREFIX_LENGTH) {
            context.send(context.getTranslated("prefixroli.length", PREFIX_LENGTH));
            return false;
        }
        context.send(context.getTranslated("prefixroli.set.success", role.getName()));
        executor.submit(() -> {
            for (Member member : context.getGuild().findMembers(m -> m.getRoles().contains(role)).get())
                listener.updateNickname(member, null);
        });

        gc.getRolePrefix().remove(role.getId());
        gc.getRolePrefix().put(role.getId(), prefix);
        guildDao.save(gc);
        return true;
    }

}
