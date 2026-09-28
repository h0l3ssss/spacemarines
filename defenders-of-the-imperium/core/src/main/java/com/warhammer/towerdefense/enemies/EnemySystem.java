package com.warhammer.towerdefense.enemies;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.EnemyDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/** Moves enemies along their paths, ticks statuses, resolves their melee and ranged attacks and death timers. */
public final class EnemySystem {
    private static final float RANGED_CAST_TIME = 0.45f;
    private static final float RANGED_RETRY = 0.5f;
    private static final float COOLDOWN_JITTER = 0.3f;

    private final GameWorld world;

    public EnemySystem(GameWorld world) {
        this.world = world;
    }

    public void update(float dt) {
        Array<Enemy> enemies = world.enemies;
        for (int i = enemies.size - 1; i >= 0; i--) {
            if (i >= enemies.size) {
                continue;
            }
            Enemy e = enemies.get(i);
            if (e.state == EnemyState.DYING) {
                e.deathTimer += dt;
                if (e.deathTimer >= Constants.ENEMY_DEATH_TIME) {
                    world.removeEnemyAt(i);
                }
                continue;
            }
            updateEnemy(e, dt);
            if (e.state == EnemyState.LEAKED) {
                world.removeEnemyAt(i);
            }
        }
    }

    /** Advances only death animations (used after the battle ended). */
    public void updateDeaths(float dt) {
        Array<Enemy> enemies = world.enemies;
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.state == EnemyState.DYING) {
                e.deathTimer += dt;
                if (e.deathTimer >= Constants.ENEMY_DEATH_TIME) {
                    world.removeEnemyAt(i);
                }
            }
        }
    }

    private void updateEnemy(Enemy e, float dt) {
        e.stateTime += dt;
        e.animTime += dt;
        e.hitFlash = Math.max(0f, e.hitFlash - dt * 5f);
        tickStatuses(e, dt);
        if (!e.isTargetable()) {
            return;
        }
        if (e.def.regen > 0f && e.hp < e.maxHp) {
            e.hp = Math.min(e.maxHp, e.hp + e.def.regen * dt);
        }
        if (e.boss != null) {
            e.boss.update(world, dt);
            if (!e.isTargetable()) {
                return;
            }
        }
        if (e.castTimer > 0f) {
            e.castTimer -= dt;
            e.state = EnemyState.CASTING;
            return;
        }
        if (e.has(StatusType.STUN)) {
            e.state = e.isBlocked() ? EnemyState.FIGHTING : EnemyState.WALKING;
            return;
        }
        if (e.isBlocked()) {
            fight(e, dt);
        } else {
            e.blocker = null;
            e.state = EnemyState.WALKING;
            walk(e, dt);
            if (e.state == EnemyState.LEAKED) {
                return;
            }
        }
        rangedAttack(e, dt);
    }

    private void tickStatuses(Enemy e, float dt) {
        float dot = e.power(StatusType.BURN) + e.power(StatusType.BLEED);
        float regen = e.power(StatusType.REGENERATION);
        for (int s = 0; s < StatusType.COUNT; s++) {
            if (e.statusTime[s] > 0f) {
                e.statusTime[s] -= dt;
                if (e.statusTime[s] <= 0f) {
                    e.statusTime[s] = 0f;
                    e.statusPower[s] = 0f;
                }
            }
        }
        if (regen > 0f) {
            e.hp = Math.min(e.maxHp, e.hp + regen * dt);
        }
        if (dot > 0f) {
            world.damage.damageOverTime(e, dot * dt);
        }
    }

    private void walk(Enemy e, float dt) {
        e.distance += e.currentSpeed() * dt;
        if (e.distance >= e.path.length()) {
            world.leak(e);
            return;
        }
        e.placeOnPath();
    }

    private void fight(Enemy e, float dt) {
        AlliedUnit defender = e.blocker;
        e.state = EnemyState.FIGHTING;
        if (Math.abs(defender.x - e.x) > 2f) {
            e.facing = defender.x >= e.x ? 1f : -1f;
        }
        EnemyDefinition def = e.def;
        float reach = def.meleeRange + defender.radius + def.radius;
        if (defender.dst2(e.x, e.y) > reach * reach) {
            return; // the defender is still closing in
        }
        e.attackTimer -= dt;
        if (e.attackTimer > 0f || def.meleeDamage <= 0f) {
            return;
        }
        e.attackTimer = def.meleeCooldown * (1f - COOLDOWN_JITTER * 0.5f + world.random.nextFloat() * COOLDOWN_JITTER);
        float damage = def.meleeDamage * e.currentDamageMultiplier();
        if (e.pounceReady && def.has(EnemyTrait.LEAPER)) {
            damage *= 2f;
        }
        e.pounceReady = false;
        float armorIgnore = def.has(EnemyTrait.RENDING) ? 0.5f : 0f;
        if (def.has(EnemyTrait.CLEAVE)) {
            world.damageUnitsInRadius(e.x, e.y, reach + 10f, damage, armorIgnore);
        } else {
            world.damage.damageUnit(defender, damage, armorIgnore);
        }
        world.events.enemyMelee(e, defender, damage);
    }

    private void rangedAttack(Enemy e, float dt) {
        EnemyDefinition def = e.def;
        if (!def.hasRangedAttack()) {
            return;
        }
        e.rangedTimer -= dt;
        if (e.rangedTimer > 0f || e.state == EnemyState.FIGHTING || !e.isOnMap()) {
            return;
        }
        float damage = def.rangedDamage * e.currentDamageMultiplier();
        boolean fired = false;
        if (def.rangedTarget != EnemyDefinition.RangedTarget.UNITS) {
            Tower tower = world.nearestTower(e.x, e.y, def.rangedRange, true);
            if (tower != null) {
                world.projectiles.fireAtTower(e, tower, def.rangedProjectile, damage, 0f);
                fired = true;
            }
        }
        if (!fired && def.rangedTarget != EnemyDefinition.RangedTarget.TOWERS) {
            AlliedUnit unit = world.nearestUnit(e.x, e.y, def.rangedRange);
            if (unit != null) {
                world.projectiles.fireAtUnit(e, unit, def.rangedProjectile, damage);
                fired = true;
            }
        }
        if (fired) {
            e.rangedTimer = def.rangedCooldown * (1f - COOLDOWN_JITTER * 0.5f + world.random.nextFloat() * COOLDOWN_JITTER);
            e.castTimer = RANGED_CAST_TIME;
            e.facing = e.dirX >= 0f ? 1f : -1f;
            world.events.enemyRanged(e);
        } else {
            e.rangedTimer = RANGED_RETRY;
        }
    }
}
