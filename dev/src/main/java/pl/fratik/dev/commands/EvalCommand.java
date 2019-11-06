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

package pl.fratik.dev.commands;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import javax.annotation.Nullable;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import java.awt.*;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import static pl.fratik.core.manager.implementation.ManagerModulowImpl.moduleClassLoader;

public class EvalCommand extends Command {

    private final Logger logger = LoggerFactory.getLogger(EvalCommand.class);
    private final ManagerKomend managerKomend;
    private final ManagerArgumentow managerArgumentow;
    private final ManagerBazyDanych managerBazyDanych;
    private final ManagerModulow managerModulow;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final MemberDao memberDao;

    private ScriptEngine engine;
    private boolean lock;
    private boolean babelEnabled;

    public EvalCommand(ManagerKomend managerKomend, ManagerArgumentow managerArgumentow, ManagerBazyDanych managerBazyDanych, ManagerModulow managerModulow, ShardManager shardManager, Tlumaczenia tlumaczenia, GuildDao guildDao, UserDao userDao, MemberDao memberDao) {
        this.managerKomend = managerKomend;
        this.managerArgumentow = managerArgumentow;
        this.managerBazyDanych = managerBazyDanych;
        this.managerModulow = managerModulow;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.guildDao = guildDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
        name = "eval";
        uzycie = new Uzycie("code", "string", true);
        type = CommandType.DEBUG;
        category = CommandCategory.SYSTEM;
        permLevel = PermLevel.BOTOWNER;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        Thread sEngineThread = new Thread(this::setupEngine);
        sEngineThread.setName("Setup Engine Thread");
        sEngineThread.start();
        aliases = new String[] {"ev"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder ebStart = new EmbedBuilder();
        ebStart.setColor(Color.YELLOW);
        ebStart.addField("\ud83d\udce4 INPUT", codeBlock("js", (String) context.getArgs()[0]), false);
        ebStart.addField("\ud83d\udce5 OUPTUT", "Oczekiwanie...", false);
        Message message = context.send(ebStart.build());
        try {
            if (lock) {
                message.editMessage("Poczekaj chwilkę, silnik się ładuje..").queue();
                while (lock) Thread.sleep(100);
            }
            if (engine == null) {
                message.editMessage("Silnik JS nie został załadowany, proszę czekać dłużej \\o/").queue();
                setupEngine();
            }
            engine.put("logger", logger);
            engine.put("jda", context.getEvent().getJDA());
            engine.put("context", context);
            engine.put("managerKomend", managerKomend);
            engine.put("managerArgumentow", managerArgumentow);
            engine.put("managerBazyDanych", managerBazyDanych);
            engine.put("managerModulow", managerModulow);
            engine.put("shardManager", shardManager);
            engine.put("tlumaczenia", tlumaczenia);
            engine.put("guildDao", guildDao);
            engine.put("userDao", userDao);
            engine.put("memberDao", memberDao);
            @Nullable Object o;
            if (babelEnabled) {
                engine.put("input", context.getArgs()[0]);
                String s = engine.eval("Babel.transform(input, { presets: ['es2015'] }).code").toString();
                o = engine.eval(s);
            } else {
                o = engine.eval((String) context.getArgs()[0]);
            }
            String e;
            if (o != null && o.getClass().isArray()) e = Arrays.toString((Object[]) o);
            else e = o == null ? "null" : o.toString();
            if (babelEnabled && e.equals("use strict")) e = "null";
            if (e.length() > 1000) e = e.substring(1000);
            if (context.checkSensitive(e)) {
                logger.info("Output evala:");
                logger.info(e);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.GREEN);
                eb.addField("\ud83d\udce4 INPUT", codeBlock("js", (String) context.getArgs()[0]), false);
                eb.addField("\ud83d\udce5 OUTPUT", "Output evala został ukryty bo zawiera prywatne " +
                        "dane: sprawdź konsolę!", false);
                message.editMessage(eb.build()).override(true).queue();
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.GREEN);
                eb.addField("\ud83d\udce4 INPUT", codeBlock("js", (String) context.getArgs()[0]), false);
                eb.addField("\ud83d\udce5 OUTPUT", codeBlock(e), false);
                message.editMessage(eb.build()).override(true).queue();
            }
        } catch (Exception e) {
            logger.error("Eval error:", e);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.RED);
            eb.addField("\ud83d\udce4 INPUT", codeBlock("js", (String) context.getArgs()[0]), false);
            eb.addField("\u2620\ufe0f ERROR", codeBlock(e.toString()), false);
            message.editMessage(eb.build()).override(true).queue();
        }
        return true;
    }

    private String codeBlock(String text) {
        return codeBlock("", text);
    }

    private String codeBlock(String code, String text) {
        return "```" + code + "\n" + text.replaceAll("`", "\u200b`\u200b") + "```";
    }

    private void setupEngine() {
        lock = true;
        engine = new NashornScriptEngineFactory().getScriptEngine(moduleClassLoader);

        if (getClass().getResource("/babel.min.js") != null) {
            logger.info("Loading Babel...");

            try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/babel.min.js"))) {
                engine.put("logger", logger);
                CompiledScript compiled = ((Compilable) engine).compile(r);
                compiled.eval();
                babelEnabled = true;
            } catch (Exception e) {
                logger.error("Error loading Babel!", e);
            }
            logger.info("Loaded Babel!");
        }

        if (getClass().getResource("/polyfill.min.js") != null) {
            logger.info("Loading Babel Polyfill...");

            try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/polyfill.min.js"))) {
                engine.put("logger", logger);
                CompiledScript compiled = ((Compilable) engine).compile(r);
                compiled.eval();
                babelEnabled = true;
            } catch (Exception e) {
                logger.error("Error loading Babel Polyfill!", e);
            }
            logger.info("Loaded Babel Polyfill!");
        }
        lock = false;
    }

}
