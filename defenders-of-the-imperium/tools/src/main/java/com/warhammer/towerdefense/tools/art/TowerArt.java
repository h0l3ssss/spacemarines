package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.geom.Path2D;

/**
 * Tower bases (the structure only; crews are unit sprites drawn on top at runtime), the empty build plot and the
 * Imperial Fortress. Origin (0,0) is the ground centre of the structure; y down.
 */
public final class TowerArt {
    private TowerArt() {
    }

    /** Stone plate with an engraved aquila: an empty build plot. */
    public static void buildSlot(Painter p) {
        p.shadow(0, 2, 44, 16, 0.35f);
        p.volume(Painter.ellipse(0, 0, 40, 17), Pal.STONE_DARK);
        p.volume(Painter.ellipse(0, -3, 37, 15), Pal.STONE);
        p.stroke(Painter.ellipse(0, -3, 30, 11.5), Pal.alpha(Pal.STONE_DARK, 0.8f), 1.2);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            p.line(Math.cos(a) * 30, -3 + Math.sin(a) * 11.5, Math.cos(a) * 37, -3 + Math.sin(a) * 15,
                    Pal.alpha(Pal.STONE_DARK, 0.7f), 0.8);
        }
        p.push();
        p.scale(1, 0.45);
        Emblems.aquila(p, 0, -8, 30, Pal.darker(Pal.STONE, 0.25f));
        p.pop();
    }

    private static void platform(Painter p, double rx, double ry, Color top, Color side) {
        p.shadow(0, 3, rx + 8, ry + 5, 0.4f);
        p.volume(Painter.round(-rx, -ry * 0.9, rx * 2, ry * 1.8, ry), side);
        p.volume(Painter.ellipse(0, -ry * 0.9, rx, ry * 0.9), top);
    }

    private static void sandbags(Painter p, double cx, double cy, double rx, double ry, int count, double from, double to) {
        for (int i = 0; i < count; i++) {
            double a = Math.toRadians(from + (to - from) * i / Math.max(1, count - 1));
            double x = cx + Math.cos(a) * rx;
            double y = cy + Math.sin(a) * ry;
            p.volume(Painter.round(x - 6, y - 4, 12, 7, 3), i % 2 == 0 ? Pal.SANDBAG : Pal.darker(Pal.SANDBAG, 0.1f));
        }
    }

    private static void banner(Painter p, double x, double y, double h, Color cloth) {
        p.metal(Painter.rect(x - 0.8, y - h, 1.6, h), Pal.GUNMETAL);
        p.volume(Painter.circle(x, y - h - 1, 1.6), Pal.GOLD, 0.6f);
        Path2D flag = Painter.poly(x + 0.8, y - h + 1, x + 13, y - h + 1, x + 13, y - h + 17, x + 7, y - h + 13,
                x + 0.8, y - h + 17);
        p.volume(flag, cloth);
        Emblems.omega(p, x + 7, y - h + 7, 2.6, Pal.IVORY);
    }

    private static void crenellatedWall(Painter p, double w, double h, double y, Color stone) {
        p.volume(Painter.rect(-w / 2, y - h, w, h), stone);
        for (double x = -w / 2; x < w / 2 - 2; x += 8) {
            p.volume(Painter.rect(x, y - h - 5, 5, 5), Pal.lighter(stone, 0.05f));
        }
        for (double yy = y - h + 6; yy < y; yy += 6) {
            p.line(-w / 2, yy, w / 2, yy, Pal.alpha(Pal.darker(stone, 0.35f), 0.6f), 0.5);
        }
    }

    // ------------------------------------------------------------------ tactical

    public static void tactical(Painter p, int level) {
        switch (level) {
            case 1:
                platform(p, 36, 15, Pal.CONCRETE, Pal.CONCRETE_DARK);
                sandbags(p, 0, -12, 32, 11, 9, 15, 165);
                p.volume(Painter.round(-30, -26, 12, 8, 1.5), Pal.darker(Pal.GUNMETAL, 0.1f));
                break;
            case 2:
                platform(p, 40, 16, Pal.CONCRETE, Pal.CONCRETE_DARK);
                p.volume(Painter.round(-38, -30, 76, 20, 4), Pal.CONCRETE);
                p.fill(Painter.rect(-38, -22, 76, 3), Pal.UM_BLUE);
                sandbags(p, 0, -8, 34, 10, 9, 20, 160);
                banner(p, -30, -28, 34, Pal.UM_BLUE);
                break;
            case 3:
                platform(p, 44, 17, Pal.STONE, Pal.STONE_DARK);
                crenellatedWall(p, 86, 24, -8, Pal.STONE);
                Emblems.aquila(p, 0, -22, 26, Pal.GOLD);
                banner(p, -34, -34, 38, Pal.UM_BLUE);
                banner(p, 34, -34, 38, Pal.UM_BLUE);
                break;
            case 'a':
                tactical(p, 3);
                p.fill(Painter.rect(-43, -34, 86, 3), Pal.GOLD);
                Emblems.skull(p, 0, -14, 4, Pal.IVORY);
                break;
            case 'b':
                tactical(p, 3);
                for (int i = 0; i < 3; i++) {
                    p.volume(Painter.round(-44 + i * 12, -12, 10, 8, 1.2), Pal.darker(Pal.HAZARD, 0.2f));
                    p.glow(-39 + i * 12, -10, 5, Pal.LASER, 0.35f);
                }
                break;
            default:
                platform(p, 42, 16, Pal.CONCRETE, Pal.CONCRETE_DARK);
                p.volume(Painter.round(-40, -34, 80, 26, 6), Pal.GUNMETAL);
                p.fill(Painter.rect(-40, -26, 80, 4), Pal.HAZARD);
                for (int i = 0; i < 8; i++) {
                    p.fill(Painter.poly(-40 + i * 10, -26, -35 + i * 10, -26, -39 + i * 10, -22, -44 + i * 10, -22), Pal.OUTLINE);
                }
                sandbags(p, 0, -8, 36, 10, 10, 15, 165);
                break;
        }
    }

    // ------------------------------------------------------------------ assault (barracks)

    public static void assault(Painter p, int level) {
        double size = level == 1 ? 0.85 : level == 2 ? 0.95 : 1.05;
        p.push();
        p.scale(size, size);
        platform(p, 42, 16, Pal.STONE, Pal.STONE_DARK);
        Color wall = level == 1 ? Pal.CONCRETE : Pal.STONE;
        p.volume(Painter.round(-36, -58, 72, 50, 4), wall);
        Path2D roof = Painter.poly(-40, -56, 0, -80, 40, -56);
        p.volume(roof, level == 'b' ? Pal.GUNMETAL : Pal.UM_BLUE_DARK);
        Path2D door = new Path2D.Double();
        door.moveTo(-10, -8);
        door.lineTo(-10, -30);
        door.quadTo(0, -42, 10, -30);
        door.lineTo(10, -8);
        door.closePath();
        p.volume(door, Pal.darker(Pal.LEATHER, 0.3f));
        p.line(0, -38, 0, -8, Pal.OUTLINE, 0.8);
        Emblems.aquila(p, 0, -48, 20, Pal.GOLD);
        if (level >= 2) {
            banner(p, -30, -56, 36, Pal.UM_BLUE);
        }
        if (level >= 3) {
            banner(p, 30, -56, 36, Pal.UM_BLUE);
            for (int s = -1; s <= 1; s += 2) {
                p.volume(Painter.round(s * 40 - 6, -70, 12, 62, 2), Pal.STONE_DARK);
                p.volume(Painter.poly(s * 40 - 8, -70, s * 40, -84, s * 40 + 8, -70), Pal.UM_BLUE_DARK);
            }
        }
        if (level == 'a') {
            Emblems.skull(p, 0, -64, 5, Pal.IVORY);
            p.fill(Painter.rect(-36, -58, 72, 3), Pal.GOLD);
        } else if (level == 'b') {
            Emblems.crux(p, 0, -64, 7, Pal.IVORY);
            p.glow(0, -22, 18, Pal.PLASMA, 0.3f);
        } else if (level == 'c') {
            p.volume(Painter.round(-36, -58, 72, 50, 4), Pal.BLACK_ARMOR);
            p.volume(door, Pal.darker(Pal.LEATHER, 0.3f));
            Emblems.skull(p, 0, -46, 7, Pal.IVORY);
            for (int s = -1; s <= 1; s += 2) {
                p.volume(Painter.round(s * 26 - 2, -24, 4, 12, 1), Pal.IVORY);
                p.glow(s * 26, -26, 5, Pal.FIRE, 0.7f);
            }
        }
        p.pop();
    }

    // ------------------------------------------------------------------ devastator

    public static void devastator(Painter p, int level) {
        double r = level == 1 ? 34 : level == 2 ? 38 : 42;
        platform(p, r, 15, Pal.CONCRETE, Pal.CONCRETE_DARK);
        p.volume(Painter.round(-r * 0.8, -20, r * 1.6, 14, 4), Pal.darker(Pal.CONCRETE, 0.1f));
        if (level >= 2) {
            for (int i = 0; i < 4; i++) {
                p.metal(Painter.round(-r + 4 + i * (r * 0.5), -30, 8, 12, 1.5), Pal.GUNMETAL);
            }
        }
        sandbags(p, 0, -6, r - 4, 9, 8, 20, 160);
        if (level == 'a') {
            for (int i = 0; i < 2; i++) {
                double x = -30 + i * 60;
                p.metal(Painter.round(x - 5, -40, 10, 26, 3), Pal.GUNMETAL);
                p.glow(x, -36, 8, Pal.LASER, 0.6f);
                p.fill(Painter.round(x - 2, -38, 4, 18, 2), Pal.LASER);
            }
        } else if (level == 'b') {
            p.metal(Painter.round(-44, -44, 26, 24, 3), Pal.GUNMETAL);
            for (int i = 0; i < 6; i++) {
                p.fill(Painter.circle(-38 + (i % 3) * 7, -38 + (i / 3) * 9, 2.4), Pal.OUTLINE);
                p.fill(Painter.circle(-38 + (i % 3) * 7, -38 + (i / 3) * 9, 1.3), Pal.IMPERIAL_RED);
            }
        } else if (level == 'c') {
            for (int i = 0; i < 2; i++) {
                double x = -34 + i * 68;
                p.metal(Painter.round(x - 6, -42, 12, 28, 5), Pal.GUNMETAL);
                p.glow(x, -30, 10, Pal.PLASMA, 0.7f);
                p.fill(Painter.round(x - 3, -40, 6, 22, 3), Pal.lighter(Pal.PLASMA, 0.3f));
            }
        }
        Emblems.aquila(p, 0, -12, 18, Pal.GOLD);
    }

    // ------------------------------------------------------------------ techmarine

    public static void techmarine(Painter p, int level) {
        platform(p, 40, 16, Pal.GUNMETAL, Pal.darker(Pal.GUNMETAL, 0.3f));
        p.metal(Painter.round(-38, -30, 22, 24, 3), Pal.IRON);
        for (int i = 0; i < 3; i++) {
            p.fill(Painter.round(-34, -26 + i * 6, 14, 3, 1), i == 1 ? Pal.LENS_GREEN : Pal.darker(Pal.LENS_GREEN, 0.5f));
        }
        Path2D pipe = new Path2D.Double();
        pipe.moveTo(18, -8);
        pipe.quadTo(34, -14, 30, -36);
        p.stroke(pipe, Pal.OUTLINE, 5);
        p.stroke(pipe, Pal.BRASS, 3.4);
        p.metal(Painter.round(24, -56, 12, 22, 2), Pal.IRON);
        p.glow(30, -58, 8, Color.GRAY, 0.35f);
        Emblems.cog(p, 22, -22, level >= 2 ? 11 : 8, Pal.STEEL, 10);
        if (level >= 3 || level == 'b') {
            Emblems.cog(p, -26, -44, 9, Pal.MARS_RED, 8);
        }
        if (level == 'a') {
            for (int i = 0; i < 3; i++) {
                Emblems.skull(p, -30 + i * 10, -40, 3.2, Pal.IVORY);
            }
        } else if (level == 'c') {
            p.metal(Painter.round(-4, -80, 8, 64, 2), Pal.GUNMETAL);
            p.glow(0, -84, 22, Pal.PLASMA, 0.6f);
            p.volume(Painter.poly(0, -96, 6, -84, 0, -72, -6, -84), Pal.lighter(Pal.PLASMA, 0.2f));
            p.stroke(Painter.ellipse(0, -60, 12, 4), Pal.alpha(Pal.PLASMA, 0.8f), 1.2);
        }
    }

    // ------------------------------------------------------------------ fortress

    public static void fortress(Painter p) {
        p.shadow(10, 4, 110, 26, 0.45f);
        p.volume(Painter.rect(-110, -150, 150, 150), Pal.STONE_DARK);
        crenellatedWall(p, 150, 120, 0, Pal.STONE);
        p.push();
        p.translate(-35, 0);
        p.pop();
        for (int i = 0; i < 2; i++) {
            double x = i == 0 ? -95 : 38;
            p.volume(Painter.rect(x - 18, -190, 36, 190), Pal.STONE);
            p.volume(Painter.poly(x - 22, -190, x, -228, x + 22, -190), Pal.UM_BLUE_DARK);
            for (int w = 0; w < 3; w++) {
                p.fill(Painter.round(x - 4, -170 + w * 40, 8, 14, 4), Pal.OUTLINE);
                p.glow(x, -163 + w * 40, 6, Pal.FIRE, 0.45f);
            }
        }
        Path2D gate = new Path2D.Double();
        gate.moveTo(-6, 0);
        gate.lineTo(-6, -44);
        gate.quadTo(18, -72, 42, -44);
        gate.lineTo(42, 0);
        gate.closePath();
        p.volume(gate, Pal.darker(Pal.LEATHER, 0.4f));
        for (int i = 0; i < 5; i++) {
            p.line(-2 + i * 10, -58 + (i == 2 ? -6 : 0), -2 + i * 10, 0, Pal.GUNMETAL, 1.4);
        }
        Emblems.aquila(p, 18, -96, 54, Pal.GOLD);
        banner(p, -60, -120, 44, Pal.UM_BLUE);
        banner(p, 70, -110, 40, Pal.UM_BLUE);
    }
}
