/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.commands.system;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OgloszenieCommand extends Command {

    private static final Pattern CODE_REGEX = Pattern.compile("<js>(.*?)</js>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ShardManager shardManager;
    private final GuildDao guildDao;
    private final EventBus eventBus;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;

    private final Cache<GuildConfig> gcCache;

    public OgloszenieCommand(ShardManager shardManager, GuildDao guildDao, EventBus eventBus, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, RedisCacheManager rcm) {
        this.guildDao = guildDao;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        name = "ogloszenie";
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        category = CommandCategory.SYSTEM;
        aliases = new String[] {"news", "broadcast"};
        allowPermLevelChange = false;
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na FratikDev");
        TextChannel kanau = shardManager.getTextChannelById(Ustawienia.instance.ogloszeniaBota);
        if (kanau == null) throw new IllegalStateException("brak kanału");
        List<Message> msgs = kanau.getHistory().retrievePast(1).complete();
        if (msgs.isEmpty()) {
            context.send(context.getTranslated("ogloszenie.no.message"));
            return false;
        }
        EmbedBuilder eb = ogloszenieEmbed(msgs.get(0), context.getTlumaczenia(), context.getLanguage(),
                context.getGuild());
        context.send(eb.build());
        return true;
    }

    @SubCommand(name = "post")
    public boolean post(@NotNull CommandContext context) {
        if (UserUtil.getPermlevel(context.getMember(), guildDao, context.getShardManager()).getNum() < 10) {
            return execute(context);
        }
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na FratikDev");
        TextChannel kanau = shardManager.getTextChannelById(Ustawienia.instance.ogloszeniaBota);
        if (kanau == null) throw new IllegalStateException("brak kanału");
        List<Message> msgs = kanau.getHistory().retrievePast(1).complete();
        if (msgs.isEmpty()) {
            context.send(context.getTranslated("ogloszenie.no.message"));
            return false;
        }
        Emote emotka = shardManager.getEmoteById(Ustawienia.instance.emotki.loading);
        Message wiadomosc;
        if (emotka == null) wiadomosc = context.send(context.getTranslated("ogloszenie.post.sending.starting",
                "\u2699"));
        else wiadomosc = context.send(context.getTranslated("ogloszenie.post.sending.starting",
                emotka.getAsMention()));
        AtomicInteger udane = new AtomicInteger();
        AtomicInteger nieudane = new AtomicInteger();
        AtomicInteger nieMaKanalu = new AtomicInteger();
        AtomicInteger nieWysylaj = new AtomicInteger();
        AtomicBoolean skonczone = new AtomicBoolean();
        final List<Guild> serwery = shardManager.getGuilds();
        Thread t = new Thread(() -> {
            for (Guild gu : serwery) { //NOSONAR
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    EmbedBuilder eb = ogloszenieEmbed(msgs.get(0), tlumaczenia, tlumaczenia.getLanguage(gu), gu);
                    GuildConfig gc = gcCache.get(gu.getId(), guildDao::get);
                    if (gc.getWysylajOgloszenia() == null || !gc.getWysylajOgloszenia()) {
                        udane.getAndAdd(1);
                        nieWysylaj.getAndAdd(1);
                        continue;
                    }
                    String admc = gc.getKanalAdministracji();
                    TextChannel channel = null;
                    if (admc != null && !admc.isEmpty()) channel = gu.getTextChannelById(admc);
                    if (channel != null) channel.sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(gu),
                            "ogloszenie.auto.repost", managerKomend.getPrefixes(gu).get(0))).embed(eb.build())
                            .complete();
                    if (channel == null) nieMaKanalu.getAndAdd(1);
                } catch (Exception e1) {
                    nieudane.getAndAdd(1);
                }
                udane.getAndAdd(1);
            }
            skonczone.set(true);
        }, "ogloszenie-poster");
        t.start();
        while (!skonczone.get()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                t.interrupt();
                break;
            }
            eventBus.post(new PluginMessageEvent("commands", "moderation", "znaneAkcje-add:" +
                    wiadomosc.getId()));
            if (emotka == null) wiadomosc.editMessage(context.getTranslated("ogloszenie.post.sending",
                    "\u2699", udane.get(), serwery.size(), nieudane.get(), nieWysylaj.get(), nieMaKanalu.get()))
                    .complete();
            else wiadomosc = wiadomosc.editMessage(context.getTranslated("ogloszenie.post.sending",
                    emotka.getAsMention(), udane.get(), serwery.size(), nieudane.get(), nieWysylaj.get(),
                    nieMaKanalu.get())).complete();
        }
        context.send(context.getTranslated("ogloszenie.post.done"));
        return true;
    }

    private EmbedBuilder ogloszenieEmbed(Message msg, Tlumaczenia t, Language jezyk, Guild g) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(msg.getAuthor()), null,
                msg.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setTitle(t.get(jezyk, "ogloszenie.title"));
        eb.setDescription(parseContent(msg.getContentRaw(), g,
                UserUtil.getPermlevel(msg.getAuthor(), shardManager) == PermLevel.BOTOWNER, jezyk));
        eb.setTimestamp(msg.isEdited() ? msg.getTimeEdited() : msg.getTimeCreated());
        if (msg.getMember() != null) eb.setColor(msg.getMember().getColor());
        else eb.setColor(UserUtil.getPrimColor(msg.getAuthor()));
        return eb;
    }

    private String parseContent(String cnt, Guild g, boolean enableCodeExec, Language jezyk) {
        GuildConfig gc = gcCache.get(g.getId(), guildDao::get);
        StringBuffer buf = new StringBuffer();
        if (enableCodeExec) {
            Matcher matcher = CODE_REGEX.matcher(cnt);
            while (matcher.find()) {
                String kod = matcher.group(1);
                Context ctx = Context.enter();
                ctx.setLanguageVersion(Context.VERSION_ES6);
                ScriptableObject scr = ctx.initStandardObjects();
                int attrib = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
                scr.defineProperty("guildConfig", gc, attrib);
                scr.defineProperty("guild", g, attrib);
                scr.defineProperty("shardManager", shardManager, attrib);
                scr.defineProperty("tlumaczenia", tlumaczenia, attrib);
                scr.defineProperty("language", jezyk, attrib);
                Script script = ctx.compileString(kod, "<ogloszeniacnt>", 1, null);
                try {
                    String res = (String) Context.jsToJava(script.exec(ctx, scr), String.class);
                    matcher.appendReplacement(buf, res);
                } catch (Exception e) {
                    matcher.appendReplacement(buf, "<EVAL ERROR>");
                }
            }
            matcher.appendTail(buf);
        } else buf = new StringBuffer(cnt);
        return buf.toString().replace("%PREFIX%", managerKomend.getPrefixes(g).get(0));
    }
}
