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

package pl.fratik.core.command;

import io.sentry.Sentry;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.ArgsMissingException;
import pl.fratik.core.entity.SilentExecutionFail;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Command {
    protected static final Logger logger = LoggerFactory.getLogger(Command.class);
    @Getter protected String name;
    @Getter protected String[] aliases = new String[0];
    @Getter protected Uzycie uzycie = new Uzycie();
    @Getter protected String uzycieDelim = "";
    @Getter protected PermLevel permLevel = PermLevel.EVERYONE;
    @Getter protected CommandCategory category = CommandCategory.BASIC;
    @Getter protected CommandType type = CommandType.NORMAL;
    @Getter protected final ArrayList<Permission> permissions = getBasicPermissions();
    @Getter protected boolean allowInDMs = false;
    @Getter protected final HashMap<String, Method> subCommands = new HashMap<>();
    @Getter protected int cooldown;

    protected boolean execute(@NotNull CommandContext context) {
        throw new UnsupportedOperationException("Komenda nie ma zaimplementowanej funkcji execute()");
    }
    public void onRegister() {}
    public void onUnregister() {}
    private ArrayList<Permission> getBasicPermissions() {
        ArrayList<Permission> list = new ArrayList<>();
        list.add(Permission.VIEW_CHANNEL);
        list.add(Permission.MESSAGE_WRITE);
        return list;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean preExecute(CommandContext context) {
        if (!context.getGuild().getSelfMember().hasPermission(context.getChannel(), Permission.MESSAGE_WRITE))
            return false;

        if (context.getRawArgs().length != 0) {
            String subcommand = context.getRawArgs()[0].toLowerCase();
            if (subcommand.equalsIgnoreCase("-h") || subcommand.equalsIgnoreCase("--help")) {
                CommonErrors.usage(context);
                return false;
            } else if (subCommands.containsKey(subcommand)) {
                List<String> argsListTmp = new ArrayList<>(Arrays.asList(context.getRawArgs()));
                argsListTmp.remove(0);
                Method m = subCommands.get(subcommand);
                try {
                    if (m.getAnnotation(SubCommand.class).emptyUsage()) {
                        return (Boolean) m.invoke(this, new CommandContext(context.getShardManager(),
                                context.getTlumaczenia(), this, context.getEvent(),
                                context.getPrefix(), context.getLabel()));
                    } else {
                        return (Boolean) m.invoke(this, new CommandContext(context.getShardManager(),
                                context.getTlumaczenia(), this, context.getEvent(),
                                context.getPrefix(), context.getLabel(), argsListTmp.toArray(new String[0])));
                    }
                } catch (ArgsMissingException e) {
                    CommonErrors.usage(context);
                } catch (InsufficientPermissionException e) {
                    CommonErrors.noPermissionBot(context, e);
                } catch (SilentExecutionFail e) {
                    throw e; // rethrow
                } catch (InvocationTargetException e) {
                    logger.error("Caught error while executing subcommand \"" + name + "\"", e.getCause());
                    Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(),
                            UserUtil.formatDiscrim(context.getSender()), null, null));
                    Sentry.capture(e);
                    Sentry.clearContext();
                    CommonErrors.exception(context, e.getCause() != null ? e.getCause() : e);
                } catch (Exception e) {
                    logger.error("Caught error while executing command \"" + name + "\"", e);
                    Sentry.getContext().setUser(new io.sentry.event.User(context.getSender().getId(),
                            UserUtil.formatDiscrim(context.getSender()), null, null));
                    Sentry.capture(e);
                    Sentry.clearContext();
                    CommonErrors.exception(context, e);
                }
                return false;
            }
        }

        if (!context.getGuild().getSelfMember().hasPermission(context.getChannel(), permissions)) {
            context.send(context.getTranslated("generic.no.botpermission", permissions.stream().map((Permission::getName)).collect(Collectors.joining(", "))));
            return false;
        }

        return execute(context);
    }
}