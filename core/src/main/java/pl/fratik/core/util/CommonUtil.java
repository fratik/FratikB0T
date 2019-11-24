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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.internal.entities.AbstractMessage;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    private CommonUtil() {}

    public static boolean checkCooldown(Map<Guild, Long> cooldowns, CommandContext context, long time) {
        if (cooldowns != null) {
            Message message = context.getEvent().getMessage();
            if (cooldowns.containsKey(message.getGuild())) {
                if (cooldowns.get(message.getGuild()) > System.currentTimeMillis()) {
                    CommonErrors.cooldown(context);
                    return true;
                } else {
                    cooldowns.remove(message.getGuild());
                    cooldowns.put(message.getGuild(), System.currentTimeMillis() + time);
                }
            } else {
                cooldowns.put(message.getGuild(), System.currentTimeMillis() + time);
            }
        }
        return false;
    }

    public static String fromStream(InputStream stream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(stream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while(result != -1) {
            buf.write((byte) result);
            result = bis.read();
        }
        bis.close();
        buf.close();
        return buf.toString("UTF-8");
    }

    public static <T> Set<T> reversedSet(Set<T> set) {
        List<T> list = new ArrayList<>(set);
        Collections.reverse(list);
        return new LinkedHashSet<>(list);
    }

    public static boolean throwsException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public static void supressException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            //nic
        }
    }

    public static <V> V supressException(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            //nic
        }
        return null;
    }

    public static <T> void supressException(Consumer<T> runnable, T arg) {
        try {
            runnable.accept(arg);
        } catch (Exception e) {
            //nic
        }
    }

    public static <T, R> R supressException(Function<T, R> function, T arg) {
        try {
            return function.apply(arg);
        } catch (Exception e) {
            return null;
        }
    }

    public static Color getPrimColorFromImageUrl(String url) {
        if (url == null) return null;
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/primColor?imageURL=" + URLEncoder.encode(url, "UTF-8"),
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null) return null;
            int r = -1;
            int g = -1;
            int b = -1;
            for (Object color : zdjecie.getJSONArray("color")) {
                if (r == -1) r = (int) color;
                if (g == -1) g = (int) color;
                if (b == -1) b = (int) color;
            }
            return new Color(r, g, b);
        } catch (Exception e) {
            LoggerFactory.getLogger(UserUtil.class).error("Błąd w uzyskiwaniu koloru!", e);
            return null;
        }
    }

    public static String getImageUrl(String content) {
        return getImageUrl(new AbstractMessage(content, "fake", false) {
            @Override
            protected void unsupported() {
                throw new IllegalStateException("gay");
            }

            @Nullable
            @Override
            public MessageActivity getActivity() {
                return null;
            }

            @Override
            public long getIdLong() {
                return 0;
            }

            @Nonnull
            @Override
            public List<Attachment> getAttachments() {
                return Collections.emptyList();
            }
        });
    }

    public static String getImageUrl(Message msg) {
        Matcher matcher = Pattern.compile("[(http(s)?)://(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6" +
                "}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(msg.getContentRaw());
        if (matcher.matches()) return matcher.group();
        if (!msg.getAttachments().isEmpty()) return msg.getAttachments().get(0).getUrl();
        return null;
    }

    public static double round(double value, int scale, RoundingMode mode) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(scale, mode);
        return bd.doubleValue();
    }
}