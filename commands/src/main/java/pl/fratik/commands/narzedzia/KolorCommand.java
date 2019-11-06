/*
 * Copyright (C) 2019 FratikB0T Contributors
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

package pl.fratik.commands.narzedzia;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KolorCommand extends Command {
    public KolorCommand() {
        name = "kolor";
        category = CommandCategory.UTILITY;
        uzycie = new Uzycie("kolor", "string", true);
        aliases = new String[] {"color", "paleta", "colors"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Color color = null;
        //#region kolory
        try {
            color = Color.decode((String) context.getArgs()[0]);
        } catch (Exception ignored) {
            try {
                color = (Color) Color.class.getField(((String) context.getArgs()[0]).toUpperCase()).get(null);
                if (color == null) color = getCssColor(context);
                if (color == null) throw new Exception();
            } catch (Exception ignored2) {
                try {
                    String[] rgbTmp = ((String) context.getArgs()[0]).split(",");
                    int[] rgb = new int[3];
                    for (int i = 0; i < rgbTmp.length; i++) {
                        rgb[i] = Integer.parseInt(rgbTmp[i].trim());
                    }
                    color = new Color(rgb[0], rgb[1], rgb[2]);
                } catch (Exception ignored3) {
                    try {
                        String[] hsbTmp = ((String) context.getArgs()[0]).split(",");
                        int[] hsb = new int[3];
                        for (int i = 0; i < hsbTmp.length; i++) {
                            hsb[i] = Integer.parseInt(hsbTmp[i].trim());
                        }
                        color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
                    } catch (Exception ignored4) {
                        if (((String) context.getArgs()[0]).equalsIgnoreCase("blurple")) color = new Color(114, 137, 218);
                    }
                }
            }
        }
        //#endregion kolory
        if (color == null) {
            context.send(context.getTranslated("kolor.unknown.color"));
            return false;
        }
        try {
            BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = (Graphics2D) img.getGraphics();
            graphics2D.setBackground(color);
            graphics2D.clearRect(0, 0, 512, 512);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            baos.flush();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(color);
            eb.setImage("attachment://" + asHex(color) + ".png");
            eb.setTitle(context.getTranslated("kolor.embed.header"));
            String[] rgb = new String[3];
            rgb[0] = String.valueOf(color.getRed());
            rgb[1] = String.valueOf(color.getGreen());
            rgb[2] = String.valueOf(color.getBlue());
            eb.addField("RGB", String.join(", ", rgb), true);
            eb.addField("Hex", "#" + asHex(color), true);
            if (getCssName(color) != null) eb.addField("CSS", getCssName(color), true);
            context.getChannel().sendMessage(eb.build()).addFile(baos.toByteArray(), asHex(color) + ".png").queue();
            baos.close();
        } catch (IOException e) {
            context.send(context.getTranslated("kolor.failed"));
        }
        return true;
    }

    private String getCssName(Color color) {
        try {
            InputStream st = getClass().getResourceAsStream("/colors.json");
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(st, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                int[] rgbTmp = new int[3];
                JsonArray kolorek = entry.getValue().getAsJsonArray();
                rgbTmp[0] = kolorek.get(0).getAsInt();
                rgbTmp[1] = kolorek.get(1).getAsInt();
                rgbTmp[2] = kolorek.get(2).getAsInt();
                Color kolorTmp = new Color(rgbTmp[0], rgbTmp[1], rgbTmp[2]);
                if (kolorTmp.getRGB() == color.getRGB()) {
                    return entry.getKey();
                }
            }
        } catch (Exception e) {
            logger.warn("Błąd w pozyskiwaniu kolorów CSS!", e);
        }
        return null;
    }

    private Color getCssColor(CommandContext context) {
        try {
            InputStream st = getClass().getResourceAsStream("/colors.json");
            JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(st, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                int[] rgba = new int[4];
                JsonArray kolorek = entry.getValue().getAsJsonArray();
                rgba[0] = kolorek.get(0).getAsInt();
                rgba[1] = kolorek.get(1).getAsInt();
                rgba[2] = kolorek.get(2).getAsInt();
                rgba[3] = kolorek.get(3).getAsInt();
                Color kolorTmp = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
                if (entry.getKey().equalsIgnoreCase(((String) context.getArgs()[0]))) return kolorTmp;
            }
        } catch (Exception e) {
            logger.warn("Błąd w pozyskiwaniu kolorów CSS!", e);
        }
        return null;
    }

    private String asHex(Color color) {
        String hexColor = Integer.toHexString(color.getRGB() & 0xffffff);
        if (hexColor.length() < 6) {
            hexColor = "000000".substring(0, 6 - hexColor.length()) + hexColor;
        }
        return hexColor;
    }
}
