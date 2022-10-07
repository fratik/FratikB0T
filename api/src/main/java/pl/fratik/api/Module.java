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

package pl.fratik.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.api.entity.*;
import pl.fratik.api.event.RundkaAnswerVoteEvent;
import pl.fratik.api.event.RundkaEndEvent;
import pl.fratik.api.event.RundkaNewAnswerEvent;
import pl.fratik.api.event.RundkaStartEvent;
import pl.fratik.api.internale.*;
import pl.fratik.core.Globals;
import pl.fratik.core.Statyczne;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

@SuppressWarnings("FieldCanBeLocal")
public class Module implements Modul {

    @Inject private NewManagerKomend managerKomend;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private UserDao userDao;
    @Inject private GbanDao gbanDao;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ShardManager shardManager;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerModulow managerModulow;
    @Inject private EventBus eventBus;
    @Inject private RedisCacheManager redisCacheManager;

    private Undertow undertow;
    private Undertow undertowWs;
    @Getter private RoutingHandler routes;
    @Getter private SocketManager socketManager;
    private Map<String, WscWrapper> webSocketChannels = new HashMap<>();
    private List<NewCommand> commands = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(Module.class);
    private RundkaDao rundkaDao;
    private RundkaGa rundkaGa;

    @Override
    public boolean startUp() {
        rundkaDao = new RundkaDao(managerBazyDanych, eventBus);
        commands = new ArrayList<>();
        routes = new RoutingHandler();
        routes.get("/api/basicinfo", ex -> {
            SelfUser selfUser = Objects.requireNonNull(shardManager.getShardById(0)).getSelfUser();
            ApplicationInfo apinfo = Objects.requireNonNull(shardManager.getShardById(0)).retrieveApplicationInfo().complete();
            Exchange.body().sendJson(ex, new BasicInfo(selfUser.getName(), selfUser.getDiscriminator(),
                    selfUser.getId(), apinfo.getDescription(), selfUser.getEffectiveAvatarUrl(), !apinfo.isBotPublic(),
                    generateInviteLink(selfUser.getId()), new ArrayList<>(tlumaczenia.getLanguages().keySet())
                    .stream().map(pl.fratik.api.entity.Language::new).collect(Collectors.toList())));
        });
        routes.get("/api/guilds", ex -> {
            List<String> ids = Exchange.queryParams().queryParams(ex, "id");
            Map<String, pl.fratik.api.entity.Guild> guilds = new HashMap<>();
            if (ids == null || ids.isEmpty()) { // plz nie
                for (Guild guild : shardManager.getGuilds())
                    guilds.put(guild.getId(), new pl.fratik.api.entity.Guild(guild));
            } else {
                for (String id : ids) {
                    Guild guild = shardManager.getGuildById(id);
                    guilds.put(id, guild == null ? null : new pl.fratik.api.entity.Guild(guild));
                }
            }
           Exchange.body().sendJson(ex, guilds);
        });
        routes.get("/api/user", ex -> {
            String userId = Exchange.queryParams().queryParam(ex, "userId").orElse(null);
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            try {
                if (shardManager.retrieveUserById(userId).complete() == null) {
                    Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                    return;
                }
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            User ussr = shardManager.retrieveUserById(userId).complete();
            Guild guild = shardManager.getGuildById(Ustawienia.instance.botGuild);
            Exchange.body().sendJson(ex, new pl.fratik.api.entity.User(ussr.getName(), ussr.getDiscriminator(),
                    ussr.getEffectiveAvatarUrl(), userId,
                    Globals.inFratikDev && Objects.requireNonNull(guild).getMember(ussr) != null,
                    Globals.ownerId == ussr.getIdLong() ||
                    (Globals.inFratikDev && Objects.requireNonNull(guild).getMember(ussr) != null &&
                            Objects.requireNonNull(guild.getMember(ussr)).getRoles()
                                    .contains(guild.getRoleById(Ustawienia.instance.gadmRole)))));
        });
        routes.get("/api/commands", ex -> {
            Language lang = getLang(ex);
            if (lang == null) return;
            List<Komenda> komendy = new ArrayList<>();
            for (Iterator<NewCommand> iter = managerKomend.commandsStream().iterator(); iter.hasNext();) {
                //fixme
                NewCommand cmd = iter.next();
                if (cmd.getType() != CommandType.NORMAL) continue;
                komendy.add(new Komenda(cmd.getName(), tlumaczenia.get(lang, cmd.getName() + ".description"),
                        cmd.getCooldown()));
            }
            komendy.sort((komenda, komenda1) -> Ordering.usingToString().compare(komenda.getNazwa(),
                    komenda1.getNazwa()));
            Exchange.body().sendJson(ex, komendy);
        });
        routes.get("/api/status", ex -> {
            List<pl.fratik.api.entity.Status.Shard> shards = new ArrayList<>();
            for (JDA jda : shardManager.getShards().stream().sorted(Comparator.comparingInt(a -> a.getShardInfo()
                    .getShardId())).collect(Collectors.toList())) {
                shards.add(new pl.fratik.api.entity.Status.Shard(shardManager.getShards().indexOf(jda),
                        jda.getGuilds().size(), jda.getStatus()));
            }
            pl.fratik.api.entity.Status status = new pl.fratik.api.entity.Status(Statyczne.startDate, shards);
            Exchange.body().sendJson(ex, status);
        });
        routes.get("/api/{userId}/userconfig", ex -> {
            String userId = Exchange.pathParams().pathParam(ex, "userId").orElse(null);
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            User user;
            try {
                user = shardManager.getUserById(userId);
            } catch (Exception ignored) {
                user = CommonUtil.supressException((Function<String, User>) id -> shardManager.retrieveUserById(id).complete(), userId);
            }
            if (user == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            ex.setStatusCode(200);
            ex.getResponseSender().send(ByteBuffer.wrap(new ObjectMapper().writeValueAsBytes(userDao.get(user))));
        });
        routes.post("/api/{userId}/userconfig", ex -> {
            String userId = Exchange.pathParams().pathParam(ex, "userId").orElse(null);
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            User user;
            try {
                user = shardManager.getUserById(userId);
            } catch (Exception ignored) {
                user = CommonUtil.supressException((Function<String, User>) id -> shardManager.retrieveUserById(id).complete(), userId);
            }
            if (user == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            UserConfig uc = userDao.get(user);
            JsonObject parsed = getJson(ex);
            try {
                merge(uc, parsed);
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_FORMAT);
            }
            userDao.save(uc);
            Exchange.body().sendJson(ex, new Successes.GenericSuccess("zapisano"));
        });
        routes.get("/api/{guildId}/config", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            ex.setStatusCode(200);
            ex.getResponseSender().send(ByteBuffer.wrap(new ObjectMapper().writeValueAsBytes(guildDao.get(guild))));
        });
        routes.post("/api/{guildId}/config", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            GuildConfig gc = guildDao.get(guild);
            JsonObject parsed = getJson(ex);
            try {
                merge(gc, parsed);
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_FORMAT);
            }
            guildDao.save(gc);
            Exchange.body().sendJson(ex, new Successes.GenericSuccess("zapisano"));
        });
        routes.get("/api/{guildId}/{userId}/permLevel", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            String userId = Exchange.pathParams().pathParam(ex, "userId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            User user = null;
            try {
                guild = shardManager.getGuildById(guildId);
                user = shardManager.retrieveUserById(userId).complete();
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            if (user == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            if (guild.getMember(user) == null) { //przyjmujemy że admin (p.lvl. 5)
                Exchange.body().sendJson(ex, 5);
                return;
            }
            Exchange.body().sendJson(ex, UserUtil.getPermlevel(guild.getMember(user), guildDao, shardManager).getNum());
        });
        routes.get("/api/{guildId}/roles", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            List<pl.fratik.api.entity.Role> roles = new ArrayList<>();
            for (Role r : guild.getRoles()) {
                roles.add(new pl.fratik.api.entity.Role(r.getName(), r.getId(), r.getPermissionsRaw(),
                        r.getPositionRaw(), r.isManaged()));
            }
            Exchange.body().sendJson(ex, roles);
        });
        routes.get("/api/{guildId}/channels", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            List<pl.fratik.api.entity.Channel> channels = new ArrayList<>();
            for (GuildChannel c : guild.getChannels()) {
                if (!(c instanceof IPositionableChannel)) continue;
                channels.add(new pl.fratik.api.entity.Channel(c.getName(), c.getId(),
                        ((IPositionableChannel) c).getPositionRaw(), c.getType(),
                        c instanceof TextChannel && ((TextChannel) c).canTalk()));
            }
            Exchange.body().sendJson(ex, channels);
        });
        routes.get("/api/{guildId}/owner", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            Member owner;
            try {
                owner = guild.retrieveOwner().complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) owner = null;
                else throw e;
            }
            if (owner == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            Exchange.body().sendJson(ex, new pl.fratik.api.entity.User(owner.getUser(), shardManager));
        });
        routes.get("/api/server", ex -> {
            String accessToken = Exchange.queryParams().queryParam(ex, "accessToken").orElse(null);
            String userId = Exchange.queryParams().queryParam(ex, "userId").orElse(null);
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            if (shardManager.retrieveUserById(userId).complete() == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                return;
            }
            if (!Globals.inFratikDev) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NOT_IN_FDEV);
                return;
            }
            RestAction<Guild.Ban> banAction = Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild))
                    .retrieveBan(User.fromId(userId));
            Guild.Ban banne = null;
            try {
                banne = banAction.submit().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof ErrorResponseException)) {
                    LOGGER.error("Śmieszny błąd", e);
                    Exchange.body().sendErrorCode(ex, Exceptions.Codes.UNKNOWN_ERROR, 500);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Śmieszny błąd", e);
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.UNKNOWN_ERROR, 500);
                return;
            }
            if (banne != null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.JOIN_BANNED);
                return;
            }
            try {
                Objects.requireNonNull(shardManager.getGuildById(Ustawienia.instance.botGuild))
                        .addMember(Objects.requireNonNull(accessToken), User.fromId(userId)).complete();
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.JOIN_ERROR);
                return;
            }
            Exchange.body().sendJson(ex, new Successes.GenericSuccess(null));
        });
        routes.get("/api/credits", ex -> {
            try {
                List<Credits.ParsedCredits> credits = new ArrayList<>();
                File cred = new File("credits.json");
                File socga = new File("socialega.json");
                Map<String, Object> hmap = new HashMap<>();
                Map<String, Credits.Sociale> socialeGa = Json.serializer().fromJson(Files.readAllBytes(socga.toPath()),
                        new TypeReference<Map<String, Credits.Sociale>>() {});
                for (Credits credit : Json.serializer().fromJson(Files.readAllBytes(cred.toPath()),
                        new TypeReference<List<Credits>>() {})) {
                    User user = shardManager.retrieveUserById(credit.getId()).complete();
                    if (user == null) continue;
                    credits.add(new Credits.ParsedCredits(user.getName(), user.getDiscriminator(), user.getAvatarUrl(),
                            credit.getKrotkiPowod(), credit.getDluzszyPowod(), credit.getSociale()));
                }
                hmap.put("credits", credits);
                Guild fdev = shardManager.getGuildById(Ustawienia.instance.botGuild);
                if (fdev != null) {
                    Map<User, Status> map = new HashMap<>();
                    for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.gadmRole)))
                        map.put(member.getUser(), Status.GLOBALADMIN);
                    for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.zgaRole)))
                        map.put(member.getUser(), Status.ZGA);
                    for (Member member : fdev.getMembersWithRoles(fdev.getRoleById(Ustawienia.instance.devRole)))
                        map.put(member.getUser(), Status.DEV);
                    List<Credits.ParsedCredits> devy = new ArrayList<>();
                    List<Credits.ParsedCredits> zga = new ArrayList<>();
                    List<Credits.ParsedCredits> ga = new ArrayList<>();
                    for (User d : map.entrySet().stream().filter(s -> s.getValue() == Status.DEV).map(Map.Entry::getKey)
                            .collect(Collectors.toList())) {
                        devy.add(new Credits.ParsedCredits(d.getName(), d.getDiscriminator(), d.getAvatarUrl(), null,
                                null, socialeGa.get(d.getId())));
                    }
                    for (User z : map.entrySet().stream().filter(s -> s.getValue() == Status.ZGA).map(Map.Entry::getKey)
                            .collect(Collectors.toList())) {
                        zga.add(new Credits.ParsedCredits(z.getName(), z.getDiscriminator(), z.getAvatarUrl(), null,
                                null, socialeGa.get(z.getId())));
                    }
                    for (User g : map.entrySet().stream().filter(s -> s.getValue() == Status.GLOBALADMIN).map(Map.Entry::getKey)
                            .collect(Collectors.toList())) {
                        ga.add(new Credits.ParsedCredits(g.getName(), g.getDiscriminator(), g.getAvatarUrl(), null,
                                null, socialeGa.get(g.getId())));
                    }
                    hmap.put("dev", devy);
                    hmap.put("zga", zga);
                    hmap.put("ga", ga);
                }
                Exchange.body().sendJson(ex, hmap);
            } catch (Exception e) {
                LOGGER.error("Nie udało się odczytać creditsów!", e);
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.UNKNOWN_ERROR, 500);
            }
        });

        routes.get("/api/{guildId}/configschema", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            if (guildId == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Language lang = getLang(ex);
            if (lang == null) return;
            Map<String, Map<?, ?>> map = new HashMap<>();

            GuildConfig def = new GuildConfig(guildId);
            for (Field field : GuildConfig.class.getDeclaredFields()) {
                String id = field.getName();

                String s = tlumaczenia.get(lang, String.format("guildconfig.%s.name", id), true);
                if (s.equals(String.format("guildconfig.%s.name", id))) continue;

                Map<String, Object> gs = new HashMap<>();
                String type = field.getType().getSimpleName();
                try {
                    gs.put("default", GuildConfig.getValue(field, def));
                } catch (Exception ignored) { }
                gs.put("array", type.equals("List"));

                ConfigField ann = field.getDeclaredAnnotation(ConfigField.class);
                if (ann != null) {
                    if (ann.dontDisplayInSettings()) continue;
                    ConfigField.Entities ent = ann.holdsEntity();
                    if (ent != ConfigField.Entities.NULL) type = ent.name();
                    if (type.equalsIgnoreCase("String") || type.equalsIgnoreCase("Integer")) {
                        gs.put("min", ann.min());
                        gs.put("max", ann.max());
                    }
                    if (ann.maxElements() != -1) gs.put("maxElements", ann.maxElements());
                    gs.put("required", ann.required());
                }
                gs.put("type", type.toLowerCase());
                gs.put("nazwa", s);
                gs.put("opis", tlumaczenia.get(lang, String.format("guildconfig.%s.desc", id)));
                map.put(id, gs);
            }

            Exchange.body().sendJson(ex, map);
        });

        routes.get("/api/perms", ex -> {
            Language lang = getLang(ex);
            if (lang == null) return;
            Map<String, String> m = new HashMap<>();
            for (PermLevel permLevel : PermLevel.values()) {
                m.put(permLevel.getNum() + "", tlumaczenia.get(lang, permLevel.getLanguageKey()));
            }
            Exchange.body().sendJson(ex, m);
        });

        routes.get("/api/websocketAuth", ex -> {
            String userId = Exchange.queryParams().queryParam(ex, "userId").orElse(null);
            if (userId == null || userId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            try {
                User user = shardManager.retrieveUserById(userId).complete();
                JsonObject obj = new JsonObject();
                obj.addProperty("authStr", socketManager.generateUserAuthString(user.getIdLong()));
                obj.addProperty("publicKey", Base64.getEncoder().encodeToString(SocketManager.getKeyPair().getPublicKey()));
                obj.addProperty("wsTimeout", TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES));
                Exchange.body().sendJson(ex, obj.toString());
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
            }
        });
        routes.post("/api/websocketInvalidate", ex -> {
            Set<String> identifiers = Exchange.body().parseJson(ex, new TypeReference<Set<String>>() {});
            if (identifiers == null || identifiers.contains(null)) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Exchange.body().sendJson(ex, socketManager.invalidate(identifiers));
        });

        Rundka rundka = rundkaDao.getAll().stream().filter(Rundka::isTrwa).findAny().orElse(null);
        if (rundka != null) {
            RundkaCommand.setNumerRundy(rundka.getIdRundki());
            RundkaCommand.setRundkaOn(true);
        }
        undertow = Undertow.builder()
                .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true)
                .addHttpListener(Ustawienia.instance.port, Ustawienia.instance.host, wrapWithMiddleware(routes))
                .build();
        undertow.start();
        socketManager = new SocketManager(shardManager);
        undertowWs = Undertow.builder()
                .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true)
                .setHandler(getWebSocketHandler())
                .addHttpListener(Ustawienia.instance.wsPort, Ustawienia.instance.wsHost)
                .build();
        undertowWs.start();
        rundkaGa = new RundkaGa(this, eventBus, rundkaDao, shardManager);
        eventBus.register(this);
        eventBus.register(rundkaGa);
        commands.add(new RundkaCommand(eventBus, rundkaDao));
//        commands.add(new TestCommand(eventBus));

        managerKomend.registerCommands(this, commands);

        return true;
    }

    private PathHandler getWebSocketHandler() {
        return path().addPrefixPath("/", websocket(socketManager));
//            LOGGER.info("Nowe połączenie websocketa rundek: {}", channel.getSourceAddress().getHostString());
//            String userId = Exchange.queryParams().queryParam(exchange, "userId").orElse(null);
//            if (userId == null || userId.isEmpty()) {
//                LOGGER.info("Websocket rundek {} nie podał ID użytkownika, zamykam", userId);
//                WebSockets.sendText(Exceptions.Codes.getJson(Exceptions.Codes.NO_PARAM), channel, null);
//                try {
//                    channel.close();
//                } catch (IOException e) {
//                    //ignore
//                }
//                return;
//            }
//            try {
//                if (shardManager.retrieveUserById(userId).complete() == null) {
//                    LOGGER.info("Websocket rundek {} podał nieprawidłowe ID użytkownika, zamykam", userId);
//                    WebSockets.sendText(Exceptions.Codes.getJson(Exceptions.Codes.INVALID_USER), channel, null);
//                    try {
//                        channel.close();
//                    } catch (IOException e) {
//                        //ignore
//                    }
//                    return;
//                }
//            } catch (Exception e) {
//                LOGGER.info("Websocket rundek {} podał nieprawidłowe ID użytkownika, zamykam", userId);
//                WebSockets.sendText(Exceptions.Codes.getJson(Exceptions.Codes.INVALID_USER), channel, null);
//                try {
//                    channel.close();
//                } catch (IOException e1) {
//                    //ignore
//                }
//                return;
//            }
//            if (!RundkaCommand.isRundkaOn()) {
//                LOGGER.info("{} próbował się zalogować do websocketa, ale rundki nie ma, zamykam",
//                        channel.getSourceAddress().getHostString());
//                WebSockets.sendText(Exceptions.Codes.getJson(Exceptions.Codes.NO_RUNDKA), channel, null);
//                try {
//                    channel.close();
//                } catch (IOException e) {
//                    //ignore
//                }
//                return;
//            }
//            webSocketChannels.put(channel.getSourceAddress().toString(), new WscWrapper(channel, userId));
//            channel.getReceiveSetter().set(new AbstractReceiveListener() {
//                @Override
//                protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
//                    LOGGER.info("Websocket rundek {} się rozłączył", webSocketChannel.getSourceAddress().getHostString());
//                    webSocketChannels.values().removeIf(wsc -> wsc.ch.equals(webSocketChannel));
//                    super.onClose(webSocketChannel, channel);
//                }
//            });
//            channel.resumeReceives();
    }

    public static JsonObject getJson(HttpServerExchange ex) throws IOException {
        return GsonUtil.GSON.fromJson(IOUtils.toString(ex.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
    }

    private HttpHandler wrapWithMiddleware(HttpHandler handler) {
        return MiddlewareBuilder.begin(BlockingHandler::new)
                .next(CustomHandlers::gzip)
                .next(CustomHandlers::accessLog)
                .next(this::exceptionHandler)
                .next(CustomHandlers::blockIP)
                .complete(handler);
    }

    private HttpHandler exceptionHandler(HttpHandler next) {
        return Handlers.exceptionHandler(next)
                .addExceptionHandler(Throwable.class, ExceptionHandlers::handleAllExceptions);
    }

    @Override
    public boolean shutDown() {
        undertow.stop();
        undertowWs.stop();
        for (WscWrapper c : webSocketChannels.values()) {
            try {
                c.ch.close();
            } catch (IOException e) {
                //ignore
            }
        }
        webSocketChannels = new HashMap<>();
        eventBus.unregister(this);
        eventBus.unregister(rundkaGa);
        managerKomend.unregisterCommands(commands);
        return true;
    }

    @Subscribe
    public void onRundkaStart(RundkaStartEvent e) {
        for (WscWrapper c : webSocketChannels.values())
            WebSockets.sendText(Json.serializer().toString(e), c.ch, null);
    }

    @Subscribe
    public void onRundkaEnd(RundkaEndEvent e) {
        for (WscWrapper c : webSocketChannels.values()) {
            WebSockets.sendText(Json.serializer().toString(e), c.ch, null);
            try {
                c.ch.close();
            } catch (Exception ex) {
                //ignored
            }
        }
        webSocketChannels = new HashMap<>();
    }

    @Subscribe
    public void onRundkaAnswer(final RundkaNewAnswerEvent finalE) {
        for (Map.Entry<String, WscWrapper> entry : webSocketChannels.entrySet()) {
            RundkaNewAnswerEvent e = finalE;
            WscWrapper c = entry.getValue();
            RundkaOdpowiedz odp = e.getOdpowiedz();
            if (odp instanceof RundkaOdpowiedzFull)
                e = new RundkaNewAnswerEvent(new RundkaOdpowiedzSanitized
                        ((RundkaOdpowiedzFull) odp, odp.getUserId().equals(c.userId) ||
                                UserUtil.isStaff(shardManager.retrieveUserById(c.userId).complete(), shardManager), shardManager));
            WebSockets.sendText(Json.serializer().toString(e), c.ch, null);
        }
    }

    @Subscribe
    public void onRundkaAnswerVote(final RundkaAnswerVoteEvent finalE) {
        for (Map.Entry<String, WscWrapper> entry : webSocketChannels.entrySet()) {
            RundkaAnswerVoteEvent e = finalE;
            if (!entry.getValue().userId.equals(e.getOdpowiedz().getUserId()) &&
                    !UserUtil.isStaff(shardManager.retrieveUserById(entry.getValue().userId).complete(), shardManager))
                continue;
            WscWrapper c = entry.getValue();
            RundkaOdpowiedz odp = e.getOdpowiedz();
            if (odp instanceof RundkaOdpowiedzFull)
                e = new RundkaAnswerVoteEvent(new RundkaOdpowiedzSanitized
                        ((RundkaOdpowiedzFull) odp, odp.getUserId().equals(c.userId) ||
                                UserUtil.isStaff(shardManager.retrieveUserById(c.userId).complete(), shardManager), shardManager));
            WebSockets.sendText(Json.serializer().toString(e), c.ch, null);
        }
    }

    private String generateInviteLink(String id) {
        return "https://discord.com/oauth2/authorize?client_id=" +
                id + "&permissions=" +
                Globals.permissions + "&scope=bot+applications.commands";
    }

    private void merge(Object obj, JsonObject update) {
        for (Map.Entry<String, JsonElement> e : update.entrySet()) {
            String fieldName = e.getKey();
            String fieldSetter = "set" + StringUtil.firstLetterUpperCase(fieldName);
            try {
                Field f = obj.getClass().getDeclaredField(fieldName);
                Method m = obj.getClass().getDeclaredMethod(fieldSetter, f.getType());
                m.invoke(obj, GsonUtil.GSON.fromJson(e.getValue(), f.getType()));
            } catch (Exception e1) {
                //nic
            }
        }

        Method[] methods = obj.getClass().getDeclaredMethods();

        for(Method fromMethod: methods){
            if (fromMethod.getName().startsWith("get") || fromMethod.getName().startsWith("is")) {

                String fromName = fromMethod.getName();
                String toName;
                if (fromName.startsWith("get")) toName = fromName.replace("get", "set");
                else if (fromName.startsWith("is")) toName = fromName.replace("is", "set");
                else toName = fromName; //to źle

                try {
                    Method toMetod = obj.getClass().getMethod(toName, fromMethod.getReturnType());
                    Object value = fromMethod.invoke(update, (Object[])null);
                    if(value != null){
                        toMetod.invoke(obj, value);
                    }
                } catch (Exception e) {
                    // nic
                }
            }
        }
    }

    @Nullable
    private Language getLang(HttpServerExchange ex) {
        Optional<String> langTmp = Exchange.queryParams().queryParam(ex, "language");
        Language lang = null;
        if (!langTmp.isPresent()) {
            Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
            return null;
        }
        for (Language l : Language.values()) {
            if (l == Language.DEFAULT) continue;
            if (l.getShortName().equals(langTmp.get())) lang = l;
        }
        if (lang == null) {
            Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_LANG);
            return null;
        }
        return lang;
    }

    @AllArgsConstructor
    private static class WscWrapper {
        private final WebSocketChannel ch;
        private final String userId;
    }

    private enum Status {
        GLOBALADMIN, ZGA, DEV
    }

}
