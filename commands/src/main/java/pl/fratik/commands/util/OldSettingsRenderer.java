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

package pl.fratik.commands.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;
import pl.fratik.core.util.UserUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings("ALL")
public class OldSettingsRenderer implements SettingsRenderer {
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

    public OldSettingsRenderer(EventWaiter eventWaiter, UserDao userDao, GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerArgumentow managerArgumentow, ShardManager shardManager, CommandContext ctx) {
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

    private void glownyRender() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.description")).append("\n");
        builder.append("1. ").append(ctx.getTranslated("ustawienia.user.ustawienia")).append("\n");
        if (ctx.getGuild() != null && UserUtil.getPermlevel(ctx.getMember(), guildDao, shardManager).getNum() >= 3)
            builder.append("2. ").append(ctx.getTranslated("ustawienia.server.ustawienia")).append("\n");
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        ctx.send(builder.toString(), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                handlerGlowny(event);
            });
            waiter.create();
        });
    }

    private void handlerGlowny(MessageReceivedEvent event) {
        switch (event.getMessage().getContentRaw().trim()) {
            case "1":
                koniecZara = false;
                renderUserConf();
                break;
            case "2":
                if (!event.isFromGuild()) {
                    event.getChannel().sendMessage(ctx.getTranslated("ustawienia.server.noserver")).queue();
                    return;
                }
                if (UserUtil.getPermlevel(event.getMember(), guildDao, shardManager).getNum() < 3) {
                    event.getChannel().sendMessage(ctx.getTranslated("ustawienia.server.nopermissions",
                            String.valueOf(UserUtil.getPermlevel(event.getMember(), guildDao, shardManager).getNum()), String.valueOf(3)))
                    .queue();
                    return;
                }
                koniecZara = false;
                renderServerConf();
                break;
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                if (ctx.getMessage().getContentRaw().equals(event.getMessage().getContentRaw())) break;
                ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                if (!koniecZara) {
                    koniecZara = true;
                    glownyRender();
                }
                break;
        }
    }

    private void renderUserConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.user.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.user.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.user.description")).append("\n");
        builder.append("1. ").append(ctx.getTranslated("ustawienia.user.privwlaczone." +
                (userConfig.isPrivWlaczone() ? "enabled" : "disabled"))).append("\n");
        builder.append("2. ").append(ctx.getTranslated("ustawienia.user.boomwlaczone." +
                (userConfig.isBoomWlaczone() ? "enabled" : "disabled"))).append("\n");
        if (userConfig.getLocation() != null)
            builder.append("3. ").append(ctx.getTranslated("ustawienia.user.location.isset", userConfig.getLocation()));
        else
            builder.append("3. ").append(ctx.getTranslated("ustawienia.user.location.isnotset"));
        builder.append("\n");
        if (!(userConfig.getReakcja() == null || Emoji.resolve(userConfig.getReakcja(), shardManager) == null))
            builder.append("4. ").append(ctx.getTranslated("ustawienia.user.reakcja.isset",
                    Objects.requireNonNull(Emoji.resolve(userConfig.getReakcja(), shardManager)).getName()));
        else
            builder.append("4. ").append(ctx.getTranslated("ustawienia.user.reakcja.isnotset"));
        builder.append("\n");
        if (!(userConfig.getReakcjaBlad() == null || Emoji.resolve(userConfig.getReakcjaBlad(), shardManager) == null))
            builder.append("5. ").append(ctx.getTranslated("ustawienia.user.reakcjablad.isset",
                    Objects.requireNonNull(Emoji.resolve(userConfig.getReakcjaBlad(), shardManager)).getName())).append("\n");
        else
            builder.append("5. ").append(ctx.getTranslated("ustawienia.user.reakcjablad.isnotset")).append("\n");
        builder.append("6. ").append(ctx.getTranslated("ustawienia.user.lvlupmessages." +
                (userConfig.isPrivWlaczone() ? "enabled" : "disabled")));
        builder.append("\n");
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        ctx.send(builder.toString(), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                handlerUserConf(event);
            });
            waiter.create();
        });
    }

    private void handlerUserConf(MessageReceivedEvent event) {
        switch (event.getMessage().getContentRaw().trim()) {
            case "1":
                koniecZara = false;
                userConfig.setPrivWlaczone(!userConfig.isPrivWlaczone());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.privwlaczone.confirm." +
                        (userConfig.isPrivWlaczone() ? "enabled" : "disabled")));
                break;
            case "2":
                koniecZara = false;
                userConfig.setBoomWlaczone(!userConfig.isBoomWlaczone());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.boomwlaczone.confirm." +
                        (userConfig.isBoomWlaczone() ? "enabled" : "disabled")));
                break;
            case "3":
                koniecZara = false;
                handleLocation();
                break;
            case "4":
                koniecZara = false;
                handleReakcja();
                break;
            case "5":
                koniecZara = false;
                handleReakcjaBlad();
                break;
            case "6":
                koniecZara = false;
                userConfig.setLvlupMessages(!userConfig.isLvlupMessages());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.lvlupmessages.confirm." +
                        (userConfig.isPrivWlaczone() ? "enabled" : "disabled")));
                break;
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                if (!koniecZara) {
                    koniecZara = true;
                    renderUserConf();
                }
                break;
        }
    }

    private void handleLocation() {
        ctx.send(ctx.getTranslated("ustawienia.user.set.location"), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                if (event.getMessage().getContentRaw().length() > 58) {
                    ctx.send(ctx.getTranslated("ustawienia.user.set.location.maxlen", String.valueOf(58)));
                    if (!koniecZara) {
                        koniecZara = true;
                        handleLocation();
                    }
                    return;
                }
                userConfig.setLocation(event.getMessage().getContentRaw());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.location.confirm"));
            });
            waiter.create();
        });
    }

    private void handleReakcja() {
        ctx.send(ctx.getTranslated("ustawienia.user.set.reakcja"), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                Emoji emotka = (Emoji) managerArgumentow.getArguments().get("emote")
                        .execute(event.getMessage().getContentRaw().split(" ")[0], tlumaczenia, ctx.getLanguage());
                if (emotka == null) {
                    ctx.send(ctx.getTranslated("ustawienia.user.set.reakcja.invalid"));
                    if (!koniecZara) {
                        koniecZara = true;
                        handleReakcja();
                    }
                    return;
                }
                userConfig.setReakcja(emotka.getId());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.set.reakcja.confirm"));
            });
            waiter.create();
        });
    }

    private void handleReakcjaBlad() {
        ctx.send(ctx.getTranslated("ustawienia.user.set.reakcjablad"), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                Emoji emotka = (Emoji) managerArgumentow.getArguments().get("emote")
                        .execute(event.getMessage().getContentRaw().split(" ")[0], tlumaczenia, ctx.getLanguage());
                if (emotka == null) {
                    ctx.send(ctx.getTranslated("ustawienia.user.set.reakcjablad.invalid"));
                    if (!koniecZara) {
                        koniecZara = true;
                        handleReakcjaBlad();
                    }
                    return;
                }
                userConfig.setReakcjaBlad(emotka.getId());
                userDao.save(userConfig);
                ctx.send(ctx.getTranslated("ustawienia.user.set.reakcjablad.confirm"));
            });
            waiter.create();
        });
    }

    private void renderServerConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.server.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.server.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.server.description")).append("\n");
//        if (guildConfig.getPrefixes().size() == 1)
//            builder.append("1. ").append(ctx.getTranslated("ustawienia.server.prefixes.single",
//                    guildConfig.getPrefixes().get(0))).append("\n");
//        else if (guildConfig.getPrefixes().size() > 1)
//            builder.append("1. ").append(ctx.getTranslated("ustawienia.server.prefixes.multiple",
//                    String.join(", ", guildConfig.getPrefixes()))).append("\n");
//        else
//            builder.append("1. ").append(ctx.getTranslated("ustawienia.server.prefixes.single",
//                    Ustawienia.instance.prefix)).append("\n");
//        if (guildConfig.getAdminRole() != null && guildConfig.getAdminRole().length() != 0
//                && ctx.getGuild().getRoleById(guildConfig.getAdminRole()) != null)
//            builder.append("2. ").append(ctx.getTranslated("ustawienia.server.adminrole.isset",
//                    ctx.getGuild().getRoleById(guildConfig.getAdminRole()).getName())).append("\n");
//        else builder.append("2. ").append(ctx.getTranslated("ustawienia.server.adminrole.isnotset")).append("\n");
//        if (guildConfig.getModRole() != null && guildConfig.getModRole().length() != 0
//                && ctx.getGuild().getRoleById(guildConfig.getModRole()) != null)
//            builder.append("3. ").append(ctx.getTranslated("ustawienia.server.modrole.isset",
//                    ctx.getGuild().getRoleById(guildConfig.getModRole()).getName())).append("\n");
//        else builder.append("3. ").append(ctx.getTranslated("ustawienia.server.modrole.isnotset")).append("\n");
        builder.append("1. ").append(ctx.getTranslated("ustawienia.server.rolezapoziomy.menu")).append("\n");
//        if (guildConfig.getKanalAdministracji() != null && !guildConfig.getKanalAdministracji().isEmpty()
//                && ctx.getGuild().getTextChannelById(guildConfig.getKanalAdministracji()) != null)
//            builder.append("5. ").append(ctx.getTranslated("ustawienia.server.kanaladm.isset",
//                    ctx.getGuild().getTextChannelById(guildConfig.getKanalAdministracji()).getName())).append("\n");
//        else builder.append("5. ").append(ctx.getTranslated("ustawienia.server.kanaladm.isnotset")).append("\n");
//        if (guildConfig.getFullLogs() != null && !guildConfig.getFullLogs().isEmpty()
//                && ctx.getGuild().getTextChannelById(guildConfig.getFullLogs()) != null)
//            builder.append("6. ").append(ctx.getTranslated("ustawienia.server.fulllogs.isset",
//                    ctx.getGuild().getTextChannelById(guildConfig.getFullLogs()).getName())).append("\n");
//        else builder.append("6. ").append(ctx.getTranslated("ustawienia.server.fulllogs.isnotset")).append("\n");
//        if (guildConfig.getPunktyWlaczone())
//            builder.append("7. ").append(ctx.getTranslated("ustawienia.server.punkty.wlaczone")).append("\n");
//        else builder.append("7. ").append(ctx.getTranslated("ustawienia.server.punkty.wylaczone")).append("\n");
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```\n").append(ctx.getTranslated("ustawienia.betterver.full",
                ctx.getManageLink(ctx.getGuild())));
        ctx.send(builder.toString(), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                handlerServerConf(event);
            });
            waiter.create();
        });
    }

    private void renderPrefixMenu() {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.server.prefixes.menu.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.server.prefixes.menu.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.server.prefixes.menu.description")).append("\n");
        builder.append("1. ").append(ctx.getTranslated("ustawienia.server.prefixes.menu.add")).append("\n");
        builder.append("2. ").append(ctx.getTranslated("ustawienia.server.prefixes.menu.set")).append("\n");
        builder.append("3. ").append(ctx.getTranslated("ustawienia.server.prefixes.menu.reset")).append("\n");
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        ctx.send(builder.toString(), message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                handlePrefixMenu(event);
            });
            waiter.create();
        });
    }

    private void renderRoleZaPoziomyMenu() {
        int i = 1;
        pages = new ArrayList<>();
        List<String> pagesRaw = new ArrayList<>();
        pagesRaw.add("1. " + ctx.getTranslated("ustawienia.server.rolezapoziomy.menu.add") + "\n");
        for (Map.Entry<Integer, String> entry : new TreeMap<>(guildConfig.getRoleZaPoziomy()).entrySet()) {
            StringBuilder builder1 = new StringBuilder();
            Role role = ctx.getGuild().getRoleById(entry.getValue());
            if (role == null) continue;
            i++;
            String format = String.format("%s -> %s", entry.getKey().toString(), role.getName());
            builder1.append(i).append(". ").append(format).append("\n");
            pagesRaw.add(builder1.toString());
            roleZaPoziomyId.put(i, entry.getKey());
        }
        StringBuilder builder1 = new StringBuilder();
        boolean firstTime = true;
        for (String page : pagesRaw) {
            if (pagesRaw.indexOf(page) % 9 == 0 && !firstTime) {
                pages.add(builder1.toString());
                builder1 = new StringBuilder();
                builder1.append(page);
            }
            else {
                firstTime = false;
                builder1.append(page);
            }
        }
        if (builder1.toString().length() != 0) pages.add(builder1.toString());
        renderRoleZaPoziomyMenu(1);
    }

    private void renderRoleZaPoziomyMenu(int pageNo) {
        StringBuilder builder = new StringBuilder();
        builder.append("```md\n");
        builder.append(ctx.getTranslated("ustawienia.server.rolezapoziomy.menu.header")).append("\n");
        builder.append(new String(new char[ctx.getTranslated("ustawienia.server.rolezapoziomy.menu.header").length()])
                .replace("\0", "=")).append("\n\n");
        builder.append(ctx.getTranslated("ustawienia.server.rolezapoziomy.menu.description")).append("\n");
        String content = pages.get(pageNo - 1);
        builder.append(content);
        builder.append("\n0. ").append(ctx.getTranslated("ustawienia.footer"));
        builder.append("```");
        if (paginatingMessage == null) {
            ctx.send(builder.toString(), message -> {
                activeMessage = message;
                MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
                if (pages.size() == 1) waiter.setTimeoutHandler(() -> onTimeout(message));
                waiter.setMessageHandler(event -> {
                    if (activeMessage != message) return;
                    message.delete().queue();
                    paginatingMessage = null;
                    activeMessage = null;
                    pages = null;
                    this.pageNo = 0;
                    handleRoleZaPoziomyMenu(event);
                });
                waiter.create();
                if (pages.size() > 1) {
                    this.pageNo = pageNo;
                    paginatingMessage = message;
                    message.addReaction(LEFT_EMOJI).queue();
                    message.addReaction(RIGHT_EMOJI).queue();
                    eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                            this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
                }
            });
        } else {
            paginatingMessage.editMessage(builder.toString()).queue(message -> {
                activeMessage = message;
                MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
                if (pages.size() == 1) waiter.setTimeoutHandler(() -> onTimeout(message));
                waiter.setMessageHandler(event -> {
                    if (activeMessage != message) return;
                    message.delete().queue();
                    paginatingMessage = null;
                    activeMessage = null;
                    pages = null;
                    this.pageNo = 0;
                    handleRoleZaPoziomyMenu(event);
                });
                waiter.create();
                if (pages.size() > 1) {
                    this.pageNo = pageNo;
                    paginatingMessage = message;
                    message.addReaction(LEFT_EMOJI).queue();
                    message.addReaction(RIGHT_EMOJI).queue();
                    eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                            this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
                }
            });
        }
    }

    private boolean checkReaction(MessageReactionAddEvent event) {
        if (event.getMessageId().equals(paginatingMessage.getId()) && !event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case LEFT_EMOJI:
                case RIGHT_EMOJI:
                    return event.getUser().getId().equals(ctx.getSender().getId());
                default:
                    return false;
            }
        }
        return false;
    }

    private void clearReactions() {
        try {
            if (paginatingMessage == null || activeMessage == null) return;
            onTimeout(paginatingMessage);
            paginatingMessage = null;
            activeMessage = null;
            pages = null;
            pageNo = 0;
            roleZaPoziomyId = new HashMap<>();
        } catch (PermissionException ignored) {/*lul*/}
    }

    private void handleReaction(MessageReactionAddEvent event) {
        if (!event.getReactionEmote().isEmote()) {
            String s = event.getReactionEmote().getName();
            if (LEFT_EMOJI.equals(s)) {
                if (pageNo > 1) pageNo--;
            } else if (RIGHT_EMOJI.equals(s) && pageNo < pages.size()) pageNo++;
        }

        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) {/*lul*/}

        renderRoleZaPoziomyMenu(pageNo);
    }

    private void handlerServerConf(MessageReceivedEvent event) {
        switch (event.getMessage().getContentRaw().trim()) {
//            case "1":
//                koniecZara = false;
//                renderPrefixMenu();
//                break;
//            case "2":
//                koniecZara = false;
//                ctx.send(ctx.getTranslated("ustawienia.server.set.role"), message -> {
//                    MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
//                    waiter.setTimeoutHandler(() -> onTimeout(message));
//                    waiter.setMessageHandler(event1 -> {
//                        Role rola = (Role) managerArgumentow.getArguments().get("role")
//                                .execute(event1.getMessage().getContentRaw().split(" ")[0], tlumaczenia,
//                                        ctx.getLanguage(), ctx.getGuild());
//                        if (rola == null) {
//                            ctx.send(ctx.getTranslated("ustawienia.server.set.role.invalid"));
//                            if (!koniecZara) {
//                                koniecZara = true;
//                                renderServerConf();
//                            }
//                            return;
//                        }
//                        guildConfig.setAdminRole(rola.getId());
//                        guildDao.save(guildConfig);
//                        ctx.send(ctx.getTranslated("ustawienia.server.set.adminrole.confirm"));
//                    });
//                    waiter.create();
//                });
//                break;
//            case "3":
//                koniecZara = false;
//                ctx.send(ctx.getTranslated("ustawienia.server.set.role"), message -> {
//                    MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
//                    waiter.setTimeoutHandler(() -> onTimeout(message));
//                    waiter.setMessageHandler(event1 -> {
//                        Role rola = (Role) managerArgumentow.getArguments().get("role")
//                                .execute(event1.getMessage().getContentRaw().split(" ")[0], tlumaczenia,
//                                        ctx.getLanguage(), ctx.getGuild());
//                        if (rola == null) {
//                            ctx.send(ctx.getTranslated("ustawienia.server.set.role.invalid"));
//                            if (!koniecZara) {
//                                koniecZara = true;
//                                renderServerConf();
//                            }
//                            return;
//                        }
//                        guildConfig.setModRole(rola.getId());
//                        guildDao.save(guildConfig);
//                        ctx.send(ctx.getTranslated("ustawienia.server.set.modrole.confirm"));
//                    });
//                    waiter.create();
//                });
//                break;
            case "1":
                koniecZara = false;
                renderRoleZaPoziomyMenu();
                break;
//            case "5":
//                koniecZara = false;
//                ctx.send(ctx.getTranslated("ustawienia.server.set.channel"), message -> {
//                    MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
//                    waiter.setTimeoutHandler(() -> onTimeout(message));
//                    waiter.setMessageHandler(event1 -> {
//                        TextChannel channel = (TextChannel) managerArgumentow.getArguments().get("channel")
//                                .execute(event1.getMessage().getContentRaw().split(" ")[0], tlumaczenia,
//                                        ctx.getLanguage(), ctx.getGuild());
//                        if (channel == null) {
//                            ctx.send(ctx.getTranslated("ustawienia.server.set.channel.invalid"));
//                            if (!koniecZara) {
//                                koniecZara = true;
//                                renderServerConf();
//                            }
//                            return;
//                        }
//                        guildConfig.setKanalAdministracji(channel.getId());
//                        guildDao.save(guildConfig);
//                        ctx.send(ctx.getTranslated("ustawienia.server.set.kanaladm.confirm"));
//                    });
//                    waiter.create();
//                });
//                break;
//            case "6":
//                koniecZara = false;
//                ctx.send(ctx.getTranslated("ustawienia.server.set.channel"), message -> {
//                    MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
//                    waiter.setTimeoutHandler(() -> onTimeout(message));
//                    waiter.setMessageHandler(event1 -> {
//                        TextChannel channel = (TextChannel) managerArgumentow.getArguments().get("channel")
//                                .execute(event1.getMessage().getContentRaw().split(" ")[0], tlumaczenia,
//                                        ctx.getLanguage(), ctx.getGuild());
//                        if (channel == null) {
//                            ctx.send(ctx.getTranslated("ustawienia.server.set.channel.invalid"));
//                            if (!koniecZara) {
//                                koniecZara = true;
//                                renderServerConf();
//                            }
//                            return;
//                        }
//                        guildConfig.setFullLogs(channel.getId());
//                        guildDao.save(guildConfig);
//                        ctx.send(ctx.getTranslated("ustawienia.server.set.fulllogs.confirm"));
//                    });
//                    waiter.create();
//                });
//                break;
//            case "7":
//                koniecZara = false;
//                guildConfig.setPunktyWlaczone(!guildConfig.getPunktyWlaczone());
//                guildDao.save(guildConfig);
//                ctx.send(ctx.getTranslated("ustawienia.server.set.punkty.confirm." +
//                        (guildConfig.getPunktyWlaczone() ? "wlaczone" : "wylaczone")));
//                break;
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                if (!koniecZara) {
                    koniecZara = true;
                    renderServerConf();
                }
                break;
        }
    }

    private void handleRoleZaPoziomyMenu(MessageReceivedEvent event) {
        switch (event.getMessage().getContentRaw().trim()) {
            case "1":
                roleZaPoziomyId = new HashMap<>();
                koniecZara = false;
                getText(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.level"), messageLevel -> {
                    Integer level = (Integer) managerArgumentow.getArguments().get("integer")
                            .execute(messageLevel.getContentRaw(), tlumaczenia, ctx.getLanguage());
                    if (level == null) {
                        ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.level.invalid"));
                        if (!koniecZara) {
                            koniecZara = true;
                            renderRoleZaPoziomyMenu();
                        }
                        return;
                    }
                    if (level >= 1000) {
                        ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.level.max", String.valueOf(1000)));
                        if (!koniecZara) {
                            koniecZara = true;
                            renderRoleZaPoziomyMenu();
                        }
                        return;
                    }
                    getText(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.role"), messageRole -> {
                        Role role = (Role) managerArgumentow.getArguments().get("role")
                                .execute(messageRole.getContentRaw(), tlumaczenia, ctx.getLanguage(), ctx.getGuild());
                        if (role == null) {
                            ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.role.invalid"));
                            if (!koniecZara) {
                                koniecZara = true;
                                renderRoleZaPoziomyMenu();
                            }
                            return;
                        }
                        if (ctx.getGuild().getSelfMember().getRoles().isEmpty() ||
                                !ctx.getGuild().getSelfMember().getRoles().get(0).canInteract(role) ||
                                !ctx.getGuild().getSelfMember().getPermissions().contains(Permission.MANAGE_ROLES)) {
                            ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.role.self.noperms"));
                            if (!koniecZara) {
                                koniecZara = true;
                                renderRoleZaPoziomyMenu();
                            }
                            return;
                        }
                        if (!ctx.getMember().canInteract(role)) {
                            ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.role.noperms"));
                            if (!koniecZara) {
                                koniecZara = true;
                                renderRoleZaPoziomyMenu();
                            }
                            return;
                        }
                        Map<Integer, String> roleZaPoziomyMap = guildConfig.getRoleZaPoziomy();
                        AtomicBoolean errored = new AtomicBoolean();
                        roleZaPoziomyMap.forEach((lvl, id) -> {
                            if (id.equals(role.getId())) {
                                ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.role.used", String.valueOf(lvl)));
                                if (!koniecZara) {
                                    koniecZara = true;
                                    renderRoleZaPoziomyMenu();
                                }
                                errored.getAndSet(true);
                            }
                            if (lvl.equals(level)) {
                                ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.level.used", role.getName()));
                                if (!koniecZara) {
                                    koniecZara = true;
                                    renderRoleZaPoziomyMenu();
                                }
                                errored.getAndSet(true);
                            }
                        });
                        if (errored.get()) return;
                        roleZaPoziomyMap.put(level, role.getId());
                        guildConfig.setRoleZaPoziomy(roleZaPoziomyMap);
                        guildDao.save(guildConfig);
                        ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.add.success", role.getName(), level.toString()));
                    });
                });
                break;
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                try {
                    if (roleZaPoziomyId.get(Integer.valueOf(event.getMessage().getContentRaw().trim())) == null) {
                        ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                        if (!koniecZara) {
                            roleZaPoziomyId = new HashMap<>();
                            koniecZara = true;
                            renderRoleZaPoziomyMenu();
                        }
                        break;
                    }
                    Integer level = roleZaPoziomyId.get(Integer.valueOf(event.getMessage().getContentRaw().trim()));
                    Map<Integer, String> roleZaPoziomy = guildConfig.getRoleZaPoziomy();
                    String roleId = roleZaPoziomy.remove(level);
                    Role role = ctx.getGuild().getRoleById(roleId);
                    guildConfig.setRoleZaPoziomy(roleZaPoziomy);
                    guildDao.save(guildConfig);
                    ctx.send(ctx.getTranslated("ustawienia.server.rolezapoziomy.remove.success", role.getName(), level.toString()));
                    roleZaPoziomyId = new HashMap<>();
                    break;
                } catch (NumberFormatException e) {
                    ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                    if (!koniecZara) {
                        roleZaPoziomyId = new HashMap<>();
                        koniecZara = true;
                        renderRoleZaPoziomyMenu();
                    }
                    break;
                }
        }
    }

    private void handlePrefixMenu(MessageReceivedEvent event) {
        switch (event.getMessage().getContentRaw().trim()) {
            case "1":
                koniecZara = false;
                getText(ctx.getTranslated("ustawienia.server.prefixes.add"), message -> {
                    if (message.getContentRaw().length() >= 25) {
                        ctx.send(ctx.getTranslated("ustawienia.server.prefixes.add.maxlen", String.valueOf(25)));
                        if (!koniecZara) {
                            koniecZara = true;
                            renderPrefixMenu();
                        }
                        return;
                    }
                    List<String> prefixes = guildConfig.getPrefixes();
                    if (prefixes.isEmpty()) prefixes.add(Ustawienia.instance.prefix);
                    prefixes.add(message.getContentRaw());
                    guildConfig.setPrefixes(prefixes);
                    guildDao.save(guildConfig);
                    ctx.send(ctx.getTranslated("ustawienia.server.prefixes.add.success"));
                });
                break;
            case "2":
                koniecZara = false;
                getText(ctx.getTranslated("ustawienia.server.prefixes.set"), message -> {
                    if (message.getContentRaw().length() >= 25) {
                        ctx.send(ctx.getTranslated("ustawienia.server.prefixes.set.maxlen", String.valueOf(25)));
                        if (!koniecZara) {
                            koniecZara = true;
                            renderPrefixMenu();
                        }
                        return;
                    }
                    ArrayList<String> prefixes = new ArrayList<>();
                    prefixes.add(message.getContentRaw());
                    guildConfig.setPrefixes(prefixes);
                    guildDao.save(guildConfig);
                    ctx.send(ctx.getTranslated("ustawienia.server.prefixes.set.success"));
                });
                break;
            case "3":
                ArrayList<String> prefixes = new ArrayList<>();
                guildConfig.setPrefixes(prefixes);
                guildDao.save(guildConfig);
                ctx.send(ctx.getTranslated("ustawienia.server.prefixes.reset.success"));
                break;
            case "0":
            case "wyjdz":
            case "wyjdź":
            case "pa":
            case "exit":
                break;
            default:
                ctx.send(ctx.getTranslated("ustawienia.invalid.choice"));
                if (!koniecZara) {
                    koniecZara = true;
                    renderPrefixMenu();
                }
                break;
        }
    }

    private void getText(String messageTxt, Consumer<Message> callback) {
        ctx.send(messageTxt, message -> {
            MessageWaiter waiter = new MessageWaiter(eventWaiter, ctx);
            waiter.setTimeoutHandler(() -> onTimeout(message));
            waiter.setMessageHandler(event -> {
                message.delete().queue();
                callback.accept(event.getMessage());
            });
            waiter.create();
        });
    }

    private void onTimeout(Message message) {
        if (wiadomoscJezyki != null) wiadomoscJezyki.delete().queue();
        if (message != null) message.delete().queue();
        ctx.send(ctx.getTranslated("ustawienia.timeout"));
    }
}
