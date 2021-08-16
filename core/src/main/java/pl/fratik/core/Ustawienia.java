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
    public String token = "tylko w przypadku szyfrowania configu";
    public String devs = "267761613438713876";
    public String gadmRole = "371306270030037024";
    public String zgaRole = "414418843352432640";
    public String devRole = "428561212796829707";
    public String botGuild = "345655892882096139";
    public String productionBotId = "338359366891732993";
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
    public String botUrl = "https://fratikbot.pl";
    public String host = "127.0.0.1";
    public int port = 8080;
    public String wsHost = "127.0.0.1";
    public int wsPort = 4000;
    public LogSettings logSettings = new LogSettings();
    public FdevStats fdevStats = new FdevStats();
    @SerializedName("allowedIPs")
    public List<String> allowedIPs = new ArrayList<>();
    public String translationUrl = "https://translate.fratikbot.pl";
    public String socketAdress = "http://localhost:4000";

    public static class GamesClass {
        public Activity.ActivityType type = Activity.ActivityType.DEFAULT;
        public List<String> games = Arrays.asList("Shard {SHARD}/{SHARDS}", "v{VERSION}", "{USERS:ALL} użytkowników", "{SERVERS:ALL} serwerów", "{PREFIX}help", "{PREFIX}pop aby zawołać wsparcie bota", "{PREFIX}dashboard", "discord.gg/CZ8pXah \uD83D\uDE03");
    }

    public static class Emotki {
        public String greenTick = "436919889207361536";
        public String redTick = "436919889232658442";
        public String fratikCoin = "477416566556721174";
        public String online = "440100180591902720";
        public String idle = "440100179920551938";
        public String dnd = "440100180008894465";
        public String offline = "440100180142850059";
        public String loading = "503651397049516053";
        public String osu300 = "648144059830894603";
        public String osu100 = "648144059877294081";
        public String osu50 = "648144059415789569";
        public String osumiss = "648144059172651010";
        public String osukatu = "648144061068214285";
        public String osugeki = "648144059478573056";
        public String osuSS = "648144059315126292";
        public String osuSSH = "648144059160068096";
        public String osuS = "648144059185233940";
        public String osuSH = "648144058635780097";
        public String osuA = "648144059231371285";
        public String osuB = "648144059692613633";
        public String osuC = "648144059025588254";
        public String osuD = "648144059105411083";
        // mody, patrz OsuCommand.java:150
        public String osuNF = "648144059126251521";
        public String osuEZ = "648144059088633897";
        public String osuTD = "648168370692423716";
        public String osuHD = "648144059314995250";
        public String osuHR = "648144059965374505";
        public String osuSD = "648144059600470041";
        public String osuDT = "648144059730493450";
        public String osuRX = "648144059268988928";
        public String osuHT = "648144059143159848";
        public String osuNC = "648144059147354112";
        public String osuFL = "648144059080245297";
        public String osuAO = "648144060539863066";
        public String osuSO = "648144059411726336";
        public String osuAP = "648144059184971776";
        public String osuPF = "648144058992295937";
        public String osu4K = "648144058757283853";
        public String osu5K = "648144058602094597";
        public String osu6K = "648144059432697856";
        public String osu7K = "648144059042627584";
        public String osu8K = "648144059285766194";
        public String osuFI = "648144059629568011";
        public String osuRD = "648144059688419339";
        public String osuCN = "648144058744569857";
        public String osuTP = "648144059491287050";
        public String osu9K = "648171467187683339";
        public String osuCOOPK = "648152364007555092";
        public String osu1K = "648169231308816394";
        public String osu3K = "648169231527182356";
        public String osu2K = "648169231510274048";
        public String osuV2 = "648158730017570816";
        public String osuLM = "648152361935831053";
        public String chinczykDefault = "<:default:867068362499948573>"; //w markdown!
        public String chinczykDark = "<:dark:867068374550970368>"; //w markdown!
        public String chinczykAmoled = "<:amoled:867346828903186432>"; //w markdown!
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
        public String ostakt = "";
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
