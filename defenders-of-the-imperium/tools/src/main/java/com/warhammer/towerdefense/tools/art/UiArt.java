package com.warhammer.towerdefense.tools.art;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.geom.Path2D;

/**
 * Imperial military interface: steel/bronze panels, parchment tactical panels, hazard stripes, buttons, icons,
 * portraits and planet thumbnails. Icons are drawn in a 32x32 art-unit box centred on the origin.
 */
public final class UiArt {
    private static final Color BRONZE = new Color(176, 124, 58);
    private static final Color BRONZE_DARK = new Color(96, 62, 28);
    private static final Color PANEL = new Color(28, 32, 42);
    private static final Color PANEL_LIGHT = new Color(52, 60, 76);
    private static final Color PARCHMENT = new Color(214, 196, 150);

    private UiArt() {
    }

    // ------------------------------------------------------------------ nine-patches (pixel units, y down from top-left)

    public static void panel(Painter p, int w, int h) {
        p.g.setPaint(new GradientPaint(0, 0, PANEL_LIGHT, 0, h, PANEL));
        p.g.fillRoundRect(2, 2, w - 4, h - 4, 12, 12);
        p.g.setStroke(new BasicStroke(3f));
        p.g.setColor(BRONZE_DARK);
        p.g.drawRoundRect(2, 2, w - 5, h - 5, 12, 12);
        p.g.setStroke(new BasicStroke(1.5f));
        p.g.setColor(BRONZE);
        p.g.drawRoundRect(5, 5, w - 11, h - 11, 8, 8);
        rivet(p, 9, 9);
        rivet(p, w - 9, 9);
        rivet(p, 9, h - 9);
        rivet(p, w - 9, h - 9);
    }

    public static void parchment(Painter p, int w, int h) {
        p.g.setPaint(new GradientPaint(0, 0, Pal.lighter(PARCHMENT, 0.1f), 0, h, Pal.darker(PARCHMENT, 0.12f)));
        p.g.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
        p.g.setStroke(new BasicStroke(3f));
        p.g.setColor(new Color(92, 64, 36));
        p.g.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
        p.g.setStroke(new BasicStroke(1f));
        p.g.setColor(new Color(150, 110, 60));
        p.g.drawRoundRect(6, 6, w - 13, h - 13, 5, 5);
    }

    public static void button(Painter p, int w, int h, Color top, Color bottom, Color border) {
        p.g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        p.g.fillRoundRect(1, 1, w - 2, h - 2, 10, 10);
        p.g.setStroke(new BasicStroke(2f));
        p.g.setColor(border);
        p.g.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
        p.g.setColor(Pal.alpha(Color.WHITE, 0.18f));
        p.g.fillRoundRect(4, 3, w - 8, h / 3, 6, 6);
    }

    public static void slot(Painter p, int w, int h) {
        p.g.setColor(new Color(16, 18, 24, 230));
        p.g.fillRoundRect(2, 2, w - 4, h - 4, 10, 10);
        p.g.setStroke(new BasicStroke(3f));
        p.g.setColor(BRONZE_DARK);
        p.g.drawRoundRect(2, 2, w - 5, h - 5, 10, 10);
        p.g.setStroke(new BasicStroke(1.2f));
        p.g.setColor(BRONZE);
        p.g.drawRoundRect(5, 5, w - 11, h - 11, 7, 7);
    }

    public static void bar(Painter p, int w, int h) {
        p.g.setColor(new Color(8, 8, 12, 230));
        p.g.fillRoundRect(0, 0, w, h, 4, 4);
        p.g.setColor(BRONZE_DARK);
        p.g.setStroke(new BasicStroke(1.5f));
        p.g.drawRoundRect(0, 0, w - 1, h - 1, 4, 4);
    }

    public static void hazard(Painter p, int size) {
        p.g.setColor(Pal.HAZARD);
        p.g.fillRect(0, 0, size, size);
        p.g.setColor(new Color(20, 20, 20));
        for (int i = -size; i < size * 2; i += size / 2) {
            p.g.fill(Painter.poly(i, 0, i + size / 4.0, 0, i - size * 0.75, size, i - size, size));
        }
    }

    private static void rivet(Painter p, double x, double y) {
        p.g.setColor(BRONZE_DARK);
        p.g.fill(Painter.circle(x, y, 2.6));
        p.g.setColor(Pal.lighter(BRONZE, 0.3f));
        p.g.fill(Painter.circle(x - 0.6, y - 0.6, 1.3));
    }

    // ------------------------------------------------------------------ icons (32x32 art box)

    public static void medallion(Painter p, Color c) {
        p.volume(Painter.circle(0, 0, 14.5), Pal.darker(c, 0.55f));
        p.stroke(Painter.circle(0, 0, 13.5), BRONZE, 1.4);
    }

    public static void requisition(Painter p) {
        p.volume(Painter.circle(0, 0, 13), Pal.GOLD);
        p.stroke(Painter.circle(0, 0, 10.5), Pal.GOLD_DARK, 1.2);
        Emblems.aquila(p, 0, 0, 17, Pal.lighter(Pal.GOLD, 0.25f));
    }

    public static void skullIcon(Painter p) {
        Emblems.skull(p, 0, 0, 11, Pal.IVORY);
    }

    public static void fortressIcon(Painter p) {
        p.volume(Painter.poly(-12, 12, -12, -6, -8, -6, -8, -10, -4, -10, -4, -6, 4, -6, 4, -10, 8, -10, 8, -6,
                12, -6, 12, 12), Pal.STONE);
        Path2D gate = new Path2D.Double();
        gate.moveTo(-4, 12);
        gate.lineTo(-4, 4);
        gate.quadTo(0, -1, 4, 4);
        gate.lineTo(4, 12);
        gate.closePath();
        p.volume(gate, Pal.darker(Pal.LEATHER, 0.3f));
        p.fill(Painter.poly(-1, -14, 1, -14, 1, -8, -1, -8), Pal.GUNMETAL);
        p.volume(Painter.poly(1, -14, 8, -12, 1, -10), Pal.UM_BLUE);
    }

    public static void pause(Painter p) {
        p.volume(Painter.round(-8, -10, 5.5, 20, 1.5), Pal.IVORY);
        p.volume(Painter.round(2.5, -10, 5.5, 20, 1.5), Pal.IVORY);
    }

    public static void play(Painter p) {
        p.volume(Painter.poly(-7, -10, 10, 0, -7, 10), Pal.IVORY);
    }

    public static void speed(Painter p, int arrows) {
        for (int i = 0; i < arrows; i++) {
            double x = -8 + i * 9 - (arrows - 1) * 2;
            p.volume(Painter.poly(x, -9, x + 9, 0, x, 9), Pal.IVORY);
        }
    }

    public static void menu(Painter p) {
        for (int i = 0; i < 3; i++) {
            p.volume(Painter.round(-10, -9 + i * 7, 20, 3.6, 1.5), Pal.IVORY);
        }
    }

    public static void gear(Painter p) {
        Emblems.cog(p, 0, 0, 12, Pal.STEEL, 9);
    }

    public static void star(Painter p, boolean filled) {
        if (filled) {
            p.glow(0, 0, 16, Pal.GOLD, 0.35f);
            p.volume(Emblems.star(0, 0, 13, 5.6, 5), Pal.GOLD);
        } else {
            p.fill(Emblems.star(0, 0, 13, 5.6, 5), Pal.alpha(Color.BLACK, 0.45f));
            p.stroke(Emblems.star(0, 0, 13, 5.6, 5), Pal.alpha(Pal.GOLD_DARK, 0.9f), 1.4);
        }
    }

    public static void lock(Painter p) {
        p.stroke(new java.awt.geom.Arc2D.Double(-6, -13, 12, 14, 0, 180, java.awt.geom.Arc2D.OPEN), Pal.STEEL, 2.8);
        p.metal(Painter.round(-9, -5, 18, 15, 3), Pal.GOLD);
        p.fill(Painter.circle(0, 1, 2), Pal.OUTLINE);
        p.fill(Painter.rect(-0.8, 1, 1.6, 5), Pal.OUTLINE);
    }

    public static void upgrade(Painter p) {
        for (int i = 0; i < 2; i++) {
            double y = 4 - i * 8;
            p.volume(Painter.poly(-10, y + 4, 0, y - 6, 10, y + 4, 10, y + 8, 0, y - 1, -10, y + 8), Pal.GOLD);
        }
    }

    public static void sell(Painter p) {
        p.volume(Painter.circle(-3, 3, 8), Pal.GOLD);
        p.volume(Painter.circle(3, -2, 8), Pal.lighter(Pal.GOLD, 0.1f));
        p.volume(Painter.poly(-2, -6, 2, -6, 2, 0, 5, 0, 0, 6, -5, 0, -2, 0), Pal.IMPERIAL_RED);
    }

    public static void repair(Painter p) {
        p.push();
        p.rotate(-45, 0, 0);
        p.metal(Painter.round(-2.5, -4, 5, 17, 1.5), Pal.STEEL);
        Path2D head = Painter.poly(-7, -13, -2.5, -13, -2.5, -8, 2.5, -8, 2.5, -13, 7, -13, 7, -6, 3, -3, -3, -3, -7, -6);
        p.metal(head, Pal.STEEL);
        p.pop();
    }

    public static void rally(Painter p) {
        EffectArt.rallyFlag(p);
    }

    public static void target(Painter p) {
        p.stroke(Painter.circle(0, 0, 10), Pal.IMPERIAL_RED, 2.2);
        p.stroke(Painter.circle(0, 0, 4.5), Pal.IMPERIAL_RED, 1.6);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            p.line(Math.cos(a) * 6, Math.sin(a) * 6, Math.cos(a) * 14, Math.sin(a) * 14, Pal.IVORY, 1.8);
        }
    }

    public static void damage(Painter p) {
        p.push();
        p.rotate(-35, 0, 0);
        p.metal(Painter.round(-3, -4, 6, 16, 2), Pal.BRASS);
        p.volume(Painter.poly(-3, -4, 0, -13, 3, -4), Pal.STEEL);
        p.pop();
    }

    public static void range(Painter p) {
        p.stroke(Painter.circle(0, 0, 12), Pal.alpha(Pal.LENS_GREEN, 0.9f), 1.6);
        p.stroke(Painter.circle(0, 0, 7), Pal.alpha(Pal.LENS_GREEN, 0.7f), 1.2);
        p.line(0, 0, 9, -8, Pal.LENS_GREEN, 1.8);
        p.fill(Painter.circle(0, 0, 2), Pal.LENS_GREEN);
    }

    public static void firerate(Painter p) {
        p.stroke(Painter.circle(0, 0, 11), Pal.IVORY, 2);
        p.line(0, 0, 0, -8, Pal.IVORY, 2);
        p.line(0, 0, 6, 3, Pal.IVORY, 2);
        p.fill(Painter.circle(0, 0, 1.6), Pal.GOLD);
    }

    public static void heart(Painter p) {
        Path2D h = new Path2D.Double();
        h.moveTo(0, 11);
        h.curveTo(-16, 0, -10, -14, 0, -5);
        h.curveTo(10, -14, 16, 0, 0, 11);
        p.volume(h, Pal.IMPERIAL_RED);
    }

    public static void armor(Painter p) {
        Path2D s = new Path2D.Double();
        s.moveTo(-10, -10);
        s.lineTo(10, -10);
        s.lineTo(9, 3);
        s.quadTo(0, 13, 0, 13);
        s.quadTo(0, 13, -9, 3);
        s.closePath();
        p.volume(s, Pal.UM_BLUE);
        Emblems.omega(p, 0, -1, 4, Pal.IVORY);
    }

    public static void close(Painter p) {
        p.line(-8, -8, 8, 8, Pal.IVORY, 3.4);
        p.line(-8, 8, 8, -8, Pal.IVORY, 3.4);
    }

    public static void elite(Painter p) {
        p.volume(Painter.poly(-10, 6, -10, -6, -5, 0, 0, -10, 5, 0, 10, -6, 10, 6), Pal.GOLD);
    }

    public static void nextWave(Painter p) {
        Emblems.skull(p, -3, 0, 9, Pal.IVORY);
        p.volume(Painter.poly(7, -6, 14, 0, 7, 6), Pal.IMPERIAL_RED);
    }

    // ------------------------------------------------------------------ status icons (small, 10x10 art box)

    public static void status(Painter p, String kind) {
        switch (kind) {
            case "slow":
                p.volume(Painter.circle(0, 0, 4.6), new Color(60, 120, 220));
                p.volume(Painter.poly(-3, -2, 0, 1.5, 3, -2, 3, 0, 0, 3.5, -3, 0), Pal.IVORY);
                break;
            case "burn":
                Path2D f = new Path2D.Double();
                f.moveTo(0, -5);
                f.quadTo(4.5, -1, 3, 3);
                f.quadTo(0, 5.5, -3, 3);
                f.quadTo(-4.5, -1, 0, -5);
                p.volume(f, Pal.FIRE);
                p.fill(Painter.ellipse(0, 1.5, 1.6, 2.2), Pal.HAZARD);
                break;
            case "bleed":
                p.push();
                p.scale(0.9, 0.9);
                EffectArt.drop(p, 3.6);
                p.pop();
                p.fill(Painter.circle(0, 0.5, 2.8), Pal.IMPERIAL_RED);
                break;
            case "armorbreak":
                p.volume(Painter.poly(-4, -4.5, 4, -4.5, 3.5, 1.5, 0, 5, -3.5, 1.5), Pal.STEEL);
                p.line(-1, -4.5, 1, -1, Pal.OUTLINE, 0.9);
                p.line(1, -1, -1, 2.5, Pal.OUTLINE, 0.9);
                break;
            case "stun":
                p.volume(Emblems.star(0, 0, 5, 2, 5), Pal.HAZARD);
                break;
            case "marked":
                p.stroke(Painter.circle(0, 0, 3.8), Pal.IMPERIAL_RED, 1.2);
                p.line(-5, 0, 5, 0, Pal.IMPERIAL_RED, 0.9);
                p.line(0, -5, 0, 5, Pal.IMPERIAL_RED, 0.9);
                break;
            case "regen":
                p.volume(Painter.circle(0, 0, 4.6), new Color(40, 150, 60));
                EffectArt.plus(p, 3);
                break;
            case "shield":
                p.volume(Painter.poly(-4, -4.5, 4, -4.5, 3.5, 1.5, 0, 5, -3.5, 1.5), Pal.WARP);
                break;
            case "synapse":
                p.glow(0, 0, 6, Pal.WARP, 0.8f);
                p.fill(Painter.circle(0, 0, 2), Pal.lighter(Pal.WARP, 0.5f));
                break;
            default:
                p.fill(Painter.circle(0, 0, 4), Color.GRAY);
        }
    }

    // ------------------------------------------------------------------ planets

    public static void planet(Painter p, Color land, Color sea, Color atmosphere, long seed) {
        java.util.Random rnd = new java.util.Random(seed);
        p.glow(0, 0, 64, atmosphere, 0.45f);
        p.fill(Painter.circle(0, 0, 50), sea);
        java.awt.Shape oldClip = p.g.getClip();
        p.g.clip(Painter.circle(0, 0, 50));
        for (int i = 0; i < 16; i++) {
            double x = rnd.nextGaussian() * 26;
            double y = rnd.nextGaussian() * 26;
            p.fill(Painter.blob(x - 14, y, x - 6, y - 9 - rnd.nextDouble() * 6, x + 10, y - 7, x + 14, y + 3,
                    x + 3, y + 10, x - 9, y + 8), land);
        }
        p.fill(Painter.circle(18, 18, 58), Pal.alpha(Color.BLACK, 0.45f));
        p.fill(Painter.circle(-16, -18, 30), Pal.alpha(Color.WHITE, 0.12f));
        p.g.setClip(oldClip);
        p.stroke(Painter.circle(0, 0, 50), Pal.alpha(Pal.lighter(atmosphere, 0.3f), 0.8f), 1.6);
    }
}
