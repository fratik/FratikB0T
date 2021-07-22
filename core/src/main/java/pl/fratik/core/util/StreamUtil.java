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

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StreamUtil {
    private StreamUtil() {}

    public static long readUnsignedInt(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        if (is.read(bb.array()) != 4) throw new EOFException();
        return Integer.toUnsignedLong(bb.getInt());
    }

    public static Color readColor(InputStream is) throws IOException {
        return new Color((int) readUnsignedInt(is));
    }

    public static long readLong(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        if (is.read(bb.array()) != 8) throw new EOFException();
        return bb.getLong();
    }

    public static String readString(InputStream is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        if (is.read(bb.array()) != 2) throw new EOFException();
        byte[] buffer = new byte[bb.getShort()];
        if (is.read(buffer) != buffer.length) throw new EOFException();
        return new String(buffer, 0, buffer.length, StandardCharsets.UTF_8);
    }

    public static void writeUnsignedInt(OutputStream os, long l) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt((int) l);
        os.write(bb.array());
    }

    public static void writeColor(OutputStream os, Color c) throws IOException {
        writeUnsignedInt(os, c.getRGB() & 0xffffff);
    }

    public static void writeLong(OutputStream os, long l) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(l);
        os.write(bb.array());
    }

    public static void writeString(OutputStream os, String s) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        bb.putShort((short) bytes.length);
        os.write(bb.array());
        os.write(bytes);
    }
}
