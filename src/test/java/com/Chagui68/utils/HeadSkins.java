package com.Chagui68.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * The skins of a model sculpted with {@link HeadSculpt}, as files: the PNGs MineSkin takes
 * ({@code website/models/stand-skins/heads.py} uploads them), and for the viewer a skin nobody has
 * uploaded yet as a data URL, so a change to the art is seen before it is uploaded.
 */
public final class HeadSkins {

    public static final Path DIR = Path.of("target", "head-skins");

    private HeadSkins() {
    }

    /** Writes every skin as {@code <name>.png} and takes away the ones no longer worn. */
    public static void write(Map<String, int[]> skins) throws IOException {
        Files.createDirectories(DIR);
        List<Path> old;
        try (var files = Files.list(DIR)) {
            old = files.filter(p -> p.toString().endsWith(".png")).toList();
        }
        for (Path file : old) {
            String name = file.getFileName().toString();
            if (!skins.containsKey(name.substring(0, name.length() - 4))) Files.delete(file);
        }
        for (Map.Entry<String, int[]> skin : skins.entrySet()) {
            // A skin is named after its pixels, so a file that is there is already right.
            if (Files.exists(DIR.resolve(skin.getKey() + ".png"))) continue;
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 64, 64, skin.getValue(), 0, 64);
            ImageIO.write(image, "png", DIR.resolve(skin.getKey() + ".png").toFile());
        }
    }

    /** A piece's skin that is {@link HeadSculpt#LOCAL} as the file {@link #write} left for it, inline. */
    public static String dataUrl(String skin) {
        try {
            byte[] png = Files.readAllBytes(DIR.resolve(skin.substring(HeadSculpt.LOCAL.length()) + ".png"));
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
