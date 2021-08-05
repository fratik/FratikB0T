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

import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static pl.fratik.core.util.StreamUtil.writeString;
import static pl.fratik.core.util.StreamUtil.writeUnsignedInt;

public class CustomBgSkin extends ChinczykSkin.SkinImpl {
    private final BufferedImage image;

    protected CustomBgSkin(BufferedImage image) {
        super(Chinczyk.DefaultSkins.DEFAULT);
        this.image = image;
        bgColor = null;
        emoji = null;
    }

    @Override
    public String getTranslated(Tlumaczenia t, Language l) {
        return "Custom";
    }

    @Override
    public String getValue() {
        return "custom";
    }

    @Override
    protected void drawBackground(Graphics g, int width, int height) {
        g.drawImage(image, 0, 0, width, height, null);
    }

    @Override
    public void serialize(OutputStream os) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (OutOfMemoryError ex) {
            Chinczyk.DefaultSkins.DEFAULT.serialize(os);
            return;
        }
        os.write(1);
        writeString(os, getClass().getName());
        writeUnsignedInt(os, baos.size());
        os.write(baos.toByteArray());
    }

    public static ChinczykSkin deserialize(InputStream is) throws IOException {
        BufferedImage image = ImageIO.read(is);
        return new CustomBgSkin(image);
    }
}
