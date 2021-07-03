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

package pl.fratik.commands.util;

import com.google.common.annotations.Beta;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Beta // !! nie używać!!
public class AutoSettingsRenderer implements SettingsRenderer {

    private final EventWaiter eventWaiter;
    private final UserDao userDao;
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerArgumentow managerArgumentow;
    private final ShardManager shardManager;
    private final CommandContext ctx;

    private static final String LEFT_EMOJI = "\u25C0";
    private static final String RIGHT_EMOJI = "\u25B6";
    private int pageNo;
    private List<String> pages;
    private Message paginatingMessage;
    private Message activeMessage;

    private Map<Integer, Integer> roleZaPoziomyId = new HashMap<>();

    private final Message wiadomoscJezyki = null;
    private GuildConfig guildConfig;
    private UserConfig userConfig;
    private boolean koniecZara;
    private Map<Integer, Opcja> opcje = new HashMap<>();

    public AutoSettingsRenderer(EventWaiter eventWaiter, UserDao userDao, GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerArgumentow managerArgumentow, ShardManager shardManager, CommandContext ctx) {
        this.eventWaiter = eventWaiter;
        this.userDao = userDao;
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.managerArgumentow = managerArgumentow;
        this.shardManager = shardManager;
        this.ctx = ctx;
    }

    @Override
    public void create() {
        if (ctx.getGuild() != null) guildConfig = guildDao.get(ctx.getGuild());
        userConfig = userDao.get(ctx.getSender());
        if (ctx.getGuild() == null) renderUserConf();
        else glownyRender();
    }

    //#region internale
    private void invoker(Method toInvoke) {
        invoker(toInvoke, null, null);
    }

    private void invoker(Method toInvoke, Field f, MessageReceivedEvent ev) { // Skrócona metoda do inwokacji funkcji
        try {
            if (toInvoke.getParameterCount() == 0) toInvoke.invoke(this);
            else toInvoke.invoke(this, f, ev);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private void onTimeout(Message message) { // Po 30s braku odp.
        if (wiadomoscJezyki != null) wiadomoscJezyki.delete().queue();
        if (message != null) message.delete().queue();
        ctx.send(ctx.getTranslated("ustawienia.timeout"));
    }

    private void handler(MessageReceivedEvent event, Method originalFunction) { // Ogólny handler - decyduje co odpalić
        switch (event.getMessage().getContentRaw().trim()) {
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                for (Map.Entry<Integer, Opcja> opcja : opcje.entrySet()) {
                    if (!opcja.getKey().toString().equals(event.getMessage().getContentRaw().trim())) continue;
                    invoker(opcja.getValue().method, opcja.getValue().field, event);
                    return;
                }
                if (ctx.getMessage().getContentRaw().equals(event.getMessage().getContentRaw())) break;
                ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                if (!koniecZara) {
                    koniecZara = true;
                    invoker(originalFunction);
                }
                break;
        }
    }

    private void renderer(String content, Method callee) { // Skrócony renderer
        ctx.send(content, message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                handler(event, callee);
            });
            waiter.create();
        });
    }
    //#endregion internale

    private void glownyRender() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.description")).append("\n");
        builder.append("1. ").append(ctx.getTranslated("ustawienia.user.ustawienia")).append("\n");
        try {
            opcje.put(1, new Opcja(getClass().getDeclaredMethod("renderUserConf"), null));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        if (ctx.getGuild() != null && UserUtil.getPermlevel(ctx.getMember(), guildDao, shardManager).getNum() >= 3) {
            builder.append("2. ").append(ctx.getTranslated("ustawienia.server.ustawienia")).append("\n");
            try {
                opcje.put(2, new Opcja(getClass().getDeclaredMethod("renderGuildConf"), null));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        renderer(builder.toString(), Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(Thread.currentThread().getStackTrace()[1].getMethodName()))
                .findFirst().orElse(null));
    }

    private void renderUserConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.user.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.user.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.user.description")).append("\n");
        // ok i tu są jajca: loopujemy po fieldach by wygenerować wszystko
        int numerStrony = 1;
        opcje = new HashMap<>();
        for (Field f : UserConfig.class.getDeclaredFields()) {
            ConfigField ann = f.getDeclaredAnnotation(ConfigField.class);
            if (ann != null && ann.dontDisplayInSettings()) continue;
            Object value;
            try {
                StringBuilder methodName = new StringBuilder("get");
                boolean first = true;
                for (char ch : f.getName().toCharArray()) {
                    if (first) {
                        methodName.append(Character.toUpperCase(ch));
                        first = false;
                    }
                    else methodName.append(ch);
                }
                try {
                    value = UserConfig.class.getDeclaredMethod(methodName.toString()).invoke(userConfig);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            } catch (NoSuchMethodException e) {
                StringBuilder methodName = new StringBuilder("is");
                boolean first = true;
                for (char ch : f.getName().toCharArray()) {
                    if (first) {
                        methodName.append(Character.toUpperCase(ch));
                        first = false;
                    }
                    else methodName.append(ch);
                }
                try {
                    value = UserConfig.class.getDeclaredMethod(methodName.toString()).invoke(userConfig);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
                    throw new IllegalStateException(e1);
                }
            }
            Class<?> holds;
            holds = ann == null ? f.getType() : null;
            if (holds == null)
                holds = ann != null && ann.holdsEntity() == ConfigField.Entities.NULL ? f.getType() :
                        ConfigField.Entities.resolveEntity(Objects.requireNonNull(ann).holdsEntity());
            String rendered = renderField("user", f.getName(), value, f.getType(), Objects.requireNonNull(holds));
            builder.append(numerStrony).append(". ").append(rendered).append("\n");
            try {
                opcje.put(numerStrony, new Opcja(getClass().getDeclaredMethod("handleUserConf", Field.class, MessageReceivedEvent.class), f));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            numerStrony++;
        }
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        renderer(builder.toString(), Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(trace[1].getMethodName()
//                        .replaceAll("\\$\\d*", "")
                ))
                .findFirst().orElse(null));
    }

    private void renderGuildConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.server.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.server.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.server.description")).append("\n");
        // ok i tu są jajca: loopujemy po fieldach by wygenerować wszystko
        int numerStrony = 1;
        opcje = new HashMap<>();
        for (Field f : GuildConfig.class.getDeclaredFields()) {
            ConfigField ann = f.getDeclaredAnnotation(ConfigField.class);
            if (ann != null && ann.dontDisplayInSettings()) continue;
            Object value = GuildConfig.getValue(f, guildConfig);
            Class<?> holds;
            holds = ann == null ? f.getType() : null;
            if (holds == null)
                holds = ann != null && ann.holdsEntity() == ConfigField.Entities.NULL ? f.getType() :
                        ConfigField.Entities.resolveEntity(Objects.requireNonNull(ann).holdsEntity());
            String rendered;
            if (f.getName().equals("prefixes")) {
                rendered = renderField("server", f.getName(), value, f.getType(),
                        Objects.requireNonNull(holds), false, Ustawienia.instance.prefix);
            } else {
                rendered = renderField("server", f.getName(), value, f.getType(), Objects.requireNonNull(holds));
            }
            builder.append(numerStrony).append(". ").append(rendered).append("\n");
            try {
                opcje.put(numerStrony, new Opcja(getClass().getDeclaredMethod("handleGuildConf", Field.class, MessageReceivedEvent.class), f));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            numerStrony++;
        }
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```\n").append(ctx.getTranslated("ustawienia.betterver",
                Ustawienia.instance.botUrl + "/dashboard/" + ctx.getGuild().getId() + "/manage"));
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        renderer(builder.toString(), Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(trace[1].getMethodName()
//                        .replaceAll("\\$\\d*", "")
                ))
                .findFirst().orElse(null));
    }

    private void handleUserConf(Field f, MessageReceivedEvent e) {

    }

    private void handleGuildConf(Field f, MessageReceivedEvent e) {
        if (f.getType().equals(Boolean.class) || f.getType().equals(Boolean.TYPE)) {
            try {
                boolean kurwa;
                kurwa = (boolean) f.get(guildConfig);
                guildConfig.getClass().getDeclaredMethod(StringUtil.firstLetterUpperCase(f.getName()), f.getType()).invoke(guildConfig, kurwa);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } // TODO: 07.06.19 22:49 to wszystko
    }

    private String renderField(String context, String fieldName, Object value, @NotNull Class<?> valueType, @NotNull Class<?> holds) {
        return renderField(context, fieldName, value, valueType, holds, true, null);
    }

    private String renderField(String context, String fieldName, Object value, @NotNull Class<?> valueType, @NotNull Class<?> holds, boolean listAllowNull, String nullValue) {
        if (!value.getClass().equals(valueType)) throw new IllegalStateException("klasa wartości nie zgadza się z objektem wartości");
        if (valueType.equals(Boolean.class) || valueType.equals(Boolean.TYPE)) {
            return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + "." +
                    ((Boolean) value ? "enabled" : "disabled"));
        }
        if (valueType.equals(String.class)) {
            String val = null;
            if (holds.equals(Role.class)) {
                try {
                    Role r = shardManager.getRoleById((String) value);
                    if (r != null) val = r.getName();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (holds.equals(GuildChannel.class)) {
                try {
                    GuildChannel t = shardManager.getTextChannelById((String) value);
                    if (t != null) val = t.getName();
                    else {
                        GuildChannel v = shardManager.getVoiceChannelById((String) value);
                        if (v != null) val = v.getName();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            if (holds.equals(User.class)) {
                try {
                    User u = shardManager.retrieveUserById((String) value).complete();
                    if (u != null) val = u.getAsTag();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (holds.equals(Emoji.class)) {
                try {
                    Emoji em = (Emoji) managerArgumentow.getArguments()
                            .get("emote").execute((String) value,tlumaczenia,ctx.getLanguage());
                    if (em != null) val = em.getName();
                } catch (Exception e) {
                    // ignore
                }
            }
            return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".is" +
                    ((val == null ? "not" : "") + "set"), val);
        }
        if (valueType.equals(Integer.class) || valueType.equals(Integer.TYPE) || valueType.equals(Long.class) ||
                valueType.equals(Long.TYPE) || valueType.equals(Double.class) || valueType.equals(Double.TYPE)) {
            return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase(), value);
        }
        if (valueType.equals(List.class)) {
            List<String> kontenty = new ArrayList<>();
            for (Object wal : (List<?>) value) {
                if (!(wal instanceof String)) continue;
                String val = (String) wal;
                if (holds.equals(Role.class)) {
                    try {
                        Role r = shardManager.getRoleById((String) wal);
                        if (r != null) val = r.getName();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (holds.equals(GuildChannel.class)) {
                    try {
                        GuildChannel t = shardManager.getTextChannelById((String) wal);
                        if (t != null) val = t.getName();
                        else {
                            GuildChannel v = shardManager.getVoiceChannelById((String) wal);
                            if (v != null) val = v.getName();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (holds.equals(User.class)) {
                    try {
                        User u = shardManager.retrieveUserById((String) wal).complete();
                        if (u != null) val = u.getAsTag();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (holds.equals(Emoji.class)) {
                    try {
                        Emoji em = (Emoji) managerArgumentow.getArguments()
                                .get("emote").execute((String) wal,tlumaczenia,ctx.getLanguage());
                        if (em != null) val = em.getName();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                kontenty.add(val);
            }
            if (kontenty.size() == 1)
                return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".single", kontenty.get(0));
            else if (kontenty.size() > 1)
                return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".multiple",
                        kontenty.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            else if (listAllowNull)
                return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".null");
            else return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".single", nullValue);
        }
        if (valueType.equals(Map.class)) {
            return ctx.getTranslated("ustawienia." + context + "." + fieldName.toLowerCase() + ".menu");
        }
        throw new IllegalStateException("nie rozpoznany typ");
    }

    private static class Opcja {
        private final Method method;
        private final Field field;

        private Opcja(Method method, Field field) {
            this.method = method;
            this.field = field;
        }
    }
}
