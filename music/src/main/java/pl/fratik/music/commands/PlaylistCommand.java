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

package pl.fratik.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.ArgsMissingException;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.StringUtil;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.entity.PiosenkaImpl;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.QueueDao;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.util.ArrayList;
import java.util.List;

public class PlaylistCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final QueueDao queueDao;

    public PlaylistCommand(NowyManagerMuzyki managerMuzyki, QueueDao queueDao) {
        this.managerMuzyki = managerMuzyki;
        this.queueDao = queueDao;
        name = "playlist";
        requireConnection = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }

    @SubCommand(name="save")
    public boolean save(CommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        Queue kolejka = new Queue(StringUtil.generateId(22, false, true, true));
        if (queueDao.get(kolejka.getId()) != null) return save(context);
        kolejka.setPiosenki(mms.getKolejka());
        queueDao.save(kolejka);
        context.send(context.getTranslated("playlist.saved", kolejka.getId()));
        return true;
    }

    @SubCommand(name="load")
    public boolean load(CommandContext context) {
        Object[] args;
        try {
            args = new Uzycie("playlistId", "string", true).resolveArgs(context);
        } catch (ArgsMissingException e) {
            CommonErrors.usage(context);
            return false;
        }
        String id = (String) args[0];
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        Queue q = queueDao.get(id);
        if (q == null || q.getPiosenki().isEmpty()) {
            context.send(context.getTranslated("playlist.load.notfound"));
            return false;
        }
        List<Piosenka> piosenki = new ArrayList<>();
        for (Piosenka p : q.getPiosenki()) {
            piosenki.add(new PiosenkaImpl(p.getRequester() + " " + context.getTranslated("playlist.added.by",
                    context.getSender().getAsTag()), p.getAudioTrack(), context.getLanguage()));
        }
        q.setPiosenki(piosenki);
        context.send(context.getTranslated("playlist.loaded"));
        mms.loadQueue(q);
        mms.skip();
        return true;
    }
}
