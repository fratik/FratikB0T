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

package pl.fratik.commands;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.commands.entity.PrivDao;
import pl.fratik.commands.narzedzia.*;
import pl.fratik.commands.system.*;
import pl.fratik.commands.zabawa.*;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private UserDao userDao;
    @Inject private GbanDao gbanDao;
    @Inject private ScheduleDao scheduleDao;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ShardManager shardManager;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerModulow managerModulow;
    @Inject private EventBus eventBus;
    private ArrayList<Command> commands;

    private MemberListener listener;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        PrivDao privDao = new PrivDao(managerBazyDanych, eventBus);

        commands = new ArrayList<>();

        commands.add(new PingCommand());
        commands.add(new HelpCommand(managerKomend, guildDao, shardManager));
        commands.add(new LanguageCommand(userDao));
        commands.add(new UstawieniaCommand(eventWaiter, userDao, guildDao, managerArgumentow, shardManager, tlumaczenia));
        commands.add(new PoziomCommand(guildDao, shardManager));
        commands.add(new BotstatsCommand(shardManager, managerModulow));
        if (Ustawienia.instance.apiUrls.get("image-server") != null && Ustawienia.instance.apiKeys.get("image-server") != null) {
            commands.add(new GraficznaCommand("blurple", "/api/image/blurple", "avatarURL", false));
            commands.add(new StarcatchCommand());
            commands.add(new HugCommand());
            commands.add(new GraficznaCommand("startouch", "/api/image/startouch", "avatarURL", false));
            commands.add(new GraficznaCommand("facepalm", "/api/image/facepalm", "avatarURL", false));
            commands.add(new GraficznaCommand("slap", "/api/image/slap", "avatarURL", true));
            commands.add(new GraficznaCommand("rip", "/api/image/rip", "avatarURL", false));
            commands.add(new GraficznaCommand("sleep", "/api/image/sleep", "avatarURL", false));
            commands.add(new GraficznaCommand("wanted", "/api/image/wanted", "avatarURL", false));
//            commands.add(new GraficznaCommand("wave", "/api/image/wave", "avatarURL"))
            commands.add(new GraficznaCommand("tapeta", "/api/image/tapeta", false));
            commands.add(new GraficznaCommand("roksana", "/api/image/roksana", "avatarURL", false));
            commands.add(new GraficznaCommand("debilizm", "/api/image/debilizm", "avatarURL", false));
            commands.add(new GraficznaCommand("god", "/api/image/god", "avatarURL", false));
            commands.add(new EatCommand());
            commands.add(new BigemojiCommand());
            commands.add(new ChainCommand());
        }
        commands.add(new OgloszenieCommand(shardManager, guildDao, eventBus, tlumaczenia, managerKomend));
        commands.add(new ServerinfoCommand(userDao, eventBus));
        commands.add(new UserinfoCommand(userDao, shardManager, eventBus));
        commands.add(new KolorCommand());
        commands.add(new DegradCommand(shardManager));
        commands.add(new CytujCommand(shardManager, eventBus));
        commands.add(new PogodaCommand(userDao));
        commands.add(new McpremiumCommand());
        commands.add(new InviteCommand());
        commands.add(new GbanCommand(gbanDao, managerArgumentow));
        commands.add(new GbanlistCommand(gbanDao, eventWaiter, eventBus));
        commands.add(new UngbanCommand(gbanDao, managerArgumentow));
        commands.add(new AvatarCommand());
        commands.add(new MemeCommand());
        commands.add(new DashboardCommand());
        commands.add(new DonateCommand());
        commands.add(new OpuscCommand());
        commands.add(new BoomCommand(eventWaiter, eventBus, userDao));
        commands.add(new PomocCommand());
        commands.add(new PopCommand(shardManager, guildDao, eventWaiter, eventBus, tlumaczenia));
        commands.add(new PowiadomOPomocyCommand(shardManager));
        commands.add(new OsiemBallCommand());
        commands.add(new ChooseCommand());
        commands.add(new RzutKosciaCommand());
        commands.add(new RzutMonetaCommand());
        commands.add(new RemindCommand(scheduleDao, eventWaiter, eventBus));
        commands.add(new PrivCommand(privDao, userDao));
        commands.add(new ZglosPrivCommand(privDao, eventWaiter, eventBus, shardManager, tlumaczenia));
        commands.add(new IgnoreCommand(userDao));
        commands.add(new BlacklistPrivCommand(userDao));
        commands.add(new AchievementCommand());
        commands.add(new EmojifyCommand());
        commands.add(new SprawdzuprawnieniaCommand());
        commands.add(new UstawPowitanieCommand(guildDao));
        commands.add(new UstawPozegnanieCommand(guildDao));
        commands.add(new GlobaladminiCommand(eventWaiter, eventBus));
        commands.add(new PasswordCommand());
        if (Ustawienia.instance.apiKeys.get("hypixelToken") != null) {
            commands.add(new HypixelCommand());
        }
        commands.add(new McstatusCommand());
        commands.add(new SelfieCommand());
        commands.add(new EmojiInfoCommand());
        commands.add(new OsuCommand(shardManager, eventWaiter, eventBus));
        commands.add(new Rule34Command(eventWaiter, eventBus, managerArgumentow));

        listener = new MemberListener(guildDao);
        eventBus.register(listener);

        commands.forEach(managerKomend::registerCommand);

        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        eventBus.unregister(listener);
        return true;
    }
}
