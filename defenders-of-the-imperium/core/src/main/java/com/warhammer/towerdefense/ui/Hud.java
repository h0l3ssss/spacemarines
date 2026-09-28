package com.warhammer.towerdefense.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.abilities.AbilitySystem;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.heroes.Hero;
import com.warhammer.towerdefense.heroes.HeroAbilitySlot;
import com.warhammer.towerdefense.input.PlayerController;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.rendering.Fx;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerBehavior;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.waves.WaveDefinition;
import com.warhammer.towerdefense.waves.WaveManager;
import com.warhammer.towerdefense.world.GameWorld;

import java.util.Locale;

/**
 * In-battle interface: top bar (requisition, wave, fortress integrity, next wave, speed, pause), boss bar, hero
 * panel with abilities, tower build bar, orbital abilities, tower panel (stats, upgrades, targeting, sell, repair,
 * rally), banners and messages. Clicks are returned as action codes handled by the game screen.
 */
public final class Hud {
    public static final int NONE = 0;
    public static final int BUILD = 1;
    public static final int UPGRADE = 2;
    public static final int SELL = 3;
    public static final int REPAIR = 4;
    public static final int RALLY = 5;
    public static final int TARGETING = 6;
    public static final int HERO_ABILITY = 7;
    public static final int GLOBAL_ABILITY = 8;
    public static final int NEXT_WAVE = 9;
    public static final int SPEED = 10;
    public static final int PAUSE = 11;
    public static final int MENU = 12;
    public static final int SELECT_HERO = 13;
    public static final int CLOSE_PANEL = 14;

    private static final float W = Constants.UI_WIDTH;
    private static final float H = Constants.UI_HEIGHT;

    private final Ui ui;
    private final GameWorld world;
    private final PlayerController control;
    private final Fx fx;
    private final Array<UpgradeOption> options = new Array<>();
    public boolean fast;
    public boolean paused;

    public Hud(Ui ui, GameWorld world, PlayerController control, Fx fx) {
        this.ui = ui;
        this.world = world;
        this.control = control;
        this.fx = fx;
    }

    public Array<UpgradeOption> currentOptions() {
        return options;
    }

    public void draw() {
        topBar();
        bossBar();
        heroPanel();
        buildBar();
        abilityBar();
        if (control.selectedTower != null) {
            towerPanel(control.selectedTower);
        } else {
            options.clear();
        }
        banners();
    }

    // ------------------------------------------------------------------ top bar

    private void topBar() {
        ui.panel(-10, H - 48, W + 20, 60);
        ui.drawIcon("icon_requisition", 30, H - 24, 32);
        ui.text(ui.assets.header, String.valueOf(world.state.requisition), 52, H - 10, Ui.GOLD);
        ui.text(ui.assets.small, "REQUISITION", 52, H - 34, Ui.GREY);

        ui.drawIcon("icon_wave", 212, H - 24, 30);
        ui.text(ui.assets.header, world.waves.currentWave() + "/" + world.waves.totalWaves(), 232, H - 10, Ui.IVORY);
        ui.text(ui.assets.small, "WAVE", 232, H - 34, Ui.GREY);

        ui.drawIcon("icon_base", 362, H - 24, 30);
        float frac = world.state.baseFraction();
        Color c = frac > 0.5f ? Ui.GREEN : frac > 0.25f ? Ui.GOLD : Ui.RED;
        ui.text(ui.assets.body, "BASE INTEGRITY " + world.state.baseHp, 382, H - 12, c);
        ui.bar(382, H - 38, 170, 8, frac, c);

        WaveManager waves = world.waves;
        if (waves.canCallNextWave()) {
            String label;
            if (waves.phase() == WaveManager.Phase.WAITING) {
                label = "START WAVE 1  [N]";
            } else {
                label = String.format(Locale.ROOT, "NEXT WAVE %.0fs  +%d  [N]", Math.max(0f, waves.countdown()), waves.earlyCallBonus());
            }
            ui.button(590, H - 44, 290, 38, label, true, "ui_button_gold", NEXT_WAVE, 0, nextWaveTooltip());
        } else if (waves.phase() == WaveManager.Phase.FINAL) {
            ui.text(ui.assets.body, "FINAL WAVE - HOLD THE LINE", 735, H - 16, Ui.RED, Align.center, 290);
        } else {
            ui.text(ui.assets.body, "WAVE IN PROGRESS", 735, H - 16, Ui.IVORY, Align.center, 290);
        }
        ui.iconButton(1106, H - 45, 40, fast ? "icon_speed2" : "icon_speed1", true, fast, SPEED, 0, "Game speed 1x / 2x  [F]");
        ui.iconButton(1152, H - 45, 40, paused ? "icon_play" : "icon_pause", true, paused, PAUSE, 0, "Pause  [SPACE]");
        ui.button(1198, H - 44, 72, 38, "MENU", true, "ui_button", MENU, 0, "Pause menu  [ESC]");
    }

    private String nextWaveTooltip() {
        WaveDefinition next = world.waves.nextWave();
        if (next == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("INCOMING: ").append(next.title).append('\n');
        for (com.badlogic.gdx.utils.ObjectIntMap.Entry<String> e : next.enemyCounts()) {
            sb.append(" - ").append(world.data.enemy(e.key).name).append(" x").append(e.value).append('\n');
        }
        return sb.toString();
    }

    private void bossBar() {
        Enemy boss = world.boss();
        if (boss == null) {
            return;
        }
        float x = 340;
        float w = 600;
        float y = H - 84;
        ui.panel(x - 14, y - 12, w + 28, 42);
        ui.bar(x, y, w, 12, boss.hpFraction(), new Color(0.75f, 0.2f, 0.85f, 1f));
        if (boss.boss != null) {
            for (int i = 1; i < boss.boss.phaseCount(); i++) {
                ui.rect(x + w * boss.boss.threshold(i) - 1, y - 2, 2, 16, Color.WHITE);
            }
        }
        if (boss.has(com.warhammer.towerdefense.combat.StatusType.SHIELD)) {
            ui.rect(x, y + 10, w, 3, new Color(0.7f, 0.5f, 1f, 1f));
        }
        ui.text(ui.assets.small, boss.def.name.toUpperCase() + "  " + Math.round(boss.hp) + " / " + Math.round(boss.maxHp),
                x + w / 2, y + 26, Ui.IVORY, Align.center, w);
    }

    // ------------------------------------------------------------------ hero

    private void heroPanel() {
        Hero hero = world.hero;
        if (hero == null) {
            return;
        }
        ui.panel(6, 6, 330, 92);
        boolean selected = control.heroSelected;
        ui.iconButton(14, 22, 62, "icon_ability_angel", true, selected, SELECT_HERO, 0,
                hero.definition.title + "\nClick to select, then click the battlefield to move him.  [H]");
        ui.text(ui.assets.small, "CAPTAIN  LVL " + hero.level, 84, 90, Ui.GOLD);
        if (hero.alive) {
            ui.bar(84, 68, 110, 7, hero.hpFraction(), Ui.GREEN);
        } else {
            ui.text(ui.assets.small, String.format(Locale.ROOT, "RETURNS IN %.0fs", hero.respawnTimer), 84, 76, Ui.RED);
        }
        ui.bar(84, 58, 110, 4, hero.levelProgress(), Ui.BLUE);
        String[] keys = {"Q", "W", "E", "R"};
        for (int i = 0; i < hero.abilities.size && i < 4; i++) {
            HeroAbilitySlot slot = hero.abilities.get(i);
            AbilityDefinition def = slot.definition;
            float x = 200 + (i % 2) * 64;
            float y = 52 - (i / 2) * 0;
            x = 84 + i * 62;
            y = 12;
            boolean unlocked = hero.isUnlocked(slot);
            boolean ready = unlocked && slot.isReady() && hero.alive;
            String tip = def.name + "  [" + keys[i] + "]\n" + def.description
                    + (unlocked ? "" : "\nUnlocks at level " + def.unlockLevel);
            ui.iconButton(x, y, 40, def.icon, ready, false, HERO_ABILITY, i, tip);
            if (!unlocked) {
                ui.drawIcon("icon_lock", x + 20, y + 20, 22);
            } else if (!slot.isReady()) {
                ui.rect(x + 3, y + 3, 34, 34 * slot.cooldownFraction(), 0f, 0f, 0f, 0.6f);
            }
            ui.text(ui.assets.small, keys[i], x + 3, y + 40, Ui.IVORY);
        }
    }

    // ------------------------------------------------------------------ build bar

    private void buildBar() {
        float size = 70;
        float x0 = 470;
        ui.panel(x0 - 14, 4, 4 * (size + 10) + 18, size + 22);
        for (int i = 0; i < world.data.towers.size && i < 4; i++) {
            TowerType type = world.data.towers.get(i);
            int cost = world.buildCost(type);
            boolean afford = world.state.canAfford(cost);
            float x = x0 + i * (size + 10);
            boolean selected = control.placing == type;
            String tip = type.name + "  [" + (i + 1) + "]\n" + type.role + "\n" + type.description + "\nCost: " + cost;
            ui.iconButton(x, 16, size, type.icon, afford, selected, BUILD, i, tip);
            ui.text(ui.assets.small, String.valueOf(cost), x + size / 2, 28, afford ? Ui.GOLD : Ui.RED, Align.center, size);
            ui.text(ui.assets.small, String.valueOf(i + 1), x + 5, 16 + size - 4, Ui.IVORY);
        }
        if (control.selectedSlot != null) {
            ui.text(ui.assets.small, "CHOOSE A TOWER FOR THIS PLOT  (1-4)", x0 + 150, 104, Ui.GOLD, Align.center, 320);
        } else if (control.placing != null) {
            ui.text(ui.assets.small, "CLICK A FREE PLOT  -  RIGHT CLICK TO CANCEL", x0 + 150, 104, Ui.GOLD, Align.center, 320);
        }
    }

    private void abilityBar() {
        AbilitySystem abilities = world.abilities;
        float size = 56;
        float x0 = W - 3 * (size + 10) - 16;
        ui.panel(x0 - 12, 4, 3 * (size + 10) + 18, size + 34);
        for (int i = 0; i < abilities.slots.size && i < 3; i++) {
            AbilitySystem.Slot slot = abilities.slots.get(i);
            float x = x0 + i * (size + 10);
            boolean can = abilities.canCast(i);
            String tip = slot.definition.name + "  [" + slot.definition.key + "]\n" + slot.definition.description
                    + "\nCost: " + slot.definition.cost + "   Cooldown: " + Math.round(slot.definition.cooldown) + "s";
            ui.iconButton(x, 24, size, slot.definition.icon, can, control.abilityTargeting == i, GLOBAL_ABILITY, i, tip);
            if (slot.cooldown > 0f) {
                ui.rect(x + 4, 28, size - 8, (size - 8) * slot.cooldownFraction(), 0f, 0f, 0f, 0.6f);
            }
            ui.text(ui.assets.small, slot.definition.cost + "  " + slot.definition.key, x + size / 2, 20,
                    world.state.canAfford(slot.definition.cost) ? Ui.GOLD : Ui.RED, Align.center, size + 10);
        }
    }

    // ------------------------------------------------------------------ tower panel

    private void towerPanel(Tower t) {
        float x = W - 326;
        float y = 104;
        float w = 316;
        world.upgradeOptions(t, options);
        float h = 250 + options.size * 46;
        ui.panel(x, y, w, h);
        float top = y + h - 14;
        ui.text(ui.assets.body, t.displayName().toUpperCase(), x + 16, top, Ui.GOLD);
        ui.button(x + w - 40, top - 22, 28, 26, "X", true, "ui_button", CLOSE_PANEL, 0, null);
        ui.text(ui.assets.small, t.type.name + "  -  LEVEL " + t.level(), x + 16, top - 24, Ui.GREY);
        float ly = top - 52;
        if (t.type.behavior == TowerBehavior.RANGED) {
            line(x, ly, "DAMAGE", String.format(Locale.ROOT, "%.0f-%.0f", t.stats.get(Stat.DAMAGE_MIN) * t.damageMultiplier(),
                    t.stats.get(Stat.DAMAGE_MAX) * t.damageMultiplier()) + (t.stats.getInt(Stat.MISSILES) > 1 ? " x" + t.stats.getInt(Stat.MISSILES) : ""));
            line(x, ly - 20, "RANGE", String.valueOf(Math.round(t.effectiveRange())));
            line(x, ly - 40, "FIRE RATE", String.format(Locale.ROOT, "%.2f/s", 1f / t.effectiveCooldown()));
            line(x, ly - 60, "TARGETS", t.stats.canHitAir ? "GROUND + AIR" : "GROUND ONLY");
        } else if (t.type.behavior == TowerBehavior.BARRACKS) {
            line(x, ly, "SQUAD", t.stats.getInt(Stat.UNIT_COUNT) + " x " + world.data.unit(t.stats.unitType).name);
            line(x, ly - 20, "HIT POINTS", String.valueOf(Math.round(t.stats.get(Stat.UNIT_HP) * t.bonusUnitHpMult)));
            line(x, ly - 40, "DAMAGE", Math.round(t.stats.get(Stat.UNIT_DAMAGE_MIN)) + "-" + Math.round(t.stats.get(Stat.UNIT_DAMAGE_MAX)));
            line(x, ly - 60, "ARMOUR", Math.round(t.stats.get(Stat.UNIT_ARMOR) * 100) + "%");
        } else {
            line(x, ly, "AURA", "+" + Math.round(t.stats.get(Stat.AURA_RANGE) * 100) + "% range, +"
                    + Math.round(t.stats.get(Stat.AURA_FIRE_RATE) * 100) + "% rate");
            line(x, ly - 20, "AURA RADIUS", String.valueOf(Math.round(t.stats.get(Stat.AURA_RADIUS))));
            line(x, ly - 40, "REPAIR", Math.round(t.stats.get(Stat.REPAIR)) + "/s");
            line(x, ly - 60, "SPECIAL", t.stats.get(Stat.SKULLS) > 0 ? t.stats.getInt(Stat.SKULLS) + " servo-skulls"
                    : t.stats.get(Stat.BARRIER_HP) > 0 ? "void barrier" : "-");
        }
        line(x, ly - 80, "KILLS / DAMAGE", t.kills + " / " + Math.round(t.damageDealt));
        float by = ly - 124;
        for (int i = 0; i < options.size; i++) {
            UpgradeOption o = options.get(i);
            boolean afford = world.state.canAfford(o.cost) && !t.isBuilding();
            String label = (o.kind == UpgradeOption.Kind.SPECIALIZATION ? "" : "UPGRADE: ") + o.upgrade.name.toUpperCase() + "  " + o.cost;
            ui.button(x + 12, by, w - 24, 40, label, afford,
                    o.kind == UpgradeOption.Kind.SPECIALIZATION ? "ui_button_gold" : "ui_button", UPGRADE, i,
                    o.upgrade.name + "\n" + o.upgrade.description + "\nCost: " + o.cost + (afford ? "" : "\nNot enough requisition"));
            by -= 46;
        }
        if (options.isEmpty()) {
            ui.text(ui.assets.small, "FULLY UPGRADED", x + w / 2, by + 26, Ui.GOLD, Align.center, w);
            by -= 30;
        }
        float bw = (w - 36) / 3f;
        if (t.type.behavior != TowerBehavior.SUPPORT && t.type.behavior != TowerBehavior.BARRACKS) {
            ui.button(x + 12, y + 12, bw, 34, t.targeting.label, true, "ui_button", TARGETING, 0,
                    "Target priority: " + t.targeting.description + "\nClick to cycle  [T]");
        } else if (t.type.behavior == TowerBehavior.BARRACKS) {
            ui.button(x + 12, y + 12, bw, 34, "RALLY", true, "ui_button", RALLY, 0, "Move the squad's rally point (on the road, in range)");
        }
        ui.button(x + 18 + bw, y + 12, bw, 34, "REPAIR " + t.repairCost(), t.needsRepair() && world.state.canAfford(t.repairCost()),
                "ui_button", REPAIR, 0, "Restore structural integrity");
        ui.button(x + 24 + bw * 2, y + 12, bw, 34, "SELL " + t.sellValue(), true, "ui_button_danger", SELL, 0,
                "Recover 60% of everything spent on this tower  [DEL]");
    }

    private void line(float x, float y, String label, String value) {
        ui.text(ui.assets.small, label, x + 16, y, Ui.GREY);
        ui.text(ui.assets.small, value, x + 150, y, Ui.IVORY);
    }

    // ------------------------------------------------------------------ banners

    private void banners() {
        if (fx.bannerTime > 0f && fx.banner != null) {
            float a = Math.min(1f, fx.bannerTime * 2f);
            ui.rect(0, H / 2 + 60, W, 90, 0f, 0f, 0f, 0.45f * a);
            ui.assets.header.getData().setScale(ui.assets.header.getData().scaleX * 1.5f);
            Color c = new Color(fx.bannerColor.r, fx.bannerColor.g, fx.bannerColor.b, a);
            ui.text(ui.assets.header, fx.banner, W / 2, H / 2 + 132, c, Align.center, W);
            ui.assets.header.getData().setScale(ui.assets.header.getData().scaleX / 1.5f);
            if (fx.bannerSub != null && !fx.bannerSub.isEmpty()) {
                ui.text(ui.assets.body, fx.bannerSub, W / 2, H / 2 + 88, new Color(0.95f, 0.93f, 0.86f, a), Align.center, W);
            }
        }
        if (control.messageTime > 0f && control.message != null) {
            ui.text(ui.assets.body, control.message, W / 2, 150, Ui.RED, Align.center, 600);
        }
        if (control.abilityTargeting >= 0) {
            ui.text(ui.assets.body, "CHOOSE TARGET  -  RIGHT CLICK TO CANCEL", W / 2, 150, Ui.GOLD, Align.center, 600);
        } else if (control.rallyMode) {
            ui.text(ui.assets.body, "CLICK ON THE ROAD TO SET THE RALLY POINT", W / 2, 150, Ui.GOLD, Align.center, 600);
        } else if (control.heroSelected) {
            ui.text(ui.assets.small, "CAPTAIN SELECTED - CLICK TO MOVE  (Q W E R ABILITIES)", W / 2, 150, Ui.GOLD, Align.center, 600);
        }
        if (fx.damageFlash > 0f) {
            float a = fx.damageFlash * 0.35f;
            ui.rect(0, 0, W, 18, 1f, 0f, 0f, a);
            ui.rect(0, H - 18, W, 18, 1f, 0f, 0f, a);
            ui.rect(0, 0, 18, H, 1f, 0f, 0f, a);
            ui.rect(W - 18, 0, 18, H, 1f, 0f, 0f, a);
        }
    }

    /** Screen-space ring of tower choices around a selected empty plot (positions given in UI coordinates). */
    public void buildRing(BuildPoint slot, float cx, float cy) {
        float r = 58;
        float[][] offs = {{-r, 0}, {0, r}, {r, 0}, {0, -r}};
        for (int i = 0; i < world.data.towers.size && i < 4; i++) {
            TowerType type = world.data.towers.get(i);
            int cost = world.buildCost(type);
            float x = cx + offs[i][0] - 24;
            float y = cy + offs[i][1] - 24;
            ui.iconButton(x, y, 48, type.icon, world.state.canAfford(cost), false, BUILD, i,
                    type.name + "  [" + (i + 1) + "]\n" + type.role + "\nCost: " + cost);
            ui.text(ui.assets.small, String.valueOf(cost), x + 24, y - 2, world.state.canAfford(cost) ? Ui.GOLD : Ui.RED,
                    Align.center, 60);
        }
    }
}
