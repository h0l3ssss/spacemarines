package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.geom.Path2D;

/** Small heraldic shapes: chapter symbol, aquila, skull, cog, crux. Fan-made interpretations, not official logos. */
public final class Emblems {
    private Emblems() {
    }

    /** Ultramarines-style inverted omega. */
    public static void omega(Painter p, double cx, double cy, double r, Color c) {
        double w = Math.max(0.5, r * 0.55);
        Path2D u = new Path2D.Double();
        u.moveTo(cx - r * 0.95, cy - r * 0.9);
        u.quadTo(cx - r * 1.05, cy + r * 1.1, cx, cy + r * 1.0);
        u.quadTo(cx + r * 1.05, cy + r * 1.1, cx + r * 0.95, cy - r * 0.9);
        p.stroke(u, Pal.OUTLINE, w + 0.7);
        p.line(cx - r * 1.35, cy - r * 0.95, cx - r * 0.55, cy - r * 0.95, Pal.OUTLINE, w + 0.7);
        p.line(cx + r * 0.55, cy - r * 0.95, cx + r * 1.35, cy - r * 0.95, Pal.OUTLINE, w + 0.7);
        p.stroke(u, c, w);
        p.line(cx - r * 1.35, cy - r * 0.95, cx - r * 0.55, cy - r * 0.95, c, w);
        p.line(cx + r * 0.55, cy - r * 0.95, cx + r * 1.35, cy - r * 0.95, c, w);
    }

    /** Stylised double-headed eagle, spanning {@code w} wide. */
    public static void aquila(Painter p, double cx, double cy, double w, Color c) {
        double h = w * 0.42;
        Path2D left = Painter.poly(
                cx, cy - h * 0.1,
                cx - w * 0.18, cy - h * 0.55,
                cx - w * 0.5, cy - h * 0.62,
                cx - w * 0.4, cy - h * 0.28,
                cx - w * 0.48, cy - h * 0.18,
                cx - w * 0.34, cy + h * 0.05,
                cx - w * 0.38, cy + h * 0.2,
                cx - w * 0.12, cy + h * 0.25);
        Path2D right = Painter.poly(
                cx, cy - h * 0.1,
                cx + w * 0.18, cy - h * 0.55,
                cx + w * 0.5, cy - h * 0.62,
                cx + w * 0.4, cy - h * 0.28,
                cx + w * 0.48, cy - h * 0.18,
                cx + w * 0.34, cy + h * 0.05,
                cx + w * 0.38, cy + h * 0.2,
                cx + w * 0.12, cy + h * 0.25);
        p.volume(left, c, 0.7f);
        p.volume(right, c, 0.7f);
        p.volume(Painter.ellipse(cx - w * 0.07, cy - h * 0.42, w * 0.06, h * 0.16), c, 0.6f);
        p.volume(Painter.ellipse(cx + w * 0.07, cy - h * 0.42, w * 0.06, h * 0.16), c, 0.6f);
        p.volume(Painter.poly(cx - w * 0.07, cy - h * 0.1, cx + w * 0.07, cy - h * 0.1, cx + w * 0.04, cy + h * 0.55,
                cx - w * 0.04, cy + h * 0.55), c, 0.6f);
    }

    public static void skull(Painter p, double cx, double cy, double r, Color bone) {
        p.volume(Painter.blob(cx - r, cy - r * 0.2, cx - r * 0.7, cy - r, cx + r * 0.7, cy - r, cx + r, cy - r * 0.2,
                cx + r * 0.55, cy + r * 0.55, cx + r * 0.35, cy + r * 0.95, cx - r * 0.35, cy + r * 0.95,
                cx - r * 0.55, cy + r * 0.55), bone);
        p.fill(Painter.ellipse(cx - r * 0.38, cy, r * 0.26, r * 0.3), Pal.OUTLINE);
        p.fill(Painter.ellipse(cx + r * 0.38, cy, r * 0.26, r * 0.3), Pal.OUTLINE);
        p.fill(Painter.poly(cx, cy + r * 0.3, cx - r * 0.12, cy + r * 0.5, cx + r * 0.12, cy + r * 0.5), Pal.OUTLINE);
    }

    public static void cog(Painter p, double cx, double cy, double r, Color c, int teeth) {
        Path2D gear = new Path2D.Double();
        for (int i = 0; i < teeth * 2; i++) {
            double a0 = Math.PI * 2 * i / (teeth * 2);
            double a1 = Math.PI * 2 * (i + 1) / (teeth * 2);
            double rr = (i % 2 == 0) ? r : r * 0.78;
            double x0 = cx + Math.cos(a0) * rr;
            double y0 = cy + Math.sin(a0) * rr;
            double x1 = cx + Math.cos(a1) * rr;
            double y1 = cy + Math.sin(a1) * rr;
            if (i == 0) {
                gear.moveTo(x0, y0);
            } else {
                gear.lineTo(x0, y0);
            }
            gear.lineTo(x1, y1);
        }
        gear.closePath();
        p.metal(gear, c);
        p.fill(Painter.circle(cx, cy, r * 0.36), Pal.darker(c, 0.55f));
    }

    /** Crux terminatus style cross. */
    public static void crux(Painter p, double cx, double cy, double r, Color c) {
        double a = r * 0.36;
        Path2D cross = Painter.poly(
                cx - a, cy - r, cx + a, cy - r, cx + a * 0.6, cy - a * 0.6, cx + r, cy - a, cx + r, cy + a,
                cx + a * 0.6, cy + a * 0.6, cx + a, cy + r, cx - a, cy + r, cx - a * 0.6, cy + a * 0.6,
                cx - r, cy + a, cx - r, cy - a, cx - a * 0.6, cy - a * 0.6);
        p.volume(cross, c, 0.7f);
    }

    /** Five-pointed star for UI stars. */
    public static Path2D star(double cx, double cy, double outer, double inner, int points) {
        Path2D s = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + Math.PI * i / points;
            double r = i % 2 == 0 ? outer : inner;
            double x = cx + Math.cos(a) * r;
            double y = cy + Math.sin(a) * r;
            if (i == 0) {
                s.moveTo(x, y);
            } else {
                s.lineTo(x, y);
            }
        }
        s.closePath();
        return s;
    }
}
