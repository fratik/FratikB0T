/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.fratikcoiny.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.EventWaiter;

import java.util.Collections;
import java.util.Map;

public class SklepAdminCommand extends SklepSharedCommand {
    public SklepAdminCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        super(guildDao, memberDao, eventWaiter, shardManager, eventBus);
        name = "sklep_admin";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES, Permission.MANAGE_SERVER);
    }

    @SubCommand(name="ustaw", usage = "<rola:role> <kwota:int> [opis:string]")
    public boolean dodaj(NewCommandContext context) {
        context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        Role rola = context.getArguments().get("rola").getAsRole();
        long kasa = context.getArguments().get("kwota").getAsInt();
        String opis = context.getArgumentOr("opis", null, OptionMapping::getAsString);
        if (gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.sendMessage(context.getTranslated("sklep.ustaw.alreadyset"));
            return false;
        }
        if (!context.getMember().getRoles().get(0).canInteract(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.DODAWANIE, rola, opis, kasa, 0, true,
                    context.getTlumaczenia(), context.getLanguage());
            context.sendMessage(eb.build());
            context.sendMessage(context.getTranslated("sklep.ustaw.noperms"));
        }
        EmbedBuilder eb = generateEmbed(Typ.DODAWANIE, rola, opis, kasa, 0, false,
                context.getTlumaczenia(), context.getLanguage());

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.setEmbeds(eb.build());
        mb.setComponents(ActionRow.of(
                Button.success("ADD", context.getTranslated("sklep.przycisk.dodaj")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));

        Message msgTmp = context.sendMessage(mb.build());
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessageComponents(Collections.emptyList()).queue();
            if (event.getComponentId().equals("ADD")) {
                GuildConfig gc2 = guildDao.get(event.getGuild());
                Map<String, Long> roleDoKupienia = gc.getRoleDoKupienia();
                roleDoKupienia.put(rola.getId(), kasa);
                Map<String, String> roleDoKupieniaOpisy = gc.getRoleDoKupieniaOpisy();
                roleDoKupieniaOpisy.put(rola.getId(), opis);
                gc2.setRoleDoKupienia(roleDoKupienia);
                gc2.setRoleDoKupieniaOpisy(roleDoKupieniaOpisy);
                guildDao.save(gc2);
                event.getHook().editOriginal(context.getTranslated("sklep.ustaw.success")).queue();
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.ustaw.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessageComponents(Collections.emptySet()).queue();
            context.sendMessage(context.getTranslated("sklep.ustaw.canceled"));
        });
        rw.create();
        return true;
    }

    @SubCommand(name="usun", usage = "<rola:role>")
    public boolean usun(NewCommandContext context) {
        context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        Role rola = context.getArguments().get("rola").getAsRole();
        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.sendMessage(context.getTranslated("sklep.usun.alreadyset"));
            return false;
        }
        if (!context.getMember().getRoles().get(0).canInteract(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.USUWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    gc.getRoleDoKupienia().get(rola.getId()), 0, true,
                    context.getTlumaczenia(), context.getLanguage());
            context.sendMessage(eb.build());
            context.sendMessage(context.getTranslated("sklep.usun.noperms"));
        }
        EmbedBuilder eb = generateEmbed(Typ.USUWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                gc.getRoleDoKupienia().get(rola.getId()), 0, false,
                context.getTlumaczenia(), context.getLanguage());

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.setEmbeds(eb.build());
        mb.setComponents(ActionRow.of(
                Button.secondary("DELETE", context.getTranslated("sklep.przycisk.usun")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));

        Message msgTmp = context.sendMessage(mb.build());

        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessageComponents(Collections.emptySet()).queue();
            if (event.getComponentId().equals("DELETE")) {
                GuildConfig gc2 = guildDao.get(event.getGuild());
                Map<String, Long> roleDoKupienia = gc.getRoleDoKupienia();
                roleDoKupienia.remove(rola.getId());
                Map<String, String> roleDoKupieniaOpisy = gc.getRoleDoKupieniaOpisy();
                roleDoKupieniaOpisy.remove(rola.getId());
                gc2.setRoleDoKupienia(roleDoKupienia);
                gc2.setRoleDoKupieniaOpisy(roleDoKupieniaOpisy);
                guildDao.save(gc2);
                event.getHook().editOriginal(context.getTranslated("sklep.usun.success")).queue();
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.usun.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessageComponents(Collections.emptyList()).queue();
            context.sendMessage(context.getTranslated("sklep.usun.canceled"));
        });
        rw.create();
        return true;
    }
}
