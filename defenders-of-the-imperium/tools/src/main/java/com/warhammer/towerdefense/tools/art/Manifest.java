package com.warhammer.towerdefense.tools.art;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects generated sprite sheets and images and writes the JSON manifests the game reads at startup
 * ({@code assets/sprites/sprites.json} and {@code assets/ui/ui.json}).
 */
public final class Manifest {
    public static final class Anim {
        final String name;
        final int row;
        final int frames;
        final float fps;
        final boolean loop;

        public Anim(String name, int row, int frames, float fps, boolean loop) {
            this.name = name;
            this.row = row;
            this.frames = frames;
            this.fps = fps;
            this.loop = loop;
        }
    }

    private final List<String> sheets = new ArrayList<>();
    private final List<String> images = new ArrayList<>();
    private final List<String> patches = new ArrayList<>();

    public void sheet(String name, String file, int frameW, int frameH, int originX, int originY, float scale,
                      List<Anim> anims) {
        StringBuilder a = new StringBuilder();
        for (int i = 0; i < anims.size(); i++) {
            Anim an = anims.get(i);
            a.append(String.format(Locale.ROOT, "\"%s\": {\"row\": %d, \"frames\": %d, \"fps\": %.1f, \"loop\": %s}",
                    an.name, an.row, an.frames, an.fps, an.loop));
            if (i < anims.size() - 1) {
                a.append(", ");
            }
        }
        sheets.add(String.format(Locale.ROOT, "    {\"name\": \"%s\", \"file\": \"%s\", \"frameWidth\": %d, \"frameHeight\": %d, "
                + "\"originX\": %d, \"originY\": %d, \"scale\": %.3f, \"animations\": {%s}}",
                name, file, frameW, frameH, originX, originY, scale, a));
    }

    public void image(String name, String file, int originX, int originY, float scale) {
        images.add(String.format(Locale.ROOT, "    {\"name\": \"%s\", \"file\": \"%s\", \"originX\": %d, \"originY\": %d, \"scale\": %.3f}",
                name, file, originX, originY, scale));
    }

    public void ninePatch(String name, String file, int left, int right, int top, int bottom) {
        patches.add(String.format(Locale.ROOT, "    {\"name\": \"%s\", \"file\": \"%s\", \"left\": %d, \"right\": %d, \"top\": %d, \"bottom\": %d}",
                name, file, left, right, top, bottom));
    }

    public void write(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"generator\": \"tools/AssetGenerator - procedural placeholder art, replace PNGs freely\",\n");
        sb.append("  \"sheets\": [\n").append(String.join(",\n", sheets)).append("\n  ],\n");
        sb.append("  \"images\": [\n").append(String.join(",\n", images)).append("\n  ],\n");
        sb.append("  \"ninePatches\": [\n").append(String.join(",\n", patches)).append("\n  ]\n}\n");
        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    public int count() {
        return sheets.size() + images.size() + patches.size();
    }
}
