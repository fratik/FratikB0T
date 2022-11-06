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

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.entities.AbstractMessage;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;

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

    public static final Pattern ID_REGEX = Pattern.compile("\\d{17,18}");
    public static final Pattern URL_PATTERN = Pattern.compile("(http(s)?)://(www\\.)?([a-zA-Z0-9@:-]{1,256}\\.)+" +
            "([a-z]{2,24})\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(http(s)?)://(www\\.)?([a-zA-Z0-9@:-]{1,256}\\.)+" +
            "([a-z]{2,24})\\b([-a-zA-Z0-9@:%_+.~#?&/=]*\\.(a?png|jpe?g|gif|webp|tiff|svg))", Pattern.CASE_INSENSITIVE);
    private static final String BLOCK = "\u2589\uFE0F";

//    public static boolean checkCooldown(Map<Guild, Long> cooldowns, CommandContext context, long time) {
//        if (cooldowns != null) {
//            Message message = context.getEvent().getMessage();
//            if (cooldowns.containsKey(message.getGuild())) {
//                if (cooldowns.get(message.getGuild()) > System.currentTimeMillis()) {
//                    CommonErrors.cooldown(context);
//                    return true;
//                } else {
//                    cooldowns.remove(message.getGuild());
//                    cooldowns.put(message.getGuild(), System.currentTimeMillis() + time);
//                }
//            } else {
//                cooldowns.put(message.getGuild(), System.currentTimeMillis() + time);
//            }
//        }
//        return false;
//    }

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
        Matcher matcher = IMAGE_PATTERN.matcher(msg.getContentRaw());
        if (matcher.find()) return matcher.group();
        if (!msg.getAttachments().isEmpty() && msg.getAttachments().get(0).isImage())
            return msg.getAttachments().get(0).getUrl();
        return null;
    }

    public static double round(double value, int scale, RoundingMode mode) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(scale, mode);
        return bd.doubleValue();
    }

//    public static String resolveName(Command cmd, Tlumaczenia t, Language l) {
//        String raw = t.get(l, cmd.getName() + ".help.name");
//        if (raw.isEmpty()) return cmd.getName();
//        else {
//            String[] aliasy = raw.toLowerCase().split("\\|");
//            if (aliasy[0].isEmpty()) return cmd.getName();
//            else return aliasy[0].trim();
//        }
//    }

    public static boolean isPomoc(ShardManager shardManager, Guild g) {
        TextChannel kanal = Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild))
                .getTextChannelById(Ustawienia.instance.popChannel);
        if (kanal == null) throw new NullPointerException("nieprawidłowy popChannel");
        List<Message> wiads = kanal.getHistory().retrievePast(50).complete();
        for (Message mess : wiads) {
            if (mess.getEmbeds().isEmpty()) continue;
            //noinspection ConstantConditions
            String id = mess.getEmbeds().get(0).getFooter().getText().split(" \\| ")[1];
            if (id.equals(g.getId())) {
                return true;
            }
        } return false;
    }

    private static String append(int ii) {
        if (ii == 0) return "";
        StringBuilder s2 = new StringBuilder();
        for (int i = 1; i < ii; i++) { s2.append(BLOCK); }
        return BLOCK + s2;
    }

    public static String generateProgressBar(int procent, boolean showPrecentage) {
        int niebieskie = (int) (CommonUtil.round(procent, -1, RoundingMode.HALF_UP) / 10);
        int biale = 10;
        if (niebieskie != 10) {
            biale -= niebieskie;
        } else biale = 0;
        String format = "[%s](%s)%s";
        if (showPrecentage) format += " %s%%";
        if (!showPrecentage) return String.format(format, append(niebieskie), Ustawienia.instance.botUrl, append(biale));
        else return String.format(format, append(niebieskie), Ustawienia.instance.botUrl, append(biale), procent);
    }

    public static String asHex(Color color) {
        String hexColor = Integer.toHexString(color.getRGB() & 0xffffff);
        if (hexColor.length() < 6) {
            hexColor = "000000".substring(0, 6 - hexColor.length()) + hexColor;
        }
        return hexColor;
    }

    public static boolean canTalk(MessageChannel chan) {
        if (chan instanceof GuildMessageChannel) return ((GuildMessageChannel) chan).canTalk();
        if (chan.getType() == ChannelType.GROUP || chan.getType() == ChannelType.PRIVATE)
            return true;
        return false;
    }
}