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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.sentry.Sentry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.NamedThreadFactory;
import pl.fratik.fratikcoiny.entity.ChinczykState;
import pl.fratik.fratikcoiny.entity.ChinczykStateDao;
import pl.fratik.fratikcoiny.entity.ChinczykStats;
import pl.fratik.fratikcoiny.util.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Chinczyk {
    private static final byte CHINCZYK_VERSION = 0x02;
    private static final byte[] CHINCZYK_HEADER = new byte[] {0x21, 0x37};
    private static final Map<String, String> BOARD_COORDS;
    private static final String FILE_NAME = "board";
    private static final String START = "START";
    private static final String CANCEL = "ABORT";
    private static final String LEAVE = "LEAVE";
    private static final String READY = "READY";
    private static final String LANGUAGE = "LANGUAGE";
    private static final String RULES = "RULES";
    private static final String NEW_CONTROL_MESSAGE = "NEW_CONTROL_MESSAGE";
    private static final String ROLL = "ROLL";
    private static final String MOVE_PREFIX = "MOVE_";
    private static final String END_MOVE = "END_MOVE";
    private static final BufferedImage plansza;
    private static final Font mulish;
    private static final Font lato;
    private static final int REPLAY_TEXT_LINES = 3;
    private static final int REPLAY_TEXT_MARGIN = 100;
    private final User executer;
    private final MessageChannel channel;
    private final long referenceMessageId;
    private final EventBus eventBus;
    private final EnumMap<Place, Player> players;
    private final Tlumaczenia t;
    private final Consumer<Chinczyk> endCallback;
    private final ScheduledExecutorService executor;
    private final EventStorage eventStorage;
    private final ReentrantLock lock;
    private Language l;
    @Getter private Status status = Status.WAITING_FOR_PLAYERS;
    private Message message;
    private Random random;
    private long randomSeed;
    private long randomSeq;
    private Place turn;
    private int turns;
    private Integer rolled;
    private int rollCounter = 0;
    @Getter private Player winner;
    private ScheduledFuture<?> timeout;
    private static final ThreadLocal<Boolean> isTimeout = ThreadLocal.withInitial(() -> false);
    @Getter private Instant start;
    @Getter private Instant end;
    private long gameDuration;
    private EnumSet<Rules> rules = EnumSet.noneOf(Rules.class);
    @Getter private boolean cheats; // tu nic nie ma 👀

    public static boolean canBeUsed() {
        return mulish != null && plansza != null;
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
        coords.put("btxt", "-59,400");
        coords.put("g1", "1637,1637");
        coords.put("g2", "1805,1637");
        coords.put("g3", "1637,1805");
        coords.put("g4", "1805,1805");
        coords.put("g5", "1637,965");
        coords.put("g6", "1469,965");
        coords.put("g7", "1301,965");
        coords.put("g8", "1133,965");
        coords.put("gtxt", "-59,1570,-");
        coords.put("y1", "125,1637");
        coords.put("y2", "293,1637");
        coords.put("y3", "125,1805");
        coords.put("y4", "293,1805");
        coords.put("y5", "965,1637");
        coords.put("y6", "965,1469");
        coords.put("y7", "965,1301");
        coords.put("y8", "965,1133");
        coords.put("ytxt", "59,1570,-");
        coords.put("r1", "125,125");
        coords.put("r2", "293,125");
        coords.put("r3", "125,293");
        coords.put("r4", "293,293");
        coords.put("r5", "293,965");
        coords.put("r6", "461,965");
        coords.put("r7", "629,965");
        coords.put("r8", "797,965");
        coords.put("rtxt", "59,400");
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
        Font m;
        Font l;
        BufferedImage i;
        try {
            try (InputStream mulishStream = Chinczyk.class.getResourceAsStream("/Mulish-Regular.ttf");
                 InputStream latoStream = Chinczyk.class.getResourceAsStream("/Lato-Bold.ttf")) {
                m = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(mulishStream));
                l = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(latoStream));
                i = ImageIO.read(Objects.requireNonNull(Chinczyk.class.getResource("/plansza_chinczyk.png")));
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Chinczyk.class).error("Nie udało się załadować czcionki i/lub planszy: ", e);
            Sentry.capture(e);
            m = null;
            l = null;
            i = null;
        }
        mulish = m;
        lato = l;
        plansza = i;
    }

    public Chinczyk(CommandContext context, EventBus eventBus, Consumer<Chinczyk> endCallback) {
        executer = context.getSender();
        channel = context.getMessageChannel();
        referenceMessageId = context.getMessage().getIdLong();
        this.eventBus = eventBus;
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Chinczyk-" + getChannel().getId()));
        lock = new ReentrantLock();
        this.endCallback = obj -> {
            end = Instant.now();
            try {
                if (isTimeout.get() == Boolean.TRUE || timeout == null || timeout.isDone() || timeout.cancel(false))
                    endCallback.accept(obj);
            } finally {
                executor.shutdownNow();
            }
        };
        players = new EnumMap<>(Place.class);
        eventStorage = new EventStorage();
        t = context.getTlumaczenia();
        l = context.getLanguage();
        eventBus.register(this);
        updateMainMessage(true);
        timeout = executor.schedule(this::timeout, 2, TimeUnit.MINUTES);
    }
    
    public Chinczyk(InputStream is, ShardManager sm, EventBus eventBus, Consumer<Chinczyk> endCallback, Tlumaczenia tlumaczenia) throws IOException {
        lock = new ReentrantLock();
        eventStorage = new EventStorage();
        t = tlumaczenia;
        lock.lock();
        try {
            byte[] header = new byte[CHINCZYK_HEADER.length];
            if (is.read(header) != CHINCZYK_HEADER.length) throw new EOFException();
            if (!Arrays.equals(CHINCZYK_HEADER, header)) throw new IOException("nieoczekiwany nagłówek");
            int version = is.read();
            if (version == -1) throw new EOFException();
            if ((byte) version != CHINCZYK_VERSION) throw new IOException("niezgodność wersji pliku");
            long executerId = readLong(is);
            try {
                executer = sm.retrieveUserById(executerId).complete();
            } catch (Exception e) {
                throw new IOException("nieznany użytkownik", e);
            }
            long channelId = readLong(is);
            if ((channel = sm.getTextChannelById(channelId)) == null)
                throw new IOException("nieznany kanał " + channelId);
            executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Chinczyk-" + getChannel().getId()));
            this.endCallback = obj -> {
                end = Instant.now();
                try {
                    if (isTimeout.get() == Boolean.TRUE || timeout == null || timeout.isDone() || timeout.cancel(false))
                        endCallback.accept(obj);
                } finally {
                    executor.shutdownNow();
                }
            };
            referenceMessageId = readLong(is);
            random = new Random(randomSeed = readLong(is));
            randomSeq = readLong(is);
            for (long i = 0; i < randomSeq; i++)
                random.nextInt(6);
            try {
                l = Language.valueOf(readString(is));
            } catch (IllegalArgumentException e) {
                throw new IOException("nieprawidłowy język", e);
            }
            int playerCount = is.read();
            if (playerCount == -1) throw new EOFException();
            this.eventBus = eventBus;
            players = new EnumMap<>(Place.class);
            for (int i = 0; i < playerCount; i++) {
                Player p = readPlayer(is, sm);
                players.put(p.getPlace(), p);
            }
            rules = EnumSet.copyOf(Rules.fromRaw(readLong(is)));
            int rawCheats = is.read();
            if (rawCheats == -1) throw new EOFException();
            cheats = rawCheats != 0;
            gameDuration = readUnsignedInt(is);
            Instant started = Instant.ofEpochMilli(readLong(is));
            Instant saved = Instant.ofEpochMilli(readLong(is));
            long addedDuration = saved.getEpochSecond() - started.getEpochSecond(); 
            gameDuration += addedDuration;
            start = Instant.now();
            int type;
            Player lastRolled = null;
            try {
                while ((type = is.read()) != -1) {
                    Event.Type t = Event.Type.getByRaw(type);
                    if (t == null && type != 0) throw new IOException("nieznany event " + type);
                    Player p;
                    try {
                        int placeOffset = is.read();
                        if (placeOffset == -1) throw new EOFException();
                        Place place = Place.getByOffset(placeOffset);
                        if (place != null) {
                            p = players.get(place);
                            if (p == null) throw new NullPointerException();
                        } else p = null;
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                    Integer rolled = is.read();
                    if (rolled == -1) throw new EOFException();
                    if (rolled == 0) rolled = null;
                    int pieceIndex = is.read();
                    if (pieceIndex == -1) throw new EOFException();
                    Piece piece = p == null ? null : p.getPieces()[pieceIndex];
                    Piece piece2;
                    int player2 = is.read();
                    if (player2 == -1) throw new EOFException();
                    if (player2 == 0) piece2 = null;
                    else {
                        int piece2Index = is.read();
                        if (piece2Index == -1) throw new EOFException();
                        piece2 = players.get(Place.getByOffset(player2)).getPieces()[piece2Index];
                    }
                    int rawRolled = is.read();
                    if (rawRolled == -1) throw new EOFException();
                    boolean fastRolled = rawRolled == 1;
                    Event e;
                    eventStorage.add(e = new Event(t, p, rolled, piece, piece2, fastRolled));
                    parseEvent(e);
                    if (t == null || t == Event.Type.THROW || t == Event.Type.LEFT_START ||
                            t == Event.Type.MOVE || t == Event.Type.ENTERED_HOME) {
                        turns++;
                        this.rolled = rolled;
                        if (p.equals(lastRolled)) rollCounter++;
                        else {
                            lastRolled = p;
                            rollCounter = 0;
                        }
                    } else if (t == Event.Type.WON) {
                        start = started;
                        end = Instant.ofEpochMilli(readLong(is));
                        gameDuration -= addedDuration;
                    }
                }
            } catch (Exception e) {
                throw new IOException("nie udało się odczytać wydarzeń", e);
            }
            if (winner == null) {
                turn = lastRolled.getPlace();
                makeTurn();
                eventBus.register(this);
                message.reply(players.values().stream().filter(Player::isPlaying)
                        .map(Player::getUser).map(User::getAsMention).collect(Collectors.joining(" ")) + "\n"
                        + t.get(l, "chinczyk.reloaded.state"))
                        .mention(players.values().stream().filter(Player::isPlaying).map(Player::getUser).collect(Collectors.toSet()))
                        .complete();
            }
        } finally {
            lock.unlock();
        }
    }

    private void parseEvent(Event event) {
        if (event.getType() == Event.Type.GAME_START) {
            status = Status.IN_PROGRESS;
        } else if (event.getType() == Event.Type.THROW) {
            Objects.requireNonNull(event.getPiece2()).position = 0;
            Objects.requireNonNull(event.getPiece()).position += event.getPiece().position == 0 ? 1 : event.getRolled();
        } else if (event.getType() == Event.Type.LEFT_START) {
            Objects.requireNonNull(event.getPiece()).position = 1;
        } else if (event.getType() == Event.Type.MOVE || event.getType() == Event.Type.ENTERED_HOME) {
            Objects.requireNonNull(event.getPiece()).position += Objects.requireNonNull(event.getRolled());
        } else if (event.getType() == Event.Type.WON) {
            winner = event.getPlayer();
            status = Status.ENDED;
        } else if (event.getType() == Event.Type.LEFT_GAME) {
            event.getPlayer().setStatus(PlayerStatus.LEFT);
        }
    }

    private interface BoardReplayRenderer extends Closeable {
        InputStream getStream();
        String getFormatExtension();
        void writeFrame(BufferedImage image) throws IOException;
    }

    public BoardReplayRenderer renderReplay() {
        if (status != Status.IN_PROGRESS && status != Status.ENDED)
            throw new IllegalStateException("Generacja powtórek jest możliwa tylko dla gier w toku lub zakończonych");
        BoardReplayRenderer videoRenderer;
        try {
            if (new ProcessBuilder("ffmpeg", "-version").start().waitFor() == 0) {
                videoRenderer = new BoardReplayRenderer() {
                    private final File temp = File.createTempFile("boardreplay", ".mp4");
                    {
                        temp.deleteOnExit();
                    }
                    private final Process process = new ProcessBuilder("ffmpeg", "-loglevel", "fatal",
                            "-framerate", "1", "-f", "image2pipe", "-y", "-i", "-", "-vcodec", "libx264",
                            "-tune", "stillimage", "-r", "15", "-pix_fmt", "yuv420p", "-movflags", "faststart",
                            temp.getAbsolutePath()).start();
                    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    private boolean closed;
                    @Override
                    public synchronized void close() throws IOException {
                        closed = true;
                        process.getOutputStream().close();
                    }

                    @Override
                    public InputStream getStream() {
                        try {
                            process.waitFor();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        try {
                            return new FileInputStream(temp);
                        } catch (FileNotFoundException e) {
                            throw new IllegalStateException(e);
                        }
                    }

                    @Override
                    public String getFormatExtension() {
                        return "mp4";
                    }

                    @Override
                    public synchronized void writeFrame(BufferedImage image) throws IOException {
                        if (closed) throw new IOException("closed");
                        baos.reset();
                        ImageIO.write(image, "png", baos);
                        process.getOutputStream().write(baos.toByteArray());
                    }

                    @Override
                    protected void finalize() {
                        if (temp.exists()) temp.delete();
                    }
                };
            } else return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException ignored) {
            return null;
        }
        lock.lock();
        try (BoardReplayRenderer r = videoRenderer) {
            for (Player player : players.values()) {
                player.initPieces(); // resetuj pionki - przywrócisz ich stan z eventów
                player.setStatus(PlayerStatus.PLAYING); // opuszczenia również wrócą
            }
            for (Event event : eventStorage) {
                parseEvent(new Event(event.getType(), event.getPlayer(), event.getRolled(),
                        event.getPiece() == null ? null : event.getPlayer().getPieces()[event.getPiece().getIndex()],
                        event.getPiece2() == null ? null : event.getPiece2().getPlayer().getPieces()[event.getPiece2().getIndex()],
                        event.getFastRolled()));
                BufferedImage image = renderBoard();
                Font font = Chinczyk.lato.deriveFont(80f);
                Graphics g = image.getGraphics();
                g.setFont(font);
                FontMetrics fontMetrics = g.getFontMetrics();
                BufferedImage frame = new BufferedImage(image.getWidth(),
                        image.getHeight() + (fontMetrics.getHeight() * REPLAY_TEXT_LINES), BufferedImage.TYPE_INT_RGB);
                g = frame.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, frame.getWidth(), frame.getHeight());
                g.setColor(Color.BLACK);
                g.setFont(font);
                StringBuilder sb = new StringBuilder();
                if (event.getRolled() != null) sb.append(t.get(l, "chinczyk.replay.rolled", event.getRolled())).append(" ");
                if (event.getType() != null) sb.append(event.getTranslated(t, l, false));
                else sb.setLength(sb.length() - 1); // usuń spację
                List<String> lines = ImageUtils.splitString(sb.toString(), fontMetrics,
                        REPLAY_TEXT_LINES, frame.getWidth() - REPLAY_TEXT_MARGIN);
                ImageUtils.renderLinesCentered(g, lines, 0, 0, frame.getWidth(), fontMetrics.getHeight() * REPLAY_TEXT_LINES);
                g.drawImage(image, 0, fontMetrics.getHeight() * REPLAY_TEXT_LINES, null);
                g.dispose();
                r.writeFrame(frame);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            lock.unlock();
        }
        return videoRenderer;
    }

    private Player readPlayer(InputStream is, ShardManager sm) throws IOException {
        int placeOffset = is.read();
        if (placeOffset == -1) throw new EOFException();
        Place place = Place.getByOffset(placeOffset);
        User user;
        try {
            user = sm.retrieveUserById(readLong(is)).complete();
        } catch (Exception e) {
            throw new IOException("nieprawidłowe ID użytkownika", e);
        }
        Language language;
        try {
            language = Language.valueOf(readString(is));
        } catch (IllegalArgumentException e) {
            throw new IOException("nieprawidłowy język", e);
        }
        Player p = new Player(place, user, null);
        p.setLanguage(language);
        p.setStatus(PlayerStatus.PLAYING);
        return p;
    }

    private long readUnsignedInt(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        if (is.read(bb.array()) != 4) throw new EOFException();
        return Integer.toUnsignedLong(bb.getInt());
    }

    private long readLong(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        if (is.read(bb.array()) != 8) throw new EOFException();
        return bb.getLong();
    }

    private String readString(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        if (is.read(bb.array()) != 2) throw new EOFException();
        byte[] buffer = new byte[bb.getShort()];
        if (is.read(buffer) != buffer.length) throw new EOFException();
        return new String(buffer, 0, buffer.length, StandardCharsets.UTF_8);
    }

    private void writePlayer(OutputStream os, Player p) throws IOException {
        os.write(p.getPlace().getOffset());
        writeLong(os, p.getUser().getIdLong());
        writeString(os, p.getLanguage().name());
    }

    private void writeUnsignedInt(OutputStream os, long l) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt((int) l);
        os.write(bb.array());
    }

    private void writeLong(OutputStream os, long l) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(l);
        os.write(bb.array());
    }

    private void writeString(OutputStream os, String s) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        bb.putShort((short) bytes.length);
        os.write(bb.array());
        os.write(bytes);
    }

    private void timeout() {
        isTimeout.set(true);
        lock.lock();
        try {
            Status status = this.status;
            if (status == Status.WAITING_FOR_PLAYERS || status == Status.WAITING) {
                this.status = Status.CANCELLED;
                aborted("chinczyk.timeout.waiting");
            }
            if (status == Status.IN_PROGRESS) {
                Player player = players.get(turn);
                player.setStatus(PlayerStatus.LEFT);
                if (player.getControlHook() != null) player.getControlHook()
                        .editOriginal(new MessageBuilder(t.get(player.getLanguage(), "chinczyk.left.timeout")).build()).complete();
                player.setControlHook(null);
                rolled = null;
                eventStorage.add(new Event(Event.Type.LEFT_GAME, player, null, null, null, false));
                makeTurn();
            }
        } finally {
            lock.unlock();
        }
        isTimeout.remove();
    }

    public BufferedImage renderBoard() {
        lock.lock();
        try {
            BufferedImage image = new BufferedImage(plansza.getWidth(), plansza.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            g.setColor(new Color(0xd1b689));
            g.fillRect(0, 0, plansza.getWidth(), plansza.getHeight());
            g.drawImage(plansza, 0, 0, null);
            g.setFont(mulish.deriveFont(60f));
            g.setColor(Color.BLACK);
            for (Player player : players.values()) {
                for (Piece piece : player.getPieces()) {
                    if (player.getStatus() == PlayerStatus.LEFT) break;
                    String value = BOARD_COORDS.get(piece.getBoardPosition());
                    int x = Integer.parseInt(value.split(",")[0]);
                    int y = Integer.parseInt(value.split(",")[1]);
                    g.drawImage(piece.render(mulish), x - 30, y - 30, null);
                }
                String[] value = BOARD_COORDS.get(player.getPlace().name().toLowerCase().charAt(0) + "txt").split(",");
                final int valX = Integer.parseInt(value[0]);
                final int valY = Integer.parseInt(value[1]);
                int x = valX;
                int y = valY;
                final int ascent = g.getFontMetrics().getAscent();
                int fontHeight = ascent;
                boolean topToBottom = value.length > 2 && value[2].equals("-");
                fontHeight = topToBottom ? -fontHeight : fontHeight;
                y += fontHeight;
                String str = player.getName();
                if (x < 0)
                    x = image.getWidth() + x - g.getFontMetrics().stringWidth(str);
                g.drawString(str, x, y);
                if (player.getStatus() == PlayerStatus.LEFT) {
                    str = t.get(l, "chinczyk.player.left");
                    x = valX;
                    y = valY + fontHeight + (topToBottom ? -ascent : ascent);
                    if (x < 0)
                        x = image.getWidth() + x - g.getFontMetrics().stringWidth(str);
                    g.drawString(str, x, y);
                }
            }
            g.dispose();
            return image;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Wystąpił błąd podczas generacji planszy!", e);
            Sentry.capture(e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public Message generateMessage(String fileName) {
        MessageBuilder mb = new MessageBuilder();
        Language l;
        if (turn != null) l = players.get(turn).getLanguage();
        else l = this.l;
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(t.get(l, "chinczyk.embed.title"))
                .setImage("attachment://" + fileName);
        if (status == Status.IN_PROGRESS || status == Status.ENDED)
            eb.setFooter(t.get(l, "chinczyk.footer.turn") + turns + " | Board: FischX • CC BY-SA 3.0");
        else eb.setFooter("Board: FischX • CC BY-SA 3.0");
        switch (status) {
            case WAITING_FOR_PLAYERS:
            case WAITING: {
                List<Component> placeComponents = new ArrayList<>();
                List<Component> controlComponents = new ArrayList<>();
                ActionRow rulesMenu = generateRulesMenu();
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
                if (!rules.isEmpty())
                    eb.addField(t.get(l, "chinczyk.embed.rules"), renderRulesString(), false);
                if (cheats)
                    eb.addField(t.get(l, "chinczyk.cheats.enabled.title"), t.get(l, "chinczyk.cheats.enabled.description"), false);
                mb.setActionRows(rulesMenu, ActionRow.of(placeComponents), ActionRow.of(controlComponents), langMenu);
                break;
            }
            case IN_PROGRESS: {
                Event lastEvent = eventStorage.getLastEvent();
                if (rules.contains(Rules.FAST_ROLLS) && lastEvent.getFastRolled() == Boolean.TRUE && rolled == null)
                    eb.appendDescription(t.get(l, "chinczyk.turn.fast.rolled", lastEvent.getRolled())).appendDescription(" ");
                if (lastEvent != null && lastEvent.type != null)
                    eb.appendDescription(lastEvent.getTranslated(t, l)).appendDescription(" ");
                if (rolled == null)
                    eb.appendDescription(t.get(l, "chinczyk.turn", players.get(turn).getUser().getAsMention()));
                else eb.appendDescription(t.get(l, "chinczyk.turn.rolled", players.get(turn).getUser().getAsMention(), rolled));
                eb.setColor(turn.bgColor);
                mb.setActionRows(ActionRow.of(
                        Button.of(ButtonStyle.PRIMARY, NEW_CONTROL_MESSAGE, t.get(l, "chinczyk.button.new.msg"))
                ));
                break;
            }
            case ENDED: {
                String ment = winner.getUser().getAsMention();
                eb.setDescription(t.get(l, "chinczyk.embed.win", ment));
                if (readyPlayerCount() == 1) eb.setDescription(t.get(l, "chinczyk.embed.win.walkover", ment));
                eb.setColor(winner.getPlace().bgColor);
                eb.addField(t.get(l, "chinczyk.embed.players"), renderPlayerString(), false);
                if (!rules.isEmpty())
                    eb.addField(t.get(l, "chinczyk.embed.rules"), renderRulesString(), false);
                Map<String, ChinczykStats> stats = ChinczykStats.getStatsFromGame(this);
                for (MessageEmbed.Field field : ChinczykStats.renderEmbed(stats.get("0"), null, t, l, false,
                        false, false, true).getFields())
                    eb.addField(field);
                List<MessageEmbed> embeds = new ArrayList<>();
                embeds.add(eb.build());
                for (Player p : players.values()) {
                    ChinczykStats s = stats.get(p.getUser().getId());
                    if (s != null) embeds.add(ChinczykStats.renderEmbed(s, p.getUser(), t, l, false, false, true, false)
                            .setTitle(t.get(l, "chinczyk.game.stats")).build());
                }
                return mb.setEmbeds(embeds).build();
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

    private ActionRow generateRulesMenu() {
        List<SelectOption> options = new ArrayList<>();
        int values = 0;
        for (Rules r : Rules.values()) {
            if (r.isCheat() && !cheats) continue;
            options.add(SelectOption.of(t.get(l, r.getKey()), r.name())
                    .withDescription(t.get(l, r.getDescriptionKey()))
                    .withDefault(rules.contains(r)));
            values++;
        }
        return ActionRow.of(SelectionMenu.create(RULES)
                .setPlaceholder(t.get(l, "chinczyk.rules.placeholder"))
                .setRequiredRange(0, values)
                .setDisabled(!players.isEmpty())
                .addOptions(options)
                .build());
    }

    private String renderPlayerString() {
        StringBuilder s = new StringBuilder();
        for (Place p : Place.values()) {
            Optional<Player> player = Optional.ofNullable(players.get(p));
            String pName = player.map(Player::getUser).map(User::getAsMention)
                    .orElseGet(() -> t.get(l, "chinczyk.no.player"));
            String pEmoji;
            if (status == Status.ENDED)
                pEmoji = player.filter(pl -> pl.equals(winner)).map(pl -> " \uD83D\uDC51")
                        .orElseGet(() -> player.map(Player::getStatus).filter(st -> st == PlayerStatus.LEFT)
                                .map(pl -> " \uD83D\uDEAA").orElse(""));
            else
                pEmoji = player.map(Player::getStatus).map(st -> st == PlayerStatus.READY ? " \u2705" : " \u274C").orElse("");
            s.append(p.emoji).append(' ').append(pName).append(pEmoji).append('\n');
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

    private String renderRulesString() {
        StringBuilder sb = new StringBuilder();
        for (Rules rule : rules)
            sb.append(t.get(l, rule.getKey())).append(" - ").append(t.get(l, rule.getDescriptionKey())).append('\n');
        return sb.toString();
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
        lock.lock();
        try {
            if (e.getMessageIdLong() == message.getIdLong()) {
                switch (e.getComponentId()) {
                    case START: {
                        if (!e.getUser().equals(executer)) {
                            Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                                    .findAny().map(Player::getLanguage).orElse(l);
                            e.reply(t.get(lang, "chinczyk.not.owner.start", executer.getAsMention()))
                                    .setEphemeral(true).complete();
                            return;
                        }
                        if (!timeout.cancel(false)) return;
                        if (status == Status.WAITING_FOR_PLAYERS || !isEveryoneReady()) return;
                        status = Status.IN_PROGRESS;
                        start = Instant.now();
                        for (Player player : players.values()) {
                            player.setStatus(PlayerStatus.PLAYING);
                        }
                        random = new Random(randomSeed = System.nanoTime() + message.getIdLong() + hashCode()
                                + players.values().stream().mapToLong(Player::getControlMessageId).sum());
                        // nieprzewidywalna wartość - nanoTime jest ciężkie do odgadnięcia w całości, hashCode też,
                        // a wiadomości kontroli są widoczne tylko dla pojedynczych osób
                        eventStorage.add(new Event(Event.Type.GAME_START, null, null, null, null, false));
                        makeTurn();
                        break;
                    }
                    case CANCEL: {
                        if (!e.getUser().equals(executer)) {
                            Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                                    .findAny().map(Player::getLanguage).orElse(l);
                            e.reply(t.get(lang, "chinczyk.not.owner.abort", executer.getAsMention()))
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
                        if (!p.isPresent() || !p.get().isPlaying()) {
                            e.reply(t.get(l, "chinczyk.not.playing")).setEphemeral(true).complete();
                            return;
                        }
                        try {
                            e.deferReply(true).complete();
                            Player player = p.get();
                            if (player.getControlHook() != null)
                                player.getControlHook().editOriginal(new MessageBuilder(t.get(player.getLanguage(),
                                        "chinczyk.invalid")).build()).queue();
                            Message control = e.getHook().sendMessage(generateControlMessage(player)).setEphemeral(true).complete();
                            player.setControlHook(e.getHook());
                            player.setControlMessageId(control.getIdLong());
                        } catch (ErrorResponseException ex) {
                            if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_INTERACTION) {
                                Player player = p.get();
                                if (player.getControlHook() != null) {
                                    player.getControlHook().editOriginal(new MessageBuilder(t.get(player.getLanguage(),
                                            "chinczyk.interaction.crashed")).build()).queue();
                                    player.setControlHook(null);
                                }
                            } else errored(ex);
                        } catch (Exception ex) {
                            errored(ex);
                        }
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
                        if (!e.getUser().equals(executer) &&
                                players.values().stream().noneMatch(p -> p.getUser().equals(executer))) {
                            e.reply(t.get(l, "chinczyk.executer.first", executer.getAsMention())).setEphemeral(true).complete();
                            return;
                        }
                        if (players.values().stream().anyMatch(p -> p.getUser().equals(e.getUser()))) {
                            e.reply(t.get(l, "chinczyk.already.playing", executer.getAsMention())).setEphemeral(true).complete();
                            return;
                        }
                        try {
                            e.deferReply(true).complete();
                            Player player = new Player(place, e.getUser(), e.getHook());
                            Message control = e.getHook().sendMessage(generateControlMessage(player)).setEphemeral(true).complete();
                            player.setControlMessageId(control.getIdLong());
                            players.put(place, player);
                        } catch (ErrorResponseException ex) {
                            if (ex.getErrorResponse() != ErrorResponse.UNKNOWN_INTERACTION) errored(ex);
                        } catch (Exception ex) {
                            errored(ex);
                        }
                        updateMainMessage(true);
                        break;
                    }
                }
                return;
            }
            Player player = players.values().stream().filter(p -> p.getControlMessageId() == e.getMessageIdLong() && p.isPlaying())
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
                case LEAVE: {
                    if (e.getUser().equals(executer) && status != Status.IN_PROGRESS) {
                        e.reply(t.get(player.getLanguage(), "chinczyk.owner.selfabort")).setEphemeral(true).complete();
                        return;
                    }
                    e.deferEdit().queue();
                    if (!player.isConfirmLeave()) {
                        player.setConfirmLeave(true);
                        updateControlMessage(player);
                        return;
                    }
                    player.setStatus(PlayerStatus.LEFT);
                    if (status != Status.IN_PROGRESS) players.remove(player.getPlace(), player);
                    updateControlMessage(player);
                    if (status == Status.WAITING && !isEveryoneReady()) status = Status.WAITING_FOR_PLAYERS;
                    if (status == Status.IN_PROGRESS) {
                        eventStorage.add(new Event(Event.Type.LEFT_GAME, player, null, null, null, false));
                        if (turn == player.getPlace() || readyPlayerCount() < 2) {
                            rolled = null;
                            makeTurn();
                            return;
                        }
                    }
                    updateMainMessage(true);
                    break;
                }
                case END_MOVE: {
                    if (turn != player.getPlace() || rolled == null) return;
                    e.deferEdit().queue();
                    player.setConfirmLeave(false);
                    eventStorage.add(new Event(null, player, rolled, null, null, false));
                    makeTurn();
                    break;
                }
                case CANCEL: {
                    if (!player.isConfirmLeave()) return;
                    e.deferEdit().queue();
                    player.setConfirmLeave(false);
                    updateControlMessage(player);
                    return;
                }
                default: {
                    if (e.getComponentId().equals(ROLL) || e.getComponentId().startsWith(ROLL)) {
                        if (turn != player.getPlace() || rolled != null) return;
                        int rollNumber;
                        try {
                            if (rules.contains(Rules.DEV_MODE) && e.getComponentId().length() > ROLL.length())
                                rollNumber = Integer.parseInt(e.getComponentId().substring(ROLL.length()));
                            else {
                                rollNumber = random.nextInt(6) + 1; // 1-6
                                randomSeq++;
                            }
                        } catch (NumberFormatException ex) {
                            return;
                        }
                        rolled = rollNumber;
                        e.deferEdit().queue();
                        if (rules.contains(Rules.FAST_ROLLS)) {
                            long canMove = Arrays.stream(player.getPieces()).filter(Piece::canMove).count();
                            if (canMove == 0) {
                                eventStorage.add(new Event(null, player, rolled, null, null, true));
                                makeTurn();
                                break;
                            }
                            if (canMove == 1) {
                                Optional<Piece> piece = Arrays.stream(player.getPieces()).filter(Piece::canMove).findFirst();
                                if (piece.isPresent()) {
                                    movePiece(piece.get(), true);
                                    makeTurn();
                                    break;
                                }
                            }
                        }
                        updateMainMessage(false);
                        updateControlMessage(player);
                        break;
                    }
                    if (e.getComponentId().startsWith(MOVE_PREFIX)) {
                        if (turn != player.getPlace() || rolled == null) return;
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
                        player.setConfirmLeave(false);
                        movePiece(piece, false);
                        makeTurn();
                    }
                }
            }
        } catch (Exception ex) {
            errored(ex);
        } finally {
            lock.unlock();
        }
    }

    private void movePiece(Piece piece, boolean fastRoll) {
        Piece thrown = null;
        String nextPosition;
        int curPosition = piece.position;
        if (piece.position == 0) nextPosition = piece.getBoardPosition(1);
        else nextPosition = piece.getBoardPosition(curPosition + rolled);
        for (Player p : players.values()) {
            for (Piece pi : p.getPieces()) {
                if (pi.getBoardPosition().equals(nextPosition) && !p.equals(piece.getPlayer())) {
                    pi.position = 0;
                    thrown = pi;
                }
            }
        }
        if (piece.position == 0) piece.position = 1;
        else piece.position += rolled;
        if (thrown != null) eventStorage.add(new Event(Event.Type.THROW, piece.getPlayer(), rolled, piece, thrown, fastRoll));
        else {
            Event.Type type;
            if (piece.getBoardPosition()
                    .startsWith(String.valueOf(piece.getPlayer().getPlace().name().toLowerCase().charAt(0))) &&
                    curPosition <= 40) type = Event.Type.ENTERED_HOME; //tylko jeżeli wejdzie na x5-x8 z <=40
            else if (curPosition == 0) type = Event.Type.LEFT_START;
            else type = Event.Type.MOVE;
            eventStorage.add(new Event(type, piece.getPlayer(), rolled, piece, null, fastRoll));
        }
    }

    private boolean checkWin() {
        if (readyPlayerCount() == 1) {
            Optional<Player> p = players.values().stream().filter(Player::isPlaying).findAny();
            if (p.isPresent()) {
                winner = p.get();
                return true;
            }
            throw new IllegalStateException("ready == 1 ale nie ma wygranego");
        }
        for (Player p : players.values()) {
            if (rules.contains(Rules.QUICK_GAME)) {
                if (Arrays.stream(p.getPieces()).anyMatch(piece -> piece.position >= 41)) {
                    winner = p;
                    return true;
                }
            } else {
                if (Arrays.stream(p.getPieces()).allMatch(piece -> piece.position >= 41)) {
                    winner = p;
                    return true;
                }
            }
        }
        return false;
    }

    private void makeTurn() {
        lock.lock();
        try {
            if (checkWin()) {
                status = Status.ENDED;
                if (eventStorage.getLastEvent() == null)
                    throw new IllegalStateException("eventStorage.getLastEvent() jest null przy wygranej?");
                eventStorage.add(new Event(Event.Type.WON, winner, null, null, null, false));
                eventBus.unregister(this);
                try {
                    endCallback.accept(this);
                } catch (Exception ignored) {
                }
                updateMainMessage(true);
                updateControlMessages();
                return;
            }
            if (readyPlayerCount() < 2) {
                status = Status.CANCELLED;
                aborted("chinczyk.aborted.not.enough.players");
                return;
            }
            turns++;
            if (turn == null) turn = players.values().stream().filter(p -> p.getUser().equals(executer))
                    .findFirst().map(Player::getPlace).orElseThrow(() -> new IllegalStateException("executer nie gra?"));
            else if (!players.get(turn).isPlaying() || (rules.contains(Rules.ONE_ROLL) || rollCounter++ >= 2 ||
                    Arrays.stream(players.get(turn).getPieces()).anyMatch(p -> p.position != 0)) &&
                    (rolled == null || rolled != 6)) {
                turn = Place.getNextPlace(turn, players.entrySet().stream()
                        .filter(p -> p.getValue().isPlaying()).map(Map.Entry::getKey).collect(Collectors.toSet()));
                rollCounter = 0;
            }
            rolled = null;
            if (isTimeout.get() == Boolean.FALSE && timeout != null && !timeout.isCancelled() && !timeout.cancel(false)) return;
            timeout = executor.schedule(this::timeout, 1, TimeUnit.MINUTES);
            updateMainMessage(isTimeout.get() == Boolean.TRUE || eventStorage.getLastEvent() == null || eventStorage.getLastEvent().getType() != null);
            updateControlMessages();
        } finally {
            lock.unlock();
        }
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
        if (e.getComponentId().equals(LANGUAGE)) {
            Language selectedLanguage;
            try {
                selectedLanguage = Language.valueOf(e.getValues().get(0));
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                return;
            }
            lock.lock();
            try {
                if (e.getMessageIdLong() == message.getIdLong()) {
                    if (!e.getUser().equals(executer)) {
                        Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                                .findAny().map(Player::getLanguage).orElse(selectedLanguage);
                        e.reply(t.get(lang, "chinczyk.not.owner.language", executer.getAsMention()))
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
                if (player == null || !player.isPlaying()) return;
                player.setLanguage(selectedLanguage);
                e.deferEdit().queue();
                updateControlMessage(player);
            } catch (Exception ex) {
                errored(ex);
            } finally {
                lock.unlock();
            }
            return;
        }
        if (e.getComponentId().equals(RULES)) {
            if (e.getMessageIdLong() != message.getIdLong()) return;
            if (!players.isEmpty()) return;
            EnumSet<Rules> setRules = EnumSet.noneOf(Rules.class);
            try {
                for (String val : e.getValues()) {
                    Rules r = Rules.valueOf(val);
                    if (r.isCheat() && !cheats) return;
                    setRules.add(r);
                }
            } catch (IllegalArgumentException ex) {
                return;
            }
            lock.lock();
            try {
                if (!e.getUser().equals(executer)) {
                    Language lang = players.values().stream().filter(p -> p.getUser().equals(e.getUser()))
                            .findAny().map(Player::getLanguage).orElse(l);
                    e.reply(t.get(lang, "chinczyk.not.owner.rules", executer.getAsMention()))
                            .setEphemeral(true).complete();
                    return;
                }
                rules.clear();
                rules.addAll(setRules);
                e.deferEdit().queue();
                updateMainMessage(false);
            } catch (Exception ex) {
                errored(ex);
            } finally {
                lock.unlock();
            }
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent e) {
        if (!e.getChannel().equals(getChannel())) return;
        if (status != Status.WAITING_FOR_PLAYERS || !players.isEmpty()) return;
        if (cheats) return;
        if (!e.getAuthor().equals(executer)) return;
        if (e.getMessage().getContentRaw().equals("\u2191\u2191\u2193\u2193\u2190\u2192\u2190\u2192BA")) {
            lock.lock();
            try {
                cheats = true;
                if (!e.isFromGuild() || e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_ADD_REACTION))
                    e.getMessage().addReaction("\uD83D\uDC40").onErrorMap(err -> null).complete();
                updateMainMessage(false);
            } finally {
                lock.unlock();
            }
        }
    }
    
    @Subscribe
    public void onMessageDelete(MessageDeleteEvent e) {
        if (message != null && e.getMessageIdLong() == message.getIdLong()) {
            status = Status.MESSAGE_DELETED;
            aborted(null);
        }
    }

    @Subscribe
    public void onMessageBulkDelete(MessageBulkDeleteEvent e) {
        if (message != null && e.getMessageIds().contains(message.getId())) {
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
        if (e.getGuild().getIdLong() == getChannel().getIdLong()) {
            status = Status.LEFT_GUILD;
            aborted(null);
        }
    }

    private void updateMainMessage(boolean rerenderBoard) {
        lock.lock();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(renderBoard(), "png", baos);
            byte[] board = baos.toByteArray();
            String fileName = FILE_NAME + ".png";
            if (message == null) message = channel.sendMessage(generateMessage(fileName))
                    .referenceById(referenceMessageId).addFile(board, fileName).complete();
            else {
                Message msg = generateMessage(fileName);
                MessageAction ma = message.editMessage(msg);
                if (rerenderBoard) ma = ma.retainFiles(Collections.emptySet()).addFile(board, fileName);
                message = ma.onErrorFlatMap(ErrorResponse.UNKNOWN_MESSAGE::test,
                        e -> channel.sendMessage(msg).referenceById(referenceMessageId).addFile(board, fileName)).complete();
            }
            if (status == Status.ENDED) {
                new Thread(() -> {
                    try {
                        BoardReplayRenderer renderer = renderReplay();
                        if (renderer == null) return;
                        MessageAction ma = message.reply(t.get(l, "chinczyk.replay"));
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            ma.addFile(renderer.getStream(), "chinczykreplay." + renderer.getFormatExtension());
                        } catch (Exception ex) {
                            return; //plik za duży, ignoruj
                        }
                        ma.complete();
                    } catch (Exception ex) {
                        if (ex instanceof ErrorResponseException) return; //ignoruj błędy wysłania
                        try {
                            Sentry.getContext().addExtra("state", Base64.getEncoder().encodeToString(captureState().toByteArray()));
                        } catch (Exception ignored) {
                            // jak state sie nie zapisze to już trudno XD
                        }
                        Sentry.capture(ex);
                        Sentry.clearContext();
                    }
                }, "ChinczykReplay-" + executer + '-' + channel.getId()).start();
            }
        } catch (Exception e) {
            errored(e);
        } finally {
            lock.unlock();
        }
    }

    private void errored(Exception e) {
        LoggerFactory.getLogger(getClass()).error("Wystąpił błąd!", e);
        Sentry.capture(e);
        status = Status.ERRORED;
        String text = t.get(l, "chinczyk.errored");
        if (message != null)
            message.editMessage(text).override(true).onErrorMap(ErrorResponse.UNKNOWN_MESSAGE::test,
                    ex -> channel.sendMessage(text).referenceById(referenceMessageId).complete()).complete();
        try {
            eventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {}
        try {
            endCallback.accept(this);
        } catch (Exception ignored) {}
        try {
            updateControlMessages();
        } catch (Exception ignored) {}
    }

    public void shutdown(ChinczykStateDao stateDao) {
        eventBus.unregister(this);
        lock.lock();
        try {
            String key;
            if (status == Status.IN_PROGRESS) key = "chinczyk.shutting.down.saving";
            else key = "chinczyk.shutting.down";
            message.editMessage(t.get(l, key)).override(true).complete();
            for (Player p : players.values()) {
                if (p.isPlaying() && p.getControlHook() != null) {
                    String controlKey;
                    if (status == Status.IN_PROGRESS) controlKey = "chinczyk.control.shutting.down.saving";
                    else controlKey = "chinczyk.control.shutting.down";
                    p.getControlHook().editOriginal(new MessageBuilder(t.get(p.getLanguage(), controlKey)).build()).queue();
                    p.setControlHook(null);
                }
            }
            if (status == Status.IN_PROGRESS) stateDao.save(new ChinczykState(channel.getId(), captureState()));
        } catch (Exception e) {
            errored(e);
        } finally {
            executor.shutdownNow();
            lock.unlock();
        }
    }

    private Message generateControlMessage(Player player) {
        MessageBuilder mb = new MessageBuilder();
        List<Component> leaveComponents = new ArrayList<>();
        leaveComponents.add(Button.danger(LEAVE, t.get(player.getLanguage(), "chinczyk.control.leave")));
        if (player.confirmLeave)
            leaveComponents.add(Button.secondary(CANCEL, t.get(player.getLanguage(), "chinczyk.control.abort")));
        ActionRow leave = ActionRow.of(leaveComponents);
        switch (status) {
            case WAITING_FOR_PLAYERS:
            case WAITING: {
                if (player.getStatus() == PlayerStatus.JOINED) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.control.start"));
                    List<Component> controlComp = new ArrayList<>();
                    controlComp.add(Button.success(READY, t.get(player.getLanguage(), "chinczyk.control.ready")));
                    controlComp.addAll(leaveComponents);
                    mb.setActionRows(
                            ActionRow.of(controlComp),
                            generateLanguageMenu(player.getLanguage())
                    );
                    if (player.isConfirmLeave())
                        mb.append('\n').append(t.get(player.getLanguage(), "chinczyk.control.leave.text"));
                } else if (player.getStatus() == PlayerStatus.READY) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.control.start.waiting"));
                    mb.setActionRows(
                            leave,
                            generateLanguageMenu(player.getLanguage())
                    );
                    if (player.isConfirmLeave())
                        mb.append('\n').append(t.get(player.getLanguage(), "chinczyk.control.leave.text"));
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
                    mb.setActionRows(leave);
                    if (player.isConfirmLeave())
                        mb.append('\n').append(t.get(player.getLanguage(), "chinczyk.control.leave.text"));
                    break;
                }
                if (rolled == null) {
                    mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.die"));
                    List<ActionRow> actionRows = new ArrayList<>();
                    actionRows.add(ActionRow.of(
                            Button.primary(ROLL, t.get(player.getLanguage(), "chinczyk.button.roll"))));
                    if (rules.contains(Rules.DEV_MODE)) {
                        actionRows.add(ActionRow.of(
                                Button.secondary(ROLL + "1", "1"),
                                Button.secondary(ROLL + "2", "2"),
                                Button.secondary(ROLL + "3", "3")
                        ));
                        actionRows.add(ActionRow.of(
                                Button.secondary(ROLL + "4", "4"),
                                Button.secondary(ROLL + "5", "5"),
                                Button.secondary(ROLL + "6", "6")
                        ));
                    }
                    actionRows.add(leave);
                    mb.setActionRows(actionRows);
                } else {
                    Map<Integer, Piece> canMove = Arrays.stream(player.getPieces()).filter(Piece::canMove)
                            .collect(Collectors.toMap(p -> p.index + 1, p -> p));
                    if (canMove.isEmpty()) {
                        mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.move.cant", rolled));
                        mb.setActionRows(ActionRow.of(
                                Button.primary(END_MOVE, t.get(player.getLanguage(), "chinczyk.button.end.move"))
                        ), leave);
                    } else {
                        mb.setContent(t.get(player.getLanguage(), "chinczyk.awaiting.move", rolled));
                        mb.setActionRows(ActionRow.of(
                                Button.primary(MOVE_PREFIX + "0", "#1").withDisabled(!canMove.containsKey(1)),
                                Button.primary(MOVE_PREFIX + "1", "#2").withDisabled(!canMove.containsKey(2)),
                                Button.primary(MOVE_PREFIX + "2", "#3").withDisabled(!canMove.containsKey(3)),
                                Button.primary(MOVE_PREFIX + "3", "#4").withDisabled(!canMove.containsKey(4))
                        ), leave);
                    }
                }
                if (player.isConfirmLeave())
                    mb.append('\n').append(t.get(player.getLanguage(), "chinczyk.control.leave.text"));
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
        lock.lock();
        try {
            if (p.getControlHook() != null)
                p.getControlHook().editOriginal(generateControlMessage(p)).queue();
        } finally {
            lock.unlock();
        }
    }

    private void updateControlMessages() {
        lock.lock();
        try {
            for (Player p : players.values()) if (p.isPlaying()) updateControlMessage(p);
        } finally {
            lock.unlock();
        }
    }

    private Piece getPieceAt(String boardPosition) {
        for (Player p : players.values()) {
            if (!p.isPlaying()) continue;
            for (Piece piece : p.getPieces()) {
                if (piece.getBoardPosition().equals(boardPosition))
                    return piece;
            }
        }
        return null;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(eventStorage);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(new HashSet<>(players.values()));
    }

    public long getGameDuration() {
        return gameDuration + (end.getEpochSecond() - start.getEpochSecond());
    }

    private boolean rolled6() {
        if (rolled == null) return false;
        if (rules.contains(Rules.ONE_LEAVES_HOME)) return rolled == 1 || rolled == 6;
        return rolled == 6;
    }

    public ByteArrayOutputStream captureState() {
        if (status != Status.IN_PROGRESS && status != Status.ENDED)
            throw new IllegalStateException("Można zachować stan gry jedynie o statusie IN_PROGRESS lub ENDED");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        Instant now = Instant.now();
        lock.lock();
        try {
            baos.write(CHINCZYK_HEADER);
            baos.write(CHINCZYK_VERSION);
            writeLong(baos, executer.getIdLong());
            writeLong(baos, channel.getIdLong());
            writeLong(baos, referenceMessageId);
            writeLong(baos, randomSeed);
            writeLong(baos, randomSeq - (rolled == null ? 0 : 1));
            writeString(baos, l.name());
            baos.write(players.size());
            for (Player p : players.values())
                writePlayer(baos, p);
            writeLong(baos, Rules.toRaw(rules));
            baos.write(cheats ? 1 : 0);
            writeUnsignedInt(baos, gameDuration);
            writeLong(baos, start.toEpochMilli());
            writeLong(baos, now.toEpochMilli());
            for (Event event : eventStorage) {
                baos.write(event.getType() == null ? 0 : event.getType().getRaw());
                baos.write(event.getPlayer() == null ? 0 : event.getPlayer().getPlace().getOffset());
                baos.write(event.getRolled() == null ? 0 : event.getRolled());
                baos.write(event.getPiece() == null ? 0 : event.getPiece().getIndex());
                baos.write(event.getPiece2() == null ? 0 : event.getPiece2().getPlayer().getPlace().getOffset());
                if (event.getPiece2() != null)
                    baos.write(event.getPiece2().getIndex());
                baos.write(event.getFastRolled() == Boolean.TRUE ? 1 : 0);
                if (event.getType() == Event.Type.WON) writeLong(baos, end.toEpochMilli());
            }
        } catch (IOException e) { //niemożliwe
            return null;
        } catch (OutOfMemoryError e) {
            throw new IllegalStateException("Nie udało się wygenerować stanu gry - przekroczono limit pamięci");
        } finally {
            lock.unlock();
        }
        return baos;
    }

    @Data
    @EqualsAndHashCode(exclude = "pieces")
    @ToString(exclude = "pieces")
    public class Player {
        private final Place place;
        private final User user;
        private final Piece[] pieces = new Piece[4];
        private InteractionHook controlHook;
        private long controlMessageId;
        private Language language;
        private PlayerStatus status = PlayerStatus.JOINED;
        private ScheduledFuture<?> handle;
        private boolean confirmLeave = false;

        public Player(Place place, User user, InteractionHook controlHook) {
            this.place = place;
            this.user = user;
            setControlHook(controlHook);
            initPieces();
        }

        private void initPieces() {
            for (int i = 0; i < pieces.length; i++)
                pieces[i] = new Piece(this, i);
        }

        public void setControlHook(InteractionHook controlHook) {
            if (handle != null) {
                handle.cancel(true);
                handle = null;
            }
            this.controlHook = controlHook;
            if (controlHook == null) return;
            handle = executor.schedule(() -> {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    if (Objects.equals(this.controlHook, controlHook)) this.controlHook = null;
                    Message msg = new MessageBuilder(t.get(getLanguage(), "chinczyk.control.expired")).build();
                    controlHook.editOriginal(msg).complete();
                } finally {
                    lock.unlock();
                }
            }, controlHook.getExpirationTimestamp() - System.currentTimeMillis() - 90000, TimeUnit.MILLISECONDS);
        }

        public Language getLanguage() {
            return language != null ? language : l;
        }

        public boolean isPlaying() {
            Status gameStatus = Chinczyk.this.status;
            if (gameStatus == Status.WAITING || gameStatus == Status.WAITING_FOR_PLAYERS)
                return getStatus() != PlayerStatus.LEFT;
            return getStatus() == PlayerStatus.PLAYING ||
                    ((gameStatus == Status.CANCELLED || gameStatus == Status.ERRORED ||
                            gameStatus == Status.LEFT_GUILD || gameStatus == Status.MESSAGE_DELETED)
                            && getStatus() == PlayerStatus.READY);
        }

        public void setStatus(PlayerStatus status) {
            if (status == PlayerStatus.LEFT) {
                initPieces();
                if (handle != null) handle.cancel(true);
            }
            this.status = status;
        }

        public String getName() {
            String str = getUser().getName();
            if (str.length() >= 13) str = str.substring(0, 10) + "...";
            return str;
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
        BLUE("\uD83D\uDFE6", 2, new Color(0x0000F8), new Color(0xFFFFFF)),
        GREEN("\uD83D\uDFE9", 12, new Color(0x007C00)),
        YELLOW("\uD83D\uDFE8", 22, new Color(0xF4F600)),
        RED("\uD83D\uDFE5", 32, new Color(0xFF0000));

        @Getter private final String emoji;
        @Getter private final int offset;
        @Getter private final Color bgColor;
        @Getter private final Color textColor;

        Place(String emoji, int offset, Color bgColor) {
            this(emoji, offset, bgColor, Color.BLACK);
        }

        Place(String emoji, int offset, Color bgColor, Color textColor) {
            this.emoji = emoji;
            this.offset = offset;
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

        public static Place getByOffset(int offset) {
            for (Place p : values()) {
                if (p.getOffset() == offset) return p;
            }
            return null;
        }
    }

    @Data
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
            Boolean captureable = canCapture();
            if (captureable != null) return captureable;
            if (rules.contains(Rules.NO_PASSES)) {
                int rolledLoop = rolled;
                do {
                    String boardPosition;
                    if (position == 0) boardPosition = getBoardPosition(1);
                    else boardPosition = getBoardPosition(position + rolledLoop);
                    Piece pieceAt = getPieceAt(boardPosition);
                    if (pieceAt != null && !pieceAt.getPlayer().equals(player)) {
                        if (rolledLoop == rolled) break; // na wszelki, choć nie powinno do tego dojść
                        return false;
                    }
                } while (--rolledLoop >= 1 && position != 0); //jeżeli pos. jest 0, wykonaj pętle tylko raz bo więcej nie ma sensu
            }
            if (rules.contains(Rules.FORCE_CAPTURE) && Arrays.stream(player.getPieces()).filter(p -> !p.equals(this))
                    .anyMatch(p -> p.canCapture() == Boolean.TRUE)) return false;
            return position != 0 || rolled6(); // pole czyste, można iść
        }

        // true - można bić, false - pole zajęte, null - pole wolne
        public Boolean canCapture() {
            String nextPosition;
            if (position == 0) {
                if (!rolled6()) return null;
                nextPosition = getBoardPosition(1);
            }
            else nextPosition = getBoardPosition(position + rolled);
            Piece pieceAt = getPieceAt(nextPosition);
            if (pieceAt != null) return !pieceAt.getPlayer().equals(player);
            return null;
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
            int offset = player.getPlace().getOffset();
            int pos = (position + offset) % 40;
            return String.valueOf(pos != 0 ? pos : 40);
        }

        public Piece copy() {
            Piece p = new Piece(player, index);
            p.position = position;
            return p;
        }
    }

    @Data
    public static class Event {
        private final Type type;
        private final Player player;
        private final Integer rolled;
        private final Piece piece;
        private final Piece piece2;
        private final Boolean fastRolled;

        private Event(Type type, Player player, Integer rolled, Piece piece, Piece piece2, Boolean fastRolled) {
            this.type = type;
            this.player = player;
            this.rolled = rolled;
            this.piece = checkType(type, Type.LEFT_START, Type.MOVE, Type.ENTERED_HOME, Type.THROW) ?
                    Objects.requireNonNull(piece) : null;
            this.piece2 = checkType(type, Type.THROW) ? Objects.requireNonNull(piece2) : null;
            this.fastRolled = checkType(true, type, Type.LEFT_START, Type.MOVE, Type.ENTERED_HOME, Type.THROW) ? fastRolled : null;
        }

        private static boolean checkType(Type type, Type... allowedTypes) {
            return checkType(false, type, allowedTypes);
        }

        private static boolean checkType(boolean allowNull, Type type, Type... allowedTypes) {
            if (allowNull && type == null) return true;
            return allowedTypes != null && Arrays.asList(allowedTypes).contains(type);
        }

        private String getTranslated(Tlumaczenia t, Language l) {
            return getTranslated(t, l, true);
        }

        private String getTranslated(Tlumaczenia t, Language l, boolean mention) {
            if (type == null) throw new IllegalArgumentException("type null");
            Function<Piece, String> pieceString = p -> t.get(l, "chinczyk.piece", p.getIndexAsString(),
                    mention ? p.player.getUser().getAsMention() : p.player.getName());
            switch (type) {
                case LEFT_START:
                case MOVE:
                case ENTERED_HOME:
                    return t.get(l, type.translationKey, StringUtils.capitalize(pieceString.apply(piece)));
                case WON:
                case LEFT_GAME:
                    return t.get(l, type.translationKey, mention ? player.getUser().getAsMention() : player.getName());
                case THROW:
                    return t.get(l, type.translationKey, StringUtils.capitalize(pieceString.apply(piece)),
                            pieceString.apply(piece2));
                default:
                    return t.get(l, type.translationKey);
            }
        }

        public enum Type {
            GAME_START(1, "game.start"),
            LEFT_START(2, "left.start"),
            MOVE(3, "move"),
            THROW(4, "throw"),
            ENTERED_HOME(5, "entered.home"),
            WON(6, "win"),
            LEFT_GAME(7, "left.game");

            @Getter private final int raw;
            private final String translationKey;

            Type(int raw, String translationKey) {
                this.raw = raw;
                this.translationKey = "chinczyk.event." + translationKey;
            }

            public static Type getByRaw(int raw) {
                for (Type t : Type.values()) {
                    if (t.getRaw() == raw) return t;
                }
                return null;
            }
        }
    }

    public static class EventStorage extends ArrayList<Event> {
        public Event getLastEvent() {
            if (size() == 0) return null;
            return get(size() - 1);
        }
    }

    public enum Rules {
        /**
         * Szybka gra - wygrywa pierwszy pionek w polu domowym
         */
        QUICK_GAME(1, "chinczyk.rule.quick.game"),
        /**
         * Jeden rzut - wyłącza 3 rzuty kostką kiedy brak pionów na mapie
         */
        ONE_ROLL(1<<1, "chinczyk.rule.one.roll"),
        /**
         * Zakaz przeskakiwania - jeżeli próbujesz przejść przez pion innego gracza, a nie możesz go zbić nie zezwalaj na ruch
         */
        NO_PASSES(1<<2, "chinczyk.rule.no.passes"),
        /**
         * Wymuś bicie - jeżeli jeden z pionków ma bicie, nie pozwalaj na ruch innym
         */
        FORCE_CAPTURE(1<<3, "chinczyk.rule.force.capture"),
        /**
         * Szybkie rzuty - kiedy jest tylko jeden (lub 0) ruchów dozwolonych, wykonaj je automatycznie
         */
        FAST_ROLLS(1<<4, "chinczyk.rule.fast.rolls"),
        /**
         * Tryb dewelopera - wybierasz rzut kostką
         */
        DEV_MODE(1<<5, true, "chinczyk.rule.dev.mode"),
        /**
         * 1 opuszcza start - wyrzucenie 1 (tak jak 6) opuszcza pole startowe
         */
        ONE_LEAVES_HOME(1<<6, "chinczyk.rule.one.leaves");

        @Getter private final int flag;
        @Getter private final boolean cheat;
        @Getter private final String key;

        Rules(int flag, String key) {
            this(flag, false, key);
        }

        Rules(int flag, boolean cheat, String key) {
            this.flag = flag;
            this.cheat = cheat;
            this.key = key;
        }

        public String getDescriptionKey() {
            return key + ".description";
        }

        public static Set<Rules> fromRaw(long raw) {
            EnumSet<Rules> rules = EnumSet.noneOf(Rules.class);
            for (Rules r : values()) if ((raw & r.flag) == r.flag) rules.add(r);
            return rules;
        }

        public static long toRaw(Set<Rules> set) {
            long raw = 0;
            for (Rules r : set) raw |= r.flag;
            return raw;
        }
    }
}
