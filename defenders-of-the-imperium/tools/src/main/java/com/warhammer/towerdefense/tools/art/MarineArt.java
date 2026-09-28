package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Parametric Space Marine rig (3/4 view, facing right), styled after heavy painted miniatures: deep blue power
 * armour with gold trim, two massive pauldrons, a sunken helmet with glowing red lenses, long bolter held across
 * the body. One routine draws every Astartes in the game by swapping colours, helmets, wargear and weapons.
 * Coordinates: art units, feet at (0,0), y down.
 */
public final class MarineArt {
    public enum Weapon {
        BOLTER, COMBI_BOLTER, CHAINSWORD, POWER_SWORD, CROZIUS, OMNISSIAN_AXE, POWER_FIST, HEAVY_BOLTER,
        MISSILE_LAUNCHER, LASCANNON, PLASMA_CANNON
    }

    public enum Helmet {
        STANDARD, SERGEANT, VETERAN, CAPTAIN, CHAPLAIN, TECHMARINE, TERMINATOR
    }

    public enum Symbol {
        OMEGA, CRUX, SKULL, COG, NONE
    }

    /** Everything that distinguishes one kind of Space Marine from another. */
    public static final class Spec {
        public Color armor = Pal.UM_BLUE;
        public Color nearPauldron = Pal.UM_BLUE;
        public Color farPauldron = Pal.UM_BLUE;
        public Color trim = Pal.GOLD;
        public Color helmetColor = Pal.UM_BLUE;
        public Color lens = Pal.LENS_RED;
        public Helmet helmet = Helmet.STANDARD;
        public Weapon weapon = Weapon.BOLTER;
        public boolean pistol;
        public boolean cape;
        public boolean halo;
        public boolean servoArm;
        public boolean terminator;
        public boolean purity;
        public Symbol symbol = Symbol.OMEGA;
        public Color symbolColor = Pal.IVORY;
        public float scale = 1f;
    }

    /** Armour contrast: deeper shadows and brighter rims than the generic painter default. */
    private static final float CONTRAST = 1.35f;
    private static final double HIP_Y = -18;

    private MarineArt() {
    }

    // ------------------------------------------------------------------ presets

    public static Spec tactical() {
        return new Spec();
    }

    public static Spec sternguard() {
        Spec s = new Spec();
        s.helmet = Helmet.VETERAN;
        s.helmetColor = Pal.IVORY;
        s.weapon = Weapon.COMBI_BOLTER;
        s.purity = true;
        return s;
    }

    public static Spec assault() {
        Spec s = new Spec();
        s.weapon = Weapon.CHAINSWORD;
        s.pistol = true;
        s.helmet = Helmet.SERGEANT;
        return s;
    }

    public static Spec veteran() {
        Spec s = new Spec();
        s.weapon = Weapon.POWER_SWORD;
        s.pistol = true;
        s.helmet = Helmet.VETERAN;
        s.helmetColor = Pal.IVORY;
        s.purity = true;
        return s;
    }

    public static Spec terminator() {
        Spec s = new Spec();
        s.terminator = true;
        s.helmet = Helmet.TERMINATOR;
        s.weapon = Weapon.POWER_FIST;
        s.symbol = Symbol.CRUX;
        s.scale = 1.1f;
        return s;
    }

    public static Spec captain() {
        Spec s = new Spec();
        s.helmet = Helmet.CAPTAIN;
        s.weapon = Weapon.POWER_SWORD;
        s.pistol = true;
        s.cape = true;
        s.halo = true;
        s.purity = true;
        s.scale = 1.1f;
        return s;
    }

    public static Spec chaplain() {
        Spec s = new Spec();
        s.armor = Pal.BLACK_ARMOR;
        s.nearPauldron = Pal.BLACK_ARMOR;
        s.farPauldron = Pal.BLACK_ARMOR;
        s.helmetColor = Pal.BLACK_ARMOR;
        s.helmet = Helmet.CHAPLAIN;
        s.weapon = Weapon.CROZIUS;
        s.pistol = true;
        s.symbol = Symbol.SKULL;
        s.purity = true;
        return s;
    }

    public static Spec techmarine() {
        Spec s = new Spec();
        s.armor = Pal.MARS_RED;
        s.farPauldron = Pal.MARS_RED;
        s.nearPauldron = Pal.UM_BLUE;
        s.helmetColor = Pal.MARS_RED;
        s.helmet = Helmet.TECHMARINE;
        s.lens = Pal.LENS_GREEN;
        s.weapon = Weapon.OMNISSIAN_AXE;
        s.servoArm = true;
        s.trim = Pal.BRASS;
        return s;
    }

    public static Spec devastator(Weapon weapon) {
        Spec s = new Spec();
        s.weapon = weapon;
        return s;
    }

    // ------------------------------------------------------------------ rig

    public static void draw(Painter p, Pose pose, Spec s) {
        float d = pose.death();
        p.push();
        if (d > 0f) {
            // topple backwards and settle on the back, kept inside the frame
            p.translate(d * 17f, -d * 10f);
            p.rotate(-84f * d, 0, 0);
        }
        p.scale(s.scale, s.scale);
        double phase = pose.t * Math.PI * 2;
        double stride = pose.stride();
        double liftNear = pose.walking() ? Math.max(0, Math.cos(phase)) * 2.4 : 0;
        double liftFar = pose.walking() ? Math.max(0, -Math.cos(phase)) * 2.4 : 0;
        double bob = pose.bob();
        double bulk = s.terminator ? 1.22 : 1.0;

        if (s.cape) {
            cape(p, pose, bob);
        }
        backpack(p, s, bob, bulk);
        if (s.halo) {
            halo(p, bob);
        }
        farPauldron(p, s, bob, bulk);
        leg(p, 2.6 * bulk, HIP_Y, 2.6 * bulk - stride * 5.5, -liftFar, s, true);
        leg(p, -2.4 * bulk, HIP_Y, -2.4 * bulk + stride * 5.5, -liftNear, s, false);
        torso(p, s, bob, bulk);
        if (s.servoArm) {
            servoArm(p, pose, bob);
        }
        head(p, s, bob, bulk);
        weaponAndArms(p, pose, s, bob, bulk);
        nearPauldron(p, s, bob, bulk);
        if (s.weapon == Weapon.MISSILE_LAUNCHER) {
            missileLauncher(p, pose, bob);
        }
        p.pop();
    }

    private static void armour(Painter p, Shape shape, Color c) {
        p.volume(shape, c, CONTRAST);
    }

    private static void leg(Painter p, double hipX, double hipY, double footX, double footY, Spec s, boolean far) {
        Color c = far ? Pal.darker(s.armor, 0.28f) : s.armor;
        double w = s.terminator ? 8.6 : 7.0;
        double kneeX = (hipX + footX) / 2 + 1.8;
        double kneeY = (hipY + footY) / 2 - 0.2;
        armour(p, Painter.limb(hipX, hipY, w, kneeX, kneeY, w * 0.92), c);
        armour(p, Painter.limb(kneeX, kneeY, w * 0.9, footX, footY - 3.2, w * 0.86), c);
        Shape pad = Painter.ellipse(kneeX + 1.0, kneeY - 0.3, w * 0.46, w * 0.5);
        armour(p, pad, Pal.lighter(c, 0.08f));
        if (!far) {
            p.stroke(Painter.ellipse(kneeX + 1.0, kneeY - 0.3, w * 0.34, w * 0.38), Pal.alpha(s.trim, 0.9f), 0.6);
        }
        Shape boot = Painter.round(footX - w * 0.5, footY - 4.6, w * 1.35, 4.8, 1.8);
        armour(p, boot, Pal.darker(c, 0.2f));
        p.line(footX - w * 0.35, footY - 1.8, footX + w * 0.75, footY - 1.8, Pal.alpha(Pal.OUTLINE, 0.6f), 0.5);
    }

    private static void torso(Painter p, Spec s, double b, double k) {
        Shape body = Painter.blob(-7 * k, -17 + b, 6 * k, -17 + b, 8.6 * k, -23 + b, 9.6 * k, -29.5 + b,
                7.2 * k, -34.5 + b, -1, -35.8 + b, -8.2 * k, -32 + b, -8.6 * k, -24 + b);
        armour(p, body, s.armor);
        // abdomen cabling
        Shape abdomen = Painter.round(-5.8 * k, -23.6 + b, 11.4 * k, 5.2, 1.6);
        p.metal(abdomen, Pal.GUNMETAL);
        for (int i = 0; i < 4; i++) {
            double x = -4.6 * k + i * 2.8 * k;
            p.line(x, -23 + b, x, -19 + b, Pal.alpha(Pal.OUTLINE, 0.7f), 0.5);
        }
        // belt and buckle
        p.flat(Painter.round(-7 * k, -19.4 + b, 14 * k, 2.8, 1), Pal.darker(Pal.LEATHER, 0.25f));
        armour(p, Painter.round(0.4, -20.1 + b, 3.6, 3.8, 0.9), Pal.GOLD);
        // tassets over the thighs
        armour(p, Painter.poly(-6.6 * k, -17 + b, -1.2, -17 + b, -1.8, -11.5 + b, -6.2 * k, -12.6 + b), Pal.darker(s.armor, 0.1f));
        armour(p, Painter.poly(1.2, -17 + b, 6.6 * k, -17 + b, 6.1 * k, -12.6 + b, 1.8, -11.5 + b), Pal.darker(s.armor, 0.02f));
        // chest plate rim and aquila
        p.stroke(new Arc2D.Double(-3, -35 + b, 13 * k, 11, 200, 140, Arc2D.OPEN), Pal.alpha(s.trim, 0.85f), 0.7);
        Emblems.aquila(p, 4.8 * k, -29.2 + b, s.terminator ? 10.5 : 9.2, Pal.GOLD);
        if (s.purity) {
            armour(p, Painter.circle(6.8 * k, -22.8 + b, 1.4), Pal.IMPERIAL_RED);
            p.flat(Painter.poly(6.1 * k, -21.7 + b, 7.5 * k, -21.7 + b, 7.9 * k, -17.4 + b, 6.5 * k, -17.7 + b), Pal.IVORY);
        }
    }

    private static void backpack(Painter p, Spec s, double b, double bulk) {
        Color c = Pal.darker(s.armor, 0.15f);
        if (s.terminator) {
            armour(p, Painter.round(-15, -41 + b, 9, 14, 2.4), c);
            p.metal(Painter.round(-14, -45 + b, 2.6, 5, 0.8), Pal.GUNMETAL);
            p.metal(Painter.round(-10.6, -45 + b, 2.6, 5, 0.8), Pal.GUNMETAL);
            return;
        }
        armour(p, Painter.round(-14.4, -40.5 + b, 10.4, 19, 3), c);
        p.stroke(Painter.round(-13.2, -39.3 + b, 8, 16.6, 2.4), Pal.alpha(s.trim, 0.6f), 0.5);
        p.metal(Painter.round(-13.6, -44.8 + b, 3.2, 6, 1.1), Pal.GUNMETAL);
        p.metal(Painter.round(-9.6, -44.8 + b, 3.2, 6, 1.1), Pal.GUNMETAL);
        p.fill(Painter.ellipse(-12, -44.6 + b, 1.3, 0.6), Pal.OUTLINE);
        p.fill(Painter.ellipse(-8, -44.6 + b, 1.3, 0.6), Pal.OUTLINE);
    }

    private static void farPauldron(Painter p, Spec s, double b, double bulk) {
        double cx = 9.2 * bulk;
        double cy = -31.2 + b;
        double rx = 6.3 * bulk;
        double ry = 6.5 * bulk;
        Color c = Pal.darker(s.farPauldron, 0.22f);
        armour(p, Painter.ellipse(cx, cy, rx, ry), c);
        p.stroke(Painter.ellipse(cx, cy, rx - 0.9, ry - 0.9), Pal.darker(s.trim, 0.25f), 0.9);
        p.outline(Painter.ellipse(cx, cy, rx, ry));
    }

    private static void nearPauldron(Painter p, Spec s, double b, double bulk) {
        double cx = -6.0 * bulk;
        double cy = -29.6 + b;
        double rx = 7.6 * bulk;
        double ry = 7.0 * bulk;
        Shape dome = Painter.ellipse(cx, cy, rx, ry);
        armour(p, dome, s.nearPauldron);
        p.stroke(Painter.ellipse(cx, cy, rx - 1.0, ry - 1.0), Pal.OUTLINE, 2.0);
        p.stroke(Painter.ellipse(cx, cy, rx - 1.0, ry - 1.0), s.trim, 1.3);
        p.outline(dome);
        p.fill(Painter.ellipse(cx - rx * 0.35, cy - ry * 0.45, rx * 0.3, ry * 0.16), Pal.alpha(Color.WHITE, 0.35f));
        double r = rx * 0.34;
        switch (s.symbol) {
            case OMEGA:
                Emblems.omega(p, cx - 0.1, cy + 0.4, r, s.symbolColor);
                break;
            case CRUX:
                Emblems.crux(p, cx - 0.1, cy + 0.3, r * 1.25, Pal.IVORY);
                break;
            case SKULL:
                Emblems.skull(p, cx - 0.1, cy + 0.3, r * 1.15, Pal.IVORY);
                break;
            case COG:
                Emblems.cog(p, cx - 0.1, cy + 0.3, r * 1.2, Pal.STEEL, 8);
                break;
            default:
                break;
        }
    }

    private static void head(Painter p, Spec s, double b, double bulk) {
        double hx = s.terminator ? 4.6 : 4.2;
        double hy = -35.2 + b + (s.terminator ? 2.6 : 0);
        p.metal(Painter.ellipse(hx - 0.6, -31.3 + b, 4.6 * bulk, 2.2), Pal.GUNMETAL);
        switch (s.helmet) {
            case CHAPLAIN:
                armour(p, Painter.ellipse(hx, hy, 4.1, 4.6), Pal.BLACK_ARMOR);
                armour(p, Painter.blob(hx + 0.3, hy - 3.2, hx + 4.3, hy - 2.0, hx + 4.6, hy + 1.8, hx + 2.8, hy + 4.0,
                        hx + 0.6, hy + 3.6), Pal.IVORY);
                p.fill(Painter.ellipse(hx + 2.7, hy - 0.7, 1.2, 1.1), Pal.OUTLINE);
                lens(p, s, hx + 2.8, hy - 0.7, 0.8);
                for (int i = 0; i < 3; i++) {
                    p.line(hx + 1.7 + i * 0.9, hy + 2.2, hx + 1.7 + i * 0.9, hy + 3.5, Pal.OUTLINE, 0.4);
                }
                return;
            case CAPTAIN: {
                Path2D crest = new Path2D.Double();
                crest.moveTo(hx - 2.6, hy - 3.6);
                crest.quadTo(hx - 1.2, hy - 8.4, hx + 3.6, hy - 6.2);
                crest.quadTo(hx + 0.6, hy - 5.8, hx + 1.4, hy - 4.0);
                crest.closePath();
                armour(p, crest, Pal.IMPERIAL_RED);
                standardHelmet(p, s, hx, hy);
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(200 + i * 22);
                    armour(p, Painter.ellipse(hx + Math.cos(a) * 4.3, hy + Math.sin(a) * 4.3, 1.1, 0.6), Pal.GOLD);
                }
                return;
            }
            case TERMINATOR:
                armour(p, Painter.round(hx - 3.4, hy - 3.8, 7.4, 7.4, 2), s.helmetColor);
                armour(p, Painter.poly(hx + 1.2, hy - 0.4, hx + 4.2, hy, hx + 3.8, hy + 3.3, hx + 1.2, hy + 3.4),
                        Pal.darker(s.helmetColor, 0.35f));
                lens(p, s, hx + 1.6, hy - 1.1, 0.9);
                lens(p, s, hx + 3.4, hy - 1.2, 0.7);
                return;
            case TECHMARINE:
                standardHelmet(p, s, hx, hy);
                Emblems.cog(p, hx - 1.6, hy - 0.6, 1.6, Pal.STEEL, 6);
                return;
            default:
                standardHelmet(p, s, hx, hy);
        }
    }

    private static void standardHelmet(Painter p, Spec s, double hx, double hy) {
        armour(p, Painter.ellipse(hx, hy, 4.2, 4.7), s.helmetColor);
        Path2D face = Painter.poly(hx + 0.8, hy - 0.2, hx + 4.4, hy + 0.3, hx + 4.0, hy + 3.4, hx + 1.0, hy + 3.9);
        armour(p, face, Pal.darker(s.helmetColor, 0.32f));
        for (int i = 0; i < 3; i++) {
            p.line(hx + 1.9 + i * 0.7, hy + 1.4, hx + 1.9 + i * 0.7, hy + 3.0, Pal.alpha(Pal.OUTLINE, 0.75f), 0.35);
        }
        if (s.helmet == Helmet.SERGEANT) {
            p.stroke(new Arc2D.Double(hx - 4.2, hy - 4.7, 8.4, 9.4, 40, 110, Arc2D.OPEN), Pal.IMPERIAL_RED, 1.3);
        }
        lens(p, s, hx + 1.7, hy - 1.0, 0.95);
        lens(p, s, hx + 3.6, hy - 1.15, 0.75);
    }

    private static void lens(Painter p, Spec s, double x, double y, double r) {
        p.glow(x, y, r * 3, s.lens, 0.5f);
        p.fill(Painter.ellipse(x, y, r * 1.15, r * 0.8), s.lens);
        p.spec(x - r * 0.3, y - r * 0.3, r * 0.3);
    }

    private static void cape(Painter p, Pose pose, double b) {
        double sway = pose.walking() ? Math.sin(pose.t * Math.PI * 2) * 1.8 : pose.frame * 0.5;
        Path2D c = new Path2D.Double();
        c.moveTo(-3, -35 + b);
        c.quadTo(-11, -34 + b, -14 - sway, -22);
        c.quadTo(-17 - sway, -10, -16 - sway * 1.4, -2.2);
        c.quadTo(-12, -3.8, -9, -1.6);
        c.quadTo(-6, -3.6, -3, -3);
        c.lineTo(-2, -16 + b);
        c.closePath();
        armour(p, c, Pal.IMPERIAL_RED);
        Path2D hem = new Path2D.Double();
        hem.moveTo(-16 - sway * 1.4, -2.4);
        hem.quadTo(-12, -4, -9, -1.8);
        hem.quadTo(-6, -3.8, -3, -3.2);
        p.stroke(hem, Pal.GOLD, 0.9);
    }

    private static void halo(Painter p, double b) {
        double cx = 2.2;
        double cy = -38 + b;
        p.glow(cx, cy, 12, Pal.GOLD, 0.35f);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            armour(p, Painter.poly(cx + Math.cos(a - 0.12) * 6.4, cy + Math.sin(a - 0.12) * 6.4,
                    cx + Math.cos(a) * 9.4, cy + Math.sin(a) * 9.4,
                    cx + Math.cos(a + 0.12) * 6.4, cy + Math.sin(a + 0.12) * 6.4), Pal.GOLD);
        }
        p.stroke(Painter.circle(cx, cy, 6.7), Pal.OUTLINE, 2.6);
        p.stroke(Painter.circle(cx, cy, 6.7), Pal.GOLD, 1.6);
    }

    private static void servoArm(Painter p, Pose pose, double b) {
        double open = pose.attacking() ? (pose.frame % 2) * 0.6 : 0.2;
        double[] j = {-9, -41 + b, -3, -49 + b, 7, -50 + b, 12, -44 + b};
        for (int i = 0; i < 3; i++) {
            p.metal(Painter.limb(j[i * 2], j[i * 2 + 1], 2.4, j[i * 2 + 2], j[i * 2 + 3], 2.0), Pal.STEEL);
            armour(p, Painter.circle(j[i * 2], j[i * 2 + 1], 1.4), Pal.GUNMETAL);
        }
        p.metal(Painter.talon(j[6], j[7], 4.6, 1.7, 60 - open * 40, 1.2), Pal.IRON);
        p.metal(Painter.talon(j[6], j[7], 4.6, 1.7, 120 + open * 40, -1.2), Pal.IRON);
        armour(p, Painter.circle(j[6], j[7], 1.5), Pal.MARS_RED);
    }

    // ------------------------------------------------------------------ weapons

    private static void arm(Painter p, double sx, double sy, double hx, double hy, Color c, double elbowBend) {
        double mx = (sx + hx) / 2;
        double my = (sy + hy) / 2;
        double dx = hx - sx;
        double dy = hy - sy;
        double len = Math.max(0.01, Math.hypot(dx, dy));
        double ex = mx - dy / len * elbowBend;
        double ey = my + dx / len * elbowBend;
        armour(p, Painter.limb(sx, sy, 5.0, ex, ey, 4.4), c);
        armour(p, Painter.limb(ex, ey, 4.6, hx, hy, 4.2), c);
        armour(p, Painter.circle(hx, hy, 2.2), Pal.darker(c, 0.2f));
    }

    private static void weaponAndArms(Painter p, Pose pose, Spec s, double b, double bulk) {
        double nsx = -5.2 * bulk;
        double nsy = -28 + b;
        double fsx = 8.4 * bulk;
        double fsy = -29.5 + b;
        Color near = s.armor;
        Color far = Pal.darker(s.armor, 0.22f);
        double r = pose.recoil();
        switch (s.weapon) {
            case BOLTER:
            case COMBI_BOLTER:
                arm(p, fsx, fsy, 12.8 - r, -24.4 + b, far, 2.2);
                bolter(p, -2.4 - r, -26.4 + b, s.weapon == Weapon.COMBI_BOLTER);
                arm(p, nsx, nsy, 2.0 - r, -22.4 + b, near, -3.0);
                if (pose.flash()) {
                    fireFlash(p, 20.6 - r, -24.6 + b, pose.frame == 0 ? 5.2 : 4.2);
                }
                break;
            case HEAVY_BOLTER:
                heavyBolter(p, -3 - r, -24 + b, pose);
                arm(p, fsx, fsy, 10 - r, -24.2 + b, far, 2.0);
                arm(p, nsx, nsy, 0.4 - r, -21 + b, near, -3.0);
                if (pose.flash()) {
                    fireFlash(p, 23.5 - r, -22.2 + b, pose.frame == 0 ? 6 : 5);
                }
                break;
            case LASCANNON:
                arm(p, fsx, fsy, 10.5 - r, -25.6 + b, far, 2.0);
                lascannon(p, -6 - r, -27 + b, pose);
                arm(p, nsx, nsy, 0.4 - r, -23 + b, near, -3.0);
                break;
            case PLASMA_CANNON:
                arm(p, fsx, fsy, 10 - r, -25.8 + b, far, 2.0);
                plasmaCannon(p, -5 - r, -27.8 + b, pose);
                arm(p, nsx, nsy, 0.6 - r, -22.4 + b, near, -3.0);
                break;
            case MISSILE_LAUNCHER:
                arm(p, fsx, fsy, 9.0 - r * 0.5, -31 + b, far, 1.5);
                arm(p, nsx, nsy, 3.0 - r * 0.5, -30.5 + b, near, -1.5);
                break;
            case POWER_FIST: {
                double swing = pose.swing();
                double fx = pose.attacking() ? 3 + swing * 11 : 3;
                double fy = -23 + b + (pose.attacking() ? -swing * 1.5 : 0);
                arm(p, fsx, fsy, 12.5 - r, -27 + b, far, 2.0);
                stormBolter(p, 10.5 - r, -29 + b);
                if (pose.flash() && pose.frame == 1) {
                    fireFlash(p, 20.5 - r, -27.8 + b, 3.6);
                }
                armour(p, Painter.limb(nsx, nsy, 6.6, fx - 2, fy - 0.5, 5.8), near);
                powerFist(p, fx, fy, pose.attacking() && pose.frame >= 1);
                break;
            }
            default:
                meleeWeapon(p, pose, s, b, nsx, nsy, fsx, fsy, near, far);
                break;
        }
    }

    private static void meleeWeapon(Painter p, Pose pose, Spec s, double b, double nsx, double nsy, double fsx,
                                    double fsy, Color near, Color far) {
        if (s.pistol) {
            arm(p, fsx, fsy, 12.0, -25 + b, far, 2.0);
            boltPistol(p, 10.4, -27.0 + b);
        }
        double hand;
        double blade;
        if (pose.attacking()) {
            double t = (pose.swing() + 0.6) / 1.6;
            hand = -125 + t * 160;
            blade = hand - 20;
        } else {
            hand = -30 + (pose.walking() ? pose.stride() * 6 : 0);
            blade = -72;
        }
        double reach = 9.5;
        double hx = nsx + Math.cos(Math.toRadians(hand)) * reach + 3.5;
        double hy = nsy + Math.sin(Math.toRadians(hand)) * reach + 2.5;
        if (pose.attacking() && pose.frame == 2) {
            swoosh(p, nsx + 3.5, nsy + 2.5, reach + 13, hand - 70, 80,
                    s.weapon == Weapon.POWER_SWORD ? Pal.PLASMA : Color.WHITE);
        }
        p.push();
        p.rotate(blade, hx, hy);
        switch (s.weapon) {
            case CHAINSWORD:
                chainsword(p, hx, hy);
                break;
            case POWER_SWORD:
                powerSword(p, hx, hy);
                break;
            case CROZIUS:
                crozius(p, hx, hy);
                break;
            case OMNISSIAN_AXE:
                axe(p, hx, hy);
                break;
            default:
                break;
        }
        p.pop();
        arm(p, nsx, nsy, hx, hy, near, -2.2);
    }

    private static void swoosh(Painter p, double cx, double cy, double r, double startDeg, double extent, Color c) {
        Arc2D arc = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, -startDeg, -extent, Arc2D.OPEN);
        p.stroke(arc, Pal.alpha(c, 0.25f), 4.4);
        p.stroke(arc, Pal.alpha(c, 0.6f), 1.8);
    }

    private static void bolter(Painter p, double x, double y, boolean combi) {
        armour(p, Painter.poly(x + 1, y + 3.8, x + 3.8, y + 3.8, x + 3.0, y + 7.6, x + 0.8, y + 7.2), Pal.GUNMETAL);
        p.metal(Painter.round(x, y, 16.5, 4.6, 1.1), Pal.GUNMETAL);
        armour(p, Painter.round(x + 2, y - 1.0, 10, 2.0, 0.9), Pal.darker(Pal.UM_BLUE, 0.35f));
        p.line(x + 2.5, y + 2.3, x + 13.5, y + 2.3, Pal.alpha(Pal.BRASS, 0.9f), 0.5);
        armour(p, Painter.round(x + 5, y + 4.2, 3.0, 5.2, 0.7), Pal.darker(Pal.GUNMETAL, 0.3f));
        p.metal(Painter.round(x + 16, y + 1.0, 5.2, 2.3, 0.7), Pal.IRON);
        p.metal(Painter.round(x + 20.6, y + 0.4, 2.0, 3.4, 0.6), Pal.GUNMETAL);
        p.fill(Painter.circle(x + 11, y + 2.3, 0.8), Pal.BRASS);
        if (combi) {
            p.metal(Painter.round(x + 10, y + 4.4, 8.5, 1.8, 0.6), Pal.IRON);
            p.glow(x + 14, y + 5.3, 2, Pal.LENS_GREEN, 0.6f);
            p.fill(Painter.circle(x + 14, y + 5.3, 0.55), Pal.LENS_GREEN);
        }
    }

    private static void boltPistol(Painter p, double x, double y) {
        p.metal(Painter.round(x, y, 7, 2.7, 0.8), Pal.GUNMETAL);
        armour(p, Painter.poly(x + 0.7, y + 2.5, x + 2.5, y + 2.5, x + 2.1, y + 5.2, x + 0.5, y + 5.0), Pal.darker(Pal.GUNMETAL, 0.2f));
        p.fill(Painter.circle(x + 4, y + 1.3, 0.45), Pal.BRASS);
    }

    private static void heavyBolter(Painter p, double x, double y, Pose pose) {
        Path2D belt = new Path2D.Double();
        belt.moveTo(x + 6, y + 6);
        belt.quadTo(x - 2, y + 10, x - 9, y - 6);
        p.stroke(belt, Pal.OUTLINE, 3.0);
        p.stroke(belt, Pal.BRASS, 1.8);
        p.metal(Painter.round(x, y, 19, 5.8, 1.3), Pal.GUNMETAL);
        armour(p, Painter.round(x + 2.2, y - 1.3, 11, 2, 0.8), Pal.darker(Pal.UM_BLUE, 0.35f));
        armour(p, Painter.circle(x + 6.5, y + 7, 2.9), Pal.darker(Pal.GUNMETAL, 0.25f));
        p.metal(Painter.round(x + 18.5, y + 1.3, 8.5, 3.1, 0.8), Pal.IRON);
        for (int i = 0; i < 3; i++) {
            p.fill(Painter.circle(x + 20 + i * 2.2, y + 2.85, 0.5), Pal.OUTLINE);
        }
        p.metal(Painter.round(x + 26.5, y + 0.7, 2, 4.5, 0.6), Pal.GUNMETAL);
        armour(p, Painter.round(x + 9, y - 3.6, 3.8, 2.4, 0.6), Pal.GUNMETAL);
        if (pose.attacking() && pose.frame == 2) {
            armour(p, Painter.round(x + 3, y + 6, 1.3, 0.7, 0.2), Pal.BRASS);
        }
    }

    private static void lascannon(Painter p, double x, double y, Pose pose) {
        Path2D cable = new Path2D.Double();
        cable.moveTo(x + 2, y + 4.5);
        cable.quadTo(x - 3, y + 10, x - 6, y - 5);
        p.stroke(cable, Pal.OUTLINE, 2.2);
        p.stroke(cable, Pal.IMPERIAL_RED, 1.2);
        p.metal(Painter.round(x, y, 17, 5.2, 1.3), Pal.GUNMETAL);
        armour(p, Painter.round(x + 2, y - 1.5, 9, 2.4, 0.8), Pal.darker(Pal.UM_BLUE, 0.35f));
        p.metal(Painter.round(x + 16.5, y + 1.1, 13, 2.7, 0.9), Pal.IRON);
        for (int i = 0; i < 3; i++) {
            p.metal(Painter.round(x + 18 + i * 3.7, y + 0.4, 1.4, 4, 0.4), Pal.GUNMETAL);
        }
        armour(p, Painter.round(x + 5.5, y + 5, 2.8, 3.8, 0.6), Pal.darker(Pal.GUNMETAL, 0.25f));
        boolean charged = pose.attacking() && pose.frame <= 1;
        p.glow(x + 30, y + 2.4, charged ? 8 : 3.4, Pal.LASER, charged ? 0.9f : 0.5f);
        p.fill(Painter.circle(x + 30, y + 2.4, charged ? 1.8 : 1.1), Pal.lighter(Pal.LASER, 0.5f));
    }

    private static void plasmaCannon(Painter p, double x, double y, Pose pose) {
        Path2D cable = new Path2D.Double();
        cable.moveTo(x + 2, y + 6.5);
        cable.quadTo(x - 3, y + 11, x - 6, y - 4);
        p.stroke(cable, Pal.OUTLINE, 2.4);
        p.stroke(cable, Pal.PLASMA, 1.2);
        p.metal(Painter.round(x, y, 16.5, 7.8, 2.2), Pal.GUNMETAL);
        for (int i = 0; i < 3; i++) {
            double cx = x + 4.4 + i * 4;
            p.fill(Painter.round(cx, y + 0.9, 2.4, 6, 0.8), Pal.darker(Pal.PLASMA, 0.4f));
            p.glow(cx + 1.2, y + 3.9, 3.4, Pal.PLASMA, 0.55f);
            p.fill(Painter.round(cx + 0.55, y + 1.5, 1.3, 4.8, 0.5), Pal.lighter(Pal.PLASMA, 0.4f));
        }
        p.metal(Painter.round(x + 16, y + 1.8, 6.5, 4.2, 1.3), Pal.IRON);
        boolean firing = pose.attacking() && pose.frame <= 1;
        p.glow(x + 23, y + 3.9, firing ? 9 : 3.8, Pal.PLASMA, firing ? 0.95f : 0.6f);
        p.fill(Painter.circle(x + 23, y + 3.9, firing ? 2.4 : 1.3), Color.WHITE);
    }

    private static void missileLauncher(Painter p, Pose pose, double b) {
        double r = pose.recoil() * 0.6;
        double x = -11 - r;
        double y = -37 + b;
        p.metal(Painter.round(x, y, 27, 6, 2.6), Pal.GUNMETAL);
        armour(p, Painter.round(x + 3, y - 0.9, 13, 2, 0.8), Pal.darker(Pal.UM_BLUE, 0.35f));
        p.metal(Painter.round(x + 9, y - 3.5, 4.2, 2.8, 0.6), Pal.IRON);
        p.fill(Painter.circle(x + 12, y - 2.1, 0.7), Pal.LENS_RED);
        armour(p, Painter.round(x + 14, y + 5, 2.6, 4.6, 0.6), Pal.darker(Pal.GUNMETAL, 0.25f));
        if (!(pose.attacking() && pose.frame <= 1)) {
            armour(p, Painter.poly(x + 27, y + 1, x + 31, y + 3, x + 27, y + 5), Pal.IMPERIAL_RED);
        }
        if (pose.flash()) {
            fireFlash(p, x + 29.5, y + 3, 4.6);
            p.glow(x - 2, y + 3, 8, Color.LIGHT_GRAY, 0.55f);
        }
    }

    private static void stormBolter(Painter p, double x, double y) {
        p.metal(Painter.round(x, y, 10, 2.4, 0.6), Pal.GUNMETAL);
        p.metal(Painter.round(x, y + 2.5, 10, 2.4, 0.6), Pal.GUNMETAL);
        armour(p, Painter.round(x + 1, y + 4.8, 2.2, 3.2, 0.5), Pal.darker(Pal.GUNMETAL, 0.2f));
        p.fill(Painter.circle(x + 5.5, y + 2.4, 0.55), Pal.BRASS);
    }

    private static void powerFist(Painter p, double x, double y, boolean active) {
        if (active) {
            p.glow(x + 2, y, 8, Pal.PLASMA, 0.7f);
        }
        armour(p, Painter.round(x - 3.6, y - 4, 8.2, 8, 2.4), Pal.darker(Pal.UM_BLUE, 0.3f));
        for (int i = 0; i < 3; i++) {
            armour(p, Painter.round(x + 3.8, y - 3.6 + i * 2.5, 2.7, 2.2, 0.8), Pal.GUNMETAL);
        }
        p.stroke(Painter.round(x - 3.6, y - 4, 8.2, 8, 2.4), Pal.alpha(Pal.PLASMA, active ? 0.9f : 0.35f), 0.6);
    }

    private static void chainsword(Painter p, double hx, double hy) {
        armour(p, Painter.round(hx - 2.4, hy - 1.0, 3.5, 2.0, 0.6), Pal.LEATHER);
        p.metal(Painter.round(hx + 0.9, hy - 2.0, 1.3, 4.0, 0.4), Pal.GOLD);
        p.metal(Painter.round(hx + 2.2, hy - 1.6, 12.8, 3.1, 1.4), Pal.GUNMETAL);
        for (double tx = hx + 3; tx < hx + 14.5; tx += 1.5) {
            p.fill(Painter.poly(tx, hy - 1.5, tx + 0.75, hy - 2.7, tx + 1.3, hy - 1.5), Pal.STEEL);
        }
        p.fill(Painter.round(hx + 3.2, hy - 0.45, 9.5, 0.9, 0.45), Pal.HAZARD);
    }

    private static void powerSword(Painter p, double hx, double hy) {
        armour(p, Painter.round(hx - 2.6, hy - 0.9, 3.5, 1.8, 0.6), Pal.LEATHER);
        armour(p, Painter.round(hx + 0.6, hy - 2.7, 1.5, 5.4, 0.5), Pal.GOLD);
        Path2D blade = Painter.poly(hx + 2.1, hy - 1.1, hx + 15, hy - 0.9, hx + 17.2, hy, hx + 15, hy + 0.9, hx + 2.1, hy + 1.1);
        p.glow(hx + 10, hy, 8, Pal.PLASMA, 0.5f);
        p.fill(blade, Pal.lighter(Pal.PLASMA, 0.2f));
        p.fill(Painter.poly(hx + 2.1, hy - 0.35, hx + 15.5, hy - 0.25, hx + 16.6, hy, hx + 15.5, hy + 0.25, hx + 2.1, hy + 0.35), Color.WHITE);
        p.outline(blade);
    }

    private static void crozius(Painter p, double hx, double hy) {
        p.metal(Painter.round(hx - 2.2, hy - 0.8, 13.5, 1.6, 0.6), Pal.GOLD);
        double x = hx + 11;
        p.glow(x + 1.6, hy, 5.5, Pal.PLASMA, 0.35f);
        armour(p, Painter.poly(x - 1.2, hy - 1.1, x + 3.3, hy - 5, x + 1.7, hy - 1.1, x + 4.4, hy, x + 1.7, hy + 1.1,
                x + 3.3, hy + 5, x - 1.2, hy + 1.1), Pal.GOLD);
        Emblems.skull(p, x + 1.1, hy, 1.5, Pal.IVORY);
    }

    private static void axe(Painter p, double hx, double hy) {
        p.metal(Painter.round(hx - 3.3, hy - 0.8, 17.5, 1.6, 0.6), Pal.GUNMETAL);
        double x = hx + 12.6;
        Emblems.cog(p, x, hy - 2.4, 3.5, Pal.STEEL, 8);
        armour(p, Painter.round(x - 1.1, hy - 3.1, 2.2, 1.6, 0.4), Pal.MARS_RED);
    }

    /** Big fiery muzzle blast with sparks, like a bolt round leaving the barrel. */
    private static void fireFlash(Painter p, double x, double y, double size) {
        p.glow(x + size * 0.8, y, size * 2.8, Pal.FIRE, 0.7f);
        Path2D flame = new Path2D.Double();
        flame.moveTo(x - size * 0.2, y);
        flame.quadTo(x + size * 0.6, y - size * 1.1, x + size * 1.3, y - size * 0.55);
        flame.quadTo(x + size * 1.9, y - size * 0.9, x + size * 2.8, y - size * 0.15);
        flame.lineTo(x + size * 3.2, y);
        flame.lineTo(x + size * 2.8, y + size * 0.15);
        flame.quadTo(x + size * 1.9, y + size * 0.9, x + size * 1.3, y + size * 0.55);
        flame.quadTo(x + size * 0.6, y + size * 1.1, x - size * 0.2, y);
        flame.closePath();
        p.fill(flame, Pal.alpha(new Color(255, 120, 20), 0.95f));
        p.fill(Painter.ellipse(x + size * 1.0, y, size * 1.1, size * 0.45), new Color(255, 214, 90));
        p.fill(Painter.ellipse(x + size * 0.6, y, size * 0.55, size * 0.25), Color.WHITE);
        for (int i = 0; i < 5; i++) {
            double a = -0.5 + i * 0.25;
            double d = size * (2.0 + (i % 2) * 0.8);
            p.fill(Painter.circle(x + Math.cos(a) * d, y + Math.sin(a) * d, size * 0.09), new Color(255, 200, 80));
        }
    }

    /** Kept for other art modules (towers, icons). */
    public static void muzzleFlash(Painter p, double x, double y, double size, Color c) {
        fireFlash(p, x, y, size);
    }
}
