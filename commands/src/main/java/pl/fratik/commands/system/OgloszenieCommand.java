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

package pl.fratik.commands.system;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OgloszenieCommand extends NewCommand {

    private static final Pattern CODE_REGEX = Pattern.compile("<js>(.*?)</js>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ShardManager shardManager;
    private final GuildDao guildDao;
    private final EventBus eventBus;
    private final Tlumaczenia tlumaczenia;

    private final Cache<GuildConfig> gcCache;

    public OgloszenieCommand(ShardManager shardManager, GuildDao guildDao, EventBus eventBus, Tlumaczenia tlumaczenia, RedisCacheManager rcm) {
        this.guildDao = guildDao;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        this.tlumaczenia = tlumaczenia;
        name = "ogloszenie";
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        context.deferAsync(false);
        if (!Globals.inFratikDev) throw new IllegalStateException("nie na FratikDev");
        TextChannel kanau = shardManager.getTextChannelById(Ustawienia.instance.ogloszeniaBota);
        if (kanau == null) throw new IllegalStateException("brak kanału");
        List<Message> msgs = kanau.getHistory().retrievePast(1).complete();
        if (msgs.isEmpty()) {
            context.reply(context.getTranslated("ogloszenie.no.message"));
            return;
        }
        EmbedBuilder eb = ogloszenieEmbed(msgs.get(0), context.getTlumaczenia(), context.getLanguage(),
                context.getGuild());
        context.sendMessage(eb.build());
    }

    private EmbedBuilder ogloszenieEmbed(Message msg, Tlumaczenia t, Language jezyk, Guild g) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(UserUtil.formatDiscrim(msg.getAuthor()), null,
                msg.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setTitle(t.get(jezyk, "ogloszenie.title"));
        eb.setImage(CommonUtil.getImageUrl(msg));
        eb.setDescription(parseContent(msg.getContentRaw(), g, UserUtil.isBotOwner(msg.getAuthor().getIdLong()), jezyk));
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
        return buf.toString();
    }
}
