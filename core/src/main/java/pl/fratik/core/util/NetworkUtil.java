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

package pl.fratik.core.util;

import lombok.Getter;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtil {
    private NetworkUtil() {}

    private static final String USER_AGENT = "FratikB0T/3.0.0 (https://fratikbot.pl)";
    private static final String UA = "User-Agent";
    private static final String AUTH = "Authorization";
    @Getter private static final OkHttpClient client = new OkHttpClient();

    public static byte[] download(String url, String authorization) throws IOException {
        Response res = downloadResponse(url, authorization);
        return res.body() == null ? new byte[0] : res.body().bytes();
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

    public static Response headRequest(String url) throws IOException {
        return client.newCall(new Request.Builder().head().header(UA, USER_AGENT).url(url).build()).execute();
    }

    public static Response postRequest(String url, MediaType type, String content, String authorization)  throws IOException {
        Request.Builder req = new Request.Builder()
                .header(UA, USER_AGENT);
        if (authorization != null) req = req.header(AUTH, authorization);
        req = req.url(url)
                .post(RequestBody.create(type, content));
        return client.newCall(req.build()).execute();
    }

    public static JSONObject getJson(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONObject(res.body().string());
    }

    public static JSONArray getJsonArray(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONArray(res.body().string());
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

    private static Response downloadResponse(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        return client.newCall(req).execute();
    }

    public static byte[] download(String url) throws IOException {
        Response res = downloadResponse(url);
        return res.body() == null ? new byte[0] : res.body().bytes();
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
}

