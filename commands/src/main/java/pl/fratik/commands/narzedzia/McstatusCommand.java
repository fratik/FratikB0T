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

package pl.fratik.commands.narzedzia;

import de.zh32.slp.ServerListPing17;
import lombok.Getter;
import me.dilley.MineStat;
import me.vankka.reserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.UserUtil;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class McstatusCommand extends Command {

    public McstatusCommand() {
        name = "mcstatus";
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("ip", "string", true);
        allowInDMs = true;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        cooldown = 13;
    }

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("((?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]*" +
            "[a-zA-Z0-9]\\.[^\\s/]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s/]{2,}|(?:www\\.|(?!www))" +
            "[a-zA-Z0-9]\\.[^\\s/]{2,}?[^/]|www\\.[a-zA-Z0-9]\\.[^\\s/]{2,})");

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Object[] sraka = resolveIpAndPort((String) context.getArgs()[0]);
        String ip = (String) sraka[0];
        int port = (Integer) sraka[1];
        List<FjuczerTask<MesydzAkszyn>> futures = new ArrayList<>();
        ExecutorService executor = null;
        try {
            futures.add(new FjuczerTask<>("ten gorszy", () -> {
                try {
                    MineStat ms = new MineStat(ip, port);
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
                    eb.setAuthor(context.getTranslated("mcstatus.embed.author", (String) context.getArgs()[0]));
                    eb.addField(context.getTranslated("mcstatus.embed.players"),
                            ms.getCurrentPlayers() + "/" + ms.getMaximumPlayers(), false);
                    eb.addField(context.getTranslated("mcstatus.embed.version"), ms.getVersion(), false);
                    eb.addField(context.getTranslated("mcstatus.embed.motd"),
                            replaceMinecraftFormatting(ms.getMotd()), false);
                    if (ms.getProtocolVersion().equals("127")) {
                        eb.addField(context.getTranslated("mcstatus.embed.old.protocol"),
                                context.getTranslated("mcstatus.embed.old.protocol.desc"), false);
                    }
                    eb.addField(context.getTranslated("mcstatus.embed.ip"), ip + ":" + port, false);
                    eb.setFooter(ms.getLatency() + " ms", null);
                    TextChannel ch = context.getChannel();
                    Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(ch.getId());
                    return new MesydzAkszyn("ten gorszy", ch.getJDA(), route, ch).embed(eb.build());
                } catch (Exception e) {
                    return null;
                }
            }));
            futures.add(new FjuczerTask<>("ten lepszy", () -> {
                try {
                    ServerListPing17.StatusResponse resp = new ServerListPing17(new InetSocketAddress(ip, port)).fetchData();
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
                    eb.setAuthor(context.getTranslated("mcstatus.embed.author", (String) context.getArgs()[0]));
                    StringBuilder players = new StringBuilder();
                    players.append("**").append(resp.getPlayers().getOnline()).append("/")
                            .append(resp.getPlayers().getMax()).append("**\n\n");
                    if (resp.getPlayers().getSample() != null && !resp.getPlayers().getSample().isEmpty()) {
                        int plejers = resp.getPlayers().getOnline();
                        for (ServerListPing17.Player p : resp.getPlayers().getSample().stream().sorted((a, b) -> a.getName()
                                .compareToIgnoreCase(b.getName())).collect(Collectors.toList())) {
                            if (players.length() + (p.getName() + "\n").length() > 1000 -
                                    (context.getTranslated("mcstatus.embed.players.more", plejers)).length()) {
                                players.append(context.getTranslated("mcstatus.embed.players.more", plejers));
                            }
                            players.append(p.getName()).append("\n");
                        }
                    }
                    eb.addField(context.getTranslated("mcstatus.embed.players"), players.toString(), false);
                    eb.addField(context.getTranslated("mcstatus.embed.version"), resp.getVersion().getName(), false);
                    eb.addField(context.getTranslated("mcstatus.embed.motd"), formatMotd(resp.getDescription()), false);
                    eb.addField(context.getTranslated("mcstatus.embed.ip"), ip + ":" + port, false);
                    eb.setFooter(resp.getTime() + " ms", null);
                    TextChannel ch = context.getChannel();
                    Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(ch.getId());
                    MesydzAkszyn ma = new MesydzAkszyn("ten lepszy", ch.getJDA(), route, ch);
                    eb.setThumbnail("https://eu.mc-api.net/v3/server/favicon/" + ip + ":" + port);
                    ma = ma.embed(eb.build());
                    return ma;
                } catch (Exception e) {
                    return null;
                }
            }));
            SecurityManager s = System.getSecurityManager();
            ThreadGroup group = (s != null) ? s.getThreadGroup() : //NOSONAR
                    Thread.currentThread().getThreadGroup();
            executor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(group, r, "McStatus-" + context.getSender().getId(),
                        0);
                if (t.isDaemon())
                    t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY)
                    t.setPriority(Thread.NORM_PRIORITY);
                return t;
            });
            futures.forEach(executor::submit);
            MesydzAkszyn ma = check(futures);
            if (ma == null) throw new IllegalStateException("serwer offline");
            Message me = ma.complete();
            if (!ma.getName().equals("ten gorszy")) return true;
            while (!futures.stream().allMatch(FutureTask::isDone)) {
                Thread.sleep(100);
            }
            for (FjuczerTask<MesydzAkszyn> f : futures) {
                if (!f.getName().equals("ten lepszy")) continue;
                MessageAction ma1 = me.editMessage(f.get().getEmbed()).override(true);
                if (f.get().getFiles() != null && !f.get().getFiles().isEmpty()) {
                    me.delete().queue();
                    ma1 = me.getTextChannel().sendMessage(f.get().getEmbed());
                    for (Map.Entry<String, InputStream> file : f.get().getFiles().entrySet()) {
                        ma1 = ma1.addFile(file.getValue(), file.getKey());
                    }
                }
                ma1.complete();
                executor.shutdown();
                return true;
            }
            executor.shutdown();
            return true;
        } catch (Exception e) {
            if (executor != null) {
                executor.shutdown();
            }
            context.send(context.getTranslated("mcstatus.offline"));
            return false;
        }
    }

    private Object[] resolveIpAndPort(String ip) {
        String[] splotIp = ip.split(":");
        if (splotIp.length == 2) {
            return resolveIpAndPort(splotIp[0], Integer.valueOf(splotIp[1]));
        } else return resolveIpAndPort(ip, null);
    }

    private Object[] resolveIpAndPort(String ip, Integer port) {
        Object[] xd = new Object[2];
        try {
            try { //NOSONAR
                if (DOMAIN_PATTERN.matcher(ip).find()) {
                    Record[] r = new Lookup("_minecraft._tcp." + ip, Type.SRV).run();
                    if (r == null || r.length == 0) {
                        InetAddress adress = InetAddress.getByName(ip);
                        xd[0] = adress.getHostAddress();
                        xd[1] = 25565;
                    } else {
                        xd[0] = ((SRVRecord) r[0]).getTarget().toString(true);
                        if (DOMAIN_PATTERN.matcher((String) xd[0]).find())
                            return resolveIpAndPort((String) xd[0], ((SRVRecord) r[0]).getPort());
                        xd[1] = ((SRVRecord) r[0]).getPort();
                    }
                } else {
                    xd[0] = ip;
                    xd[1] = 25565;
                }
            } catch (Exception e) {
                if (DOMAIN_PATTERN.matcher(ip).find()) {
                    Record[] r = new Lookup("_minecraft._tcp." + ip, Type.SRV).run();
                    xd[0] = ((SRVRecord) r[0]).getTarget().toString(true);
                    if (DOMAIN_PATTERN.matcher((String) xd[0]).find())
                        return resolveIpAndPort((String) xd[0], ((SRVRecord) r[0]).getPort());
                    xd[1] = ((SRVRecord) r[0]).getPort();
                } else {
                    xd[0] = ip;
                    xd[1] = 25565;
                }
            }
        } catch (Exception e) {
            xd[0] = ip;
            xd[1] = 25565;
        }
        if (port != null) xd[1] = port;
        return xd;
    }

    private MesydzAkszyn check(List<FjuczerTask<MesydzAkszyn>> futures) throws Exception {
        while (futures.stream().noneMatch(FutureTask::isDone)) {
            Thread.sleep(100);
        }
        List<FutureTask<MesydzAkszyn>> mas = futures.stream().filter(FutureTask::isDone).collect(Collectors.toList());
        if (mas.size() == 2 && mas.stream().map(t -> {
            try {
                return t.get();
            } catch (Exception e) {
                return null;
            }
        }).allMatch(Objects::isNull)) {
            return null;
        } else if (mas.size() == 1 && mas.get(0).get() == null) {
            while (futures.stream().allMatch(FutureTask::isDone)) {
                Thread.sleep(100);
            }
        } else if (mas.size() == 0) throw new IllegalStateException("tu nie dojdziemy raczej");
        return mas.stream().map(a -> {
            try {
                return a.get();
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).findAny().orElse(null);
    }

    private String formatMotd(ServerListPing17.Description motd) {
        StringBuilder sb = new StringBuilder();
        sb.append(replaceMinecraftFormatting(motd.getText()));
        if (motd.getExtra() != null) {
            for (ServerListPing17.Extra extra : motd.getExtra()) {
                if (extra.isObfuscated()) continue;
                if (extra.isBold()) sb.append("**");
                if (extra.isItalic()) sb.append("_");
                if (extra.isStrikethrough()) sb.append("~~");
                if (extra.isUnderlined()) sb.append("__");
                sb.append(extra.getText());
                if (extra.isUnderlined()) sb.append("__");
                if (extra.isStrikethrough()) sb.append("~~");
                if (extra.isItalic()) sb.append("_");
                if (extra.isBold()) sb.append("**");
                sb.append("\u200b");
            }
        }
        return sb.toString();
    }

    private String replaceMinecraftFormatting(String text) {
        return DiscordSerializer.serialize(LegacyComponentSerializer.legacy().deserialize(text,
                text.contains("\uFFFD") ? '\uFFFD' : '\u00A7'), true);
    }

    private static class FjuczerTask<V> extends FutureTask<V> {
        @Getter private final String name;

        FjuczerTask(@NotNull String name, @NotNull Callable<V> callable) {
            super(callable);
            this.name = name;
        }
    }

    private static class MesydzAkszyn extends MessageActionImpl {
        @Getter private final String name;

        MesydzAkszyn(String name, JDA api, Route.CompiledRoute route, MessageChannel channel) {
            super(api, route, channel);
            this.name = name;
        }

        MessageEmbed getEmbed() {
            return embed;
        }

        Map<String, InputStream> getFiles() {
            return files;
        }

        @NotNull
        @Override
        public MesydzAkszyn embed(MessageEmbed embed) {
            //noinspection ResultOfMethodCallIgnored
            super.embed(embed);
            return this;
        }

        @NotNull
        @Override
        public MesydzAkszyn addFile(@Nonnull byte[] data, @Nonnull String name, @Nonnull AttachmentOption... options) {
            //noinspection ResultOfMethodCallIgnored
            super.addFile(data, name);
            return this;
        }
    }

}
