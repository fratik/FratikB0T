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

package pl.fratik.punkty.komendy;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;
import pl.fratik.punkty.entity.PunktyDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class NukepointsCommand extends NewCommand {

    private final EventWaiter eventWaiter;
    private final GuildDao guildDao;
    private final LicznikPunktow licznikPunktow;
    private final PunktyDao punktyDao;

    public NukepointsCommand(EventWaiter eventWaiter, GuildDao guildDao, LicznikPunktow licznikPunktow, PunktyDao punktyDao) {
        this.eventWaiter = eventWaiter;
        this.guildDao = guildDao;
        this.licznikPunktow = licznikPunktow;
        this.punktyDao = punktyDao;
        name = "nukepoints";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String content = context.getTranslated("nukepoints.warning");
        MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
        msgBuilder.setContent(content);
        msgBuilder.setComponents(ActionRow.of(
            Button.danger("YES", context.getTranslated("generic.yes")),
            Button.secondary("NO", context.getTranslated("generic.no"))
        ));
        InteractionHook hook = context.reply(msgBuilder.build());
        ButtonWaiter rw = new ButtonWaiter(eventWaiter, context, hook.getInteraction(), ButtonWaiter.ResponseType.REPLY);
        rw.setButtonHandler(e -> new Thread(() -> {
            hook.editOriginal(content).setComponents(Collections.emptySet()).queue();
            if (e.getComponentId().equals("YES")) {
                licznikPunktow.emptyCache();
                licznikPunktow.setLock(true);
                List<Future<?>> futures = new ArrayList<>();
                for (GuildConfig gc : guildDao.getAll()) {
                    Guild guild = context.getShardManager().getGuildById(gc.getGuildId());
                    if (guild == null) continue;
                    for (Member mem : guild.loadMembers().get()) {
                        List<Role> rolesToRemove = new ArrayList<>();
                        for (String rId : gc.getRoleZaPoziomy().values()) {
                            if (rId == null || rId.isEmpty()) continue;
                            Role rola = guild.getRoleById(rId);
                            if (mem.getRoles().contains(rola)) rolesToRemove.add(rola);
                        }
                        futures.add(guild.modifyMemberRoles(mem, new ArrayList<>(), rolesToRemove).submit());
                    }
                }
                FutureTask<?> ft = new FutureTask<>(() -> punktyDao.getAll().forEach(punktyDao::delete), Void.TYPE);
                ft.run();
                futures.add(ft);
                do {
                    long gotowe = futures.stream().filter(Future::isDone).count();
                    long doZrobienia = futures.size();
                    e.getHook().editOriginal(context.getTranslated("nukepoints.nuking.progress", gotowe, doZrobienia)).queue();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                } while (futures.stream().filter(Future::isDone).count() != futures.size());
                e.getHook().editOriginal(context.getTranslated("nukepoints.success")).queue();
            }
            if (e.getComponentId().equals("NO"))
                e.getHook().editOriginal(context.getTranslated("nukepoints.cancel")).queue();
        }, "nukepoints-runner").start());
        rw.setTimeoutHandler(() -> {
            hook.editOriginal(context.getTranslated("nukepoints.cancel")).setComponents(Collections.emptySet()).queue();
        });
        rw.create();
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return false;
        }
        return true;
    }
}
