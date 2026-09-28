package com.warhammer.towerdefense.tools.art;

import java.awt.Color;

/** Colour palette shared by every generated asset, plus colour math helpers. */
public final class Pal {
    // outlines
    public static final Color OUTLINE = new Color(14, 14, 22);
    public static final Color OUTLINE_WARM = new Color(28, 16, 20);

    // Ultramarines
    public static final Color UM_BLUE = new Color(36, 66, 160);
    public static final Color UM_BLUE_DARK = new Color(20, 38, 100);
    public static final Color UM_BLUE_LIGHT = new Color(84, 124, 214);
    public static final Color GOLD = new Color(226, 180, 66);
    public static final Color GOLD_DARK = new Color(150, 110, 36);
    public static final Color IVORY = new Color(236, 232, 214);
    public static final Color IMPERIAL_RED = new Color(186, 36, 40);
    public static final Color GUNMETAL = new Color(62, 66, 76);
    public static final Color IRON = new Color(98, 102, 112);
    public static final Color STEEL = new Color(150, 156, 168);
    public static final Color BLACK_ARMOR = new Color(36, 36, 44);
    public static final Color MARS_RED = new Color(158, 38, 32);
    public static final Color LENS_RED = new Color(255, 60, 50);
    public static final Color LENS_GREEN = new Color(90, 255, 120);
    public static final Color BONE = new Color(222, 210, 180);
    public static final Color LEATHER = new Color(94, 58, 34);
    public static final Color BRASS = new Color(196, 150, 70);

    // Tyranids (Leviathan-inspired: bone flesh, purple carapace)
    public static final Color TYR_CARAPACE = new Color(96, 40, 118);
    public static final Color TYR_CARAPACE_DARK = new Color(58, 22, 72);
    public static final Color TYR_FLESH = new Color(226, 210, 178);
    public static final Color TYR_FLESH_DARK = new Color(170, 142, 112);
    public static final Color TYR_MAW = new Color(190, 58, 84);
    public static final Color TYR_CLAW = new Color(244, 236, 214);
    public static final Color TYR_EYE = new Color(255, 70, 40);
    public static final Color TYR_VENOM = new Color(150, 230, 60);
    public static final Color GENESTEALER_SKIN = new Color(78, 84, 170);
    public static final Color GENESTEALER_DARK = new Color(46, 44, 110);

    // environment
    public static final Color SANDBAG = new Color(156, 132, 92);
    public static final Color CONCRETE = new Color(142, 140, 132);
    public static final Color CONCRETE_DARK = new Color(96, 94, 90);
    public static final Color STONE = new Color(170, 162, 146);
    public static final Color STONE_DARK = new Color(112, 104, 94);
    public static final Color HAZARD = new Color(230, 186, 40);

    // effects
    public static final Color BOLT_CORE = new Color(255, 244, 190);
    public static final Color BOLT_TRAIL = new Color(255, 160, 40);
    public static final Color PLASMA = new Color(120, 210, 255);
    public static final Color LASER = new Color(255, 80, 70);
    public static final Color FIRE = new Color(255, 140, 30);
    public static final Color WARP = new Color(170, 80, 255);
    public static final Color NECRON = new Color(80, 255, 120);

    private Pal() {
    }

    public static Color rgb(int r, int g, int b) {
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    public static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(Math.round(a * 255)));
    }

    public static Color lighter(Color c, float amount) {
        return mix(c, Color.WHITE, amount);
    }

    public static Color darker(Color c, float amount) {
        return mix(c, Color.BLACK, amount);
    }

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                clamp(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t)),
                clamp(Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t)),
                clamp(Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t)),
                clamp(Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
