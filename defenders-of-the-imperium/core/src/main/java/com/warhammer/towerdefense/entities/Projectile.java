package com.warhammer.towerdefense.entities;

import com.badlogic.gdx.utils.Pool;
import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.towers.Tower;

/**
 * A pooled projectile: position, velocity, target, damage payload, splash and lifetime. Its movement model comes
 * from {@link ProjectileType#motion}.
 */
public final class Projectile implements Pool.Poolable {
    public ProjectileType type;
    public boolean active;
    public boolean hostile;

    public float x;
    public float y;
    public float vx;
    public float vy;
    public float angle;
    /** Visual height above the ground for lobbed shells. */
    public float height;
    public float startX;
    public float startY;
    public float targetX;
    public float targetY;
    public float age;
    /** Flight time for ballistic shells, visual lifetime for beams. */
    public float duration;
    public float lifetime = 3f;
    public float arc;
    public float speed;

    public Enemy target;
    public int targetSerial;
    public Tower towerTarget;
    public AlliedUnit unitTarget;
    public int unitTargetSerial;

    public final DamageInfo damage = new DamageInfo();

    public boolean hasValidTarget() {
        return target != null && target.serial == targetSerial && target.isTargetable();
    }

    public boolean hasValidUnitTarget() {
        return unitTarget != null && unitTarget.alive && unitTarget.serial == unitTargetSerial;
    }

    @Override
    public void reset() {
        type = null;
        active = false;
        hostile = false;
        x = y = vx = vy = angle = height = 0f;
        startX = startY = targetX = targetY = 0f;
        age = duration = arc = speed = 0f;
        lifetime = 3f;
        target = null;
        towerTarget = null;
        unitTarget = null;
        targetSerial = unitTargetSerial = 0;
        damage.reset();
    }
}
