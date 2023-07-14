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

package pl.fratik.commands.narzedzia;

import de.zh32.slp.ServerListPing17;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

public class McstatusCommand extends NewCommand {

    private DiscordSerializer discordSerializer = new DiscordSerializer(DiscordSerializerOptions.defaults().withEmbedLinks(true));

    public McstatusCommand() {
        name = "mcstatus";
        usage = "<ip:string>";
        allowInDMs = true;
        cooldown = 8;
        allowInDMs = true;
    }

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("((?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]*" +
            "[a-zA-Z0-9]\\.[^\\s/]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s/]{2,}|(?:www\\.|(?!www))" +
            "[a-zA-Z0-9]\\.[^\\s/]{2,}?[^/]|www\\.[a-zA-Z0-9]\\.[^\\s/]{2,})");

    @Override
    public void execute(@NotNull NewCommandContext context) {
        context.defer(false);
        Object[] sraka = resolveIpAndPort(context.getArguments().get("ip").getAsString());
        String ip = (String) sraka[0];
        int port = (Integer) sraka[1];
        try {
            ServerListPing17.StatusResponse resp = new ServerListPing17(new InetSocketAddress(ip, port)).fetchData();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
            eb.setAuthor(context.getTranslated("mcstatus.embed.author", context.getArguments().get("ip").getAsString()));
            StringBuilder players = new StringBuilder();
            players.append("**").append(resp.getPlayers().getOnline()).append("/")
                    .append(resp.getPlayers().getMax()).append("**\n\n");
            if (resp.getPlayers().getSample() != null && !resp.getPlayers().getSample().isEmpty()) {
                int plejers = resp.getPlayers().getOnline();
                for (ServerListPing17.Player p : resp.getPlayers().getSample()/*.stream().sorted((a, b) -> a.getName()
                                .compareToIgnoreCase(b.getName())).collect(Collectors.toList())*/) {
                    if (players.length() + (p.getName() + "\n").length() > 1000 -
                            (context.getTranslated("mcstatus.embed.players.more", plejers)).length()) {
                        players.append(context.getTranslated("mcstatus.embed.players.more", plejers));
                    }
                    players.append(replaceMinecraftFormatting(p.getName())).append("\n");
                }
            }
            eb.addField(context.getTranslated("mcstatus.embed.players"), players.toString(), false);
            eb.addField(context.getTranslated("mcstatus.embed.version"),
                    replaceMinecraftFormatting(resp.getVersion().getName()), false);
            eb.addField(context.getTranslated("mcstatus.embed.motd"), discordSerializer.serialize(resp.getDescription()), false);
            eb.addField(context.getTranslated("mcstatus.embed.ip"), ip + ":" + port, false);
            eb.setFooter(resp.getTime() + " ms", null);
            eb.setThumbnail("https://eu.mc-api.net/v3/server/favicon/" + ip + ":" + port);
            context.sendMessage(eb.build());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("mcstatus.offline"));
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
                        xd[0] = ((SRVRecord) r[0]).getTarget().toString(false);
//                        if (DOMAIN_PATTERN.matcher((String) xd[0]).find())
//                            return resolveIpAndPort((String) xd[0], ((SRVRecord) r[0]).getPort());
                        xd[1] = ((SRVRecord) r[0]).getPort();
                    }
                } else {
                    xd[0] = ip;
                    xd[1] = 25565;
                }
            } catch (Exception e) {
                if (DOMAIN_PATTERN.matcher(ip).find()) {
                    Record[] r = new Lookup("_minecraft._tcp." + ip, Type.SRV).run();
                    xd[0] = ((SRVRecord) r[0]).getTarget().toString(false);
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

    private String replaceMinecraftFormatting(String text) {
        return discordSerializer.serialize(LegacyComponentSerializer.legacy(text.contains("\uFFFD") ? '\uFFFD' : '§').deserialize(text));
    }

}
