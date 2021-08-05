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

package pl.fratik.core.util;

import lombok.Data;
import lombok.Getter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pl.fratik.core.Statyczne;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class NetworkUtil {
    private NetworkUtil() {}

    private static final String USER_AGENT = "FratikB0T/" + Statyczne.WERSJA_BEZ_BUILDA + " (https://fratikbot.pl)";
    private static final String UA = "User-Agent";
    private static final String AUTH = "Authorization";
    @Getter private static final OkHttpClient client = new OkHttpClient();
    private static Cache<ContentInformation> ciCache;

    public static void setUpContentInformationCache(RedisCacheManager rcm) {
        ciCache = rcm.new CacheRetriever<ContentInformation>(){}.getCache();
    }

    public static byte[] download(String url, String authorization) throws IOException {
        try (Response res = downloadResponse(url, authorization)) {
            return res.body() == null ? new byte[0] : res.body().bytes();
        }
    }

    public static Response downloadResponse(String url, String authorization) throws IOException {
        return downloadResponse(url, authorization, new HashMap<>());
    }

    public static Response downloadResponse(String url, String authorization, Map<String, String> headers) throws IOException {
        Request.Builder req = new Request.Builder()
                .header(UA, USER_AGENT);
        if (authorization != null) req = req.header(AUTH, authorization);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            req.header(e.getKey(), e.getValue());
        }
        req = req.url(url);
        return client.newCall(req.build()).execute();
    }

    public static ContentInformation contentInformation(String url) {
        return contentInformation(url, true);
    }

    public static ContentInformation contentInformation(String url, boolean cache) {
        final Supplier<ContentInformation> getter = () -> {
            AtomicReference<ContentInformation> ci = new AtomicReference<>();
            OkHttpClient client = new OkHttpClient.Builder(NetworkUtil.client).eventListener(new EventListener() {
                @Override
                public void responseHeadersEnd(@NotNull Call call, @NotNull Response response) {
                    // sprawdź content-type zanim zaczniesz bawić się body
                    String cntype = response.header("Content-Type");
                    if (response.isRedirect()) return;
                    if (cntype != null && !cntype.startsWith("text/html")) {
                        // jeżeli content-type nie jest "text/html", przerywamy żądanie - nie potrzebujemy body
                        ci.set(new ContentInformation(response.code(), cntype, response.header("Content-Length")));
                        call.cancel();
                    }
                }
            }).build();
            Call call = client.newCall(new Request.Builder().header(UA, USER_AGENT).url(url).build());
            call.timeout().timeout(5, TimeUnit.SECONDS); // przyczyna chyba oczywista
            try (final Response resp = call.execute()) {
                ContentInformation ogCi = new ContentInformation(resp.code(), resp.header("Content-Type"), resp.header("Content-Length"));
                ResponseBody body = resp.body();
                if (body == null || body.contentType() == null) throw new IOException();
                Document doc;
                try {
                    //noinspection ConstantConditions
                    doc = Jsoup.parse(body.byteStream(), body.contentType().charset(StandardCharsets.UTF_8).name(), "");
                } catch (Exception e) {
                    return ogCi; // nieprawidłowy dokument HTML? zwróć dane o odpowiedzi z nim
                }
                String type = doc.head().getElementsByTag("meta").stream().filter(el -> el.attr("property").equals("og:type"))
                        .findFirst().map(el -> el.attr("content")).orElse("website");
                if (!type.startsWith("video")) return ogCi; //gify / filmy są oznaczane jako video, nie ma typu na zdjęcia
                String imageUrl = doc.head().getElementsByTag("meta").stream().filter(el -> el.attr("property").equals("og:image"))
                        .findFirst().map(el -> el.attr("content")).orElse(null);
                String videoUrl = doc.head().getElementsByTag("meta").stream().filter(el -> el.attr("property").equals("og:video") ||
                        el.attr("property").equals("og:video:url")).findFirst().map(el -> el.attr("content")).orElse(null);
                if (imageUrl != null && imageUrl.contains(".gif")) return contentInformation(imageUrl, cache);
                else if (videoUrl != null) return contentInformation(videoUrl, cache);
                return ogCi;
            } catch (IOException e) {
                return ci.get();
            }
        };
        if (cache && ciCache != null) return ciCache.get(encodeURIComponent(url), unused -> getter.get());
        else return getter.get();
    }

    public static Response postRequest(String url, MediaType type, String content, String authorization)  throws IOException {
        Request.Builder req = new Request.Builder()
                .header(UA, USER_AGENT);
        if (authorization != null) req = req.header(AUTH, authorization);
        req = req.url(url)
                .post(RequestBody.create(content, type));
        return client.newCall(req.build()).execute();
    }

    public static JSONResponse getJson(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        try (Response res = client.newCall(req).execute()) {
            return res.body() == null ? null : new JSONResponse(res.body().string(), res.code());
        }
    }

    public static JSONArray getJsonArray(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        try (Response res = client.newCall(req).execute()) {
            return res.body() == null ? null : new JSONArray(res.body().string());
        }
    }

    public static JSONObject getJson(String url, String authorization) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .header(AUTH, authorization)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONObject(res.body().string());
    }

    public static Response downloadResponse(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        return client.newCall(req).execute();
    }

    public static byte[] download(String url) throws IOException {
        try (Response res = downloadResponse(url)) {
            return res.body() == null ? new byte[0] : res.body().bytes();
        }
    }

    public static byte[] getBytesFromBufferArray(int[] bufferArray) {
        byte[] arr = new byte[bufferArray.length];
        for (int i = 0; i < bufferArray.length; i++) {
            arr[i] = (byte) bufferArray[i];
        }
        return arr;
    }

    public static byte[] getBytesFromBufferArray(JSONArray bufferArray) {
        byte[] arr = new byte[bufferArray.length()];
        for (int i = 0; i < bufferArray.length(); i++) {
            arr[i] = (byte) ((int) bufferArray.get(i));
        }
        return arr;
    }

    /**
     * Decodes the passed UTF-8 String using an algorithm that's compatible with
     * JavaScript's <code>decodeURIComponent</code> function. Returns
     * <code>null</code> if the String is <code>null</code>.
     *
     * @param s The UTF-8 encoded String to be decoded
     * @return the decoded String
     */
    public static String decodeURIComponent(String s)
    {
        if (s == null)
        {
            return null;
        }

        String result;

        try
        {
            result = URLDecoder.decode(s, "UTF-8");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e)
        {
            result = s;
        }

        return result;
    }

    /**
     * Encodes the passed String as UTF-8 using an algorithm that's compatible
     * with JavaScript's <code>encodeURIComponent</code> function. Returns
     * <code>null</code> if the String is <code>null</code>.
     *
     * @param s The String to be encoded
     * @return the encoded String
     */
    public static String encodeURIComponent(String s)
    {
        String result;

        try
        {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e)
        {
            result = s;
        }

        return result;
    }

    @Data
    public static class ContentInformation {
        private final int code;
        private final String contentType;
        private final String contentLength;
    }
}

