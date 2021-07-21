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
import lombok.experimental.Delegate;
import net.dv8tion.jda.api.entities.Emoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.StreamUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

// aka nie mam pojęcia czemu to istnieje
@Getter
public enum SpecialSkins implements ChinczykSkin {
    SENKO(1, "anime hlep", "https://i.fratikbot.pl/TN2P7jb.png") {
        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            return "Senko Bread";
        }
    },
    PIOTR_GRYF(1<<1, "w tym memie chodzi o to że jebać pis", "https://i.fratikbot.pl/x61TPLN.png") {
        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            return "Peter Griffin";
        }
    },
    PAPAJ(1<<2/*137*/, "papiez2137", "https://i.fratikbot.pl/wwREWg8.png") {
        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            return "Jan Paweł 2";
        }
    },
    WEEB(1<<3, "uwu owo >.<", "https://i1.sndcdn.com/artworks-000200048978-glvasd-t500x500.jpg") {
        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            return "Weeb";
        }
    };

    private final Logger logger = LoggerFactory.getLogger(SpecialSkins.class);

    private final int flag;
    private final String password;
    private interface DelExc {
        void serialize(OutputStream os) throws IOException;
        Emoji getEmoji();
        String getValue();
        String getTranslated(Tlumaczenia t, Language l) throws IOException;
    }
    private final @Delegate(types = ChinczykSkin.class, excludes = DelExc.class) SpecialSkinImpl skin;
    private final BufferedImage background;

    public class SpecialSkinImpl extends SkinImpl {
        public SpecialSkinImpl(ChinczykSkin skin) {
            super(skin.getTextColor(), null, skin.getCircleStroke(), skin.getCircleFill(),
                    skin.getRedFill(), skin.getRedStartFill(), skin.getGreenFill(), skin.getGreenStartFill(),
                    skin.getBlueFill(), skin.getBlueStartFill(), skin.getYellowFill(), skin.getYellowStartFill(),
                    skin.getArrowStroke(), skin.getArrowFill(), skin.getLineStroke(),
                    skin.getPieceStroke(), skin.getEmoji());
        }

        @Override
        protected void drawBackground(Graphics g, int width, int height) {
            Color c = g.getColor();
            g.setColor(Chinczyk.DefaultSkins.DEFAULT.getBgColor());
            g.fillRect(0, 0, width, height);
            g.setColor(c);
            g.drawImage(background, 0, 0, width, height, null);
        }
    }

    SpecialSkins(int flag, String password, String bgImage) {
        this.flag = flag;
        this.password = password;
        this.skin = new SpecialSkinImpl(Chinczyk.DefaultSkins.DEFAULT);
        BufferedImage bg;
        try {
            bg = ImageIO.read(new URL(bgImage));
        } catch (Exception e) {
            logger.error("Nie udało się pobrać tła!", e);
            bg = null;
        }
        background = bg;
    }

    SpecialSkins(int flag, String password, URL bgImage) {
        BufferedImage bg;
        this.flag = flag;
        this.password = password;
        this.skin = new SpecialSkinImpl(Chinczyk.DefaultSkins.DEFAULT.getSkin());
        try {
            bg = ImageIO.read(bgImage);
        } catch (IOException e) {
            logger.error("Nie udało się pobrać tła!", e);
            bg = null;
        }
        background = bg;
    }

    public boolean isAvailable() {
        return background != null;
    }

    public static SpecialSkins fromRaw(long raw) {
        for (SpecialSkins s : values()) if ((raw & s.flag) == s.flag) return s;
        return null;
    }

    public static SpecialSkins fromPassword(String password) {
        for (SpecialSkins s : values()) if (password.equals(s.getPassword())) return s;
        return null;
    }

    @Override
    public void serialize(OutputStream os) throws IOException {
        os.write(1);
        StreamUtil.writeString(os, getClass().getName());
        StreamUtil.writeLong(os, flag);
    }

    public static ChinczykSkin deserialize(InputStream is) throws IOException {
        return fromRaw(StreamUtil.readLong(is));
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public Emoji getEmoji() {
        return null;
    }
}
