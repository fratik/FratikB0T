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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SklepCommand extends SklepSharedCommand {
    public SklepCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        super(guildDao, memberDao, eventWaiter, shardManager, eventBus);
        name = "sklep";
    }

    @SubCommand(name="kup", usage = "<rola:role>")
    public void kup(NewCommandContext context) {
        context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());

        Role rola = context.getArguments().get("rola").getAsRole();

        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.sendMessage(context.getTranslated("sklep.kup.cantbuythis"));
            return;
        }
        long kasa = gc.getRoleDoKupienia().get(rola.getId());
        MemberConfig mc = memberDao.get(context.getMember());
        if (context.getMember().getRoles().contains(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), true, context.getTlumaczenia(), context.getLanguage());
            context.sendMessage(eb.build());
            context.sendMessage(context.getTranslated("sklep.kup.hasrole"));
            return;
        }
        if (mc.getFratikCoiny() < gc.getRoleDoKupienia().get(rola.getId())) {
            EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());
            context.sendMessage(eb.build());
            return;
        }
        EmbedBuilder eb = generateEmbed(Typ.KUPOWANIE, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.setEmbeds(eb.build());
        mb.setComponents(ActionRow.of(
                Button.success("BUY", context.getTranslated("sklep.przycisk.kup")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));

        Message msgTmp = context.sendMessage(mb.build());
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessageComponents(Collections.emptySet()).queue();
            if (event.getComponentId().equals("BUY")) {
                try {
                    context.getGuild().addRoleToMember(context.getMember(), rola).queue(aVoid -> {
                        MemberConfig mc2 = memberDao.get(Objects.requireNonNull(event.getMember()));
                        if (mc2.getFratikCoiny() < kasa) {
                            event.getHook().editOriginal(context.getTranslated("sklep.kup.brakhajsu")).queue();
                            return;
                        }
                        mc2.setFratikCoiny(mc2.getFratikCoiny() - kasa);
                        memberDao.save(mc2);
                        event.getHook().editOriginal(context.getTranslated("sklep.kup.success")).queue();
                    }, err -> event.getHook().editOriginal(context.getTranslated("sklep.kup.failed")).queue());
                } catch (Exception e) {
                    event.getHook().editOriginal(context.getTranslated("sklep.kup.failed")).queue();
                }
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.kup.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessageComponents(Collections.emptySet()).queue();
            context.sendMessage(context.getTranslated("sklep.kup.canceled"));
        });
        rw.create();
    }

    @SubCommand(name="sprzedaj", usage = "<rola:role>")
    public void sprzedaj(NewCommandContext context) {
        context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        Role rola = context.getArguments().get("rola").getAsRole();
        if (!gc.getRoleDoKupienia().containsKey(rola.getId())) {
            context.sendMessage(context.getTranslated("sklep.sprzedaj.cantsellthis"));
            return;
        }
        long kasa = gc.getRoleDoKupienia().get(rola.getId());
        MemberConfig mc = memberDao.get(context.getMember());
        if (!context.getMember().getRoles().contains(rola)) {
            EmbedBuilder eb = generateEmbed(Typ.SPRZEDAZ, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                    kasa, mc.getFratikCoiny(), false, context.getTlumaczenia(), context.getLanguage());
            context.sendMessage(eb.build());
            context.sendMessage(context.getTranslated("sklep.sprzedaj.hasrole"));
            return;
        }
        EmbedBuilder eb = generateEmbed(Typ.SPRZEDAZ, rola, gc.getRoleDoKupieniaOpisy().get(rola.getId()),
                kasa, mc.getFratikCoiny(), true, context.getTlumaczenia(), context.getLanguage());

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.setEmbeds(eb.build());
        mb.setComponents(ActionRow.of(
                Button.success("SELL", context.getTranslated("sklep.przycisk.sprzedaj")),
                Button.danger("CANCEL", context.getTranslated("sklep.przycisk.anuluj"))
        ));
        Message msgTmp = context.sendMessage(mb.build());

        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, msgTmp.getIdLong(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(event -> {
            msgTmp.editMessageComponents(Collections.emptyList()).queue();
            if (event.getComponentId().equals("SELL")) {
                context.getGuild().removeRoleFromMember(context.getMember(), rola)
                        .queue(
                                aVoid -> {
                                    MemberConfig mc2 = memberDao.get(Objects.requireNonNull(event.getMember()));
                                    mc2.setFratikCoiny(mc2.getFratikCoiny() + (int) Math.floor((double) kasa / 2));
                                    memberDao.save(mc2);
                                    event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.success")).queue();
                                },
                                err -> event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.failed")).queue());
            }
            if (event.getComponentId().equals("CANCEL"))
                event.getHook().editOriginal(context.getTranslated("sklep.sprzedaj.canceled")).queue();
        });
        rw.setTimeoutHandler(() -> {
            msgTmp.editMessageComponents(Collections.emptyList()).queue();
            context.sendMessage(context.getTranslated("sklep.sprzedaj.canceled"));
        });
        rw.create();
    }

    @SubCommand(name="lista")
    public void lista(NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        MemberConfig mc = memberDao.get(context.getMember());
        List<EmbedBuilder> strony = new ArrayList<>();
        for (String rId : gc.getRoleDoKupienia().keySet()) {
            long kasa = gc.getRoleDoKupienia().get(rId);
            Role rola = context.getGuild().getRoleById(rId);
            if (rola == null) continue;
            EmbedBuilder eb = generateEmbed(Typ.INFO, rola, gc.getRoleDoKupieniaOpisy().get(rId), kasa,
                    mc.getFratikCoiny(), context.getMember().getRoles().contains(rola), context.getTlumaczenia(),
                    context.getLanguage());
            strony.add(eb);
        }
        if (strony.isEmpty()) {
            context.sendMessage(context.getTranslated("sklep.list.empty"));
            return;
        }
        new ClassicEmbedPaginator(eventWaiter, strony, context.getSender(),
                context.getLanguage(), context.getTlumaczenia(), eventBus).create(hook);
    }
}
