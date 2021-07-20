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

package pl.fratik.fratikcoiny.util;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    private ImageUtils() {}

    private static int findBreakBefore(String text, int pos) {
        for (int i = pos; i >= 0; i--) if (Character.isWhitespace(text.charAt(i))) return i;
        return -1;
    }

    private static int findBreakAfter(String text, int pos) {
        for (int i = pos; i < text.length(); i++) if (Character.isWhitespace(text.charAt(i))) return i;
        return -1;
    }
    
    public static List<String> splitString(String text, FontMetrics fontMetrics, int lineCount, int imageWidth) {
        List<String> lines = new ArrayList<>();
        while (fontMetrics.stringWidth(text) > imageWidth) {
            int breakGuess = text.length() * imageWidth / fontMetrics.stringWidth(text);
            int breakPos;
            int width = fontMetrics.stringWidth(text.substring(0, breakGuess).trim());
            if (width > imageWidth) {
                breakPos = findBreakBefore(text, breakGuess);
                while (breakPos != -1 && fontMetrics.stringWidth(text.substring(0, breakPos).trim()) > imageWidth)
                    breakPos = findBreakBefore(text, breakPos - 1);
            } else {
                breakPos = findBreakAfter(text, breakGuess);
                if (breakPos != -1) {
                    while (fontMetrics.stringWidth(text.substring(0, breakPos).trim()) > imageWidth)
                        breakPos = findBreakBefore(text, breakPos - 1);
                } else {
                    breakPos = findBreakBefore(text, breakGuess);
                    while (breakPos != -1 && fontMetrics.stringWidth(text.substring(0, breakPos).trim()) > imageWidth)
                        breakPos = findBreakBefore(text, breakPos - 1);
                }
            }
            if (breakPos == -1) breakPos = breakGuess;
            lines.add(text.substring(0, breakPos).trim());
            text = text.substring(breakPos).trim();
            if (lineCount <= lines.size()) break;
        }
        if (lineCount > lines.size() && !text.isEmpty()) lines.add(text);
        return lines;
    }

    public static void renderLinesCentered(Graphics g, List<String> lines, int x, int y, final int width, final int height) {
        if (lines.isEmpty()) return;
        FontMetrics fontMetrics = g.getFontMetrics();
        final int initialX = x;
        final int initialY = y;
        y = initialY + ((height - (fontMetrics.getHeight() * lines.size())) / 2) + fontMetrics.getAscent();
        for (String line : lines) {
            x = initialX + (width - fontMetrics.stringWidth(line)) / 2;
            g.drawString(line, x, y);
            y += fontMetrics.getHeight();
        }
    }
}
