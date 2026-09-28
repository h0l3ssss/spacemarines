package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.Random;

/** Projectiles, particle textures, decals and battlefield props (drop pod, gunship, barrier...). */
public final class EffectArt {
    private EffectArt() {
    }

    public static void white(Painter p) {
        p.fill(Painter.rect(-2, -2, 4, 4), Color.WHITE);
    }

    public static void tracer(Painter p, double length, double width, Color core, Color trail) {
        p.glow(0, 0, width * 2.2, trail, 0.35f);
        Path2D body = Painter.poly(-length, 0, -length * 0.2, -width / 2, length * 0.35, -width * 0.35,
                length * 0.5, 0, length * 0.35, width * 0.35, -length * 0.2, width / 2);
        p.fill(body, Pal.alpha(trail, 0.85f));
        p.fill(Painter.ellipse(length * 0.2, 0, length * 0.3, width * 0.32), core);
    }

    public static void orb(Painter p, double r, Color c) {
        p.glow(0, 0, r * 2, c, 0.6f);
        p.fill(Painter.circle(0, 0, r), Pal.lighter(c, 0.2f));
        p.fill(Painter.circle(-r * 0.2, -r * 0.2, r * 0.55), Color.WHITE);
    }

    public static void missile(Painter p, double length) {
        p.glow(-length * 0.55, 0, 4, Pal.FIRE, 0.7f);
        p.fill(Painter.poly(-length * 0.5, -1.2, -length * 0.8, 0, -length * 0.5, 1.2), Pal.FIRE);
        p.metal(Painter.round(-length * 0.5, -1.6, length * 0.8, 3.2, 1.2), Pal.STEEL);
        p.volume(Painter.poly(length * 0.3, -1.6, length * 0.55, 0, length * 0.3, 1.6), Pal.IMPERIAL_RED);
        p.fill(Painter.poly(-length * 0.45, -1.6, -length * 0.3, -3, -length * 0.25, -1.6), Pal.GUNMETAL);
        p.fill(Painter.poly(-length * 0.45, 1.6, -length * 0.3, 3, -length * 0.25, 1.6), Pal.GUNMETAL);
    }

    public static void beam(Painter p, double length, double width, Color c) {
        p.fill(Painter.round(0, -width / 2, length, width, width / 2), Pal.alpha(c, 0.55f));
        p.fill(Painter.round(0, -width * 0.28, length, width * 0.56, width * 0.28), Pal.alpha(Pal.lighter(c, 0.3f), 0.9f));
        p.fill(Painter.round(0, -width * 0.1, length, width * 0.2, width * 0.1), Color.WHITE);
    }

    public static void soft(Painter p, double r) {
        p.glow(0, 0, r, Color.WHITE, 1f);
    }

    public static void spark(Painter p, double length, double width) {
        p.fill(Painter.ellipse(0, 0, length / 2, width / 2), Pal.alpha(Color.WHITE, 0.6f));
        p.fill(Painter.ellipse(0, 0, length / 2.6, width / 4), Color.WHITE);
    }

    public static void smoke(Painter p, double r, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < 9; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = rnd.nextDouble() * r * 0.45;
            p.glow(Math.cos(a) * d, Math.sin(a) * d, r * (0.45 + rnd.nextDouble() * 0.25), Color.WHITE, 0.45f);
        }
    }

    public static void flash(Painter p, double r) {
        p.glow(0, 0, r, Color.WHITE, 0.8f);
        p.fill(Emblems.star(0, 0, r * 0.95, r * 0.22, 8), Pal.alpha(Color.WHITE, 0.9f));
    }

    public static void ring(Painter p, double r, double width) {
        p.stroke(Painter.circle(0, 0, r - width), Pal.alpha(Color.WHITE, 0.35f), width * 1.6);
        p.stroke(Painter.circle(0, 0, r - width), Color.WHITE, width * 0.6);
    }

    public static void debris(Painter p, double r) {
        p.volume(Painter.poly(-r, -r * 0.3, -r * 0.2, -r, r, -r * 0.4, r * 0.6, r * 0.8, -r * 0.5, r), Color.LIGHT_GRAY);
    }

    public static void drop(Painter p, double r) {
        Path2D d = new Path2D.Double();
        d.moveTo(0, -r * 1.3);
        d.quadTo(r, 0, 0, r);
        d.quadTo(-r, 0, 0, -r * 1.3);
        p.fill(d, Color.WHITE);
    }

    public static void plus(Painter p, double r) {
        p.fill(Painter.round(-r * 0.3, -r, r * 0.6, r * 2, r * 0.2), Color.WHITE);
        p.fill(Painter.round(-r, -r * 0.3, r * 2, r * 0.6, r * 0.2), Color.WHITE);
    }

    public static void splat(Painter p, double r, long seed) {
        Random rnd = new Random(seed);
        p.fill(Painter.ellipse(0, 0, r * 0.7, r * 0.42), Color.WHITE);
        for (int i = 0; i < 9; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = r * (0.5 + rnd.nextDouble() * 0.5);
            double s = r * (0.08 + rnd.nextDouble() * 0.15);
            p.fill(Painter.ellipse(Math.cos(a) * d, Math.sin(a) * d * 0.6, s, s * 0.7), Color.WHITE);
        }
    }

    public static void crater(Painter p, double r) {
        p.fill(Painter.ellipse(0, 0, r, r * 0.5), Pal.alpha(new Color(40, 30, 22), 0.75f));
        p.fill(Painter.ellipse(0, 1, r * 0.72, r * 0.32), Pal.alpha(new Color(20, 14, 10), 0.8f));
        p.stroke(Painter.ellipse(0, -1, r * 0.95, r * 0.47), Pal.alpha(new Color(120, 100, 70), 0.6f), 1.5);
    }

    public static void scorch(Painter p, double r) {
        p.glow(0, 0, r, new Color(18, 12, 8), 0.85f);
    }

    public static void shadow(Painter p) {
        p.shadow(0, 0, 15, 6, 0.5f);
    }

    public static void rangeFill(Painter p, double r) {
        p.fill(Painter.circle(0, 0, r), Pal.alpha(Color.WHITE, 0.16f));
    }

    public static void reticle(Painter p, double r) {
        p.stroke(Painter.circle(0, 0, r - 2), Pal.alpha(Color.WHITE, 0.9f), 2);
        p.stroke(Painter.circle(0, 0, r * 0.62), Pal.alpha(Color.WHITE, 0.55f), 1.2);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            p.line(Math.cos(a) * r * 0.45, Math.sin(a) * r * 0.45, Math.cos(a) * r * 0.9, Math.sin(a) * r * 0.9,
                    Color.WHITE, 2);
        }
        p.fill(Painter.circle(0, 0, 2), Color.WHITE);
    }

    public static void rallyFlag(Painter p) {
        p.metal(Painter.rect(-0.6, -20, 1.2, 20), Pal.GUNMETAL);
        p.volume(Painter.poly(0.6, -20, 10, -17, 0.6, -13), Pal.UM_BLUE);
        p.volume(Painter.circle(0, -20.5, 1.2), Pal.GOLD, 0.6f);
    }

    public static void dropPod(Painter p, boolean open) {
        if (!open) {
            p.glow(0, 6, 12, Pal.FIRE, 0.7f);
            p.fill(Painter.poly(-5, 2, 0, 16, 5, 2), Pal.alpha(Pal.FIRE, 0.9f));
            p.metal(Painter.poly(-10, 0, -8, -30, 0, -40, 8, -30, 10, 0), Pal.UM_BLUE);
            p.metal(Painter.poly(-13, 0, -10, -10, -8, 0), Pal.GUNMETAL);
            p.metal(Painter.poly(13, 0, 10, -10, 8, 0), Pal.GUNMETAL);
            Emblems.omega(p, 0, -18, 3, Pal.IVORY);
            return;
        }
        p.shadow(0, 2, 22, 7, 0.4f);
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(-170 + i * 40);
            double x = Math.cos(a) * 14;
            double y = Math.sin(a) * 6 - 4;
            p.metal(Painter.poly(x - 5, y, x + 5, y, x + Math.cos(a) * 12, y + Math.sin(a) * 4 - 2), Pal.UM_BLUE_DARK);
        }
        p.metal(Painter.round(-8, -26, 16, 26, 4), Pal.GUNMETAL);
        p.volume(Painter.poly(-7, -26, 0, -34, 7, -26), Pal.UM_BLUE);
        p.glow(0, -12, 8, Pal.FIRE, 0.4f);
    }

    public static void thunderhawk(Painter p) {
        p.metal(Painter.poly(-70, 6, -40, -12, 40, -14, 70, -2, 40, 12, -40, 16), Pal.UM_BLUE);
        p.metal(Painter.poly(-20, -12, 10, -46, 30, -46, 20, -12), Pal.UM_BLUE_DARK);
        p.metal(Painter.poly(-20, 14, 10, 44, 30, 44, 20, 14), Pal.UM_BLUE_DARK);
        p.metal(Painter.poly(-66, 4, -80, -14, -70, -14, -56, -2), Pal.UM_BLUE_DARK);
        p.volume(Painter.round(38, -8, 22, 12, 5), Pal.lighter(Pal.PLASMA, 0.1f));
        p.metal(Painter.round(55, -4, 22, 3, 1), Pal.GUNMETAL);
        p.metal(Painter.round(55, 2, 22, 3, 1), Pal.GUNMETAL);
        Emblems.omega(p, 14, -30, 5, Pal.IVORY);
        Emblems.aquila(p, -10, 2, 26, Pal.GOLD);
        p.glow(-72, 0, 12, Pal.FIRE, 0.6f);
    }

    public static void servoSkull(Painter p) {
        p.metal(Painter.round(-6, -1, 12, 3, 1), Pal.BRASS);
        Emblems.skull(p, 0, -2, 4, Pal.IVORY);
        p.glow(1.5, -2.2, 3, Pal.LENS_RED, 0.8f);
        p.fill(Painter.circle(1.5, -2.2, 0.8), Pal.LENS_RED);
    }

    public static void barrier(Painter p) {
        p.fill(Painter.ellipse(0, -12, 20, 16), Pal.alpha(Pal.PLASMA, 0.28f));
        for (int i = -2; i <= 2; i++) {
            p.stroke(new java.awt.geom.Arc2D.Double(-20, -28 + Math.abs(i) * 2, 40, 32, 10 + i * 5, 160 - Math.abs(i) * 10,
                    java.awt.geom.Arc2D.OPEN), Pal.alpha(Pal.lighter(Pal.PLASMA, 0.4f), 0.6f), 0.8);
        }
        p.stroke(Painter.ellipse(0, -12, 20, 16), Pal.alpha(Color.WHITE, 0.8f), 1.2);
        for (int s = -1; s <= 1; s += 2) {
            p.metal(Painter.round(s * 18 - 2, -12, 4, 12, 1), Pal.GUNMETAL);
            p.glow(s * 18, -12, 4, Pal.PLASMA, 0.8f);
        }
    }

    public static void lance(Painter p) {
        p.glow(0, -60, 20, Pal.PLASMA, 0.5f);
        p.fill(Painter.round(-8, -120, 16, 120, 8), Pal.alpha(Pal.PLASMA, 0.6f));
        p.fill(Painter.round(-4, -120, 8, 120, 4), Pal.alpha(Color.WHITE, 0.95f));
    }

    public static void shieldBubble(Painter p, double r) {
        p.fill(Painter.circle(0, 0, r), Pal.alpha(Pal.WARP, 0.22f));
        p.stroke(Painter.circle(0, 0, r - 1), Pal.alpha(Pal.lighter(Pal.WARP, 0.4f), 0.85f), 1.5);
        p.fill(Painter.ellipse(-r * 0.35, -r * 0.4, r * 0.3, r * 0.18), Pal.alpha(Color.WHITE, 0.4f));
    }
}
