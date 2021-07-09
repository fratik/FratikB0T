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

package pl.fratik.fratikcoiny.libs.chinczyk;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.sentry.Sentry;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.NamedThreadFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Chinczyk {
    private static final Map<String, String> BOARD_COORDS;
    private static final String FILE_NAME = "board.png";
    private static final String START = "START";
    private static final String CANCEL = "ABORT";
    private static final String READY = "READY";
    private static final String LANGUAGE = "LANGUAGE";
    private static final String NEW_CONTROL_MESSAGE = "NEW_CONTROL_MESSAGE";
    private static final String ROLL = "ROLL";
    private static final String MOVE_PREFIX = "MOVE_";
    private static final String END_MOVE = "END_MOVE";
    private static final BufferedImage plansza;
    private static final Font font;
    private final CommandContext context;
    private final EventBus eventBus;
    private final EnumMap<Place, Player> players;
    private final Tlumaczenia t;
    private final Consumer<Chinczyk> endCallback;
    private final ScheduledExecutorService executor;
    private Language l;
    private Status status = Status.WAITING_FOR_PLAYERS;
    private Message message;
    private Random random;
    private Place turn;
    private Integer rolled;
    private Event lastEvent;
    private Player winner;
    private ScheduledFuture<?> timeout;

    public static boolean canBeUsed() {
        return font != null && plansza != null;
    }

    static {
        //#region koordynaty pól na mapie
        //kurwa, robiłem to 53 minuty
        Map<String, String> coords = new HashMap<>();
        coords.put("b1", "1637,125");
        coords.put("b2", "1805,125");
        coords.put("b3", "1637,293");
        coords.put("b4", "1805,293");
        coords.put("b5", "965,293");
        coords.put("b6", "965,461");
        coords.put("b7", "965,629");
        coords.put("b8", "965,797");
        coords.put("g1", "1637,1637");
        coords.put("g2", "1805,1637");
        coords.put("g3", "1637,1805");
        coords.put("g4", "1805,1805");
        coords.put("g5", "1637,965");
        coords.put("g6", "1469,965");
        coords.put("g7", "1301,965");
        coords.put("g8", "1133,965");
        coords.put("y1", "125,1637");
        coords.put("y2", "293,1637");
        coords.put("y3", "125,1805");
        coords.put("y4", "293,1805");
        coords.put("y5", "965,1637");
        coords.put("y6", "965,1469");
        coords.put("y7", "965,1301");
        coords.put("y8", "965,1133");
        coords.put("r1", "125,125");
        coords.put("r2", "293,125");
        coords.put("r3", "125,293");
        coords.put("r4", "293,293");
        coords.put("r5", "293,965");
        coords.put("r6", "461,965");
        coords.put("r7", "629,965");
        coords.put("r8", "797,965");
        coords.put("1", "797,125");
        coords.put("2", "965,125");
        coords.put("3", "1133,125");
        coords.put("4", "1133,293");
        coords.put("5", "1133,461");
        coords.put("6", "1133,629");
        coords.put("7", "1133,797");
        coords.put("8", "1301,797");
        coords.put("9", "1469,797");
        coords.put("10", "1637,797");
        coords.put("11", "1805,797");
        coords.put("12", "1805,965");
        coords.put("13", "1805,1133");
        coords.put("14", "1637,1133");
        coords.put("15", "1469,1133");
        coords.put("16", "1301,1133");
        coords.put("17", "1133,1133");
        coords.put("18", "1133,1301");
        coords.put("19", "1133,1469");
        coords.put("20", "1133,1637");
        coords.put("21", "1133,1805");
        coords.put("22", "965,1805");
        coords.put("23", "797,1805");
        coords.put("24", "797,1637");
        coords.put("25", "797,1469");
        coords.put("26", "797,1301");
        coords.put("27", "797,1133");
        coords.put("28", "629,1133");
        coords.put("29", "461,1133");
        coords.put("30", "293,1133");
        coords.put("31", "125,1133");
        coords.put("32", "125,965");
        coords.put("33", "125,797");
        coords.put("34", "293,797");
        coords.put("35", "461,797");
        coords.put("36", "629,797");
        coords.put("37", "797,797");
        coords.put("38", "797,629");
        coords.put("39", "797,461");
        coords.put("40", "797,293");
        BOARD_COORDS = Collections.unmodifiableMap(coords);
        //#endregion
        Font f;
        BufferedImage i;
        try (InputStream stream = Chinczyk.class.getResourceAsStream("/Mulish-Regular.ttf")) {
            f = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(stream));
            i = ImageIO.read(Objects.requireNonNull(Chinczyk.class.getResource("/plansza_chinczyk.png")));
        } catch (Exception e) {
            LoggerFactory.getLogger(Chinczyk.class).error("Nie udało się załadować czcionki i/lub planszy: ", e);
            Sentry.capture(e);
            f = null;
            i = null;
        }
        font = f;
        plansza = i;
    }

    public Chinczyk(CommandContext context, EventBus eventBus, Consumer<Chinczyk> endCallback) {
        this.context = context;
        this.eventBus = eventBus;
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Chinczyk-" + getChannel().getId()));
        this.endCallback = obj -> {
            try {
                if (timeout == null || timeout.isDone() || timeout.cancel(false)) endCallback.accept(obj);
            } finally {
                executor.shutdown();
            }
        };
        players = new EnumMap<>(Place.class);
        t = context.getTlumaczenia();
        l = context.getLanguage();
        eventBus.register(this);
        updateMainMessage(true);
        timeout = executor.schedule(this::timeout, 2, TimeUnit.MINUTES);
    }

    private void timeout() {
        Status status = this.status;
        this.status = Status.CANCELLED;
        if (status == Status.WAITING_FOR_PLAYERS || status == Status.WAITING)
            aborted("chinczyk.timeout.waiting");
        if (status == Status.IN_PROGRESS) {
            Player player = players.remove(turn);
            player.setStatus(PlayerStatus.LEFT);
            if (player.getControlHook() != null) player.getControlHook()
                    .editOriginal(t.get(player.getLanguage(), "chinczyk.left.timeout")).complete();
            player.setControlHook(null);
            rolled = null;
            makeTurn();
        }
    }

    public byte[] renderBoard() {
        try {
            BufferedImage image = new BufferedImage(plansza.getWidth(), plansza.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            g.drawImage(plansza, 0, 0, null);
            for (Player player : players.values()) {
                for (Piece piece : player.getPieces()) {
                    String value = BOARD_COORDS.get(piece.getBoardPosition());
                    int x = Integer.parseInt(value.split(",")[0]);
                    int y = Integer.parseInt(value.split(",")[1]);
                    g.drawImage(piece.render(font), x-30, y-30, null);
                }
            }
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            Sentry.capture(e);
            return null;
        }
    }

    public Message generateMessage() {
        MessageBuilder mb = new MessageBuilder();
        Language l;
        if (turn != null) l = players.get(turn).getLanguage();
        else l = this.l;
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(t.get(l, "chinczyk.embed.title"))
                .setImage("attachment://" + FILE_NAME)
                .setFooter("Board: FischX • CC BY-SA 3.0");
        switch (status) {
            case WAITING_FOR_PLAYERS:
            case WAITING: {
                List<Component> placeComponents = new ArrayList<>();
                List<Component> controlComponents = new ArrayList<>();
                ActionRow langMenu = generateLanguageMenu(l);
                for (Place p : Place.values()) {
                    placeComponents.add(Button.of(ButtonStyle.SECONDARY, p.name(),
                            t.get(l, "chinczyk.place." + p.name().toLowerCase()),
                            Emoji.fromUnicode(p.emoji)).withDisabled(players.containsKey(p)));
                }
                controlComponents.add(Button.of(ButtonStyle.PRIMARY, START, t.get(l, "chinczyk.button.start"))
                        .withDisabled(status == Status.WAITING_FOR_PLAYERS || !isEveryoneReady()));
                controlComponents.add(Button.of(ButtonStyle.DANGER, CANCEL, t.get(l, "chinczyk.button.abort")));
                eb
                        .setDescription(t.get(l, "chinczyk.embed.description"))
                        .addField(t.get(l, "chinczyk.embed.players"), renderPlayerString(), true)
                        .addField(t.get(l, "chinczyk.embed.state"), renderStateString(), true);
                mb.setActionRows(ActionRow.of(placeComponents), ActionRow.of(controlComponents), langMenu);
                break;
            }
            case IN_PROGRESS: {
                if (lastEvent != null) eb.appendDescription(lastEvent.getTranslated(t, l)).appendDescription(" ");
                if (rolled == null) eb.appendDescription(t.get(l, "chinczyk.turn", players.get(turn).getUser().getAsMention()));
                else eb.appendDescription(t.get(l, "chinczyk.turn.rolled", players.get(turn).getUser().getAsMention(), rolled));
                eb.setColor(turn.bgColor);
                mb.setActionRows(ActionRow.of(
                        Button.of(ButtonStyle.PRIMARY, NEW_CONTROL_MESSAGE, t.get(l, "chinczyk.button.new.msg"))
                ));
                break;
            }
            case ENDED: {
                eb.setDescription(t.get(l, "chinczyk.embed.win", winner.getUser().getAsMention()));
                eb.setColor(winner.getPlace().bgColor);
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }
        return mb.setEmbeds(eb.build()).build();
    }

    private ActionRow generateLanguageMenu(Language currentLang) {
        List<SelectOption> options = new ArrayList<>();
        for (Language lang : Language.values()) {
            if (lang.equals(Language.DEFAULT)) continue;
            options.add(SelectOption.of(lang.getLocalized(), lang.name())
                    .withDefault(lang == currentLang)
                    .withEmoji(Emoji.fromUnicode(lang.getEmoji().toString())));
        }
        return ActionRow.of(SelectionMenu.create(LANGUAGE)
                .setPlaceholder(t.get(currentLang, "language.change.placeholder"))
                .setRequiredRange(1, 1)
                .addOptions(options)
                .build());
    }

    private String renderPlayerString() {
        StringBuilder s = new StringBuilder();
        for (Place p : Place.values()) {
            Optional<Player> player = Optional.ofNullable(players.get(p));
            String pName = player.map(Player::getUser).map(User::getAsMention)
                    .orElseGet(() -> t.get(l, "chinczyk.no.player"));
            String pReady = player.map(Player::getStatus).map(st -> st == PlayerStatus.READY ? " \u2705" : " \u274C")
                    .orElse("");
            s.append(p.emoji).append(' ').append(pName).append(pReady).append('\n');
        }
        s.setLength(s.length() - 1);
        return s.toString();
    }

    private String renderStateString() {
        if (status == Status.WAITING_FOR_PLAYERS) {
            String s = readyPlayerCount() + "/" + Math.max(2, players.size());
            return t.get(l, "chinczyk.waiting.for.players", s);
        } else if (status == Status.WAITING) {
            return t.get(l, "chinczyk.waiting.for.start");
        }
        throw new IllegalStateException();
    }

    private long readyPlayerCount() {
        return players.values().stream().filter(p -> p.getStatus() == PlayerStatus.READY || p.getStatus() == PlayerStatus.PLAYING).count();
    }

    private boolean isEveryoneReady() {
        return readyPlayerCount() >= 2 && players.values().stream().allMatch(p -> p.getStatus() == PlayerStatus.READY);
    }

    @Subscribe
    public void onButtonClick(ButtonClickEvent e) {
        if (!e.getChannel().equals(getChannel())) return;
        if (e.getMessageIdLong() == message.getIdLong()) {
            switch (e.getComponentId()) {
                case START: {
                    if (!e.getUser().equals(context.getSender())) {
                        Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                                .findAny().map(Player::getLanguage).orElse(l);
                        e.reply(t.get(lang, "chinczyk.not.owner.start", context.getSender().getAsMention()))
                                .setEphemeral(true).complete();
                        return;
                    }
                    if (!timeout.cancel(false)) return;
                    status = Status.IN_PROGRESS;
                    for (Player player : players.values()) {
                        player.setStatus(PlayerStatus.PLAYING);
                    }
                    random = new Random(System.nanoTime() + message.getIdLong() + hashCode()
                            + players.values().stream().mapToLong(Player::getControlMessageId).sum());
                    // nieprzewidywalna wartość - nanoTime jest ciężkie do odgadnięcia w całości, hashCode też,
                    // a wiadomości kontroli są widoczne tylko dla pojedynczych osób
                    lastEvent = new Event(Event.Type.GAME_START, null, null);
                    makeTurn();
                    break;
                }
                case CANCEL: {
                    if (!e.getUser().equals(context.getSender())) {
                        Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                                .findAny().map(Player::getLanguage).orElse(l);
                        e.reply(t.get(lang, "chinczyk.not.owner.abort", context.getSender().getAsMention()))
                                .setEphemeral(true).complete();
                        return;
                    }
                    e.deferEdit().queue();
                    status = Status.CANCELLED;
                    timeout.cancel(false);
                    aborted(null);
                    break;
                }
                case NEW_CONTROL_MESSAGE: {
                    Optional<Player> p = players.values().stream().filter(h -> h.getUser().equals(e.getUser())).findFirst();
                    if (!p.isPresent()) {
                        e.reply(t.get(l, "chinczyk.not.playing")).setEphemeral(true).complete();
                        return;
                    }
                    e.deferReply(true).queue();
                    Player player = p.get();
                    if (player.getControlHook() != null)
                        player.getControlHook().editOriginal(new MessageBuilder(t.get(player.getLanguage(),
                                "chinczyk.invalid")).build()).queue();
                    Message control = e.getHook().sendMessage(generateControlMessage(player)).setEphemeral(true).complete();
                    player.setControlHook(e.getHook());
                    player.setControlMessageId(control.getIdLong());
                    break;
                }
                default: {
                    Place place;
                    try {
                        place = Place.valueOf(e.getComponentId());
                    } catch (IllegalArgumentException ex) {
                        place = null;
                    }
                    if (place == null || players.containsKey(place)) return;
                    if (!e.getUser().equals(context.getSender()) &&
                            players.values().stream().noneMatch(p -> p.getUser().equals(context.getSender()))) {
                        e.reply(t.get(l, "chinczyk.executer.first", context.getSender().getAsMention())).setEphemeral(true).complete();
                        return;
                    }
                    if (players.values().stream().anyMatch(p -> p.getUser().equals(e.getUser()))) {
                        e.reply(t.get(l, "chinczyk.already.playing", context.getSender().getAsMention())).setEphemeral(true).complete();
                        return;
                    }
                    e.deferReply(true).queue();
                    Player player = new Player(place, e.getUser(), e.getHook());
                    Message control = e.getHook().sendMessage(generateControlMessage(player)).setEphemeral(true).complete();
                    player.setControlMessageId(control.getIdLong());
                    players.put(place, player);
                    updateMainMessage(true);
                    break;
                }
            }
            return;
        }
        Player player = players.values().stream().filter(p -> p.getControlMessageId() == e.getMessageIdLong())
                .findAny().orElse(null);
        if (player == null) return;
        switch (e.getComponentId()) {
            case READY: {
                player.setStatus(PlayerStatus.READY);
                if (isEveryoneReady()) status = Status.WAITING;
                else status = Status.WAITING_FOR_PLAYERS;
                e.deferEdit().queue();
                updateMainMessage(false);
                updateControlMessage(player);
                break;
            }
            case CANCEL: {
                if (e.getUser().equals(context.getSender()) && status != Status.IN_PROGRESS) {
                    e.reply(t.get(player.getLanguage(), "chinczyk.owner.selfabort")).setEphemeral(true).complete();
                    return;
                }
                e.deferEdit().queue();
                player.setStatus(PlayerStatus.LEFT);
                players.remove(player.getPlace(), player);
                updateControlMessage(player);
                if (status == Status.WAITING && !isEveryoneReady()) status = Status.WAITING_FOR_PLAYERS;
                if (status == Status.IN_PROGRESS && turn == player.getPlace()) {
                    rolled = null;
                    makeTurn();
                    return;
                }
                if (readyPlayerCount() < 2) {
                    status = Status.CANCELLED;
                    aborted("chinczyk.aborted.not.enough.players");
                    return;
                }
                updateMainMessage(true);
                break;
            }
            case ROLL: {
                if (turn != player.getPlace()) return;
                rolled = random.nextInt(6) + 1; //1-6
                e.deferEdit().queue();
                updateMainMessage(false);
                updateControlMessage(player);
                break;
            }
            case END_MOVE: {
                if (turn != player.getPlace()) return;
                e.deferEdit().queue();
                lastEvent = null;
                makeTurn();
                break;
            }
            default: {
                if (!e.getComponentId().startsWith(MOVE_PREFIX)) return;
                if (turn != player.getPlace()) return;
                int pieceIndex;
                try {
                    pieceIndex = Integer.parseInt(e.getComponentId().substring(MOVE_PREFIX.length()));
                } catch (NumberFormatException ex) {
                    return;
                }
                Piece piece;
                try {
                    if (!(piece = player.getPieces()[pieceIndex]).canMove()) return;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    return;
                }
                e.deferEdit().queue();
                Piece thrown = null;
                String nextPosition;
                if (piece.position == 0) nextPosition = piece.getBoardPosition(1);
                else nextPosition = piece.getBoardPosition(piece.position + rolled);
                for (Player p : players.values()) {
                    for (Piece pi : p.getPieces()) {
                        if (pi.getBoardPosition().equals(nextPosition) && !p.equals(player)) {
                            pi.position = 0;
                            thrown = pi;
                        }
                    }
                }
                if (piece.position == 0) piece.position = 1;
                else piece.position += rolled;
                if (thrown != null) lastEvent = new Event(Event.Type.THROW, piece, thrown);
                else lastEvent = new Event(piece.getBoardPosition()
                        .startsWith(String.valueOf(player.getPlace().name().toLowerCase().charAt(0)))
                        ? Event.Type.ENTERED_HOME : Event.Type.MOVE, piece, null);
                makeTurn();
            }
        }
    }

    private boolean checkWin() {
        for (Player p : players.values()) {
            if (Arrays.stream(p.getPieces()).allMatch(piece -> piece.position >= 41)) {
                winner = p;
                return true;
            }
        }
        return false;
    }

    private void makeTurn() {
        if (checkWin()) {
            status = Status.ENDED;
            if (lastEvent == null) throw new IllegalStateException("lastEvent jest null przy wygranej?");
            lastEvent = new Event(Event.Type.WON, lastEvent.piece, null);
            eventBus.unregister(this);
            try {
                endCallback.accept(this);
            } catch (Exception ignored) {}
            updateMainMessage(true);
            updateControlMessages();
            return;
        }
        if (readyPlayerCount() < 2) {
            status = Status.CANCELLED;
            aborted("chinczyk.aborted.not.enough.players");
            return;
        }
        if (turn == null) turn = players.values().stream().filter(p -> p.getUser().equals(context.getSender()))
                .findFirst().map(Player::getPlace).orElseThrow(() -> new IllegalStateException("executer nie gra?"));
        else if (rolled == null || rolled != 6) turn = Place.getNextPlace(turn, players.keySet());
        rolled = null;
        updateMainMessage(true);
        updateControlMessages();
    }

    private void aborted(String key) {
        try {
            eventBus.unregister(this);
        } catch (IllegalArgumentException e) {
            // nic
        }
        try {
            endCallback.accept(this);
        } catch (Exception ignored) {}
        if (message != null) message.editMessage(t.get(l, key == null ? "chinczyk.aborted" : key)).override(true).queue();
        try {
            updateControlMessages();
        } catch (Exception ignored) {}
    }

    @Subscribe
    public void onMenu(SelectionMenuEvent e) {
        if (!e.getChannel().equals(getChannel())) return;
        if (!e.getComponentId().equals(LANGUAGE)) return;
        Language selectedLanguage;
        try {
            selectedLanguage = Language.valueOf(e.getValues().get(0));
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return;
        }
        if (e.getMessageIdLong() == message.getIdLong()) {
            if (!e.getUser().equals(context.getSender())) {
                Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                        .findAny().map(Player::getLanguage).orElse(selectedLanguage);
                e.reply(t.get(lang, "chinczyk.not.owner.language", context.getSender().getAsMention()))
                        .setEphemeral(true).complete();
                return;
            }
            l = selectedLanguage;
            e.deferEdit().queue();
            updateMainMessage(false);
            updateControlMessages();
            return;
        }
        Player player = players.values().stream().filter(p -> p.getControlMessageId() == e.getMessageIdLong())
                .findAny().orElse(null);
        if (player == null) return;
        player.setLanguage(l);
        e.deferEdit().queue();
        updateControlMessage(player);
    }
    
    @Subscribe
    public void onMessageDelete(MessageDeleteEvent e) {
        if (e.getMessageIdLong() == message.getIdLong()) {
            status = Status.MESSAGE_DELETED;
            aborted(null);
        }
    }
    
    @Subscribe
    public void onChannelDelete(TextChannelDeleteEvent e) {
        if (e.getChannel().getIdLong() == getChannel().getIdLong()) {
            status = Status.CANCELLED;
            aborted(null);
        }
    }

    @Subscribe
    public void onGuildLeave(GuildLeaveEvent e) {
        if (e.getGuild().getIdLong() == message.getGuild().getIdLong()) {
            status = Status.LEFT_GUILD;
            aborted(null);
        }
    }

    private void updateMainMessage(boolean rerenderBoard) {
        try {
            byte[] board = renderBoard();
            if (message == null)
                message = context.replyAsAction(generateMessage()).addFile(board, FILE_NAME).complete();
            else {
                MessageAction ma = message.editMessage(generateMessage());
                if (rerenderBoard) ma = ma.retainFiles(Collections.emptySet()).addFile(board, FILE_NAME);
                message = ma.onErrorFlatMap(ErrorResponse.UNKNOWN_MESSAGE::test,
                        e -> context.replyAsAction(generateMessage()).addFile(board, FILE_NAME)).complete();
            }
        } catch (Exception e) {
            status = Status.ERRORED;
            String text = t.get(l, "chinczyk.errored");
            if (message != null)
                message.editMessage(text).onErrorMap(ErrorResponse.UNKNOWN_MESSAGE::test,
                        ex -> context.reply(text)).complete();
            try {
                eventBus.unregister(this);
            } catch (IllegalArgumentException ex) {
                // nic
            }
            try {
                endCallback.accept(this);
            } catch (Exception ignored) {}
            try {
                updateControlMessages();
            } catch (Exception ignored) {}
        }
    }

    private Message generateControlMessage(Player player) {
        MessageBuilder mb = new MessageBuilder();
        Button leave = Button.danger(CANCEL, t.get(player.getLanguage(), "chinczyk.control.abort"));
        switch (status) {
            case WAITING_FOR_PLAYERS:
            case WAITING: {
                if (player.getStatus() == PlayerStatus.JOINED) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.control.start"));
                    mb.setActionRows(
                            ActionRow.of(
                                    Button.success(READY, t.get(player.getLanguage(), "chinczyk.control.ready")),
                                    leave
                            ),
                            generateLanguageMenu(player.getLanguage())
                    );
                } else if (player.getStatus() == PlayerStatus.READY) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.control.start.waiting"));
                    mb.setActionRows(
                            ActionRow.of(leave),
                            generateLanguageMenu(player.getLanguage())
                    );
                } else if (player.getStatus() == PlayerStatus.LEFT) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.left"));
                } else throw new IllegalStateException("?");
                break;
            }
            case IN_PROGRESS: {
                if (player.getStatus() == PlayerStatus.LEFT) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.left"));
                    break;
                }
                if (turn != player.getPlace()) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.turn"));
                    mb.setActionRows(ActionRow.of(leave));
                    break;
                }
                if (rolled == null) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.die"));
                    mb.setActionRows(ActionRow.of(
                            Button.primary(ROLL, t.get(player.getLanguage(), "chinczyk.button.roll"))
                    ), ActionRow.of(leave));
                } else {
                    Map<Integer, Piece> canMove = Arrays.stream(player.getPieces()).filter(Piece::canMove)
                            .collect(Collectors.toMap(p -> p.index + 1, p -> p));
                    if (canMove.isEmpty()) {
                        mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.move.cant", rolled));
                        mb.setActionRows(ActionRow.of(
                                Button.primary(END_MOVE, t.get(player.getLanguage(), "chinczyk.button.end.move"))
                        ), ActionRow.of(leave));
                    } else {
                        mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.move", rolled));
                        mb.setActionRows(ActionRow.of(
                                Button.primary(MOVE_PREFIX + "0", "#1").withDisabled(!canMove.containsKey(1)),
                                Button.primary(MOVE_PREFIX + "1", "#2").withDisabled(!canMove.containsKey(2)),
                                Button.primary(MOVE_PREFIX + "2", "#3").withDisabled(!canMove.containsKey(3)),
                                Button.primary(MOVE_PREFIX + "3", "#4").withDisabled(!canMove.containsKey(4))
                        ), ActionRow.of(leave));
                    }
                }
                break;
            }
            case ENDED: {
                mb.setContent(t.get(player.getLanguage(), "chinczyk.ended", winner.getUser().getAsMention()));
                break;
            }
            case CANCELLED: {
                mb.setContent(t.get(player.getLanguage(), "chinczyk.aborted"));
                break;
            }
            case ERRORED: {
                mb.setContent(t.get(player.getLanguage(), "chinczyk.errored"));
                break;
            }
            case MESSAGE_DELETED: {
                mb.setContent(t.get(player.getLanguage(), "chinczyk.aborted.deleted"));
                break;
            }
            case LEFT_GUILD: {
                mb.setContent(t.get(player.getLanguage(), "chinczyk.aborted.left"));
                break;
            }
        }
        return mb.build();
    }

    private void updateControlMessage(Player p) {
        if (p.getControlHook() != null) p.getControlHook().editOriginal(generateControlMessage(p)).queue();
    }

    private void updateControlMessages() {
        for (Player p : players.values()) updateControlMessage(p);
    }

    public MessageChannel getChannel() {
        return context.getMessageChannel();
    }

    @Data
    public class Player {
        private final Place place;
        private final User user;
        private final Piece[] pieces = new Piece[4];
        private InteractionHook controlHook;
        private long controlMessageId;
        private Language language;
        private PlayerStatus status = PlayerStatus.JOINED;
        private ScheduledFuture<?> handle;

        public Player(Place place, User user, InteractionHook controlHook) {
            this.place = place;
            this.user = user;
            setControlHook(controlHook);
            for (int i = 0; i < pieces.length; i++)
                pieces[i] = new Piece(this, i);
        }

        public void setControlHook(InteractionHook controlHook) {
            if (handle != null) {
                handle.cancel(false);
                handle = null;
            }
            this.controlHook = controlHook;
            if (controlHook == null) return;
            handle = executor.schedule(() -> {
                Message msg = new MessageBuilder(t.get(getLanguage(), "chinczyk.control.expired")).build();
                controlHook.editOriginal(msg).complete();
                if (Objects.equals(this.controlHook, controlHook)) this.controlHook = null;
            }, controlHook.getExpirationTimestamp() - System.currentTimeMillis() - 60000, TimeUnit.MILLISECONDS);
        }

        public Language getLanguage() {
            return language != null ? language : l;
        }
    }

    public enum PlayerStatus {
        /**
         * Dołączył do rozgrywki - czeka na gotowość
         */
        JOINED,
        /**
         * Gotowy do gry
         */
        READY,
        /**
         * W grze
         */
        PLAYING,
        /**
         * Anulował / opuścił grę
         */
        LEFT
    }

    public enum Status {
        /**
         * Za mało graczy
         */
        WAITING_FOR_PLAYERS,
        /**
         * Czeka na start
         */
        WAITING,
        /**
         * W toku
         */
        IN_PROGRESS,
        /**
         * Gra ukończona
         */
        ENDED,
        /**
         * Gra anulowana
         */
        CANCELLED,
        /**
         * Coś się popsuło
         */
        ERRORED,
        /**
         * Wiadomość usunięta, gra anulowana
         */
        MESSAGE_DELETED,
        /**
         * Serwer opuszczony, gra anulowana
         */
        LEFT_GUILD
    }

    public enum Place {
        BLUE("\uD83D\uDFE6", new Color(0x0000F8), new Color(0xFFFFFF)),
        GREEN("\uD83D\uDFE9", new Color(0x007C00)),
        YELLOW("\uD83D\uDFE8", new Color(0xF4F600)),
        RED("\uD83D\uDFE5", new Color(0xFF0000));

        private final String emoji;
        private final Color bgColor;
        private final Color textColor;

        Place(String emoji, Color bgColor) {
            this(emoji, bgColor, Color.BLACK);
        }

        Place(String emoji, Color bgColor, Color textColor) {
            this.emoji = emoji;
            this.bgColor = bgColor;
            this.textColor = textColor;
        }

        public static Place getNextPlace(Place currentPlace, Set<Place> places) {
            Place nextPlace;
            if (currentPlace == BLUE) nextPlace = GREEN;
            else if (currentPlace == GREEN) nextPlace = YELLOW;
            else if (currentPlace == YELLOW) nextPlace = RED;
            else if (currentPlace == RED) nextPlace = BLUE;
            else throw new IllegalArgumentException("Nieoczekiwana wartość " + currentPlace);
            if (places.contains(nextPlace)) return nextPlace;
            else return getNextPlace(nextPlace, places);
        }
    }

    public class Piece {
        private final Player player;
        private final int index;
        private int position = 0;

        public Piece(Player player, int index) {
            this.player = player;
            this.index = index;
        }

        public BufferedImage render(Font f) {
            BufferedImage square = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics g = square.getGraphics();
            g.setColor(player.getPlace().bgColor);
            g.fillRect(0, 0, 59, 59);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, 59, 59);
            g.setColor(player.getPlace().textColor);
            g.setFont(f.deriveFont(Font.BOLD, 44f));
            FontMetrics fontMetrics = g.getFontMetrics();
            int sX = (60 - fontMetrics.stringWidth(getIndexAsString())) / 2;
            int sY = ((60 - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
            g.drawString(getIndexAsString(), sX, sY);
            g.dispose();
            return square;
        }

        public boolean canMove() {
            if (rolled == null) return false;
            if (position + rolled > 44) return false; // jeśli przekroczona ilość pól + strefy końcowej, nie można
            String nextPosition;
            if (position == 0) nextPosition = getBoardPosition(1);
            else nextPosition = getBoardPosition(position + rolled);
            for (Player p : players.values()) {
                for (Piece piece : p.getPieces()) {
                    if (piece.getBoardPosition().equals(nextPosition))
                        return position == 0 ? (rolled == 6 && !p.equals(player)) : !p.equals(player); // zezwól ruch tylko jeżeli bicie
                }
            }
            return position != 0 || rolled == 6; // pole czyste, można iść
        }

        public String getIndexAsString() {
            return String.valueOf(index + 1);
        }

        public String getBoardPosition() {
            return getBoardPosition(position);
        }

        public String getBoardPosition(int position) {
            if (position == 0) return String.valueOf(player.getPlace().name().toLowerCase().charAt(0)) + (index + 1);
            if (position > 40) return String.valueOf(player.getPlace().name().toLowerCase().charAt(0)) + (position - 36);
            int offset;
            switch (player.getPlace()) {
                case BLUE:
                    offset = 2;
                    break;
                case GREEN:
                    offset = 12;
                    break;
                case YELLOW:
                    offset = 22;
                    break;
                case RED:
                    offset = 32;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + player.getPlace());
            }
            int pos = (position + offset) % 40;
            return String.valueOf(pos != 0 ? pos : 40);
        }
    }

    private static class Event {
        private final Type type;
        private final Piece piece;
        private final Piece piece2;

        private Event(Type type, Piece piece, Piece piece2) {
            this.type = type;
            this.piece = checkType(type, Type.LEFT_START, Type.MOVE, Type.ENTERED_HOME, Type.WON, Type.THROW) ?
                    Objects.requireNonNull(piece) : null;
            this.piece2 = checkType(type, Type.THROW) ? Objects.requireNonNull(piece2) : null;
        }

        private static boolean checkType(Type type, Type... allowedTypes) {
            return Arrays.asList(allowedTypes).contains(type);
        }

        private String getTranslated(Tlumaczenia t, Language l) {
            Function<Piece, String> pieceString = p -> t.get(l, "chinczyk.piece", p.getIndexAsString(),
                    p.player.getUser().getAsMention());
            switch (type) {
                case LEFT_START:
                case MOVE:
                case ENTERED_HOME:
                    return t.get(l, type.translationKey, StringUtils.capitalize(pieceString.apply(piece)));
                case WON:
                    return t.get(l, type.translationKey, piece.player.getUser().getAsMention());
                case THROW:
                    return t.get(l, type.translationKey, StringUtils.capitalize(pieceString.apply(piece)),
                            pieceString.apply(piece2));
                default:
                    return t.get(l, type.translationKey);
            }
        }

        private enum Type {
            GAME_START("game.start"),
            LEFT_START("left.start"),
            MOVE("move"),
            THROW("throw"),
            ENTERED_HOME("entered.home"),
            WON("win");

            private final String translationKey;

            Type(String translationKey) {
                this.translationKey = "chinczyk.event." + translationKey;
            }
        }
    }
}
