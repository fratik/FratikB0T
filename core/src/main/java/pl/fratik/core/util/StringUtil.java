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

import net.dv8tion.jda.api.entities.User;
import okhttp3.MediaType;
import okhttp3.Response;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StringUtil {

    private StringUtil() {}

    private static final SecureRandom rnd = new SecureRandom();

    public static String formatDiscrim(User u) {
        return u.getName() + "#" + u.getDiscriminator();
    }

    public static String zeroHexFill(String s) {
        if (s.length() < 4) {

            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < 4)
                sb.insert(0, "0");

            return sb.toString();
        }
        return s;
    }

    public static int getOccurencies(String string, String subString) {
        int lastIndex = 0;
        int count = 0;

        while (lastIndex != -1) {
            lastIndex = string.indexOf(subString, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += subString.length();
            }
        }

        return count;
    }

    public static String prettyPeriod(long millis) {
        if (millis == Long.MAX_VALUE) return "streaming";
        // because java builtin methods sucks...

        final long sekundy = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        final long minuty = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        final long godziny = TimeUnit.MILLISECONDS.toHours(millis);

        return String.format("%02d:%02d:%02d", godziny, minuty, sekundy);
    }

    public static String firstLetterUpperCase(String text) {
        return text.substring(0 ,1).toUpperCase() + text.substring(1);
    }

    public static String escapeMarkdown(String text) {
        return text == null ? "" : text.replaceAll("_", "\\_").replaceAll("\\*", "\\*")
                .replaceAll("~", "\\~").replaceAll("`", "\\`");
    }

    public static String generateId() {
        return generateId(9);
    }

    private static String generateId(int length) {
        return generateId(length, true);
    }

    private static String generateId(int length, boolean duzeLitery) {
        return generateId(length, duzeLitery, true);
    }

    private static String generateId(int length, boolean duzeLitery, boolean maleLitery) {
        return generateId(length, duzeLitery, maleLitery, false);
    }

    public static String generateId(int length, boolean duzeLitery, boolean maleLitery, boolean liczby) {
        return generateId(length, duzeLitery, maleLitery, liczby, false);
    }

    public static String generateId(int length, boolean duzeLitery, boolean maleLitery, boolean liczby, boolean symbole) {
        String possible = "";
        if (duzeLitery) possible += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (maleLitery) possible += "abcdefghijklmnopqrstuvwxyz";
        if (liczby) possible += "0123456789";
        if (symbole) possible += "`~!@#$%^&*()-_=+[{]}:;\"',.<>/?\\|";
        if (possible.isEmpty()) throw new IllegalArgumentException("Możliwe litery są puste");
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(possible.charAt(rnd.nextInt(possible.length())));
        return sb.toString();
    }

    public static String haste(String tresc) {
        try {
            Response res = NetworkUtil.postRequest("https://hastebin.com/documents",
                    MediaType.get("text/plain; charset=utf-8"), tresc, null);
            return new JSONObject(Objects.requireNonNull(res.body() != null ? res.body().string() : null)).getString("key");
        } catch (Exception e) {
            return null;
        }
    }
}
