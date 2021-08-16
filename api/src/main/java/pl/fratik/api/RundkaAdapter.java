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

package pl.fratik.api;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.api.entity.RundkaOdpowiedz;
import pl.fratik.api.entity.RundkaOdpowiedzFull;
import pl.fratik.api.entity.RundkaOdpowiedzSanitized;
import pl.fratik.api.event.RundkaAnswerVoteEvent;
import pl.fratik.api.event.RundkaEndEvent;
import pl.fratik.api.event.RundkaNewAnswerEvent;
import pl.fratik.api.event.RundkaStartEvent;
import pl.fratik.core.util.UserUtil;

import java.util.HashSet;
import java.util.Set;

public class RundkaAdapter implements SocketAdapter {
    private final Set<SocketManager.Connection> subscribedConnections = new HashSet<>();
    private final ShardManager shardManager;

    public RundkaAdapter(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public String getChannelName() {
        return "rundka";
    }

    @Override
    public void subscribe(SocketManager.Connection connection) throws RegisterException {
        if (connection.getAuthenticatedUser() == null)
            throw new RegisterException(Exceptions.Codes.WS_NOT_LOGGED_IN);
        if (!RundkaCommand.isRundkaOn())
            throw new RegisterException(Exceptions.Codes.NO_RUNDKA);
        subscribedConnections.add(connection);
    }

    @Override
    public void unsubscribe(SocketManager.Connection connection) {
        subscribedConnections.remove(connection);
    }

    @Subscribe
    public void onRundkaStart(RundkaStartEvent e) {
        for (SocketManager.Connection con : subscribedConnections) {
            con.sendMessage(getChannelName(), "start", null);
        }
    }

    @Subscribe
    public void onRundkaEnd(RundkaEndEvent e) {
        for (SocketManager.Connection con : subscribedConnections) {
            con.sendMessage(getChannelName(), "end", null);
        }
    }

    @Subscribe
    public void onRundkaAnswer(final RundkaNewAnswerEvent e) {
        for (SocketManager.Connection con : subscribedConnections) {
            User user = con.getAuthenticatedUser();
            RundkaOdpowiedz odp = e.getOdpowiedz();
            if (odp instanceof RundkaOdpowiedzFull)
                odp = new RundkaOdpowiedzSanitized((RundkaOdpowiedzFull) odp, odp.getUserId().equals(user.getId()) ||
                                UserUtil.isStaff(user, shardManager), shardManager);
            con.sendMessage(getChannelName(), "answer", odp);
        }
    }

    @Subscribe
    public void onRundkaAnswerVote(final RundkaAnswerVoteEvent e) {
        for (SocketManager.Connection con : subscribedConnections) {
            User user = con.getAuthenticatedUser();
            if (!user.getId().equals(e.getOdpowiedz().getUserId()) && !UserUtil.isStaff(user, shardManager))
                continue;
            RundkaOdpowiedz odp = e.getOdpowiedz();
            if (odp instanceof RundkaOdpowiedzFull)
                odp = new RundkaOdpowiedzSanitized((RundkaOdpowiedzFull) odp,
                        odp.getUserId().equals(user.getId()) || UserUtil.isStaff(user, shardManager), shardManager);
            con.sendMessage(getChannelName(), "vote", odp);
        }
    }
}
