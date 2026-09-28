package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Shooting towers (Tactical Bastion, Devastator Emplacement). Handles cooldowns, multi-missile volleys, the
 * "extra shot" chance of Bolter Discipline and the overheat cycle of the Plasma Annihilator.
 */
public final class RangedAttackStrategy implements TowerAttackStrategy {
    private static final float EXTRA_SHOT_DELAY = 0.09f;

    private int shotsSinceCooling;
    private float overheatTimer;
    private float extraShotTimer = -1f;

    @Override
    public void update(Tower tower, GameWorld world, float dt) {
        tower.fireAnim = Math.max(0f, tower.fireAnim - dt);
        int overheatShots = tower.stats.getInt(Stat.OVERHEAT_SHOTS);
        if (overheatTimer > 0f) {
            overheatTimer -= dt;
            tower.overheated = overheatTimer > 0f;
            tower.heat = Math.max(0f, overheatTimer / Math.max(0.01f, tower.stats.get(Stat.OVERHEAT_TIME)));
            return;
        }
        tower.overheated = false;
        tower.heat = overheatShots > 0 ? shotsSinceCooling / (float) overheatShots : 0f;
        if (!tower.canAct()) {
            return;
        }
        if (extraShotTimer >= 0f) {
            extraShotTimer -= dt;
            if (extraShotTimer < 0f) {
                Enemy extra = acquire(tower, world);
                if (extra != null) {
                    fire(tower, world, extra);
                }
            }
        }
        tower.cooldown -= dt;
        if (tower.cooldown > 0f) {
            return;
        }
        Enemy target = acquire(tower, world);
        if (target == null) {
            tower.cooldown = 0f;
            return;
        }
        fire(tower, world, target);
        tower.cooldown = tower.effectiveCooldown();

        float extraChance = tower.stats.get(Stat.EXTRA_SHOT_CHANCE);
        if (extraChance > 0f && world.random.nextFloat() < extraChance) {
            extraShotTimer = EXTRA_SHOT_DELAY;
        }
        if (overheatShots > 0) {
            shotsSinceCooling++;
            if (shotsSinceCooling >= overheatShots) {
                shotsSinceCooling = 0;
                overheatTimer = tower.stats.get(Stat.OVERHEAT_TIME);
                tower.overheated = true;
                world.events.towerOverheated(tower);
            }
        }
    }

    private Enemy acquire(Tower tower, GameWorld world) {
        return world.targeting.findTarget(tower.x, tower.y, tower.effectiveRange(), tower.targeting,
                tower.stats.canHitAir, true);
    }

    private void fire(Tower tower, GameWorld world, Enemy target) {
        int missiles = Math.max(1, tower.stats.getInt(Stat.MISSILES));
        for (int i = 0; i < missiles; i++) {
            world.projectiles.fireFromTower(tower, target, i, missiles);
        }
        tower.fireAnim = 0.25f;
        tower.shotIndex++;
        tower.aimX = target.x;
        tower.aimY = target.y;
    }
}
