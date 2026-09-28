package com.warhammer.towerdefense.debug;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.warhammer.towerdefense.abilities.AbilitySystem;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerTargeting;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * A scripted commander that plays a battle through the public player API. It powers the balance simulation tests
 * and the {@code --autoplay} QA mode (screenshots, soak tests). It is deliberately decent but not optimal.
 */
public final class AutoCommander {
    private static final String[] BUILD_ORDER = {
            "tactical", "assault", "tactical", "devastator", "tactical", "devastator", "techmarine",
            "assault", "devastator", "tactical"
    };
    private static final float THINK_INTERVAL = 0.4f;
    private static final float COVERAGE_RADIUS = 165f;
    private static final int ABILITY_RESERVE = 90;

    private final GameWorld world;
    private final boolean callWavesEarly;
    private final Array<BuildPoint> slotOrder = new Array<>();
    private final Array<UpgradeOption> options = new Array<>();
    private final Array<Enemy> scratch = new Array<>(false, 64);
    private final Vector2 tmp = new Vector2();
    private int buildIndex;
    private float thinkTimer;
    private boolean heroPositioned;

    public AutoCommander(GameWorld world, boolean callWavesEarly) {
        this.world = world;
        this.callWavesEarly = callWavesEarly;
        rankSlots();
    }

    /** Orders build plots by how much road they cover, so the best spots are used first on any map. */
    private void rankSlots() {
        ObjectFloatMap<BuildPoint> coverage = new ObjectFloatMap<>();
        for (BuildPoint slot : world.map.buildPoints) {
            float score = 0f;
            for (GamePath path : world.paths) {
                for (float d = 0f; d < path.length(); d += 20f) {
                    path.pointAt(d, tmp);
                    if (tmp.dst2(slot.x, slot.y) <= COVERAGE_RADIUS * COVERAGE_RADIUS) {
                        score += 1f;
                    }
                }
            }
            coverage.put(slot, score);
            slotOrder.add(slot);
        }
        slotOrder.sort((a, b) -> Float.compare(coverage.get(b, 0f), coverage.get(a, 0f)));
    }

    public void update(float dt) {
        thinkTimer -= dt;
        if (thinkTimer > 0f || world.state.isOver()) {
            return;
        }
        thinkTimer = THINK_INTERVAL;
        if (world.waves.currentWave() == 0) {
            spend();
            world.callNextWave();
        } else if (callWavesEarly && world.activeEnemyCount() == 0 && world.waves.canCallNextWave()) {
            world.callNextWave();
        }
        spend();
        adjustTargeting();
        commandHero();
        useGlobalAbilities();
    }

    /** Heavy weapons focus the biggest threat while a boss is on the field, like a sensible player would. */
    private void adjustTargeting() {
        boolean boss = world.boss() != null;
        for (int i = 0; i < world.towers.size; i++) {
            Tower t = world.towers.get(i);
            if ("devastator".equals(t.type.id)) {
                t.targeting = boss || t.specIndex == 0 ? TowerTargeting.STRONGEST : TowerTargeting.FIRST;
            }
        }
    }

    /**
     * Builds towers progressively (more slots as waves advance) and otherwise upgrades the lowest-level tower,
     * saving up when the next upgrade is not affordable yet so specialisations actually get bought.
     */
    private void spend() {
        for (int guard = 0; guard < 10; guard++) {
            int wanted = Math.min(Math.min(slotOrder.size, BUILD_ORDER.length), 4 + world.waves.currentWave());
            if (world.towers.size < wanted) {
                if (!tryBuild()) {
                    return;
                }
                continue;
            }
            if (!tryUpgrade()) {
                return;
            }
        }
    }

    private boolean tryBuild() {
        if (buildIndex >= slotOrder.size || buildIndex >= BUILD_ORDER.length) {
            return false;
        }
        BuildPoint slot = slotOrder.get(buildIndex);
        TowerType type = world.data.tower(BUILD_ORDER[buildIndex]);
        if (world.build(slot, type) != null) {
            buildIndex++;
            return true;
        }
        return false;
    }

    private boolean tryUpgrade() {
        Tower pick = null;
        UpgradeOption pickOption = null;
        int bestLevel = Integer.MAX_VALUE;
        for (int i = 0; i < world.towers.size; i++) {
            Tower t = world.towers.get(i);
            world.upgradeOptions(t, options);
            if (options.isEmpty() || t.isBuilding()) {
                continue;
            }
            int level = t.level() + ("techmarine".equals(t.type.id) ? 1 : 0) - ("devastator".equals(t.type.id) ? 2 : 0);
            if (level < bestLevel) {
                bestLevel = level;
                pick = t;
                pickOption = options.get(Math.min(preferredSpecialization(t), options.size - 1));
            }
        }
        if (pick == null) {
            return tryBuild();
        }
        return world.upgrade(pick, pickOption);
    }

    /** Specialisation choices, varied so the simulation exercises most trees during a battle. */
    private int preferredSpecialization(Tower t) {
        int ordinal = 0;
        for (int i = 0; i < world.towers.size && world.towers.get(i) != t; i++) {
            if (world.towers.get(i).type == t.type) {
                ordinal++;
            }
        }
        switch (t.type.id) {
            case "tactical":
                return ordinal % 2 == 0 ? 2 : 0;
            case "devastator":
                return ordinal % 3;
            case "assault":
                return 1;
            case "techmarine":
                return ordinal % 2 == 0 ? 1 : 2;
            default:
                return 0;
        }
    }

    private void commandHero() {
        if (world.hero == null || !world.hero.alive) {
            heroPositioned = false;
            return;
        }
        if (!heroPositioned) {
            GamePath path = world.paths.first();
            path.pointAt(path.length() * 0.78f, tmp);
            world.orderHero(tmp.x, tmp.y + 18f);
            heroPositioned = true;
        }
        int nearby = countEnemies(world.hero.x, world.hero.y, 160f);
        if (nearby >= 3) {
            world.castHeroAbility(0);
            world.castHeroAbility(2);
        }
        if (nearby >= 2) {
            world.castHeroAbility(1);
        }
        if (world.boss() != null || world.activeEnemyCount() > 30) {
            world.castHeroAbility(3);
        }
    }

    private void useGlobalAbilities() {
        for (int i = 0; i < world.abilities.slots.size; i++) {
            AbilitySystem.Slot slot = world.abilities.slots.get(i);
            if (!world.abilities.canCast(i) || world.state.requisition < slot.definition.cost + ABILITY_RESERVE) {
                continue;
            }
            Enemy center = densestEnemy(slot.definition.targetRadius);
            if (center == null) {
                continue;
            }
            int density = countEnemies(center.x, center.y, slot.definition.targetRadius);
            boolean worth = center.def.boss || center.def.elite || density >= 10;
            if (worth) {
                world.castGlobalAbility(i, center.x, center.y);
                return;
            }
        }
    }

    private Enemy densestEnemy(float radius) {
        Enemy best = null;
        int bestCount = 0;
        for (int i = 0; i < world.enemies.size; i += 3) {
            Enemy e = world.enemies.get(i);
            if (!e.isTargetable() || !e.isOnMap()) {
                continue;
            }
            int n = countEnemies(e.x, e.y, radius) + (e.def.boss ? 20 : e.def.elite ? 5 : 0);
            if (n > bestCount) {
                bestCount = n;
                best = e;
            }
        }
        return best;
    }

    private int countEnemies(float x, float y, float radius) {
        scratch.clear();
        return world.targeting.collect(x, y, radius, true, true, scratch);
    }
}
