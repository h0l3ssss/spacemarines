package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Main menu backdrop: a burning hive city at dusk, a Space Marine squad on a ridge, swarms on the horizon. */
public final class MenuArt {
    private MenuArt() {
    }

    public static BufferedImage background(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Painter p = new Painter(g);
        Random rnd = new Random(40000);
        g.setPaint(new GradientPaint(0, 0, new Color(24, 10, 18), 0, h * 0.75f, new Color(150, 52, 30)));
        g.fillRect(0, 0, w, h);
        p.glow(w * 0.72, h * 0.28, h * 0.22, new Color(255, 200, 150), 0.5f);
        p.fill(Painter.circle(w * 0.72, h * 0.28, h * 0.1), new Color(255, 214, 170));
        p.fill(Painter.circle(w * 0.75, h * 0.26, h * 0.1), new Color(170, 70, 50, 200));
        for (int layer = 0; layer < 3; layer++) {
            Color c = Pal.mix(new Color(70, 30, 30), new Color(12, 8, 12), layer / 2f);
            double base = h * (0.62 + layer * 0.07);
            Path2D city = new Path2D.Double();
            city.moveTo(0, h);
            double x = 0;
            while (x < w) {
                double bw = 30 + rnd.nextDouble() * 70;
                double bh = 60 + rnd.nextDouble() * (240 - layer * 60);
                city.lineTo(x, base - bh);
                city.lineTo(x + bw * 0.5, base - bh - rnd.nextDouble() * 60);
                city.lineTo(x + bw, base - bh);
                x += bw + rnd.nextDouble() * 20;
            }
            city.lineTo(w, h);
            city.closePath();
            p.fill(city, c);
            for (int i = 0; i < 40; i++) {
                p.fill(Painter.rect(rnd.nextDouble() * w, base - rnd.nextDouble() * 200, 3, 4),
                        Pal.alpha(new Color(255, 170, 60), 0.5f - layer * 0.15f));
            }
        }
        for (int i = 0; i < 12; i++) {
            p.glow(rnd.nextDouble() * w, h * 0.6 + rnd.nextDouble() * h * 0.1, 40 + rnd.nextDouble() * 80,
                    new Color(255, 110, 30), 0.35f);
        }
        for (int i = 0; i < 600; i++) {
            double sx = rnd.nextDouble() * w;
            double sy = h * 0.74 + rnd.nextDouble() * 20;
            p.fill(Painter.circle(sx, sy, 1.2), new Color(40, 16, 30));
        }
        Path2D ridge = new Path2D.Double();
        ridge.moveTo(0, h);
        ridge.lineTo(0, h * 0.8);
        ridge.quadTo(w * 0.3, h * 0.72, w * 0.55, h * 0.8);
        ridge.quadTo(w * 0.8, h * 0.86, w, h * 0.78);
        ridge.lineTo(w, h);
        ridge.closePath();
        p.fill(ridge, new Color(10, 8, 12));
        double[][] squad = {{0.16, 0.765, 6.2}, {0.25, 0.745, 7.4}, {0.34, 0.75, 6.4}};
        for (double[] m : squad) {
            p.push();
            p.translate(w * m[0], h * m[1]);
            p.scale(m[2], m[2]);
            MarineArt.draw(p, Pose.of(Pose.Kind.IDLE, 0, 2), MarineArt.tactical());
            p.pop();
        }
        g.setPaint(new GradientPaint(0, h * 0.7f, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 170)));
        g.fillRect(0, (int) (h * 0.7f), w, h);
        for (int i = 0; i < 160; i++) {
            p.glow(rnd.nextDouble() * w, rnd.nextDouble() * h, 1.5 + rnd.nextDouble() * 2.5, new Color(255, 160, 60), 0.8f);
        }
        g.dispose();
        return img;
    }
}
