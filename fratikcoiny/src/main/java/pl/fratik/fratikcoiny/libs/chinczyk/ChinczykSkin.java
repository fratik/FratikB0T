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

import lombok.Getter;
import net.dv8tion.jda.api.entities.Emoji;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.fratikcoiny.util.ImageUtils;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;

import static pl.fratik.core.util.CommonUtil.asHex;
import static pl.fratik.core.util.StreamUtil.*;

public interface ChinczykSkin {
    Color getTextColor();
    Color getBgColor();
    Color getCircleStroke();
    Color getCircleFill();
    Color getRedFill();
    Color getRedStartFill();
    Color getGreenFill();
    Color getGreenStartFill();
    Color getBlueFill();
    Color getBlueStartFill();
    Color getYellowFill();
    Color getYellowStartFill();
    Color getArrowStroke();
    Color getArrowFill();
    Color getLineStroke();
    Color getPieceStroke();
    Emoji getEmoji();
    String getValue();

    void drawBoard(Graphics g, int width, int height);
    String getTranslated(Tlumaczenia t, Language l);
    void serialize(OutputStream os) throws IOException;

    static ChinczykSkin deserialize(InputStream is) throws IOException {
        int read = is.read();
        if (read == -1) throw new EOFException();
        boolean custom = read == 1;
        if (!custom) return Chinczyk.DefaultSkins.fromRaw(readLong(is));
        String deserializer = readString(is);
        int size = Math.toIntExact(readUnsignedInt(is));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        while (baos.size() < size) {
            int b = is.read();
            if (b == -1) throw new EOFException();
            baos.write(b);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try {
            Class<?> cls = Class.forName(deserializer);
            Method m = cls.getDeclaredMethod("deserialize", InputStream.class);
            return (ChinczykSkin) m.invoke(null, bais);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    static ChinczykSkin of(Color textColor,
                           Color bgColor,
                           Color circleStroke,
                           Color circleFill,
                           Color redFill,
                           Color redStartFill,
                           Color greenFill,
                           Color greenStartFill,
                           Color blueFill,
                           Color blueStartFill,
                           Color yellowFill,
                           Color yellowStartFill,
                           Color arrowStroke,
                           Color arrowFill,
                           Color lineStroke,
                           Color pieceStroke,
                           Emoji emoji) {
        return new SkinImpl(textColor, bgColor, circleStroke, circleFill, redFill, redStartFill,
                greenFill, greenStartFill, blueFill, blueStartFill, yellowFill, yellowStartFill, arrowStroke,
                arrowFill, lineStroke, pieceStroke, emoji);
    }

    @Getter
    class SkinImpl implements ChinczykSkin {
        protected final Color textColor;
        protected final Color bgColor;
        protected final Color circleStroke;
        protected final Color circleFill;
        protected final Color redFill;
        protected final Color redStartFill;
        protected final Color greenFill;
        protected final Color greenStartFill;
        protected final Color blueFill;
        protected final Color blueStartFill;
        protected final Color yellowFill;
        protected final Color yellowStartFill;
        protected final Color arrowStroke;
        protected final Color arrowFill;
        protected final Color lineStroke;
        protected final Color pieceStroke;
        protected final Emoji emoji;

        protected SkinImpl(Color textColor,
                         Color bgColor,
                         Color circleStroke,
                         Color circleFill,
                         Color redFill,
                         Color redStartFill,
                         Color greenFill,
                         Color greenStartFill,
                         Color blueFill,
                         Color blueStartFill,
                         Color yellowFill,
                         Color yellowStartFill,
                         Color arrowStroke,
                         Color arrowFill,
                         Color lineStroke,
                         Color pieceStroke,
                         Emoji emoji) {
            this.textColor = textColor;
            this.bgColor = bgColor;
            this.circleStroke = circleStroke;
            this.circleFill = circleFill;
            this.redFill = redFill;
            this.redStartFill = redStartFill;
            this.greenFill = greenFill;
            this.greenStartFill = greenStartFill;
            this.blueFill = blueFill;
            this.blueStartFill = blueStartFill;
            this.yellowFill = yellowFill;
            this.yellowStartFill = yellowStartFill;
            this.arrowStroke = arrowStroke;
            this.arrowFill = arrowFill;
            this.lineStroke = lineStroke;
            this.pieceStroke = pieceStroke;
            this.emoji = emoji;
        }

        protected void drawBackground(Graphics g, int width, int height) {
            // do nadpisania przez super-klasy - domyślnie nie rób nic, a tło ustawiaj w #drawBoard
        }

        @Override
        public String getValue() {
            return getClass().getSimpleName();
        }

        @Override
        public void drawBoard(Graphics g, int width, int height) {
            ImageUtils.BufferedImageTranscoder trans = new ImageUtils.BufferedImageTranscoder();
            SVGDocument planszaSvg = (SVGDocument) Chinczyk.plansza.cloneNode(true);
            planszaSvg.getElementById("circle").setAttribute("stroke", "#" + asHex(circleStroke));
            ((Element) planszaSvg.getElementById("marker-w").getElementsByTagName("use").item(0))
                    .setAttribute("fill", "#" + asHex(circleFill));
            ((Element) planszaSvg.getElementById("marker-r").getElementsByTagName("use").item(0))
                    .setAttribute("fill", "#" + asHex(redFill));
            ((Element) planszaSvg.getElementById("marker-g").getElementsByTagName("use").item(0))
                    .setAttribute("fill", "#" + asHex(greenFill));
            ((Element) planszaSvg.getElementById("marker-b").getElementsByTagName("use").item(0))
                    .setAttribute("fill", "#" + asHex(blueFill));
            ((Element) planszaSvg.getElementById("marker-y").getElementsByTagName("use").item(0))
                    .setAttribute("fill", "#" + asHex(yellowFill));
            planszaSvg.getElementById("arrow").setAttribute("fill", "#" + asHex(arrowFill));
            planszaSvg.getElementById("arrow").setAttribute("stroke", "#" + asHex(arrowStroke));
            planszaSvg.getElementById("bg").setAttribute("fill", bgColor == null ? "none" : "#" + asHex(bgColor));
            planszaSvg.getElementById("line").setAttribute("stroke", "#" + asHex(lineStroke));
            planszaSvg.getElementById("rs").setAttribute("fill", "#" + asHex(redStartFill));
            planszaSvg.getElementById("gs").setAttribute("fill", "#" + asHex(greenStartFill));
            planszaSvg.getElementById("bs").setAttribute("fill", "#" + asHex(blueStartFill));
            planszaSvg.getElementById("ys").setAttribute("fill", "#" + asHex(yellowStartFill));
            trans.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) width);
            trans.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float) height);
            trans.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, new Color(0,0,0,0));
            try {
                trans.transcode(new TranscoderInput(planszaSvg), null);
            } catch (TranscoderException e) {
                // niemożliwe, ale na wszelki
                throw new IllegalStateException(e);
            }
            drawBackground(g, width, height);
            g.drawImage(trans.getImage(), 0, 0, width, height, null);
        }

        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            return t.get(l, "chinczyk.skin.custom");
        }

        @Override
        public void serialize(OutputStream os) throws IOException {
            os.write(1);
            writeString(os, getClass().getName());
            writeColor(os, textColor);
            writeColor(os, bgColor);
            writeColor(os, circleStroke);
            writeColor(os, circleFill);
            writeColor(os, redFill);
            writeColor(os, redStartFill);
            writeColor(os, greenFill);
            writeColor(os, greenStartFill);
            writeColor(os, blueFill);
            writeColor(os, blueStartFill);
            writeColor(os, yellowFill);
            writeColor(os, yellowStartFill);
            writeColor(os, arrowStroke);
            writeColor(os, arrowFill);
            writeColor(os, lineStroke);
            writeColor(os, pieceStroke);
            writeString(os, emoji == null ? "" : emoji.getAsMention());
        }

        public static ChinczykSkin deserialize(InputStream is) throws IOException {
            String emojiStr = readString(is);
            Emoji emoji = emojiStr.isEmpty() ? null : Emoji.fromMarkdown(emojiStr);
            return of(readColor(is), readColor(is), readColor(is), readColor(is), readColor(is), readColor(is),
                    readColor(is), readColor(is), readColor(is), readColor(is), readColor(is), readColor(is),
                    readColor(is), readColor(is), readColor(is), readColor(is), emoji);
        }
    }
}
