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

package pl.fratik.commands.zabawa;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.eventbus.EventBus;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.scienjus.client.PixivParserClient;
import com.scienjus.model.Work;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.NetworkUtil;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.FutureTask;

public class Rule34Command extends NewCommand {
    private final Logger logger;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private PixivParserClient pixiv;

    public Rule34Command(EventWaiter eventWaiter, EventBus eventBus) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "rule34";
        cooldown = 5;
        pixiv = new PixivParserClient();
        usage = "<zrodlo:string> <tagi:string> [pokaz:boolean]";
        try {
            pixiv.setUsername(Ustawienia.instance.apiKeys.get("pixiv").split(":=:")[0]);
            pixiv.setPassword(Ustawienia.instance.apiKeys.get("pixiv").split(":=:")[1]);
            if (!pixiv.login()) throw new LoginException("XD");
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Pixiv nie możliwy do użycia - nieprawidłowe dane logowania!");
            pixiv = null;
        }
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (context.getChannel().getType() != ChannelType.TEXT || !context.getChannel().asTextChannel().isNSFW()) {
            context.replyEphemeral(context.getTranslated("generic.not.nsfw"));
            return false;
        }
        return true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        InteractionHook hook = context.defer(!context.getArgumentOr("pokaz", false, OptionMapping::getAsBoolean));
        try {
            List<FutureTask<EmbedBuilder>> pages;
            switch (Sources.valueOf(context.getArguments().get("zrodlo").getAsString())) {
                case RULE34:
                    pages = resolveRule34(context);
                    break;
                case E621:
                    pages = resolveE621(context);
                    break;
                case PIXIV:
                    if (pixiv == null) throw new IOException("gej");
                    pages = resolvePixiv(context);
                    break;
                case PAHEAL:
                    pages = resolvePaheal(context);
                    break;
                case DANBOORU:
                    pages = resolveDanbooru(context);
                    break;
                default:
                    return;
            }
            if (pages.isEmpty()) {
                context.sendMessage(context.getTranslated("rule34.empty"));
                return;
            }
            new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                    context.getTlumaczenia(), eventBus).setEnableShuffle(true).setEnableDelett(true).setTimeout(300).create(hook);
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("rule34.fail"));
        }
    }

    public List<FutureTask<EmbedBuilder>> resolveRule34(NewCommandContext context) throws IOException {
        String tagi = context.getArguments().get("tagi").getAsString();
        int count = resolvePostsNumber("https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=" +
                NetworkUtil.encodeURIComponent(tagi));
        if (count == 0) {
            return Collections.emptyList();
        }
        return generatePages(context, "https://rule34.xxx/index.php?page=dapi&json=1&s=post&q=index&tags=" +
                NetworkUtil.encodeURIComponent(tagi), "pid", count,
                100, new TypeToken<List<Rule34Post>>() {});
    }

    private <T extends List<? extends Post>> List<FutureTask<EmbedBuilder>> generatePages(NewCommandContext context, String url, String pgq, int count, int limit, TypeToken<T> postCls) {
        Map<Integer, FutureTask<T>> strony = new HashMap<>();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        for (int i = 0; i <= count / limit; i++) {
            int finalI = i;
            strony.put(i, new FutureTask<>(() -> {
                String download = new String(NetworkUtil.download(url + "&" + pgq + "=" + finalI), StandardCharsets.UTF_8);
                try {
                    return GsonUtil.fromJSON(download, postCls);
                } catch (JsonParseException ex) {
                    logger.error("chujwdupei", ex);
                    logger.error(download);
                    throw ex;
                }
            }));
        }
        int strona = -1;
        for (int i = 0; i < count; i++) {
            if (i % limit == 0) strona++;
            int finalStrona = strona;
            int finalI = i;
            pages.add(new FutureTask<>(() -> {
                if (!strony.get(finalStrona).isDone()) strony.get(finalStrona).run();
                T posts = strony.get(finalStrona).get();
                Post post = posts.get(finalI % 100);
                return generateEmbed(context, post);
            }));
        }
        return pages;
    }

    @NotNull
    private EmbedBuilder generateEmbed(NewCommandContext context, Post post) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(context.getMember().getColor());
        eb.addField(context.getTranslated("rule34.score"), String.valueOf(post.getScore()), true);
        eb.addField(context.getTranslated("rule34.rating"),
                (post.getRating() == null ? Rating.EXPLICIT : post.getRating()).getShortName().toUpperCase(),
                true);
        if (post.isBanned()) {
            eb.setDescription(context.getTranslated("rule34.banned"));
            return eb;
        }
        if (post.getSourceImageUrl() == null) {
            eb.setDescription(context.getTranslated("rule34.no.image.found"));
            return eb;
        }
        eb.setDescription(String.format("[%s](%s)", context.getTranslated("rule34.direct"), post.getSourceImageUrl()));
        eb.setImage(post.getImageUrl() == null ? post.getSourceImageUrl() : post.getImageUrl());
        return eb;
    }

    public List<FutureTask<EmbedBuilder>> resolveE621(NewCommandContext context) throws IOException {
        String tagi = context.getArguments().get("tagi").getAsString();
        Map<Integer, List<E621Post>> strony = new HashMap<>();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        int i = 0;
        do {
            if (strony.size() == 5) break;
            String url;
            if (strony.isEmpty())
                url = "https://e621.net/posts.json?limit=320&tags=" +
                        NetworkUtil.encodeURIComponent(tagi + " -type:swf ");
            else {
                long lastId = -1;
                for (E621Post post : strony.get(i - 1)) {
                    if (lastId == -1) lastId = post.getId();
                    else if (lastId >= post.getId()) lastId = post.getId();
                }
                url = "https://e621.net/posts.json?limit=320&page=b" + lastId + "&tags=" +
                        NetworkUtil.encodeURIComponent(tagi + " -type:swf ");
            }
            strony.put(i, GsonUtil.fromJSON(NetworkUtil.download(url), new TypeToken<E621Wrapper>() {}).getPosts());
            i++;
        } while (!strony.get(i - 1).isEmpty());
        if (strony.get(i - 1).isEmpty())
            strony.remove(i - 1);
        if (strony.isEmpty()) return Collections.emptyList();
        int strona = -1;
        for (int j = 0; j < ((strony.size() - 1) * 320) + strony.get(strony.size() - 1).size(); j++) {
            if (j % 320 == 0) strona++;
            int finalStrona = strona;
            int finalI = j;
            pages.add(new FutureTask<>(() -> {
                List<E621Post> posts = strony.get(finalStrona);
                Post post = posts.get(finalI % 320);
                return generateEmbed(context, post);
            }));
        }
        return pages;
    }

    public List<FutureTask<EmbedBuilder>> resolvePixiv(NewCommandContext context) {
        String tagi = context.getArguments().get("tagi").getAsString();
        List<Work> works = pixiv.search(tagi);
        if (works.isEmpty()) {
            return Collections.emptyList();
        }
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        for (Work work : works) {
            pages.add(new FutureTask<>(() -> {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(context.getMember().getColor());
                eb.addField(context.getTranslated("rule34.score"), String.valueOf(work.getStats().getScore()), true);
                eb.addField(context.getTranslated("rule34.rating"), work.getAgeLimit(), true);
                String origImageUrl = null;
                String imageUrl = null;
                if (work.getImageUrls().getLarge() != null) {
                    origImageUrl = work.getImageUrls().getLarge();
                    imageUrl = work.getImageUrls().getLarge();
                }
                if (work.getImageUrls().getMedium() != null) {
                    imageUrl = work.getImageUrls().getMedium();
                    if (origImageUrl == null) origImageUrl = imageUrl;
                }
                if (work.getImageUrls().getSmall() != null) {
                    if (imageUrl == null) imageUrl = work.getImageUrls().getSmall();
                    if (origImageUrl == null) origImageUrl = work.getImageUrls().getSmall();
                }
                if (imageUrl == null) {
                    eb.setDescription(context.getTranslated("rule34.no.image.found"));
                    return eb;
                }
                eb.setDescription(String.format("[%s](%s)", context.getTranslated("rule34.direct"),
                        "https://api.fratikbot.pl/api/proxy/pixiv?path=" +
                        NetworkUtil.encodeURIComponent(origImageUrl.replace("https://i.pximg.net/", ""))));
                eb.setImage("https://api.fratikbot.pl/api/proxy/pixiv?path=" +
                        NetworkUtil.encodeURIComponent(imageUrl.replace("https://i.pximg.net/", "")));
                return eb;
            }));
        }
        return pages;
    }

    public List<FutureTask<EmbedBuilder>> resolvePaheal(NewCommandContext context) throws IOException {
        String tagi = context.getArguments().get("tagi").getAsString();
        String url = "https://rule34.paheal.net/api/danbooru/find_posts/index.xml?s=post&tags=" +
                NetworkUtil.encodeURIComponent(tagi);
        PahealPostsRoot root = GsonUtil.GSON.fromJson(XML.toJSONObject(new String(NetworkUtil.download(url))).toString(), PahealPostsRoot.class);
        int count = root.getPosts().getCount();
        if (count == 0) {
            return Collections.emptyList();
        }
        Map<Integer, FutureTask<PahealPostsRoot>> strony = new HashMap<>();
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        for (int i = 0; i <= count / 100; i++) {
            int finalI = i;
            strony.put(i, new FutureTask<>(() -> GsonUtil.GSON.fromJson(XML.toJSONObject(new String(NetworkUtil
                    .download(url + "&page=" + (finalI + 1)))).toString(), PahealPostsRoot.class)));
        }
        int strona = -1;
        for (int i = 0; i < count; i++) {
            if (i % 100 == 0) strona++;
            int finalStrona = strona;
            int finalI = i;
            pages.add(new FutureTask<>(() -> {
                if (!strony.get(finalStrona).isDone()) strony.get(finalStrona).run();
                PahealPostsRoot posts = strony.get(finalStrona).get();
                Post post = posts.getPosts().getPosts().get(finalI % 100);
                return generateEmbed(context, post);
            }));
        }
        return pages;
    }

    public List<FutureTask<EmbedBuilder>> resolveDanbooru(NewCommandContext context) throws IOException {
        String tagi = context.getArguments().get("tagi").getAsString();
        int count = GsonUtil.fromJSON(NetworkUtil.download("https://danbooru.donmai.us/counts/posts.json?tags=" +
                NetworkUtil.encodeURIComponent(tagi)), JsonObject.class).getAsJsonObject("counts")
                .get("posts").getAsInt();
        if (count == 0) {
            return Collections.emptyList();
        }
        return generatePages(context, "https://danbooru.donmai.us/posts.json?limit=200&tags=" +
                        NetworkUtil.encodeURIComponent(tagi), "page", count,
                200, new TypeToken<List<DanbooruPost>>() {});
    }

    private int resolvePostsNumber(String url) throws IOException {
        List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ApiXMLResponse resp = xmlMapper.readValue(NetworkUtil.download(url), ApiXMLResponse.class);
        Map<Integer, FutureTask<List<Rule34Post>>> strony = new HashMap<>();
        return resp.count;
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("zrodlo")) {
            for (Sources source : Sources.values()) {
                if (source == Sources.PIXIV && pixiv == null) continue;
                option.addChoice(source.name().toLowerCase(), source.name());
            }
        }
    }

    public enum Sources {
        RULE34, PAHEAL, E621, DANBOORU, PIXIV
    }

    @Data
    @AllArgsConstructor
    @JacksonXmlRootElement(namespace = "posts")
    public static class ApiXMLResponse {
        @JacksonXmlProperty(isAttribute = true)
        private int count;
        @JacksonXmlProperty(isAttribute = true)
        private int offset;
    }

    public interface Post {
        default String getSourceImageUrl() {
            return getImageUrl();
        }
        String getImageUrl();
        int getScore();
        Rating getRating();
        default boolean isBanned() {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class E621Post implements Post {
        private File file;
        private Sample sample;
        private Score score;
        private long id;
        private Rating rating;

        @Override
        public String getSourceImageUrl() {
            return file.getUrl();
        }

        @Override
        public String getImageUrl() {
            return sample.getUrl();
        }

        @Override
        public int getScore() {
            return score.getTotal();
        }

        public Score getScoreObj() {
            return score;
        }

        @Data
        @AllArgsConstructor
        public static class Score {
            private final int up;
            private final int down;
            private final int total;
        }

        @Data
        @AllArgsConstructor
        public static class File {
            private final String url;
        }

        @Data
        @AllArgsConstructor
        public static class Sample {
            private final String url;
        }
    }

    @Data
    @AllArgsConstructor
    public static class DanbooruPost implements Post {
        @SerializedName("file_url")
        private String fileUrl;
        private int score;
        @SerializedName("tag_string")
        private String tagString;
        private Rating rating;
        @SerializedName("is_banned")
        private boolean banned;

        @Override
        public String getImageUrl() {
            return fileUrl;
        }

        @Override
        public int getScore() {
            return score;
        }

    }

    @Data
    @AllArgsConstructor
    public static class Rule34Post implements Post {
        private String directory;
        private String image;
        private Rating rating;
        private int score;
        private String tags;

        @Override
        public String getImageUrl() {
            return String.format("https://img.rule34.xxx/images/%s/%s", directory, image);
        }

        @Override
        public int getScore() {
            return score;
        }

    }

    public static class SourcesArgument extends Argument {
        public SourcesArgument() {
            name = "source";
        }

        @Override
        protected Object execute(ArgumentContext context) {
            if (context.getArg().equalsIgnoreCase("r34")) return Sources.RULE34;
            if (context.getArg().equalsIgnoreCase("paheal")) return Sources.PAHEAL;
            if (context.getArg().equalsIgnoreCase("e621")) return Sources.E621;
            if (context.getArg().equalsIgnoreCase("danbooru")) return Sources.DANBOORU;
            if (context.getArg().equalsIgnoreCase("pixiv")) return Sources.PIXIV;
            return null;
        }
    }

    @Getter
    public enum Rating {
        /**
         * Safe for family and friends. If you had any.
         */
        @SerializedName("s")
        SAFE("s"),

        /**
         * Questionable board images. Borderline explicit.
         * Would you show this to your grandma?
         */
        @SerializedName("q")
        QUESTIONABLE("q"),

        /**
         * Default rating, assume the worst.
         * Board images with explicit/NSFW ratings.
         * Dirty af. Go see a therapist.
         */
        @SerializedName("e")
        EXPLICIT("e");

        String shortName, longName;

        Rating(String shortName) {
            this.shortName = shortName;
            this.longName = this.name().toLowerCase();
        }
    }

    @Data
    @AllArgsConstructor
    public static class PahealPostsRoot {
        private PahealPostData posts;

        @Data
        @AllArgsConstructor
        public static class PahealPostData {
            private int offset;
            private int count;
            @SerializedName("tag")
            @JsonAdapter(value = PahealPostAdapter.class)
            private List<PahealPost> posts;

            @Data
            @AllArgsConstructor
            public static class PahealPost implements Post {
                @SerializedName("file_url")
                private String fileUrl;
                private int score;
                private String tags;

                @Override
                public String getImageUrl() {
                    return fileUrl;
                }

                @Override
                public int getScore() {
                    return score;
                }

                @Override
                public Rating getRating() {
                    return null;
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class E621Wrapper {
        private final List<E621Post> posts;
    }

    private static class PahealPostAdapter implements JsonDeserializer<List<PahealPostsRoot.PahealPostData.PahealPost>> {
        @Override
        public List<PahealPostsRoot.PahealPostData.PahealPost> deserialize(JsonElement json, Type t, JsonDeserializationContext ctx) throws JsonParseException {
            if (json.isJsonArray()) {
                List<PahealPostsRoot.PahealPostData.PahealPost> h = new ArrayList<>();
                for (JsonElement el : json.getAsJsonArray()) {
                     h.add(ctx.deserialize(el, PahealPostsRoot.PahealPostData.PahealPost.class));
                }
                return h;
            }
            return Collections.singletonList(ctx.deserialize(json.getAsJsonObject(), PahealPostsRoot.PahealPostData.PahealPost.class));
        }
    }
}
