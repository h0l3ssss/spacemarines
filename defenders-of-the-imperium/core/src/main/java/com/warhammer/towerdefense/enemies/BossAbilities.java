package com.warhammer.towerdefense.enemies;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.BossDefinition;
import com.warhammer.towerdefense.data.EnemyDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/** Implementations of the data-driven boss abilities. Each returns false when it could not be used right now. */
final class BossAbilities {
    private static final float SPAWN_SPACING = 14f;
    private static final float UNIT_STUN = 1f;
    private static final float CAST_TIME = 0.8f;
    private static final Array<Tower> TOWERS = new Array<>(false, 8);

    private BossAbilities() {
    }

    static boolean execute(BossDefinition.Ability a, Enemy boss, GameWorld world) {
        switch (a.type) {
            case SPAWN:
                return spawn(a, boss, world);
            case PSYCHIC_SCREAM:
                return scream(a, boss, world);
            case LEAP:
                return leap(a, boss, world);
            case BIO_PLASMA:
                return bioPlasma(a, boss, world);
            case WARP_SHIELD:
                boss.applyStatus(StatusType.SHIELD, a.duration, a.amount);
                world.events.bossAbility(boss, a.type, boss.x, boss.y, boss.def.radius * 2f);
                return true;
            case REGENERATE:
                boss.applyStatus(StatusType.REGENERATION, a.cooldown + 0.25f, a.amount);
                return true;
            default:
                return false;
        }
    }

    private static boolean spawn(BossDefinition.Ability a, Enemy boss, GameWorld world) {
        EnemyDefinition minion = world.data.enemy(a.enemy);
        for (int i = 0; i < a.count; i++) {
            float back = Math.max(0f, boss.distance - 20f - i * SPAWN_SPACING);
            float lateral = (world.random.nextFloat() * 2f - 1f) * boss.path.width * 0.35f;
            world.spawnOnPath(minion, boss.path, back, lateral, boss.waveIndex, 1f);
        }
        boss.castTimer = CAST_TIME;
        world.events.bossAbility(boss, a.type, boss.x, boss.y, 60f);
        return true;
    }

    private static boolean scream(BossDefinition.Ability a, Enemy boss, GameWorld world) {
        float r2 = a.radius * a.radius;
        boolean hit = false;
        for (Tower t : world.towers) {
            if (t.dst2(boss.x, boss.y) <= r2 && !t.isOffline()) {
                world.damage.suppressTower(t, a.duration);
                hit = true;
            }
        }
        for (AlliedUnit u : world.units) {
            if (u.alive && u.dst2(boss.x, boss.y) <= r2) {
                world.damage.damageUnit(u, a.damage, 0.5f);
                u.stunTimer = Math.max(u.stunTimer, UNIT_STUN);
                hit = true;
            }
        }
        if (world.hero != null && world.hero.alive && world.hero.dst2(boss.x, boss.y) <= r2) {
            world.damage.damageUnit(world.hero, a.damage, 0.5f);
            world.hero.stunTimer = Math.max(world.hero.stunTimer, UNIT_STUN);
            hit = true;
        }
        if (hit) {
            boss.castTimer = CAST_TIME;
            world.events.bossAbility(boss, a.type, boss.x, boss.y, a.radius);
        }
        return hit;
    }

    private static boolean leap(BossDefinition.Ability a, Enemy boss, GameWorld world) {
        if (!boss.isBlocked()) {
            return false;
        }
        AlliedUnit holder = boss.blocker;
        holder.engaged.removeValue(boss, true);
        boss.blocker = null;
        float startX = boss.x;
        float startY = boss.y;
        boss.distance = Math.min(boss.path.length() - 60f, boss.distance + a.range);
        boss.placeOnPath();
        world.damageUnitsInRadius(boss.x, boss.y, a.radius, a.damage, 0f);
        world.events.bossAbility(boss, a.type, startX, startY, a.radius);
        world.events.bossAbility(boss, a.type, boss.x, boss.y, a.radius);
        return true;
    }

    private static boolean bioPlasma(BossDefinition.Ability a, Enemy boss, GameWorld world) {
        TOWERS.clear();
        float r2 = a.range * a.range;
        for (Tower t : world.towers) {
            if (!t.isOffline() && t.dst2(boss.x, boss.y) <= r2) {
                TOWERS.add(t);
            }
        }
        if (TOWERS.isEmpty()) {
            return false;
        }
        TOWERS.sort((t1, t2) -> Float.compare(t1.dst2(boss.x, boss.y), t2.dst2(boss.x, boss.y)));
        int shots = Math.min(a.count, TOWERS.size);
        for (int i = 0; i < shots; i++) {
            world.projectiles.fireAtTower(boss, TOWERS.get(i), ProjectileType.BIO_PLASMA, a.damage, a.radius);
        }
        boss.castTimer = CAST_TIME;
        world.events.bossAbility(boss, a.type, boss.x, boss.y, 40f);
        return true;
    }
}
