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

package pl.fratik.dev.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Arrays;

public class EvalCommand extends NewCommand {

    private final Logger logger = LoggerFactory.getLogger(EvalCommand.class);
    private final NewManagerKomend managerKomend;
    private final ManagerArgumentow managerArgumentow;
    private final ManagerBazyDanych managerBazyDanych;
    private final ManagerModulow managerModulow;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final MemberDao memberDao;

    public EvalCommand(NewManagerKomend managerKomend, ManagerArgumentow managerArgumentow, ManagerBazyDanych managerBazyDanych, ManagerModulow managerModulow, ShardManager shardManager, Tlumaczenia tlumaczenia, GuildDao guildDao, UserDao userDao, MemberDao memberDao) {
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
        usage = "<pokaz:bool> <kod:string>";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        String code = context.getArguments().get("kod").getAsString();
        boolean ephemeral = !context.getArguments().get("pokaz").getAsBoolean();
        EmbedBuilder ebStart = new EmbedBuilder();
        ebStart.setColor(Color.YELLOW);
        ebStart.addField("\ud83d\udce4 INPUT", codeBlock("js", code), false);
        ebStart.addField("\ud83d\udce5 OUPTUT", "Oczekiwanie...", false);
        InteractionHook hook = ephemeral ? context.replyEphemeral(ebStart.build()) : context.reply(ebStart.build());
        try {
            Context ctx = Context.enter();
            ctx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject scr = ctx.initStandardObjects();
            int flags = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
            scr.defineProperty("logger", logger, flags);
            scr.defineProperty("managerKomend", managerKomend, flags);
            scr.defineProperty("managerArgumentow", managerArgumentow, flags);
            scr.defineProperty("managerBazyDanych", managerBazyDanych, flags);
            scr.defineProperty("managerModulow", managerModulow, flags);
            scr.defineProperty("shardManager", shardManager, flags);
            scr.defineProperty("tlumaczenia", tlumaczenia, flags);
            scr.defineProperty("guildDao", guildDao, flags);
            scr.defineProperty("userDao", userDao, flags);
            scr.defineProperty("memberDao", memberDao, flags);
            scr.defineProperty("context", context, flags);
            @Nullable Object o;
            Script eval = ctx.compileString(code, "<eval>", 1, null);
            o = eval.exec(ctx, scr);
            String e;
            if (o instanceof NativeArray) e = Arrays.toString(((NativeArray) o).toArray());
            else if (o instanceof NativeJavaArray) {
                NativeJavaArray nja = (NativeJavaArray) o;
                if (nja.unwrap() instanceof long[]) e = Arrays.toString((long[]) nja.unwrap());
                else if (nja.unwrap() instanceof int[]) e = Arrays.toString((int[]) nja.unwrap());
                else if (nja.unwrap() instanceof short[]) e = Arrays.toString((short[]) nja.unwrap());
                else if (nja.unwrap() instanceof char[]) e = Arrays.toString((char[]) nja.unwrap());
                else if (nja.unwrap() instanceof byte[]) e = Arrays.toString((byte[]) nja.unwrap());
                else if (nja.unwrap() instanceof boolean[]) e = Arrays.toString((boolean[]) nja.unwrap());
                else if (nja.unwrap() instanceof float[]) e = Arrays.toString((float[]) nja.unwrap());
                else if (nja.unwrap() instanceof double[]) e = Arrays.toString((double[]) nja.unwrap());
                else e = toString((Object[]) nja.unwrap());
            }
            else if (o != null && o.getClass().isArray()) {
                if (o instanceof long[]) e = Arrays.toString((long[]) o);
                else if (o instanceof int[]) e = Arrays.toString((int[]) o);
                else if (o instanceof short[]) e = Arrays.toString((short[]) o);
                else if (o instanceof char[]) e = Arrays.toString((char[]) o);
                else if (o instanceof byte[]) e = Arrays.toString((byte[]) o);
                else if (o instanceof boolean[]) e = Arrays.toString((boolean[]) o);
                else if (o instanceof float[]) e = Arrays.toString((float[]) o);
                else if (o instanceof double[]) e = Arrays.toString((double[]) o);
                else e = toString((Object[]) o);
            }
            else e = Undefined.isUndefined(o) || o == null ? "undefined" : (String) Context.jsToJava(o, String.class);
//            if (babelEnabled && e.equals("use strict")) e = "null";
            if (e.length() > 1000) e = e.substring(0, 1000);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.GREEN);
            eb.addField("\ud83d\udce4 INPUT", codeBlock("js", code), false);
            eb.addField("\ud83d\udce5 OUTPUT", codeBlock(e), false);
            hook.editOriginalEmbeds(eb.build()).queue();
        } catch (Exception e) {
            logger.error("Eval error:", e);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.RED);
            eb.addField("\ud83d\udce4 INPUT", codeBlock("js", code), false);
            eb.addField("\u2620\ufe0f ERROR", codeBlock(e.toString()), false);
            hook.editOriginalEmbeds(eb.build()).queue();
        }
    }

    private String toString(Object[] arr) {
        Object[] arr2 = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) arr2[i] = Context.jsToJava(arr[i], String.class);
        return Arrays.toString(arr2);
    }

    private String codeBlock(String text) {
        return codeBlock("", text);
    }

    private String codeBlock(String code, String text) {
        return "```" + code + "\n" + text.replace("`", "\u200b`\u200b") + "```";
    }

}
