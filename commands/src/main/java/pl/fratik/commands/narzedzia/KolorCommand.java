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

package pl.fratik.commands.narzedzia;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.CommonUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KolorCommand extends NewCommand {
    private final Logger logger;

    public KolorCommand() {
        logger = LoggerFactory.getLogger(getClass());
        name = "kolor";
        usage = "<kolor:string>";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Color color = null;
        String arg = context.getArguments().get("kolor").getAsString();
        //#region kolory
        try {
            color = Color.decode(arg);
        } catch (Exception ignored) {
            try {
                color = (Color) Color.class.getField(arg.toUpperCase()).get(null);
                if (color == null) color = getCssColor(arg);
                if (color == null) throw new Exception();
            } catch (Exception ignored2) {
                try {
                    String[] rgbTmp = arg.split(",");
                    int[] rgb = new int[3];
                    for (int i = 0; i < rgbTmp.length; i++) {
                        rgb[i] = Integer.parseInt(rgbTmp[i].trim());
                    }
                    color = new Color(rgb[0], rgb[1], rgb[2]);
                } catch (Exception ignored3) {
                    try {
                        String[] hsbTmp = arg.split(",");
                        int[] hsb = new int[3];
                        for (int i = 0; i < hsbTmp.length; i++) {
                            hsb[i] = Integer.parseInt(hsbTmp[i].trim());
                        }
                        color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
                    } catch (Exception ignored4) {
                        if (arg.equalsIgnoreCase("blurple")) color = new Color(114, 137, 218);
                    }
                }
            }
        }
        //#endregion kolory
        if (color == null) {
            context.replyEphemeral(context.getTranslated("kolor.unknown.color"));
            return;
        }
        InteractionHook hook = context.defer(false);
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
            eb.setImage("attachment://" + CommonUtil.asHex(color) + ".png");
            eb.setTitle(context.getTranslated("kolor.embed.header"));
            String[] rgb = new String[3];
            rgb[0] = String.valueOf(color.getRed());
            rgb[1] = String.valueOf(color.getGreen());
            rgb[2] = String.valueOf(color.getBlue());
            eb.addField("RGB", String.join(", ", rgb), true);
            eb.addField("Hex", "#" + CommonUtil.asHex(color), true);
            if (getCssName(color) != null) eb.addField("CSS", getCssName(color), true);
            hook.editOriginalEmbeds(eb.build()).setFiles(FileUpload.fromData(baos.toByteArray(), CommonUtil.asHex(color) + ".png")).queue();
            baos.close();
        } catch (IOException e) {
            context.sendMessage(context.getTranslated("kolor.failed"));
        }
    }

    private String getCssName(Color color) {
        try {
            InputStream st = getClass().getResourceAsStream("/colors.json");
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(st, StandardCharsets.UTF_8))
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

    private Color getCssColor(String arg) {
        try {
            InputStream st = getClass().getResourceAsStream("/colors.json");
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(st, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                int[] rgba = new int[4];
                JsonArray kolorek = entry.getValue().getAsJsonArray();
                rgba[0] = kolorek.get(0).getAsInt();
                rgba[1] = kolorek.get(1).getAsInt();
                rgba[2] = kolorek.get(2).getAsInt();
                rgba[3] = kolorek.get(3).getAsInt();
                Color kolorTmp = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
                if (entry.getKey().equalsIgnoreCase(arg)) return kolorTmp;
            }
        } catch (Exception e) {
            logger.warn("Błąd w pozyskiwaniu kolorów CSS!", e);
        }
        return null;
    }

}
