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

package pl.fratik.tags;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.webhook.WebhookManager;
import pl.fratik.tags.commands.TagCommand;
import pl.fratik.tags.entity.TagsDao;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class Module implements Modul {
    public static final int MAX_TAG_NAME_LENGTH = 32;

    @Inject private EventBus eventBus;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private EventWaiter eventWaiter;
    @Inject private NewManagerKomend managerKomend;
    @Inject private ShardManager shardManager;
    @Inject private ManagerModulow managerModulow;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private WebhookManager webhookManager;
    @Inject private RedisCacheManager redisCacheManager;

    private TagsDao tagsDao;
    private TagsManager tagsManager;
    private ArrayList<NewCommand> commands;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        tagsDao = new TagsDao(managerBazyDanych, eventBus);
        tagsManager = new TagsManager(tagsDao, managerKomend, shardManager, tlumaczenia, redisCacheManager);
        commands = new ArrayList<>();

        commands.add(new TagCommand(tagsDao, eventWaiter, eventBus, tagsManager));

        managerKomend.registerCommands(this, commands);
        eventBus.register(tagsManager);
        return true;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
        eventBus.unregister(tagsManager);
        return true;
    }
}
