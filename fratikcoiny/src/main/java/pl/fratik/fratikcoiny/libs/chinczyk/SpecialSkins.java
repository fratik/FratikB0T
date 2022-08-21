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
import net.dv8tion.jda.api.entities.emoji.Emoji;
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
import java.lang.ref.SoftReference;
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
    WEEB(1<<3, "uwu owo >.<", "https://i.fratikbot.pl/LmbUteC.jpg") {
        @Override
        public String getTranslated(Tlumaczenia t, Language l) {
            //noinspection SpellCheckingInspection - celowe
            return "Anime gril";
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
    private URL url;
    private SoftReference<BufferedImage> background;

    public class SpecialSkinImpl extends SkinImpl {
        public SpecialSkinImpl(ChinczykSkin skin) {
            super(skin);
            bgColor = null;
        }

        @Override
        protected void drawBackground(Graphics g, int width, int height) {
            if (!isAvailable()) throw new IllegalStateException("skin nie jest dostępny");
            g.drawImage(getBackground(), 0, 0, width, height, null);
        }
    }

    SpecialSkins(int flag, String password) {
        this.flag = flag;
        this.password = password;
        this.skin = new SpecialSkinImpl(Chinczyk.DefaultSkins.DEFAULT);
    }

    SpecialSkins(int flag, String password, String bgImage) {
        this(flag, password);
        try {
            url = new URL(bgImage);
        } catch (Exception e) {
            logger.error("Nieprawidłowy link!", e);
            url = null;
        }
    }

    SpecialSkins(int flag, String password, URL bgImage) {
        this(flag, password);
        url = bgImage;
    }

    private synchronized BufferedImage getBackground() {
        if (background != null && background.get() != null) return background.get();
        try {
            BufferedImage image = ImageIO.read(url);
            background = new SoftReference<>(image);
            return image;
        } catch (Exception e) {
            logger.error("Nie udało się pobrać zdjęcia!", e);
            background = null;
            return null;
        }
    }

    public boolean isAvailable() {
        return getBackground() != null;
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
        StreamUtil.writeString(os, SpecialSkins.class.getName());
        StreamUtil.writeUnsignedInt(os, 8);
        StreamUtil.writeLong(os, flag);
    }

    public static ChinczykSkin deserialize(InputStream is) throws IOException {
        SpecialSkins s = fromRaw(StreamUtil.readLong(is));
        if (s == null || !s.isAvailable()) throw new IOException("skin nie jest dostępny");
        return s;
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
