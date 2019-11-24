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

package pl.fratik.core;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Activity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("ALL")
public class Ustawienia {
    @SuppressWarnings("squid:S1444")
    public static Ustawienia instance;
    public String devs = "267761613438713876";
    public String gadmRole = "371306270030037024";
    public String zgaRole = "414418843352432640";
    public String devRole = "428561212796829707";
    public String botGuild = "345655892882096139";
    public String prefix = "fb!";
    public ShardSettings shard = new ShardSettings();
    public Lavalink lavalink = new Lavalink();
    public Map<String, String> apiKeys = new HashMap<>();
    public Map<String, String> apiUrls = new HashMap<>();
    public PostgresSettings postgres = new PostgresSettings();
    public GamesClass games = new GamesClass();
    public Emotki emotki = new Emotki();
    public String ogloszeniaBota = "423164388699013124";
    public String popChannel = "423759965048668160";
    public String popLogChannel = "603744065179353088";
    public String popRole = "423855296415268865";
    public String zglosPrivChannel = "423759965048668160";
    public String botGuildInvite = "";
    public String host = "127.0.0.1";
    public String botUrl = "https://fratikbot.pl";
    public int port = 8080;
    public LogSettings logSettings = new LogSettings();
    public FdevStats fdevStats = new FdevStats();

    public static class GamesClass {
        public Activity.ActivityType type = Activity.ActivityType.DEFAULT;
        public List<String> games = Arrays.asList("Shard {SHARD}/{SHARDS}", "v{VERSION}", "{USERS:ALL} użytkowników", "{SERVERS:ALL} serwerów", "{PREFIX}help", "{PREFIX}pop aby zawołać wsparcie bota", "{PREFIX}dashboard", "discord.gg/CZ8pXah \uD83D\uDE03");
    }

    public static class Emotki {
        public String greenTick = "0";
        public String redTick = "0";
        public String fratikCoin = "0";
        public String online = "0";
        public String idle = "0";
        public String dnd = "0";
        public String offline = "0";
        public String loading = "0";
        public String osu300 = "0";
        public String osu100 = "0";
        public String osu50 = "0";
        public String osumiss = "0";
        public String osukatu = "0";
        public String osugeki = "0";
        public String osuSS = "0";
        public String osuSSH = "0";
        public String osuS = "0";
        public String osuSH = "0";
        public String osuA = "0";
        public String osuB = "0";
        public String osuC = "0";
        public String osuD = "0";
    }

    public static class ShardSettings {
        @SerializedName("shard-string")
        public String shardString = "0:0:1";
    }

    public static class PostgresSettings {
        @SerializedName("jdbc-url")
        public String jdbcUrl = "jdbc:postgresql://localhost/core";
        public String user = "core";
        public String password = "no chyba cie pogrzało nie znajdziesz tu hasła"; //NOSONAR
    }

    public static class LogSettings {
        @JsonAdapter(value = WebhookiAdapter.class, nullSafe = false)
        public Webhooki webhooki = new Webhooki();
    }

    public static class Lavalink {
        public List<LavalinkNode> nodes = new ArrayList<>();
        public String defaultPass = "";

        @AllArgsConstructor
        @NoArgsConstructor
        public static class LavalinkNode {
            public String address = "xxx.xxx.xxx.xxx";
            public String password = "";
            public int wsPort = 80;
            public int restPort = 2333;

            public String toString() {
                return "LavalinkNode(address=" + address + ", password=" + password + ", wsPort=" +
                        wsPort + ", restPort=" + restPort + ")";
            }
        }
    }

    public static class FdevStats {
        public String status = "";
        public String uptime = "";
        public String wersja = "";
        public String ping = "";
        public String users = "";
        public String serwery = "";
        public String ram = "";
        public String komdzis = "";
    }

    public static class Webhooki extends AbstractMap<String, String> {

        Webhooki() {
            super();
        }

        private Map<String, String> storage = new HashMap<>();

        public String commands = "";
        public String ga = "";
        public String gaPermLvl = "";
        public String lvlup = "";
        public String gban = "";
        public String bot = "";

        @NotNull
        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = new HashSet<>();
            for (Field k : getClass().getDeclaredFields()) {
                try {
                    set.add(new Entry<String, String>() {
                        private String value = (String) k.get(Webhooki.this);

                        @Override
                        public String getKey() {
                            return k.getName();
                        }

                        @Override
                        public String getValue() {
                            return value;
                        }

                        @Override
                        public String setValue(String value) {
                            this.value = value;
                            return value;
                        }
                    });
                } catch (Exception e) {
                    //nic
                }
            }
            set.addAll(storage.entrySet());
            return set;
        }

        @Override
        public String put(String key, String value) {
            try {
                getClass().getDeclaredField(key);
                throw new UnsupportedOperationException("nie można nadpisać fielda który istnieje!");
            } catch (NoSuchFieldException e) {
                return storage.put(key, value);
            }
        }
    }

    public static class WebhookiAdapter extends TypeAdapter<Webhooki> {
        @Override
        public void write(JsonWriter out, Webhooki value) throws IOException {
            out.beginObject();
            for (Map.Entry<String, String> e : value.entrySet())
                out.name(e.getKey()).value(e.getValue());
            out.endObject();
        }

        @Override
        public Webhooki read(JsonReader in) throws IOException {
            Webhooki val = new Webhooki();
            in.beginObject();
            while (in.peek() == JsonToken.NAME) {
                String xd = in.nextName();
                try {
                    Webhooki.class.getDeclaredField(xd).set(val, in.nextString());
                } catch (NoSuchFieldException e) {
                    val.put(xd, in.nextString());
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            in.endObject();
            return val;
        }
    }
}
