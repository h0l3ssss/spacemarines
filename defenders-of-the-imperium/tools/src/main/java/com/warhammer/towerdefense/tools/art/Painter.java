package com.warhammer.towerdefense.tools.art;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Java2D helpers for a "hand-painted cartoon" look: every volume gets a light-to-shadow gradient, an inner rim
 * light, an inner shade band and a dark outline. Coordinates are in art units with y pointing DOWN (Java2D).
 */
public final class Painter {
    public final Graphics2D g;
    private final Deque<AffineTransform> transforms = new ArrayDeque<>();
    /** Outline width in art units. */
    public float outline = 0.9f;
    public Color outlineColor = Pal.OUTLINE;

    public Painter(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    public static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    // ------------------------------------------------------------------ transforms

    public void push() {
        transforms.push(g.getTransform());
    }

    public void pop() {
        g.setTransform(transforms.pop());
    }

    public void translate(double x, double y) {
        g.translate(x, y);
    }

    public void rotate(double degrees, double px, double py) {
        g.rotate(Math.toRadians(degrees), px, py);
    }

    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }

    // ------------------------------------------------------------------ shapes

    public static Shape ellipse(double cx, double cy, double rx, double ry) {
        return new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    public static Shape circle(double cx, double cy, double r) {
        return ellipse(cx, cy, r, r);
    }

    public static Shape rect(double x, double y, double w, double h) {
        return new Rectangle2D.Double(x, y, w, h);
    }

    public static Shape round(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    /** Closed polygon from x,y pairs. */
    public static Path2D poly(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    /** Smooth closed blob through the given points (quadratic midpoint spline). */
    public static Path2D blob(double... xy) {
        int n = xy.length / 2;
        Path2D.Double p = new Path2D.Double();
        double mx = (xy[0] + xy[2]) / 2;
        double my = (xy[1] + xy[3]) / 2;
        p.moveTo(mx, my);
        for (int i = 1; i <= n; i++) {
            int a = i % n;
            int b = (i + 1) % n;
            double nx = (xy[a * 2] + xy[b * 2]) / 2;
            double ny = (xy[a * 2 + 1] + xy[b * 2 + 1]) / 2;
            p.quadTo(xy[a * 2], xy[a * 2 + 1], nx, ny);
        }
        p.closePath();
        return p;
    }

    /** A tapered limb/blade from (x1,y1) width w1 to (x2,y2) width w2 with rounded ends. */
    public static Path2D limb(double x1, double y1, double w1, double x2, double y2, double w2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.max(0.0001, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x1 + nx * w1 / 2, y1 + ny * w1 / 2);
        p.lineTo(x2 + nx * w2 / 2, y2 + ny * w2 / 2);
        p.quadTo(x2 + dx / len * w2 * 0.6, y2 + dy / len * w2 * 0.6, x2 - nx * w2 / 2, y2 - ny * w2 / 2);
        p.lineTo(x1 - nx * w1 / 2, y1 - ny * w1 / 2);
        p.quadTo(x1 - dx / len * w1 * 0.6, y1 - dy / len * w1 * 0.6, x1 + nx * w1 / 2, y1 + ny * w1 / 2);
        p.closePath();
        return p;
    }

    /** Curved blade/talon: from base (x,y) sweeping along an arc, pointed tip. */
    public static Path2D talon(double x, double y, double length, double width, double angleDeg, double curve) {
        double a = Math.toRadians(angleDeg);
        double tipX = x + Math.cos(a) * length;
        double tipY = y + Math.sin(a) * length;
        double nx = -Math.sin(a);
        double ny = Math.cos(a);
        double cx = (x + tipX) / 2 + nx * curve;
        double cy = (y + tipY) / 2 + ny * curve;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x + nx * width / 2, y + ny * width / 2);
        p.quadTo(cx + nx * width * 0.3, cy + ny * width * 0.3, tipX, tipY);
        p.quadTo(cx - nx * width * 0.5, cy - ny * width * 0.5, x - nx * width / 2, y - ny * width / 2);
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ painting

    public void fill(Shape s, Paint p) {
        g.setPaint(p);
        g.fill(s);
    }

    public void stroke(Shape s, Color c, double width) {
        g.setPaint(c);
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    public void line(double x1, double y1, double x2, double y2, Color c, double width) {
        stroke(new java.awt.geom.Line2D.Double(x1, y1, x2, y2), c, width);
    }

    public void outline(Shape s) {
        stroke(s, outlineColor, outline);
    }

    /** Flat colour with outline. */
    public void flat(Shape s, Color c) {
        fill(s, c);
        outline(s);
    }

    /** Painted volume: gradient body, rim light, inner shade and outline. */
    public void volume(Shape s, Color base) {
        volume(s, base, 1f);
    }

    public void volume(Shape s, Color base, float contrast) {
        Rectangle2D b = s.getBounds2D();
        if (b.getWidth() <= 0 || b.getHeight() <= 0) {
            return;
        }
        Color hi = Pal.lighter(base, 0.28f * contrast);
        Color lo = Pal.darker(base, 0.38f * contrast);
        Point2D start = new Point2D.Double(b.getX() + b.getWidth() * 0.25, b.getY());
        Point2D end = new Point2D.Double(b.getX() + b.getWidth() * 0.75, b.getMaxY());
        if (start.distance(end) < 0.01) {
            end = new Point2D.Double(start.getX() + 0.01, start.getY() + 0.01);
        }
        fill(s, new LinearGradientPaint(start, end, new float[]{0f, 0.5f, 1f}, new Color[]{hi, base, lo},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        Shape oldClip = g.getClip();
        g.clip(s);
        double band = Math.max(0.6, Math.min(b.getWidth(), b.getHeight()) * 0.12);
        AffineTransform down = AffineTransform.getTranslateInstance(band * 0.7, band);
        stroke(down.createTransformedShape(s), Pal.alpha(Pal.lighter(base, 0.55f), 0.55f), band);
        AffineTransform up = AffineTransform.getTranslateInstance(-band * 0.6, -band * 0.9);
        stroke(up.createTransformedShape(s), Pal.alpha(Pal.darker(base, 0.55f), 0.45f), band * 1.2);
        g.setClip(oldClip);
        outline(s);
    }

    /** Metallic surface: sharper, brighter specular band. */
    public void metal(Shape s, Color base) {
        Rectangle2D b = s.getBounds2D();
        if (b.getWidth() <= 0 || b.getHeight() <= 0) {
            return;
        }
        Point2D start = new Point2D.Double(b.getX(), b.getY());
        Point2D end = new Point2D.Double(b.getX(), b.getMaxY());
        if (b.getWidth() > b.getHeight() * 1.6) {
            end = new Point2D.Double(b.getX(), b.getMaxY() + 0.01);
        }
        fill(s, new LinearGradientPaint(start, end, new float[]{0f, 0.3f, 0.45f, 1f},
                new Color[]{Pal.lighter(base, 0.25f), Pal.lighter(base, 0.55f), base, Pal.darker(base, 0.45f)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        outline(s);
    }

    /** Soft additive-looking glow (drawn with normal alpha). */
    public void glow(double cx, double cy, double r, Color c, float alpha) {
        if (r <= 0) {
            return;
        }
        Color inner = Pal.alpha(c, alpha);
        Color outer = Pal.alpha(c, 0f);
        fill(circle(cx, cy, r), new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r,
                new float[]{0f, 1f}, new Color[]{inner, outer}));
    }

    public void shadow(double cx, double cy, double rx, double ry, float alpha) {
        Color c = new Color(0, 0, 0, Math.round(255 * alpha));
        Color mid = new Color(0, 0, 0, Math.round(255 * alpha * 0.55f));
        AffineTransform squash = new AffineTransform();
        squash.translate(cx, cy);
        squash.scale(1, ry / rx);
        squash.translate(-cx, -cy);
        Point2D center = new Point2D.Double(cx, cy);
        fill(ellipse(cx, cy, rx, ry), new RadialGradientPaint(center, (float) rx, center, new float[]{0f, 0.6f, 1f},
                new Color[]{c, mid, new Color(0, 0, 0, 0)}, MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB, squash));
    }

    public void alpha(float a) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
    }

    public Composite composite() {
        return g.getComposite();
    }

    public void composite(Composite c) {
        g.setComposite(c);
    }

    /** Small highlight dot/spec. */
    public void spec(double x, double y, double r) {
        fill(circle(x, y, r), Pal.alpha(Color.WHITE, 0.75f));
    }
}
