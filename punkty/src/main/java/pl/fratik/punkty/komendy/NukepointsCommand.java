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

package pl.fratik.punkty.komendy;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.ReactionWaiter;
import pl.fratik.punkty.LicznikPunktow;
import pl.fratik.punkty.entity.PunktyDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class NukepointsCommand extends Command {

    private final EventWaiter eventWaiter;
    private final GuildDao guildDao;
    private final LicznikPunktow licznikPunktow;
    private final PunktyDao punktyDao;
    private final EventBus eventBus;

    private static final String POTW = "\u2705";
    private static final String ODRZ = "\u274c";

    public NukepointsCommand(EventWaiter eventWaiter, GuildDao guildDao, LicznikPunktow licznikPunktow, PunktyDao punktyDao, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.guildDao = guildDao;
        this.licznikPunktow = licznikPunktow;
        this.punktyDao = punktyDao;
        this.eventBus = eventBus;
        name = "nukepoints";
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        permissions.add(Permission.MESSAGE_ADD_REACTION);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message msg = context.getChannel().sendMessage(context.getTranslated("nukepoints.warning")).complete();
        msg.addReaction(POTW).queue();
        msg.addReaction(ODRZ).queue();
        ReactionWaiter rw = new ReactionWaiter(eventWaiter, context) {
            @Override
            protected boolean checkReaction(MessageReactionAddEvent event) {
                return super.checkReaction(event) && !event.getReactionEmote().isEmote() &&
                        (event.getReactionEmote().getName().equals(POTW) ||
                                event.getReactionEmote().getName().equals(ODRZ));
            }
        };
        Runnable anuluj = () -> context.send(context.getTranslated("nukepoints.cancel"));
        rw.setReactionHandler(e -> {
            msg.editMessage(context.getTranslated("nukepoints.nuking")).queue();
            if (e.getReactionEmote().getName().equals(POTW)) {
                licznikPunktow.emptyCache();
                licznikPunktow.setLock(true);
                List<Future<?>> futures = new ArrayList<>();
                for (GuildConfig gc : guildDao.getAll()) {
                    Guild guild = context.getShardManager().getGuildById(gc.getGuildId());
                    if (guild == null) continue;
                    for (Member mem : guild.getMembers()) {
                        List<Role> rolesToRemove = new ArrayList<>();
                        for (String rId : gc.getRoleZaPoziomy().values()) {
                            if (rId == null || rId.isEmpty()) continue;
                            Role rola = guild.getRoleById(rId);
                            if (mem.getRoles().contains(rola)) rolesToRemove.add(rola);
                        }
                        futures.add(guild.modifyMemberRoles(mem, new ArrayList<>(), rolesToRemove).submit());
                    }
                }
                Future ft = new FutureTask<>(() -> punktyDao.getAll().forEach(punktyDao::delete), Void.TYPE);
                ((FutureTask) ft).run();
                futures.add(ft);
                do {
                    long gotowe = futures.stream().filter(Future::isDone).count();
                    long doZrobienia = futures.size();
                    eventBus.post(new PluginMessageEvent("punkty", "moderation", "znaneAkcje-add:" + msg.getId()));
                    msg.editMessage(context.getTranslated("nukepoints.nuking.progress", gotowe, doZrobienia)).queue();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                } while (futures.stream().filter(Future::isDone).count() != futures.size());
                context.send(context.getTranslated("nukepoints.success"));
            }
            if (e.getReactionEmote().getName().equals(ODRZ)) {
                anuluj.run();
            }
        });
        rw.setTimeoutHandler(anuluj);
        rw.create();
        return true;
    }

}
