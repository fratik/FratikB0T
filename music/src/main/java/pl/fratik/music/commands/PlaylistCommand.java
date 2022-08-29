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

package pl.fratik.music.commands;

import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
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

    @SubCommand(name = "save")
    public void save(NewCommandContext context) {
        context.defer(true);
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        Queue kolejka;
        kolejka = new Queue(StringUtil.generateId(22, false, true, true));
        while (queueDao.get(kolejka.getId()) != null) {
            kolejka = new Queue(StringUtil.generateId(22, false, true, true));
        }
        kolejka.setPiosenki(mms.getKolejka());
        queueDao.save(kolejka);
        context.sendMessage(context.getTranslated("playlist.saved", kolejka.getId()));
    }

    @SubCommand(name = "load", usage = "<id:string>")
    public void load(NewCommandContext context) {
        String id = context.getArguments().get("id").getAsString();
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        context.defer(false);
        Queue q = queueDao.get(id);
        if (q == null || q.getPiosenki().isEmpty()) {
            context.sendMessage(context.getTranslated("playlist.load.notfound"));
            return;
        }
        List<Piosenka> piosenki = new ArrayList<>();
        for (Piosenka p : q.getPiosenki()) {
            piosenki.add(new PiosenkaImpl(p.getRequester() + " " + context.getTranslated("playlist.added.by",
                    context.getSender().getAsTag()), p.getAudioTrack(), context.getLanguage()));
        }
        q.setPiosenki(piosenki);
        context.sendMessage(context.getTranslated("playlist.loaded"));
        mms.loadQueue(q); // TODO: 04.12.2019 mozliwosc uzycia tego kiedy muzyka jest wyłączona
    }
}
