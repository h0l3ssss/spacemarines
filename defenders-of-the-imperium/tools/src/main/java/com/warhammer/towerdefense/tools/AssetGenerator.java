package com.warhammer.towerdefense.tools;

import com.warhammer.towerdefense.data.FileSystemDataSource;
import com.warhammer.towerdefense.data.GameData;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.tools.art.EffectArt;
import com.warhammer.towerdefense.tools.art.Emblems;
import com.warhammer.towerdefense.tools.art.Manifest;
import com.warhammer.towerdefense.tools.art.MapPainter;
import com.warhammer.towerdefense.tools.art.MarineArt;
import com.warhammer.towerdefense.tools.art.MenuArt;
import com.warhammer.towerdefense.tools.art.Pal;
import com.warhammer.towerdefense.tools.art.SpriteWriter;
import com.warhammer.towerdefense.tools.art.TowerArt;
import com.warhammer.towerdefense.tools.art.TyranidArt;
import com.warhammer.towerdefense.tools.art.UiArt;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Offline asset pipeline: renders placeholder sprite sheets, UI and map backgrounds into /assets and writes the
 * manifests the game reads. Run with {@code gradlew generateAssets} ({@code -Ponly=sprites,ui,maps}).
 * Custom art lives in {@code assets/sprites/custom.json}, which this tool never touches.
 */
public final class AssetGenerator {
    private static final String TOWERS = "sprites/towers";
    private static final String FX = "sprites/effects";
    private static final String ICONS = "ui/icons";

    private AssetGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path assets = Paths.get(args.length > 0 ? args[0] : "assets").toAbsolutePath().normalize();
        Set<String> only = new HashSet<>();
        String filter = System.getProperty("generator.only", "");
        if (!filter.isBlank()) {
            only.addAll(Arrays.asList(filter.toLowerCase(Locale.ROOT).split(",")));
        }
        long start = System.currentTimeMillis();
        int files = 0;
        if (wants(only, "sprites")) {
            Manifest sprites = new Manifest();
            SpriteWriter w = new SpriteWriter(assets, sprites);
            generateMarines(w);
            generateTyranids(w);
            generateTowers(w);
            generateEffects(w);
            sprites.write(assets.resolve("sprites/sprites.json"));
            files += w.written();
        }
        if (wants(only, "ui")) {
            Manifest ui = new Manifest();
            SpriteWriter w = new SpriteWriter(assets, ui);
            generateUi(w);
            ui.write(assets.resolve("ui/ui.json"));
            files += w.written();
        }
        if (wants(only, "maps")) {
            GameData data = GameData.load(new FileSystemDataSource(assets));
            for (MapDefinition map : data.maps) {
                if (map.available) {
                    SpriteWriter.writeJpg(new MapPainter(map, 1.5f).paint(), assets.resolve(map.background), 0.9f);
                    files++;
                }
            }
            SpriteWriter.writeJpg(MenuArt.background(1920, 1080), assets.resolve("ui/menu_background.jpg"), 0.9f);
            files++;
        }
        System.out.printf(Locale.ROOT, "Generated %d files into %s in %d ms%n", files, assets,
                System.currentTimeMillis() - start);
    }

    private static boolean wants(Set<String> only, String group) {
        return only.isEmpty() || only.contains(group);
    }

    static void generateMarines(SpriteWriter w) {
        String dir = "sprites/marines";
        w.unitSheet("marine_tactical", dir, 152, 112, 16, (p, pose) -> MarineArt.draw(p, pose, MarineArt.tactical()));
        w.unitSheet("marine_sternguard", dir, 152, 112, 16, (p, pose) -> MarineArt.draw(p, pose, MarineArt.sternguard()));
        w.unitSheet("marine_assault", dir, 128, 112, 16, (p, pose) -> MarineArt.draw(p, pose, MarineArt.assault()));
        w.unitSheet("marine_veteran", dir, 128, 112, 16, (p, pose) -> MarineArt.draw(p, pose, MarineArt.veteran()));
        w.unitSheet("marine_terminator", dir, 144, 136, 18, (p, pose) -> MarineArt.draw(p, pose, MarineArt.terminator()));
        w.unitSheet("marine_captain", dir, 144, 136, 18, (p, pose) -> MarineArt.draw(p, pose, MarineArt.captain()));
        w.unitSheet("marine_chaplain", dir, 128, 112, 16, (p, pose) -> MarineArt.draw(p, pose, MarineArt.chaplain()));
        w.unitSheet("marine_techmarine", dir, 144, 136, 18, (p, pose) -> MarineArt.draw(p, pose, MarineArt.techmarine()));
        w.unitSheet("marine_heavybolter", dir, 176, 112, 16,
                (p, pose) -> MarineArt.draw(p, pose, MarineArt.devastator(MarineArt.Weapon.HEAVY_BOLTER)));
        w.unitSheet("marine_devastator_missile", dir, 176, 120, 16,
                (p, pose) -> MarineArt.draw(p, pose, MarineArt.devastator(MarineArt.Weapon.MISSILE_LAUNCHER)));
        w.unitSheet("marine_devastator_lascannon", dir, 176, 112, 16,
                (p, pose) -> MarineArt.draw(p, pose, MarineArt.devastator(MarineArt.Weapon.LASCANNON)));
        w.unitSheet("marine_devastator_plasma", dir, 160, 112, 16,
                (p, pose) -> MarineArt.draw(p, pose, MarineArt.devastator(MarineArt.Weapon.PLASMA_CANNON)));
    }

    static void generateTyranids(SpriteWriter w) {
        String dir = "sprites/tyranids";
        w.unitSheet("tyranid_termagant", dir, 100, 84, 12, TyranidArt::termagant);
        w.unitSheet("tyranid_hormagaunt", dir, 108, 104, 12, TyranidArt::hormagaunt);
        w.unitSheet("tyranid_gargoyle", dir, 140, 124, 12, TyranidArt::gargoyle);
        w.unitSheet("tyranid_warrior", dir, 136, 136, 14, TyranidArt::warrior);
        w.unitSheet("tyranid_genestealer", dir, 104, 92, 12, TyranidArt::genestealer);
        w.unitSheet("tyranid_carnifex", dir, 216, 184, 16, TyranidArt::carnifex);
        w.unitSheet("tyranid_hivetyrant", dir, 320, 320, 24, TyranidArt::hiveTyrant);
    }

    static void generateTowers(SpriteWriter w) {
        int[] levels = {1, 2, 3, 'a', 'b', 'c'};
        for (int level : levels) {
            String suffix = level <= 3 ? String.valueOf(level) : String.valueOf((char) level);
            w.image("tower_tactical_" + suffix, TOWERS, 280, 240, 140, 36, p -> TowerArt.tactical(p, level));
            w.image("tower_assault_" + suffix, TOWERS, 280, 240, 140, 36, p -> TowerArt.assault(p, level));
            w.image("tower_devastator_" + suffix, TOWERS, 280, 240, 140, 36, p -> TowerArt.devastator(p, level));
            w.image("tower_techmarine_" + suffix, TOWERS, 280, 240, 140, 36, p -> TowerArt.techmarine(p, level));
        }
        w.image("tower_slot", TOWERS, 200, 96, 100, 44, TowerArt::buildSlot);
        w.image("fortress", TOWERS, 480, 520, 250, 50, TowerArt::fortress);
    }

    static void generateEffects(SpriteWriter w) {
        w.image("white", FX, 8, 8, 4, 4, EffectArt::white);
        w.image("proj_bolt", FX, 48, 16, 24, 8, p -> EffectArt.tracer(p, 10, 3, Pal.BOLT_CORE, Pal.BOLT_TRAIL));
        w.image("proj_heavybolt", FX, 56, 20, 28, 10, p -> EffectArt.tracer(p, 13, 4.2, Pal.BOLT_CORE, Pal.FIRE));
        w.image("proj_plasma", FX, 48, 48, 24, 24, p -> EffectArt.orb(p, 6, Pal.PLASMA));
        w.image("proj_missile", FX, 48, 24, 24, 12, p -> EffectArt.missile(p, 16));
        w.image("proj_rocket", FX, 40, 20, 20, 10, p -> EffectArt.missile(p, 12));
        w.image("proj_venom", FX, 36, 36, 18, 18, p -> EffectArt.orb(p, 4.5, Pal.TYR_VENOM));
        w.image("proj_bioplasma", FX, 56, 56, 28, 28, p -> EffectArt.orb(p, 7, Pal.mix(Pal.TYR_VENOM, Pal.HAZARD, 0.4f)));
        w.image("beam_laser", FX, 64, 24, 0, 12, p -> EffectArt.beam(p, 32, 8, Pal.LASER));
        w.image("beam_zap", FX, 64, 16, 0, 8, p -> EffectArt.beam(p, 32, 4, Pal.PLASMA));
        w.image("fx_soft", FX, 64, 64, 32, 32, p -> EffectArt.soft(p, 16));
        w.image("fx_spark", FX, 32, 12, 16, 6, p -> EffectArt.spark(p, 14, 4));
        w.image("fx_smoke", FX, 96, 96, 48, 48, p -> EffectArt.smoke(p, 22, 7));
        w.image("fx_flash", FX, 96, 96, 48, 48, p -> EffectArt.flash(p, 22));
        w.image("fx_ring", FX, 160, 160, 80, 80, p -> EffectArt.ring(p, 38, 2));
        w.image("fx_debris", FX, 20, 20, 10, 10, p -> EffectArt.debris(p, 4));
        w.image("fx_plus", FX, 24, 24, 12, 12, p -> EffectArt.plus(p, 5));
        w.image("fx_shield", FX, 112, 112, 56, 56, p -> EffectArt.shieldBubble(p, 26));
        w.image("decal_crater", FX, 120, 64, 60, 32, p -> EffectArt.crater(p, 28));
        for (int i = 0; i < 3; i++) {
            long seed = 11 + i * 17;
            w.image("decal_splat_" + i, FX, 80, 56, 40, 28, p -> EffectArt.splat(p, 18, seed));
        }
        w.image("shadow", FX, 64, 28, 32, 14, p -> EffectArt.shadow(p));
        w.image("range_fill", FX, 256, 256, 128, 128, p -> EffectArt.rangeFill(p, 62));
        w.image("range_ring", FX, 256, 256, 128, 128, p -> EffectArt.ring(p, 63, 1.2));
        w.image("aoe_target", FX, 200, 200, 100, 100, p -> EffectArt.reticle(p, 48));
        w.image("rally_flag", FX, 40, 56, 10, 6, EffectArt::rallyFlag);
        w.image("droppod", FX, 64, 112, 32, 24, p -> EffectArt.dropPod(p, false));
        w.image("droppod_open", FX, 112, 96, 56, 20, p -> EffectArt.dropPod(p, true));
        w.image("thunderhawk", FX, 360, 220, 180, 110, EffectArt::thunderhawk);
        w.image("servo_skull", FX, 32, 28, 16, 12, p -> EffectArt.servoSkull(p));
        w.image("barrier", FX, 96, 80, 48, 16, EffectArt::barrier);
        w.image("lance", FX, 64, 280, 32, 16, p -> EffectArt.lance(p));
        String[] statuses = {"slow", "burn", "bleed", "armorbreak", "stun", "marked", "regen", "shield", "synapse"};
        for (String s : statuses) {
            w.image("status_" + s, FX, 24, 24, 12, 12, p -> UiArt.status(p, s));
        }
    }

    static void generateUi(SpriteWriter w) {
        String dir = "ui";
        w.ninePatch("ui_panel", dir, 64, 64, 20, 20, 20, 20, p -> UiArt.panel(p, 64, 64));
        w.ninePatch("ui_button", dir, 64, 40, 14, 14, 12, 12,
                p -> UiArt.button(p, 64, 40, new Color(70, 86, 116), new Color(34, 42, 60), new Color(176, 124, 58)));
        w.ninePatch("ui_button_hover", dir, 64, 40, 14, 14, 12, 12,
                p -> UiArt.button(p, 64, 40, new Color(96, 118, 158), new Color(48, 60, 86), new Color(226, 180, 66)));
        w.ninePatch("ui_button_disabled", dir, 64, 40, 14, 14, 12, 12,
                p -> UiArt.button(p, 64, 40, new Color(60, 60, 64), new Color(36, 36, 40), new Color(90, 90, 90)));
        w.ninePatch("ui_button_gold", dir, 64, 40, 14, 14, 12, 12,
                p -> UiArt.button(p, 64, 40, new Color(214, 170, 70), new Color(128, 88, 30), new Color(255, 226, 140)));
        w.ninePatch("ui_button_danger", dir, 64, 40, 14, 14, 12, 12,
                p -> UiArt.button(p, 64, 40, new Color(170, 50, 44), new Color(92, 22, 20), new Color(226, 180, 66)));
        w.ninePatch("ui_slot", dir, 64, 64, 16, 16, 16, 16, p -> UiArt.slot(p, 64, 64));
        icon(w, "icon_requisition", UiArt::requisition);
        icon(w, "icon_wave", UiArt::skullIcon);
        icon(w, "icon_base", UiArt::fortressIcon);
        icon(w, "icon_pause", UiArt::pause);
        icon(w, "icon_play", UiArt::play);
        icon(w, "icon_speed1", p -> UiArt.speed(p, 1));
        icon(w, "icon_speed2", p -> UiArt.speed(p, 2));
        icon(w, "icon_star", p -> UiArt.star(p, true));
        icon(w, "icon_star_empty", p -> UiArt.star(p, false));
        icon(w, "icon_lock", UiArt::lock);
        icon(w, "icon_upgrade", UiArt::upgrade);
        icon(w, "icon_sell", UiArt::sell);
        icon(w, "icon_repair", UiArt::repair);
        icon(w, "icon_next_wave", UiArt::nextWave);
        towerIcon(w, "icon_tower_tactical", p -> TowerArt.tactical(p, 2));
        towerIcon(w, "icon_tower_assault", p -> TowerArt.assault(p, 2));
        towerIcon(w, "icon_tower_devastator", p -> TowerArt.devastator(p, 2));
        towerIcon(w, "icon_tower_techmarine", p -> TowerArt.techmarine(p, 2));
        ability(w, "icon_ability_boltstorm", Pal.BOLT_TRAIL, p -> EffectArt.tracer(p, 12, 4, Pal.BOLT_CORE, Pal.BOLT_TRAIL));
        ability(w, "icon_ability_rally", Pal.UM_BLUE, p -> {
            p.translate(-4, 11);
            EffectArt.rallyFlag(p);
        });
        ability(w, "icon_ability_powersword", Pal.PLASMA, p -> {
            p.rotate(-50, 0, 0);
            EffectArt.beam(p, 14, 4, Pal.PLASMA);
            p.translate(-14, 0);
            EffectArt.beam(p, 14, 4, Pal.PLASMA);
        });
        ability(w, "icon_ability_angel", Pal.GOLD, p -> Emblems.aquila(p, 0, 2, 26, Pal.GOLD));
        ability(w, "icon_ability_droppod", Pal.UM_BLUE, p -> {
            p.translate(0, 11);
            p.scale(0.6, 0.6);
            EffectArt.dropPod(p, false);
        });
        ability(w, "icon_ability_bombard", Pal.PLASMA, p -> {
            p.translate(0, 13);
            p.scale(0.22, 0.22);
            EffectArt.lance(p);
        });
        ability(w, "icon_ability_thunderhawk", Pal.UM_BLUE_DARK, p -> {
            p.scale(0.17, 0.17);
            EffectArt.thunderhawk(p);
        });
        w.image("aquila_emblem", ICONS, 256, 128, 128, 64, p -> Emblems.aquila(p, 0, 0, 58, Pal.GOLD));
        w.image("planet_agri", ICONS, 256, 256, 128, 128,
                p -> UiArt.planet(p, new Color(92, 140, 60), new Color(40, 80, 130), new Color(120, 180, 255), 3));
        w.image("planet_tomb", ICONS, 256, 256, 128, 128,
                p -> UiArt.planet(p, new Color(196, 170, 110), new Color(120, 100, 70), new Color(80, 255, 120), 5));
        w.image("planet_hive", ICONS, 256, 256, 128, 128,
                p -> UiArt.planet(p, new Color(140, 60, 150), new Color(70, 30, 70), new Color(220, 80, 255), 9));
    }

    private static void icon(SpriteWriter w, String name, SpriteWriter.Still still) {
        w.image(name, ICONS, 64, 64, 32, 32, still);
    }

    private static void towerIcon(SpriteWriter w, String name, SpriteWriter.Still tower) {
        w.image(name, ICONS, 96, 96, 48, 48, p -> {
            UiArt.medallion(p, Pal.UM_BLUE);
            p.translate(0, 13);
            p.scale(0.36, 0.36);
            tower.draw(p);
        });
    }

    private static void ability(SpriteWriter w, String name, Color tint, SpriteWriter.Still still) {
        w.image(name, ICONS, 80, 80, 40, 40, p -> {
            p.scale(1.25, 1.25);
            UiArt.medallion(p, tint);
            still.draw(p);
        });
    }
}
