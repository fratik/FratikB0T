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

package pl.fratik.commands;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.commands.entity.BlacklistDao;
import pl.fratik.commands.entity.PrivDao;
import pl.fratik.commands.images.*;
import pl.fratik.commands.narzedzia.*;
import pl.fratik.commands.system.*;
import pl.fratik.commands.zabawa.*;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.webhook.WebhookManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class Module implements Modul {
    @Inject private NewManagerKomend managerKomend;
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
    @Inject private RedisCacheManager redisCacheManager;
    @Inject private WebhookManager webhookManager;
    private ArrayList<NewCommand> commands;

    private MemberListener listener;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        PrivDao privDao = new PrivDao(managerBazyDanych, eventBus);
        BlacklistDao blacklistDao = new BlacklistDao(managerBazyDanych, eventBus);

        commands = new ArrayList<>();

        commands.add(new PingCommand());
//        commands.add(new HelpCommand(managerKomend, guildDao, shardManager, redisCacheManager));
//        commands.add(new LanguageCommand(eventBus, userDao, tlumaczenia));
        commands.add(new UstawieniaCommand(userDao, eventWaiter));
//        commands.add(new PoziomCommand(guildDao, shardManager));
        commands.add(new BotstatsCommand(shardManager, managerModulow));
        if (Ustawienia.instance.apiUrls.get("image-server") != null && Ustawienia.instance.apiKeys.get("image-server") != null) {
            Set<NewCommand> graficzne = new HashSet<>();
            graficzne.add(new BlurpleCommand());
            graficzne.add(new StarcatchCommand());
            graficzne.add(new HugCommand());
            graficzne.add(new GraficznaCommand("startouch", "/api/image/startouch", "avatarURL", false));
            graficzne.add(new GraficznaCommand("slap", "/api/image/slap", "avatarURL", true));
            graficzne.add(new GraficznaCommand("rip", "/api/image/rip", "avatarURL", false));
            graficzne.add(new GraficznaCommand("sleep", "/api/image/sleep", "avatarURL", false));
            graficzne.add(new GraficznaCommand("wanted", "/api/image/wanted", "avatarURL", false));
//            graficzne.add(new GraficznaCommand("wave", "/api/image/wave", "avatarURL"))
            graficzne.add(new GraficznaCommand("tapeta", "/api/image/tapeta", false));
            graficzne.add(new GraficznaCommand("roksana", "/api/image/roksana", "avatarURL", false));
            graficzne.add(new GraficznaCommand("debilizm", "/api/image/debilizm", "avatarURL", false));
            graficzne.add(new GraficznaCommand("bog", "/api/image/god", "avatarURL", false));
            graficzne.add(new EatCommand());
            graficzne.add(new BigemojiCommand());
            graficzne.add(new ChainCommand());
            commands.add(new GraficznaWrapper(graficzne));
        }
        commands.add(new OgloszenieCommand(shardManager, guildDao, eventBus, tlumaczenia, redisCacheManager));
        commands.add(new ServerinfoCommand(userDao, eventBus));
        commands.add(new UserinfoCommand(userDao, shardManager, eventBus));
        commands.add(new KolorCommand());
        commands.add(new DegradCommand(shardManager));
//        commands.add(new CytujCommand(shardManager, eventBus, webhookManager, guildDao, userDao, tlumaczenia, redisCacheManager));
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
//        commands.add(new BoomCommand(eventWaiter, userDao, redisCacheManager));
//        commands.add(new PomocCommand());
        PopCommand pop = new PopCommand(shardManager, eventWaiter, eventBus, tlumaczenia, blacklistDao, redisCacheManager);
        commands.add(pop);
        commands.add(new PopAdminCommand(pop));
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
        commands.add(new AchievementCommand(tlumaczenia));
        commands.add(new EmojifyCommand());
        commands.add(new SprawdzuprawnieniaCommand());
        commands.add(new UstawPowitanieCommand(guildDao));
        commands.add(new UstawPozegnanieCommand(guildDao));
        commands.add(new GlobaladminiCommand(eventWaiter, eventBus));
        commands.add(new PasswordCommand());
        if (Ustawienia.instance.apiKeys.get("hypixelToken") != null) {
            commands.add(new HypixelCommand(redisCacheManager));
        }
        commands.add(new McstatusCommand());
        commands.add(new SelfieCommand());
        commands.add(new EmojiInfoCommand());
        if (Ustawienia.instance.apiKeys.get("osu") != null)
            commands.add(new OsuCommand(shardManager, eventWaiter, eventBus));
//        commands.add(new Rule34Command(eventWaiter, eventBus));
        commands.add(new CoronastatsCommand(eventWaiter, eventBus));
//        commands.add(new UstawPoziomCommand(guildDao, managerKomend));
//        commands.add(new PoziomyUprawnienCommand());
        commands.add(new BlacklistPopCommand(blacklistDao));
        commands.add(new ShipCommand());
//        commands.add(new AdministratorzyCommand(guildDao, redisCacheManager));
        commands.add(new SasinCommand());

        listener = new MemberListener(guildDao, eventBus, redisCacheManager);
        eventBus.register(listener);

        managerKomend.registerCommands(this, commands);

        return true;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
        eventBus.unregister(listener);
        return true;
    }
}
