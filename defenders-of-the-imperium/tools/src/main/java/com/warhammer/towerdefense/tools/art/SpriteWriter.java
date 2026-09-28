package com.warhammer.towerdefense.tools.art;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Renders drawing routines into PNG sprite sheets / images and registers them in a {@link Manifest}.
 * Art is authored in "art units" (1 art unit = 1 world unit) and rasterised at {@link #ART_SCALE} pixels per unit.
 */
public final class SpriteWriter {
    public interface Frame {
        void draw(Painter p, Pose pose);
    }

    public interface Still {
        void draw(Painter p);
    }

    public static final float ART_SCALE = 2f;
    public static final float RUNTIME_SCALE = 1f / ART_SCALE;

    /** Standard unit animation layout: one row per animation. */
    public static final List<Manifest.Anim> UNIT_ANIMS = Arrays.asList(
            new Manifest.Anim("idle", 0, 2, 2.5f, true),
            new Manifest.Anim("walk", 1, 4, 8f, true),
            new Manifest.Anim("attack", 2, 4, 10f, true),
            new Manifest.Anim("death", 3, 4, 7f, false));

    private final Path assets;
    private final Manifest manifest;
    private int written;

    public SpriteWriter(Path assets, Manifest manifest) {
        this.assets = assets;
        this.manifest = manifest;
    }

    /** A 4x4 unit sheet (idle, walk, attack, death). Feet sit {@code footPx} above the bottom of each cell. */
    public void unitSheet(String name, String dir, int cellW, int cellH, int footPx, Frame frame) {
        sheet(name, dir, cellW, cellH, footPx, frame, UNIT_ANIMS);
    }

    public void sheet(String name, String dir, int cellW, int cellH, int footPx, Frame frame, List<Manifest.Anim> anims) {
        int cols = 0;
        int rows = 0;
        for (Manifest.Anim a : anims) {
            cols = Math.max(cols, a.frames);
            rows = Math.max(rows, a.row + 1);
        }
        BufferedImage img = Painter.image(cols * cellW, rows * cellH);
        Graphics2D g = img.createGraphics();
        for (Manifest.Anim a : anims) {
            Pose.Kind kind = Pose.Kind.valueOf(a.name.toUpperCase());
            for (int f = 0; f < a.frames; f++) {
                Painter p = new Painter(g);
                p.push();
                g.setClip(f * cellW, a.row * cellH, cellW, cellH);
                p.translate(f * cellW + cellW / 2.0, a.row * cellH + cellH - footPx);
                p.scale(ART_SCALE, ART_SCALE);
                frame.draw(p, Pose.of(kind, f, a.frames));
                p.pop();
                g.setClip(null);
            }
        }
        g.dispose();
        String file = dir + "/" + name + ".png";
        writePng(img, assets.resolve(file));
        manifest.sheet(name, file, cellW, cellH, cellW / 2, footPx, RUNTIME_SCALE, anims);
    }

    /**
     * A single image. The art origin (0,0) is placed {@code originX} px from the left and {@code originY} px from
     * the bottom, which is also the anchor used by the game when drawing it.
     */
    public void image(String name, String dir, int w, int h, int originX, int originY, Still still) {
        image(name, dir, w, h, originX, originY, ART_SCALE, still);
    }

    public void image(String name, String dir, int w, int h, int originX, int originY, float artScale, Still still) {
        BufferedImage img = render(w, h, originX, originY, artScale, still);
        String file = dir + "/" + name + ".png";
        writePng(img, assets.resolve(file));
        manifest.image(name, file, originX, originY, 1f / artScale);
    }

    /** Nine-patch image drawn in pixel units (scale 1). */
    public void ninePatch(String name, String dir, int w, int h, int left, int right, int top, int bottom, Still still) {
        BufferedImage img = render(w, h, 0, h, 1f, still);
        String file = dir + "/" + name + ".png";
        writePng(img, assets.resolve(file));
        manifest.ninePatch(name, file, left, right, top, bottom);
    }

    public static BufferedImage render(int w, int h, int originX, int originY, float artScale, Still still) {
        BufferedImage img = Painter.image(w, h);
        Graphics2D g = img.createGraphics();
        Painter p = new Painter(g);
        p.translate(originX, h - originY);
        p.scale(artScale, artScale);
        still.draw(p);
        g.dispose();
        return img;
    }

    public void writePng(BufferedImage img, Path file) {
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(img, "png", file.toFile());
            written++;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    public static void writeJpg(BufferedImage rgbImage, Path file, float quality) {
        try {
            Files.createDirectories(file.getParent());
            BufferedImage rgb = new BufferedImage(rgbImage.getWidth(), rgbImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(rgbImage, 0, 0, null);
            g.dispose();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            Files.deleteIfExists(file);
            try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    public int written() {
        return written;
    }
}
