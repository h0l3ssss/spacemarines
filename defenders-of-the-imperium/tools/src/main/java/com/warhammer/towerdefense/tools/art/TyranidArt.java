package com.warhammer.towerdefense.tools.art;

import java.awt.Color;
import java.awt.geom.Path2D;

/**
 * Tyranid bio-forms (3/4 side view, facing right): bone-white flesh under purple chitin, glowing red eyes.
 * Every creature has a distinct silhouette so it reads at a glance: gun-toting termagants, blade-armed
 * hormagaunts, winged gargoyles, crested warriors, long-skulled genestealers, the hulking carnifex and the
 * winged Hive Tyrant.
 */
public final class TyranidArt {
    private static final Color CARA = Pal.TYR_CARAPACE;
    private static final Color CARA_D = Pal.TYR_CARAPACE_DARK;
    private static final Color FLESH = Pal.TYR_FLESH;
    private static final Color FLESH_D = Pal.TYR_FLESH_DARK;

    private TyranidArt() {
    }

    // ------------------------------------------------------------------ shared parts

    /** Reverse-jointed leg: hip -> knee (forward) -> hock (back) -> toes. */
    static void leg(Painter p, double hx, double hy, double fx, double fy, double thick, Color flesh, Color armour) {
        double kx = hx + thick * 1.1;
        double ky = hy + (fy - hy) * 0.38;
        double jx = fx - thick * 1.05;
        double jy = fy - thick * 1.1;
        p.volume(Painter.limb(hx, hy, thick * 1.25, kx, ky, thick), armour);
        p.volume(Painter.limb(kx, ky, thick * 0.95, jx, jy, thick * 0.65), flesh);
        p.volume(Painter.limb(jx, jy, thick * 0.6, fx, fy - thick * 0.25, thick * 0.5), flesh);
        p.volume(Painter.talon(fx - thick * 0.2, fy - thick * 0.2, thick * 1.1, thick * 0.35, 10, 0.3), Pal.TYR_CLAW);
        p.volume(Painter.talon(fx - thick * 0.5, fy - thick * 0.25, thick * 0.9, thick * 0.3, 40, 0.3), Pal.TYR_CLAW);
    }

    /** Segmented chitin plates along a curve from (x0,y0) to (x1,y1), bulging upwards. */
    static void plates(Painter p, double x0, double y0, double x1, double y1, int count, double height, Color c) {
        for (int i = count - 1; i >= 0; i--) {
            double t0 = i / (double) count;
            double t1 = (i + 1.25) / count;
            double ax = x0 + (x1 - x0) * t0;
            double ay = y0 + (y1 - y0) * t0;
            double bx = x0 + (x1 - x0) * Math.min(1, t1);
            double by = y0 + (y1 - y0) * Math.min(1, t1);
            double mx = (ax + bx) / 2;
            double my = (ay + by) / 2 - height;
            Path2D plate = new Path2D.Double();
            plate.moveTo(ax, ay + height * 0.25);
            plate.quadTo(ax - height * 0.2, my, mx, my);
            plate.quadTo(bx + height * 0.3, my + height * 0.1, bx, by + height * 0.3);
            plate.closePath();
            p.volume(plate, i % 2 == 0 ? c : Pal.lighter(c, 0.06f));
        }
    }

    static void tail(Painter p, double bx, double by, double length, double thick, double sway, Color flesh, Color armour) {
        Path2D t = new Path2D.Double();
        double tipX = bx - length;
        double tipY = by + length * 0.1 + sway;
        t.moveTo(bx, by - thick);
        t.quadTo(bx - length * 0.5, by - thick * 0.8 + sway * 0.5, tipX, tipY);
        t.quadTo(bx - length * 0.5, by + thick * 0.9 + sway * 0.5, bx, by + thick);
        t.closePath();
        p.volume(t, flesh);
        plates(p, bx, by - thick * 0.6, tipX + length * 0.2, tipY - thick * 0.3 + sway * 0.2, 3, thick * 0.6, armour);
        p.volume(Painter.talon(tipX + 1, tipY, thick * 1.8, thick * 0.9, 190, -0.8), armour);
    }

    /** Gaunt-type head facing right; returns nothing, draws crest, face, eye and teeth. */
    static void gauntHead(Painter p, double x, double y, double s, double mouth) {
        Path2D crest = new Path2D.Double();
        crest.moveTo(x - 3.2 * s, y - 0.5 * s);
        crest.quadTo(x - 5.5 * s, y - 3.8 * s, x - 7.5 * s, y - 3.2 * s);
        crest.quadTo(x - 3 * s, y - 4.6 * s, x + 2.5 * s, y - 2.6 * s);
        crest.quadTo(x + 0.5 * s, y - 0.6 * s, x - 3.2 * s, y - 0.5 * s);
        p.volume(crest, CARA);
        Path2D face = Painter.blob(x - 3 * s, y - 1.2 * s, x + 1.5 * s, y - 2.6 * s, x + 4.8 * s, y - 1.0 * s,
                x + 5.6 * s, y + 0.8 * s, x + 2.8 * s, y + 1.9 * s, x - 1.8 * s, y + 1.8 * s);
        p.volume(face, FLESH);
        Path2D jaw = Painter.poly(x + 0.5 * s, y + 0.9 * s, x + 5.2 * s, y + 0.8 * s + mouth * s,
                x + 3.8 * s, y + 2.4 * s + mouth * s, x + 0.2 * s, y + 2.3 * s);
        p.volume(jaw, FLESH_D);
        p.fill(Painter.poly(x + 1.2 * s, y + 1.0 * s, x + 5.0 * s, y + 0.9 * s + mouth * s * 0.6,
                x + 1.5 * s, y + 1.8 * s), Pal.TYR_MAW);
        for (int i = 0; i < 3; i++) {
            double tx = x + (2 + i * 1.0) * s;
            p.fill(Painter.poly(tx, y + 0.9 * s, tx + 0.35 * s, y + 1.6 * s, tx + 0.7 * s, y + 0.9 * s), Pal.TYR_CLAW);
        }
        eye(p, x + 1.9 * s, y - 0.7 * s, 0.75 * s);
    }

    static void eye(Painter p, double x, double y, double r) {
        p.glow(x, y, r * 3.2, Pal.TYR_EYE, 0.55f);
        p.fill(Painter.ellipse(x, y, r * 1.1, r * 0.8), Pal.TYR_EYE);
        p.spec(x + r * 0.3, y - r * 0.2, r * 0.3);
    }

    /** Scything talon: chitin sheath at the base, long curved bone blade. */
    static void scythe(Painter p, double x, double y, double length, double width, double angle, boolean far) {
        Color blade = far ? Pal.darker(Pal.TYR_CLAW, 0.2f) : Pal.TYR_CLAW;
        p.volume(Painter.talon(x, y, length, width, angle, width * 1.3), blade);
        double a = Math.toRadians(angle);
        p.volume(Painter.ellipse(x + Math.cos(a) * width * 0.6, y + Math.sin(a) * width * 0.6, width * 0.95, width * 0.8),
                far ? CARA_D : CARA);
    }

    /** Organic gun: bulbous tube with a dark orifice at the front. */
    static void bioGun(Painter p, double x, double y, double length, double thick, boolean flash) {
        Path2D gun = Painter.blob(x, y - thick * 0.4, x + length * 0.35, y - thick * 0.9, x + length * 0.8, y - thick * 0.6,
                x + length, y - thick * 0.3, x + length, y + thick * 0.3, x + length * 0.7, y + thick * 0.7,
                x + length * 0.2, y + thick * 0.8);
        p.volume(gun, FLESH_D);
        plates(p, x + length * 0.1, y - thick * 0.45, x + length * 0.85, y - thick * 0.45, 3, thick * 0.35, CARA);
        p.fill(Painter.ellipse(x + length - thick * 0.15, y, thick * 0.28, thick * 0.35), Pal.OUTLINE);
        if (flash) {
            p.glow(x + length + thick * 0.8, y, thick * 2.4, Pal.TYR_VENOM, 0.8f);
            p.fill(Emblems.star(x + length + thick * 0.6, y, thick * 1.2, thick * 0.45, 5), Pal.lighter(Pal.TYR_VENOM, 0.4f));
        }
    }

    static void wing(Painter p, double sx, double sy, double span, double angle, boolean far) {
        Color bone = far ? CARA_D : CARA;
        Color membrane = far ? Pal.alpha(Pal.darker(Pal.TYR_MAW, 0.45f), 0.85f) : Pal.alpha(Pal.darker(Pal.TYR_MAW, 0.2f), 0.88f);
        double a = Math.toRadians(angle);
        double[] fingers = {-0.55, -0.1, 0.35};
        double[] lens = {1.0, 0.85, 0.62};
        double ex = sx + Math.cos(a) * span * 0.45;
        double ey = sy + Math.sin(a) * span * 0.45;
        Path2D m = new Path2D.Double();
        m.moveTo(sx, sy);
        double[] tips = new double[6];
        for (int i = 0; i < 3; i++) {
            double fa = a + fingers[i];
            tips[i * 2] = ex + Math.cos(fa) * span * lens[i] * 0.6;
            tips[i * 2 + 1] = ey + Math.sin(fa) * span * lens[i] * 0.6;
        }
        m.lineTo(ex, ey);
        m.lineTo(tips[0], tips[1]);
        m.quadTo((tips[0] + tips[2]) / 2 + 1, (tips[1] + tips[3]) / 2 + 2, tips[2], tips[3]);
        m.quadTo((tips[2] + tips[4]) / 2 + 1, (tips[3] + tips[5]) / 2 + 2, tips[4], tips[5]);
        m.quadTo((tips[4] + sx) / 2 + 2, (tips[5] + sy) / 2 + 3, sx + 1, sy + 2);
        m.closePath();
        p.fill(m, membrane);
        p.outline(m);
        p.volume(Painter.limb(sx, sy, span * 0.09, ex, ey, span * 0.07), bone);
        for (int i = 0; i < 3; i++) {
            p.volume(Painter.limb(ex, ey, span * 0.06, tips[i * 2], tips[i * 2 + 1], span * 0.025), bone);
        }
    }

    /** Death: the creature slumps and flattens onto the ground (stays inside its frame). */
    private static void collapse(Painter p, Pose pose, double lean) {
        float d = pose.death();
        if (d > 0f) {
            p.rotate(lean * d, 0, 0);
            p.scale(1 + 0.12 * d, 1 - 0.55 * d);
        }
    }

    // ------------------------------------------------------------------ creatures

    public static void termagant(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -18);
        p.scale(1.3, 1.3);
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride();
        double bob = pose.bob() * 1.2;
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 2.4 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 2.4 : 0;
        double sway = pose.walking() ? Math.sin(ph) * 1.6 : pose.frame * 0.8;
        double recoil = pose.attacking() ? pose.recoil() * 0.6 : 0;

        tail(p, -4, -12 + bob, 13, 2.2, sway, FLESH, CARA);
        leg(p, 0, -11 + bob, 1 - stride * 4.5, -lift2, 2.4, FLESH_D, CARA_D);
        p.volume(Painter.blob(-6.5, -13 + bob, -2, -17 + bob, 5, -17.5 + bob, 8.5, -14 + bob, 6, -9.5 + bob,
                -1, -9 + bob, -6, -10 + bob), FLESH);
        plates(p, -6.5, -14.5 + bob, 7.5, -16.5 + bob, 4, 2.4, CARA);
        leg(p, -2, -11 + bob, -1 + stride * 4.5, -lift1, 2.5, FLESH, CARA);
        bioGun(p, 2.5 - recoil, -11 + bob, 10, 3.0, pose.flash());
        p.volume(Painter.limb(4.5, -13.5 + bob, 1.8, 6.5 - recoil, -11 + bob, 1.4), FLESH);
        p.volume(Painter.circle(6.5 - recoil, -11 + bob, 1.1), FLESH_D);
        gauntHead(p, 9.5, -17.2 + bob, 1.0, pose.attacking() ? 0.8 : 0.2);
        p.pop();
    }

    public static void hormagaunt(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -18);
        p.scale(1.3, 1.3);
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride() * 1.25;
        double bob = pose.walking() ? -Math.abs(Math.sin(ph)) * 3.2 : pose.bob();
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 3 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 3 : 0;
        double sway = pose.walking() ? Math.sin(ph) * 2 : pose.frame;
        double swing = pose.swing();
        double farAngle = pose.attacking() ? -100 + (swing + 0.6) / 1.6 * 125 : -70 + sway * 3;
        double nearAngle = pose.attacking() ? -120 + (swing + 0.6) / 1.6 * 140 : -58 - sway * 3;

        tail(p, -4, -13 + bob, 14, 2.2, sway, FLESH, CARA);
        scythe(p, 6, -17 + bob, 12, 2.6, farAngle, true);
        leg(p, 0, -12 + bob, 1 - stride * 5, -lift2, 2.5, FLESH_D, CARA_D);
        p.volume(Painter.blob(-6, -14 + bob, -2, -18 + bob, 5, -19 + bob, 8.5, -15.5 + bob, 6, -10.5 + bob,
                -1, -10 + bob, -5.5, -11 + bob), FLESH);
        plates(p, -6, -15.5 + bob, 7.5, -18 + bob, 4, 2.6, CARA);
        leg(p, -2, -12 + bob, -1 + stride * 5, -lift1, 2.6, FLESH, CARA);
        gauntHead(p, 9.5, -18.6 + bob, 1.0, pose.attacking() ? 1.2 : 0.3);
        scythe(p, 4.5, -16 + bob, 13, 2.8, nearAngle, false);
        p.pop();
    }

    public static void gargoyle(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -15);
        p.scale(1.3, 1.3);
        double flap = pose.dying() ? -0.8 : pose.wing();
        double bob = flap * 1.5;
        wing(p, -1, -19 + bob, 25, -150 + flap * 35, true);
        tail(p, -4, -12 + bob, 10, 1.6, flap * 1.5, FLESH, CARA);
        p.volume(Painter.limb(-1, -11 + bob, 2.2, -2.5, -4 + bob, 1.6), FLESH_D);
        p.volume(Painter.limb(1, -11 + bob, 2.2, 0.5, -3.5 + bob, 1.6), FLESH);
        p.volume(Painter.blob(-5, -13 + bob, -1, -17 + bob, 4.5, -17 + bob, 7.5, -14 + bob, 5, -10 + bob,
                -1, -9 + bob, -5, -10 + bob), FLESH);
        plates(p, -5, -14 + bob, 6.5, -16 + bob, 3, 2.2, CARA);
        bioGun(p, 2, -10.5 + bob, 7.5, 2.4, pose.flash());
        gauntHead(p, 8.5, -16.5 + bob, 0.9, pose.attacking() ? 0.9 : 0.3);
        wing(p, 0, -18 + bob, 27, -130 + flap * 40, false);
        p.pop();
    }

    public static void warrior(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -20);
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride();
        double bob = pose.bob() * 1.4;
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 3 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 3 : 0;
        double sway = pose.walking() ? Math.sin(ph) * 2 : pose.frame;
        double swing = pose.swing();
        double sword = pose.attacking() ? -130 + (swing + 0.6) / 1.6 * 160 : -75 + sway * 2;

        tail(p, -5, -22 + bob, 20, 3.4, sway * 1.5, FLESH, CARA);
        scythe(p, 5, -40 + bob, 20, 4.2, sword + 12, true);
        leg(p, 2, -22 + bob, 3 - stride * 6, -lift2, 4.2, FLESH_D, CARA_D);
        Path2D body = Painter.blob(-6, -22 + bob, -6.5, -32 + bob, -3, -41 + bob, 5, -43 + bob, 9, -37 + bob,
                8, -28 + bob, 5, -20 + bob, -1, -19 + bob);
        p.volume(body, FLESH);
        for (int i = 0; i < 4; i++) {
            double yy = -24 - i * 3.6 + bob;
            p.stroke(new java.awt.geom.Arc2D.Double(1, yy - 1.6, 7, 3.2, 200, 140, java.awt.geom.Arc2D.OPEN),
                    Pal.alpha(FLESH_D, 0.9f), 0.8);
        }
        plates(p, -6.5, -24 + bob, 2, -42 + bob, 4, 3.4, CARA);
        leg(p, -2, -22 + bob, -1 + stride * 6, -lift1, 4.4, FLESH, CARA);
        bioGun(p, 3 - pose.recoil(), -25 + bob, 15, 5.2, pose.flash() && pose.frame == 0);
        p.volume(Painter.limb(4, -30 + bob, 2.6, 7 - pose.recoil(), -25.5 + bob, 2.2), FLESH);
        // head with the iconic swept-back crest
        double hx = 7.5;
        double hy = -45 + bob;
        Path2D crest = new Path2D.Double();
        crest.moveTo(hx - 2, hy + 1);
        crest.quadTo(hx - 7, hy - 6, hx - 13, hy - 9);
        crest.quadTo(hx - 6, hy - 9, hx + 2, hy - 3);
        crest.closePath();
        p.volume(crest, CARA);
        gauntHead(p, hx + 1, hy, 1.35, pose.attacking() ? 1.0 : 0.3);
        scythe(p, 3, -38 + bob, 22, 4.4, sword, false);
        p.glow(3 + Math.cos(Math.toRadians(sword)) * 12, -38 + bob + Math.sin(Math.toRadians(sword)) * 12, 5, Pal.TYR_VENOM, 0.25f);
        p.pop();
    }

    public static void genestealer(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -18);
        p.scale(1.1, 1.1);
        Color skin = Pal.GENESTEALER_SKIN;
        Color skinD = Pal.GENESTEALER_DARK;
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride() * 1.2;
        double bob = pose.walking() ? -Math.abs(Math.sin(ph)) * 2.2 : pose.bob();
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 2.6 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 2.6 : 0;
        double swing = pose.swing();
        double reach = pose.attacking() ? swing * 6 : pose.walking() ? pose.stride() * 2 : 0;

        claws(p, 5, -20 + bob, 9 + reach, -8 - reach * 2, skinD, true);
        leg(p, 1, -12 + bob, 2 - stride * 5, -lift2, 3.0, skinD, CARA_D);
        Path2D body = Painter.blob(-7, -13 + bob, -5, -19 + bob, 1, -23 + bob, 7, -22 + bob, 9, -17 + bob,
                5, -11 + bob, -2, -10 + bob);
        p.volume(body, skin);
        plates(p, -7, -15 + bob, 5, -22.5 + bob, 4, 3.0, Pal.darker(CARA, 0.25f));
        leg(p, -2, -12 + bob, -1 + stride * 5, -lift1, 3.1, skin, Pal.darker(CARA, 0.2f));
        // long domed genestealer skull, sweeping back
        double hx = 9;
        double hy = -22 + bob;
        Path2D skull = Painter.blob(hx - 9, hy - 5, hx - 3, hy - 6.5, hx + 3, hy - 4, hx + 5, hy - 0.5, hx + 3, hy + 2.5,
                hx - 2, hy + 1.5, hx - 7, hy - 1.5);
        p.volume(skull, skin);
        p.stroke(new java.awt.geom.Arc2D.Double(hx - 8, hy - 6, 9, 6, 90, 110, java.awt.geom.Arc2D.OPEN),
                Pal.alpha(skinD, 0.9f), 0.7);
        p.volume(Painter.poly(hx + 1, hy + 0.5, hx + 5.5, hy + 0.5 + (pose.attacking() ? 1 : 0), hx + 3.5, hy + 3,
                hx + 0.5, hy + 2.6), Pal.GENESTEALER_DARK);
        for (int i = 0; i < 3; i++) {
            double tx = hx + 1.6 + i * 1.1;
            p.fill(Painter.poly(tx, hy + 0.6, tx + 0.4, hy + 1.6, tx + 0.8, hy + 0.6), Pal.TYR_CLAW);
        }
        eye(p, hx + 2.2, hy - 1.8, 0.8);
        claws(p, 3.5, -18 + bob, 11 + reach, -3 - reach, skin, false);
        p.pop();
    }

    private static void claws(Painter p, double sx, double sy, double reachX, double reachY, Color skin, boolean far) {
        double hx = sx + reachX;
        double hy = sy + 8 + reachY * 0.3;
        p.volume(Painter.limb(sx, sy, 2.6, (sx + hx) / 2 + 1, (sy + hy) / 2 + 1, 2.2), skin);
        p.volume(Painter.limb((sx + hx) / 2 + 1, (sy + hy) / 2 + 1, 2.2, hx, hy, 1.8), skin);
        Color claw = far ? Pal.darker(Pal.TYR_CLAW, 0.2f) : Pal.TYR_CLAW;
        for (int i = 0; i < 3; i++) {
            p.volume(Painter.talon(hx, hy, 6.5 - i * 0.8, 1.1, 10 + i * 25, 1.2), claw);
        }
    }

    public static void carnifex(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -10);
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride() * 0.8;
        double bob = pose.walking() ? -Math.abs(Math.sin(ph)) * 1.8 : pose.bob();
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 3 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 3 : 0;
        double swing = pose.swing();
        double upper = pose.attacking() ? -120 + (swing + 0.6) / 1.6 * 150 : -62 + pose.stride() * 4;
        double lower = pose.attacking() ? -60 + (swing + 0.6) / 1.6 * 90 : 12 - pose.stride() * 4;

        tail(p, -18, -30 + bob, 26, 6, pose.stride() * 3, FLESH, CARA);
        scythe(p, 16, -46 + bob, 30, 5.5, upper + 10, true);
        leg(p, 6, -26 + bob, 8 - stride * 7, -lift2, 8, FLESH_D, CARA_D);
        Path2D body = Painter.blob(-24, -30 + bob, -18, -52 + bob, 0, -62 + bob, 18, -58 + bob, 26, -44 + bob,
                22, -26 + bob, 6, -18 + bob, -14, -20 + bob);
        p.volume(body, FLESH);
        for (int i = 0; i < 5; i++) {
            double yy = -24 - i * 4.4 + bob;
            p.stroke(new java.awt.geom.Arc2D.Double(8, yy - 2, 14, 4.4, 200, 140, java.awt.geom.Arc2D.OPEN),
                    Pal.alpha(FLESH_D, 0.9f), 1.0);
        }
        plates(p, -24, -34 + bob, 18, -60 + bob, 6, 8, CARA);
        for (int i = 0; i < 4; i++) {
            double sx = -14 + i * 8;
            double sy = -52 - i * 2.5 + bob;
            p.volume(Painter.talon(sx, sy, 7 + i, 2.6, -110 + i * 8, -1.5), Pal.TYR_CLAW);
        }
        leg(p, -6, -26 + bob, -4 + stride * 7, -lift1, 8.4, FLESH, CARA);
        scythe(p, 18, -34 + bob, 26, 5.2, lower, true);
        // armoured head, low in front, with tusks
        double hx = 30;
        double hy = -40 + bob;
        p.volume(Painter.blob(hx - 8, hy - 4, hx - 2, hy - 9, hx + 6, hy - 6, hx + 8, hy, hx + 4, hy + 5, hx - 5, hy + 4),
                CARA);
        p.volume(Painter.blob(hx - 2, hy - 2, hx + 6, hy - 1, hx + 9, hy + 2, hx + 6, hy + 6, hx - 1, hy + 5), FLESH);
        p.volume(Painter.talon(hx + 5, hy + 4, 8, 2.2, -30, -1.5), Pal.TYR_CLAW);
        p.fill(Painter.poly(hx + 1, hy + 3, hx + 8, hy + 3.5 + (pose.attacking() ? 2 : 0), hx + 2, hy + 5.5), Pal.TYR_MAW);
        eye(p, hx + 3.5, hy - 1.5, 1.2);
        scythe(p, 20, -44 + bob, 32, 6, upper, false);
        p.pop();
    }

    public static void hiveTyrant(Painter p, Pose pose) {
        p.push();
        collapse(p, pose, -14);
        double ph = pose.t * Math.PI * 2;
        double stride = pose.stride();
        double bob = pose.bob() * 2;
        double lift1 = pose.walking() ? Math.max(0, Math.cos(ph)) * 4 : 0;
        double lift2 = pose.walking() ? Math.max(0, -Math.cos(ph)) * 4 : 0;
        double sway = pose.walking() ? Math.sin(ph) * 3 : pose.frame * 1.5;
        double swing = pose.swing();
        double sword = pose.attacking() ? -140 + (swing + 0.6) / 1.6 * 170 : -80 + sway;
        double wingBeat = pose.dying() ? 20 : pose.wing() * 6;

        p.glow(4, -80 + bob, 38, Pal.WARP, pose.attacking() ? 0.35f : 0.18f);
        wing(p, -6, -78 + bob, 62, -150 + wingBeat, true);
        tail(p, -10, -44 + bob, 38, 6.5, sway * 2, FLESH, CARA);
        scythe(p, 8, -82 + bob, 32, 5.4, sword + 14, true);
        leg(p, 4, -44 + bob, 6 - stride * 9, -lift2, 9.5, FLESH_D, CARA_D);
        Path2D body = Painter.blob(-12, -44 + bob, -14, -64 + bob, -8, -84 + bob, 6, -90 + bob, 16, -80 + bob,
                16, -62 + bob, 10, -42 + bob, -2, -38 + bob);
        p.volume(body, FLESH);
        for (int i = 0; i < 6; i++) {
            double yy = -48 - i * 5 + bob;
            p.stroke(new java.awt.geom.Arc2D.Double(2, yy - 2.4, 13, 4.8, 200, 140, java.awt.geom.Arc2D.OPEN),
                    Pal.alpha(FLESH_D, 0.9f), 1.1);
        }
        plates(p, -14, -48 + bob, 4, -88 + bob, 6, 7, CARA);
        leg(p, -4, -44 + bob, -2 + stride * 9, -lift1, 10, FLESH, CARA);
        // venom cannon held low
        bioGun(p, 6 - pose.recoil(), -54 + bob, 26, 9, pose.flash() && pose.frame == 0);
        p.volume(Painter.limb(8, -66 + bob, 4.6, 12 - pose.recoil(), -55 + bob, 3.8), FLESH);
        // head and crown
        double hx = 14;
        double hy = -92 + bob;
        for (int i = 0; i < 5; i++) {
            double a = -160 + i * 22;
            p.volume(Painter.talon(hx - 3, hy - 2, 14 - Math.abs(i - 2) * 2, 3.4, a, -1.4), CARA);
        }
        Path2D crest = new Path2D.Double();
        crest.moveTo(hx - 4, hy + 2);
        crest.quadTo(hx - 12, hy - 8, hx - 22, hy - 10);
        crest.quadTo(hx - 10, hy - 14, hx + 3, hy - 5);
        crest.closePath();
        p.volume(crest, CARA);
        gauntHead(p, hx + 2, hy, 2.4, pose.attacking() ? 1.1 : 0.4);
        p.glow(hx + 6.5, hy - 1.6, 9, Pal.WARP, 0.5f);
        scythe(p, 6, -80 + bob, 36, 6.2, sword, false);
        p.glow(6 + Math.cos(Math.toRadians(sword)) * 22, -80 + bob + Math.sin(Math.toRadians(sword)) * 22, 9, Pal.WARP, 0.3f);
        wing(p, -2, -80 + bob, 66, -118 + wingBeat, false);
        p.pop();
    }
}
