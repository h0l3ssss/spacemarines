package com.warhammer.towerdefense.tools.art;

import com.badlogic.gdx.math.Vector2;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.map.PathDefinition;
import com.warhammer.towerdefense.map.TerrainObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Paints a map background from its JSON definition: ground, roads following the real enemy paths, and every
 * terrain prop. World y points up in the data, so the painter flips it. Output is authored at {@code scale}
 * pixels per world unit.
 */
public final class MapPainter {
    private static final Color GRASS = new Color(92, 124, 60);
    private static final Color GRASS_DARK = new Color(70, 98, 46);
    private static final Color GRASS_LIGHT = new Color(124, 152, 74);
    private static final Color DIRT = new Color(146, 118, 82);
    private static final Color DIRT_DARK = new Color(108, 84, 58);
    private static final Color DIRT_EDGE = new Color(84, 70, 44);

    private final MapDefinition map;
    private final float scale;
    private final Random rnd;

    public MapPainter(MapDefinition map, float scale) {
        this.map = map;
        this.scale = scale;
        this.rnd = new Random(map.id.hashCode());
    }

    public BufferedImage paint() {
        int w = Math.round(map.width * scale);
        int h = Math.round(map.height * scale);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Painter p = new Painter(g);
        p.translate(0, h);
        p.scale(scale, -scale);
        ground(p);
        List<Path2D> roads = roads();
        for (Path2D road : roads) {
            p.stroke(road, DIRT_EDGE, 76);
        }
        for (Path2D road : roads) {
            p.stroke(road, DIRT_DARK, 68);
        }
        for (Path2D road : roads) {
            p.stroke(road, DIRT, 58);
        }
        roadDetails(p);
        List<TerrainObject> sorted = new ArrayList<>();
        for (TerrainObject t : map.terrain) {
            sorted.add(t);
        }
        sorted.sort((a, b) -> Float.compare(b.y, a.y));
        for (TerrainObject t : sorted) {
            if (isFlat(t.type)) {
                prop(p, t);
            }
        }
        for (TerrainObject t : sorted) {
            if (!isFlat(t.type)) {
                prop(p, t);
            }
        }
        vignette(g, w, h);
        g.dispose();
        return img;
    }

    private static boolean isFlat(String type) {
        return "crater".equals(type) || "wheat".equals(type) || "skulls".equals(type);
    }

    private void ground(Painter p) {
        p.fill(Painter.rect(0, 0, map.width, map.height), GRASS);
        for (int i = 0; i < 420; i++) {
            double x = rnd.nextDouble() * map.width;
            double y = rnd.nextDouble() * map.height;
            double r = 10 + rnd.nextDouble() * 50;
            Color c = rnd.nextBoolean() ? GRASS_DARK : GRASS_LIGHT;
            p.glow(x, y, r, c, 0.35f);
        }
        for (int i = 0; i < 2600; i++) {
            double x = rnd.nextDouble() * map.width;
            double y = rnd.nextDouble() * map.height;
            Color c = rnd.nextInt(3) == 0 ? GRASS_LIGHT : GRASS_DARK;
            p.line(x, y, x + (rnd.nextDouble() - 0.5) * 2, y + 2 + rnd.nextDouble() * 3, Pal.alpha(c, 0.8f), 0.7);
        }
        for (int i = 0; i < 90; i++) {
            double x = rnd.nextDouble() * map.width;
            double y = rnd.nextDouble() * map.height;
            Color flower = rnd.nextBoolean() ? new Color(236, 214, 90) : new Color(220, 220, 230);
            p.fill(Painter.circle(x, y, 1.1), flower);
        }
    }

    private List<Path2D> roads() {
        List<Path2D> out = new ArrayList<>();
        Vector2 v = new Vector2();
        for (PathDefinition def : map.paths) {
            GamePath path = new GamePath(def);
            Path2D road = new Path2D.Double();
            for (float d = 0; d <= path.length(); d += 6f) {
                path.pointAt(d, v);
                if (d == 0) {
                    road.moveTo(v.x, v.y);
                } else {
                    road.lineTo(v.x, v.y);
                }
            }
            path.pointAt(path.length(), v);
            road.lineTo(v.x, v.y);
            out.add(road);
        }
        return out;
    }

    private void roadDetails(Painter p) {
        Vector2 v = new Vector2();
        Vector2 dir = new Vector2();
        for (PathDefinition def : map.paths) {
            GamePath path = new GamePath(def);
            for (float d = 0; d < path.length(); d += 9f) {
                path.sample(d, 0, v, dir);
                for (int side = -1; side <= 1; side += 2) {
                    double ox = -dir.y * 12 * side;
                    double oy = dir.x * 12 * side;
                    p.fill(Painter.circle(v.x + ox, v.y + oy, 2.2), Pal.alpha(DIRT_DARK, 0.35f));
                }
                if (rnd.nextInt(5) == 0) {
                    double ox = (rnd.nextDouble() - 0.5) * 50;
                    double oy = (rnd.nextDouble() - 0.5) * 50;
                    p.fill(Painter.circle(v.x + ox, v.y + oy, 0.8 + rnd.nextDouble() * 1.6), Pal.alpha(DIRT_EDGE, 0.7f));
                }
            }
        }
    }

    private void prop(Painter p, TerrainObject t) {
        p.push();
        p.translate(t.x, t.y);
        p.scale(t.flip ? -t.scale : t.scale, -t.scale);
        switch (t.type) {
            case "tree":
                tree(p, 1.0);
                break;
            case "forest":
                for (int i = 0; i < 9; i++) {
                    p.push();
                    p.translate((rnd.nextDouble() - 0.5) * 90, (rnd.nextDouble() - 0.5) * 50);
                    tree(p, 0.8 + rnd.nextDouble() * 0.5);
                    p.pop();
                }
                break;
            case "wheat":
                wheat(p);
                break;
            case "silo":
                silo(p);
                break;
            case "ruin":
                ruin(p, t.variant);
                break;
            case "statue":
                statue(p);
                break;
            case "wreck":
                wreck(p);
                break;
            case "crater":
                EffectArt.crater(p, 26);
                break;
            case "barricade":
                barricade(p);
                break;
            case "crates":
                crates(p);
                break;
            case "bunker":
                bunker(p);
                break;
            case "pipes":
                pipes(p);
                break;
            case "fence":
                fence(p);
                break;
            case "rocks":
                rocks(p);
                break;
            case "skulls":
                for (int i = 0; i < 4; i++) {
                    Emblems.skull(p, (rnd.nextDouble() - 0.5) * 20, (rnd.nextDouble() - 0.5) * 8, 2.2, Pal.BONE);
                }
                break;
            default:
                p.volume(Painter.circle(0, -5, 5), Color.GRAY);
                break;
        }
        p.pop();
    }

    private void tree(Painter p, double s) {
        p.shadow(6 * s, 2 * s, 20 * s, 7 * s, 0.35f);
        p.volume(Painter.round(-2.5 * s, -14 * s, 5 * s, 14 * s, 2 * s), Pal.LEATHER);
        Color leaf = rnd.nextBoolean() ? new Color(58, 96, 44) : new Color(74, 110, 50);
        for (int i = 0; i < 4; i++) {
            double x = (rnd.nextDouble() - 0.5) * 14 * s;
            double y = -22 * s - rnd.nextDouble() * 10 * s;
            p.volume(Painter.circle(x, y, (9 + rnd.nextDouble() * 5) * s), Pal.mix(leaf, GRASS_DARK, rnd.nextFloat() * 0.4f));
        }
    }

    private void wheat(Painter p) {
        p.fill(Painter.round(-50, -22, 100, 44, 10), new Color(196, 168, 84));
        for (int i = 0; i < 16; i++) {
            double y = -20 + i * 2.7;
            p.line(-48, y, 48, y, Pal.alpha(new Color(150, 120, 60), 0.6f), 0.9);
        }
        p.stroke(Painter.round(-50, -22, 100, 44, 10), new Color(120, 96, 50), 1.5);
    }

    private void silo(Painter p) {
        p.shadow(10, 2, 22, 7, 0.35f);
        p.metal(Painter.round(-12, -46, 24, 46, 4), new Color(150, 150, 140));
        p.volume(Painter.ellipse(0, -46, 12, 5), new Color(170, 170, 160));
        p.fill(Painter.rect(-12, -30, 24, 3), Pal.HAZARD);
    }

    private void ruin(Painter p, int variant) {
        p.shadow(6, 2, 34, 9, 0.35f);
        Color stone = Pal.STONE;
        p.volume(Painter.poly(-30, 0, -30, -34, -22, -40, -18, -26, -12, -30, -10, 0), stone);
        p.volume(Painter.poly(8, 0, 8, -22, 14, -28, 20, -18, 28, -24, 30, 0), Pal.darker(stone, 0.08f));
        if (variant == 0) {
            p.fill(Painter.round(-26, -26, 8, 12, 4), Pal.OUTLINE);
        }
        for (int i = 0; i < 6; i++) {
            p.volume(Painter.round(-10 + rnd.nextDouble() * 20, -4 - rnd.nextDouble() * 4, 5, 4, 1), Pal.STONE_DARK);
        }
    }

    private void statue(Painter p) {
        p.shadow(8, 2, 20, 6, 0.4f);
        p.volume(Painter.round(-10, -12, 20, 12, 2), Pal.STONE_DARK);
        p.volume(Painter.round(-5, -40, 10, 28, 4), Pal.STONE);
        p.volume(Painter.circle(0, -44, 5), Pal.STONE);
        p.volume(Painter.poly(5, -38, 16, -58, 18, -56, 8, -34), Pal.STONE);
        Emblems.aquila(p, 0, -8, 14, Pal.darker(Pal.STONE, 0.3f));
    }

    private void wreck(Painter p) {
        p.shadow(8, 2, 36, 10, 0.4f);
        p.metal(Painter.poly(-32, 0, -34, -16, -20, -24, 24, -24, 32, -12, 30, 0), new Color(70, 80, 70));
        p.metal(Painter.round(-10, -32, 20, 10, 3), new Color(60, 66, 58));
        p.fill(Painter.round(-30, -4, 60, 6, 2), Pal.GUNMETAL);
        p.glow(10, -26, 14, Color.DARK_GRAY, 0.5f);
        p.fill(Painter.circle(-14, -14, 4), Pal.OUTLINE);
    }

    private void barricade(Painter p) {
        for (int i = 0; i < 5; i++) {
            double x = -24 + i * 12;
            p.metal(Painter.poly(x - 6, 0, x, -12, x + 6, 0), Pal.GUNMETAL);
        }
        p.volume(Painter.round(-30, -6, 60, 6, 2), Pal.SANDBAG);
    }

    private void crates(Painter p) {
        p.shadow(6, 2, 18, 6, 0.35f);
        p.volume(Painter.rect(-14, -12, 12, 12), new Color(110, 90, 50));
        p.volume(Painter.rect(-1, -12, 12, 12), new Color(100, 82, 46));
        p.volume(Painter.rect(-8, -24, 12, 12), new Color(118, 96, 54));
        p.fill(Painter.rect(-8, -19, 12, 2), Pal.HAZARD);
    }

    private void bunker(Painter p) {
        p.shadow(8, 2, 34, 10, 0.4f);
        p.volume(Painter.round(-30, -22, 60, 22, 6), Pal.CONCRETE);
        p.fill(Painter.round(-18, -14, 36, 5, 2), Pal.OUTLINE);
        p.fill(Painter.rect(-30, -6, 60, 3), Pal.UM_BLUE);
    }

    private void pipes(Painter p) {
        for (int i = 0; i < 3; i++) {
            p.metal(Painter.round(-40, -6 - i * 7, 80, 5.5, 2.7), i == 1 ? Pal.BRASS : Pal.IRON);
        }
        p.metal(Painter.round(-4, -24, 8, 24, 2), Pal.GUNMETAL);
    }

    private void fence(Painter p) {
        for (int i = 0; i < 7; i++) {
            p.volume(Painter.rect(-36 + i * 12, -14, 3, 14), Pal.LEATHER);
        }
        p.volume(Painter.rect(-38, -11, 80, 2.4), Pal.darker(Pal.LEATHER, 0.1f));
        p.volume(Painter.rect(-38, -5, 80, 2.4), Pal.darker(Pal.LEATHER, 0.1f));
    }

    private void rocks(Painter p) {
        for (int i = 0; i < 4; i++) {
            double x = (rnd.nextDouble() - 0.5) * 30;
            double y = (rnd.nextDouble() - 0.5) * 8;
            double r = 5 + rnd.nextDouble() * 6;
            p.volume(Painter.blob(x - r, y, x - r * 0.4, y - r, x + r * 0.7, y - r * 0.8, x + r, y, x, y + r * 0.3),
                    Pal.mix(Pal.STONE, Pal.STONE_DARK, rnd.nextFloat()));
        }
    }

    private static void vignette(Graphics2D g, int w, int h) {
        g.setTransform(new java.awt.geom.AffineTransform());
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 90), 0, h * 0.18f, new Color(0, 0, 0, 0)));
        g.fillRect(0, 0, w, (int) (h * 0.18f));
        g.setPaint(new GradientPaint(0, h, new Color(0, 0, 0, 110), 0, h * 0.8f, new Color(0, 0, 0, 0)));
        g.fillRect(0, (int) (h * 0.8f), w, h);
        g.setStroke(new BasicStroke(1f));
    }
}
