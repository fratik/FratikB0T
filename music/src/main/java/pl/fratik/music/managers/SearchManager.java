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

package pl.fratik.music.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.sentry.Sentry;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.TimeUtil;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SearchManager {
    private final Logger logger = LoggerFactory.getLogger(SearchManager.class);
    private final String ytApiUrl;
    private final String ytApiUrlThumbnails;
    private final String ytApiUrlDuration;
    private final String ytApiUrlThumbnailsAndDuration;
    private final NowyManagerMuzyki managerMuzyki;

    private final Cache<String, SearchResult> youtubeResults = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(2000).build();
    private final Cache<String, SearchResult.SearchEntry> entryCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(2000).build();

    public SearchManager(String ytApiKey, String ytApiKeyThumbnails, NowyManagerMuzyki managerMuzyki) {
        ytApiUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&key=" + ytApiKey + "&maxResults=20&q=";
        ytApiUrlThumbnails = "https://www.googleapis.com/youtube/v3/videos?part=snippet&key=" + ytApiKeyThumbnails + "&id=";
        ytApiUrlDuration = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails&key=" + ytApiKeyThumbnails + "&id=";
        ytApiUrlThumbnailsAndDuration = "https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails&key=" + ytApiKeyThumbnails + "&id=";
        this.managerMuzyki = managerMuzyki;
    }

    public SearchResult searchYouTube(String query) {
        return searchYouTube(query, true);
    }

    @SuppressWarnings("squid:S1192")
    public SearchResult searchYouTube(String query, boolean thumbnailAndDuration) {
        final String origQuery = query;
        SearchResult result = youtubeResults.getIfPresent(origQuery.toLowerCase());
        if (result != null) return result;
        try {
            query = URLEncoder.encode(query, "UTF-8");
            String data = new String(NetworkUtil.download(ytApiUrl + query), Charsets.UTF_8);
            JsonObject element = GsonUtil.GSON.fromJson(data, JsonObject.class);

            SearchResult resultObj = new SearchResult();

            if (element.has("error"))
                throw new IllegalStateException("Błąd żądania: " + data);
            try { //NOSONAR
                element.getAsJsonArray("items").forEach(jsonElement -> {
                    JsonObject o = jsonElement.getAsJsonObject();
                    if (o.get("id").getAsJsonObject().get("kind").getAsString().equals("youtube#video")) {
                        String videoId = o.get("id").getAsJsonObject().get("videoId").getAsString();
                        String title = o.get("snippet").getAsJsonObject().get("title").getAsString();
                        long duration = 0;
                        String thumbnailURL = null;
                        if (thumbnailAndDuration) {
                            try {
                                SearchResult.SearchEntry e = entryCache.getIfPresent(videoId);
                                if (e != null) {
                                    duration = e.getDurationAsInt();
                                    thumbnailURL = e.getThumbnailURL();
                                } else {
                                    String data2 = new String(NetworkUtil.download(ytApiUrlThumbnailsAndDuration + videoId), Charsets.UTF_8);
                                    JsonObject element2 = GsonUtil.GSON.fromJson(data2, JsonObject.class);

                                    JsonObject thumbnails = element2.get("items").getAsJsonArray().get(0).getAsJsonObject()
                                            .get("snippet").getAsJsonObject().get("thumbnails").getAsJsonObject();
                                    JsonObject maxResThumbnail = null;
                                    for (String key : thumbnails.keySet()) {
                                        if (maxResThumbnail == null) {
                                            maxResThumbnail = thumbnails.get(key).getAsJsonObject();
                                            continue;
                                        }
                                        if (thumbnails.get(key).getAsJsonObject().get("height").getAsInt() > maxResThumbnail.get("height").getAsInt())
                                            maxResThumbnail = thumbnails.get(key).getAsJsonObject();
                                    }
                                    if (maxResThumbnail == null) throw new IllegalStateException("brak miniatur");
                                    thumbnailURL = maxResThumbnail.get("url").getAsString();
                                    duration = TimeUnit.MILLISECONDS.convert(Duration.parse(element2.get("items").getAsJsonArray().get(0).getAsJsonObject()
                                            .get("contentDetails").getAsJsonObject().get("duration").getAsString()).get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
                                    SearchResult searchResult = new SearchResult();
                                    searchResult.addEntry(title, "https://youtube.com/watch?v=" + videoId, duration, thumbnailURL);
                                    entryCache.put(videoId, searchResult.entries.get(0));
                                }
                            } catch (Exception ignored) {
                                /*lul*/
                            }
                        }
                        resultObj.addEntry(title, "https://youtube.com/watch?v=" + videoId, duration, thumbnailURL);
                    }
                });

            } catch (Exception ex) {
                throw new IllegalStateException(ex.toString() + ": " + data);
            }
            youtubeResults.put(origQuery.toLowerCase(), resultObj);
            return resultObj;
        } catch (Exception e) {
            logger.error("Nie udało się przeszukać YouTube'a!", e);
            try {
                List<AudioTrack> audioTracks = managerMuzyki.getAudioTracks("ytsearch:" + NetworkUtil.decodeURIComponent(query));
                SearchResult resultObj = new SearchResult();
                for (AudioTrack track : audioTracks) {
                    String thumbnailURL = null;
                    try { //NOSONAR
                        String data2 = new String(NetworkUtil.download(ytApiUrlThumbnails + extractIdFromUri(track.getInfo().uri)), Charsets.UTF_8);
                        JsonObject element2 = GsonUtil.GSON.fromJson(data2, JsonObject.class);

                        JsonObject thumbnails = element2.get("items").getAsJsonArray().get(0).getAsJsonObject()
                                .get("snippet").getAsJsonObject().get("thumbnails").getAsJsonObject();
                        LinkedList<String> list = new LinkedList<>(thumbnails.keySet());
                        list.sort(Comparator.reverseOrder());
                        thumbnailURL = thumbnails.get(list.get(0)).getAsJsonObject().get("url").getAsString();
                        SearchResult searchResult = new SearchResult();
                        searchResult.addEntry(track.getInfo().title, track.getInfo().uri, track.getDuration(), thumbnailURL);
                        entryCache.put(extractIdFromUri(track.getInfo().uri), searchResult.entries.get(0));
                    } catch (Exception ignored) {/*lul*/}
                    resultObj.addEntry(track.getInfo().title, track.getInfo().uri, track.getDuration(), thumbnailURL);
                }
                youtubeResults.put(origQuery.toLowerCase(), resultObj);
                return resultObj;
            } catch (Exception e2) {
                logger.error("Nie udało się przeszukać YouTube'a przez lavalinka!", e);
            }
            Sentry.capture(e);
        }
        return null;
    }

    public String extractIdFromUri(String uri) {
        return uri.substring(uri.length() - 11);
    }

    /* używaj odpowiedniego endpoint'a, quota się kłania */

    public SearchResult.SearchEntry getThumbnail(String videoId) {
        SearchResult.SearchEntry kupa = entryCache.getIfPresent(videoId);
        if (kupa != null && kupa.getThumbnailURL() != null) return kupa;
        try {
            String data = new String(NetworkUtil.download(ytApiUrlThumbnails + videoId), Charsets.UTF_8);
            JsonObject element2 = GsonUtil.GSON.fromJson(data, JsonObject.class);
            JsonObject thumbnails = element2.get("items").getAsJsonArray().get(0).getAsJsonObject()
                    .get("snippet").getAsJsonObject().get("thumbnails").getAsJsonObject();
            JsonObject maxResThumbnail = null;
            for (String key : thumbnails.keySet()) {
                if (maxResThumbnail == null) {
                    maxResThumbnail = thumbnails.get(key).getAsJsonObject();
                    continue;
                }
                if (thumbnails.get(key).getAsJsonObject().get("height").getAsInt() > maxResThumbnail.get("height").getAsInt())
                    maxResThumbnail = thumbnails.get(key).getAsJsonObject();
            }
            if (maxResThumbnail == null) throw new IllegalStateException("brak miniatur");
            SearchResult obj = new SearchResult();
            obj.addEntry(element2.get("items").getAsJsonArray().get(0).getAsJsonObject().get("snippet").getAsJsonObject()
                            .get("title").getAsString(),
                    "https://youtube.com/watch?v=" + videoId, 0,
                    maxResThumbnail.getAsJsonObject().get("url").getAsString());
            SearchResult.SearchEntry odp = obj.entries.get(0);
            entryCache.put(videoId, odp);
            return odp;
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;

    }

    public SearchResult.SearchEntry getThumbnailAndDuration(String videoId) {
        SearchResult.SearchEntry kupa = entryCache.getIfPresent(videoId);
        if (kupa != null && kupa.hasDuration() && kupa.getThumbnailURL() != null) return kupa;
        try {
            String data = new String(NetworkUtil.download(ytApiUrlThumbnailsAndDuration + videoId), Charsets.UTF_8);
            JsonObject element2 = GsonUtil.GSON.fromJson(data, JsonObject.class);
            JsonObject thumbnails = element2.get("items").getAsJsonArray().get(0).getAsJsonObject()
                    .get("snippet").getAsJsonObject().get("thumbnails").getAsJsonObject();
            JsonObject maxResThumbnail = null;
            for (String key : thumbnails.keySet()) {
                if (maxResThumbnail == null) {
                    maxResThumbnail = thumbnails.get(key).getAsJsonObject();
                    continue;
                }
                if (thumbnails.get(key).getAsInt() > maxResThumbnail.get("height").getAsInt())
                    maxResThumbnail = thumbnails.get(key).getAsJsonObject();
            }
            if (maxResThumbnail == null) throw new IllegalStateException("brak miniatur");
            SearchResult obj = new SearchResult();
            obj.addEntry(element2.get("items").getAsJsonArray().get(0).getAsJsonObject().get("snippet").getAsJsonObject()
                            .get("title").getAsString(), "https://youtube.com/watch?v=" + videoId,
                    TimeUnit.MILLISECONDS.convert(Duration.parse(element2.get("items").getAsJsonArray()
                            .get(0).getAsJsonObject().get("contentDetails").getAsJsonObject().get("duration")
                            .getAsString()).get(ChronoUnit.SECONDS), TimeUnit.SECONDS),
                    maxResThumbnail.getAsJsonObject().get("url").getAsString());
            SearchResult.SearchEntry odp = obj.entries.get(0);
            entryCache.put(videoId, odp);
            return odp;
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }

    public SearchResult.SearchEntry getDuration(String videoId) {
        SearchResult.SearchEntry kupa = entryCache.getIfPresent(videoId);
        if (kupa != null && kupa.hasDuration() && kupa.getThumbnailURL() != null) return kupa;
        try {
            String data = new String(NetworkUtil.download(ytApiUrlDuration + videoId), Charsets.UTF_8);
            JsonObject element2 = GsonUtil.GSON.fromJson(data, JsonObject.class);
            SearchResult obj = new SearchResult();
            obj.addEntry(null, "https://youtube.com/watch?v=" + videoId,
                    TimeUnit.MILLISECONDS.convert(Duration.parse(element2.get("items").getAsJsonArray()
                            .get(0).getAsJsonObject().get("contentDetails").getAsJsonObject().get("duration")
                            .getAsString()).get(ChronoUnit.SECONDS), TimeUnit.SECONDS),
                    null);
            SearchResult.SearchEntry odp = obj.entries.get(0);
            entryCache.put(videoId, odp);
            return odp;
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }

    public static class SearchResult {
        @Getter private final List<SearchEntry> entries = new ArrayList<>();

        public void addEntry(String title, String url, long duration, String thumbnailURL) {
            entries.add(new SearchEntry(title, url, duration, thumbnailURL));
        }

        @Getter
        public static class SearchEntry {
            private final String title;
            private final String url;
            private final long duration;
            private final String thumbnailURL;

            SearchEntry(String title, String url, long duration, String thumbnailURL) {
                this.title = title;
                this.url = url;
                this.duration = duration;
                this.thumbnailURL = thumbnailURL;
            }

            public String getDuration() {
                return TimeUtil.getStringFromMillis(duration);
            }

            public long getDurationAsInt() {
                return duration;
            }

            boolean hasDuration() {
                return duration != 0;
            }
        }
    }

}
