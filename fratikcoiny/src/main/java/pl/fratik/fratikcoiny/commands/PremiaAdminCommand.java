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

package pl.fratik.fratikcoiny.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.EventWaiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PremiaAdminCommand extends MoneyCommand {
    private final GuildDao guildDao;
    private final MemberDao memberDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public PremiaAdminCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "premia_admin";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES, Permission.MANAGE_SERVER);
    }

    @SubCommand(name = "ustaw", usage = "<rola:role> <coiny:int> <cooldown:string>")
    public void set(@NotNull NewCommandContext context) {
        context.defer(false);
        try {
            Role role = context.getArguments().get("rola").getAsRole();
            Long fc = context.getArguments().get("coiny").getAsLong();
            String cooldown = context.getArguments().get("cooldown").getAsString();
            DurationUtil.Response response = DurationUtil.parseDuration(cooldown);
            GuildConfig gc = guildDao.get(context.getGuild());
            int minut = Math.toIntExact(TimeUnit.MINUTES.convert(response.getDuration(), TimeUnit.MILLISECONDS));
            if (minut < 5) {
                context.sendMessage(context.getTranslated("premia.set.cooldown"));
                return;
            }
            if (fc < 0 || fc > 1_000_000_000L) {
                context.sendMessage(context.getTranslated("premia.set.limit"));
                return;
            }
            gc.getWyplaty().put(role.getId(), new GuildConfig.Wyplata(fc, minut));
            guildDao.save(gc);
            context.sendMessage(context.getTranslated("premia.set.success"));
        } catch (IllegalArgumentException | ArithmeticException e) {
            context.sendMessage(context.getTranslated("sklep.kup.failed"));
        }
    }

    @SubCommand(name = "usun", usage = "<rola:role>")
    public void remove(@NotNull NewCommandContext context) {
        context.defer(false);
        try {
            Role role = context.getArguments().get("rola").getAsRole();
            GuildConfig gc = guildDao.get(context.getGuild());
            if (gc.getWyplaty().remove(role.getId()) == null) {
                context.sendMessage(context.getTranslated("premia.unset.null"));
                return;
            }
            guildDao.save(gc);
            context.sendMessage(context.getTranslated("premia.unset.success"));
        } catch (IllegalArgumentException | ArithmeticException e) {
            context.sendMessage(context.getTranslated("sklep.kup.failed"));
        }
    }

    @SubCommand(name = "lista")
    public void list(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        GuildConfig gc = guildDao.get(context.getGuild());
        List<EmbedBuilder> pages = new ArrayList<>();
        //noinspection ConstantConditions
        gc.getWyplaty().entrySet().stream().filter(e -> context.getGuild().getRoleById(e.getKey()) != null)
                .sorted((a, b) -> context.getGuild().getRoleById(a.getKey()).getName()
                        .compareToIgnoreCase(context.getGuild().getRoleById(b.getKey()).getName()))
                .forEachOrdered(e -> {
            Role role = context.getGuild().getRoleById(e.getKey());
            if (role == null) return; // filter wyżej sprawdza, ale potem IDE się pluje więc niech zostanie
            GuildConfig.Wyplata wyplata = e.getValue();
            pages.add(new EmbedBuilder()
                    .setColor(role.getColorRaw())
                    .setTitle(role.getName())
                    .addField(context.getTranslated("premia.list.kwota"), String.valueOf(wyplata.getKwota()), false)
                    .addField(context.getTranslated("premia.list.cooldown"),
                            DurationUtil.humanReadableFormat((long) wyplata.getCooldown() * 60 * 1000, false), false)
            );
        });
        if (pages.isEmpty()) {
            context.sendMessage(context.getTranslated("premia.list.empty"));
            return;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(hook);
    }
}
