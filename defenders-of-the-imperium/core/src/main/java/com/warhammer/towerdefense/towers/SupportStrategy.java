package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Barrier;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.ServoSkull;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Techmarine Fortification: buffs nearby towers (range, fire rate, damage, armour), repairs structures, and
 * depending on the specialisation launches servo-skulls or projects void barriers onto the road.
 */
public final class SupportStrategy implements TowerAttackStrategy {
    private static final float SKULL_ORBIT_RADIUS = 30f;
    private static final float SKULL_ORBIT_HEIGHT = 58f;
    private static final float SKULL_ORBIT_SPEED = 45f;
    private static final float BARRIER_LEAD_DISTANCE = 50f;
    private static final float OFFLINE_REPAIR_FACTOR = 0.08f;

    private final Array<ServoSkull> skulls = new Array<>();
    private final Vector2 tmp = new Vector2();
    private Barrier barrier;
    private float barrierTimer = 1.5f;

    @Override
    public void applyAura(Tower tower, GameWorld world) {
        if (!tower.canAct()) {
            return;
        }
        float radius = tower.stats.get(Stat.AURA_RADIUS);
        if (radius <= 0f) {
            return;
        }
        float r2 = radius * radius;
        float range = tower.stats.get(Stat.AURA_RANGE);
        float fireRate = tower.stats.get(Stat.AURA_FIRE_RATE);
        float damage = tower.stats.get(Stat.AURA_DAMAGE);
        float armor = tower.stats.get(Stat.AURA_ARMOR);
        for (Tower other : world.towers) {
            if (other == tower || other.dst2(tower.x, tower.y) > r2) {
                continue;
            }
            other.buffRange = Math.max(other.buffRange, range);
            other.buffFireRate = Math.max(other.buffFireRate, fireRate);
            other.buffDamage = Math.max(other.buffDamage, damage);
            other.buffArmor = Math.max(other.buffArmor, armor);
        }
        if (armor > 0f) {
            for (AlliedUnit u : world.units) {
                if (u.alive && u.dst2(tower.x, tower.y) <= r2) {
                    u.auraArmor = Math.max(u.auraArmor, armor);
                }
            }
            if (world.hero != null && world.hero.alive && world.hero.dst2(tower.x, tower.y) <= r2) {
                world.hero.auraArmor = Math.max(world.hero.auraArmor, armor);
            }
        }
    }

    @Override
    public void update(Tower tower, GameWorld world, float dt) {
        if (tower.isBuilding()) {
            return;
        }
        repair(tower, world, dt);
        updateSkulls(tower, world, dt);
        updateBarrier(tower, world, dt);
    }

    private void repair(Tower tower, GameWorld world, float dt) {
        float rate = tower.stats.get(Stat.REPAIR);
        float radius = tower.stats.get(Stat.AURA_RADIUS);
        if (rate <= 0f || radius <= 0f || tower.isOffline()) {
            return;
        }
        float r2 = radius * radius;
        for (Tower other : world.towers) {
            if (other.dst2(tower.x, tower.y) > r2) {
                continue;
            }
            if (other.isOffline()) {
                other.offlineTimer -= dt * rate * OFFLINE_REPAIR_FACTOR;
            } else if (other.integrity < other.maxIntegrity) {
                other.integrity = Math.min(other.maxIntegrity, other.integrity + rate * dt);
            }
        }
    }

    private void updateSkulls(Tower tower, GameWorld world, float dt) {
        int wanted = tower.stats.getInt(Stat.SKULLS);
        while (skulls.size < wanted) {
            ServoSkull s = new ServoSkull();
            s.owner = tower;
            s.alive = true;
            s.cooldown = 0.4f * skulls.size;
            skulls.add(s);
            world.skulls.add(s);
        }
        while (skulls.size > wanted) {
            ServoSkull s = skulls.pop();
            s.alive = false;
            world.skulls.removeValue(s, true);
        }
        for (int i = 0; i < skulls.size; i++) {
            ServoSkull s = skulls.get(i);
            s.orbitAngle += SKULL_ORBIT_SPEED * dt;
            float angle = s.orbitAngle + i * 360f / skulls.size;
            s.x = tower.x + MathUtils.cosDeg(angle) * SKULL_ORBIT_RADIUS;
            s.y = tower.y + SKULL_ORBIT_HEIGHT + MathUtils.sinDeg(angle) * 9f;
            s.zapTimer = Math.max(0f, s.zapTimer - dt);
            if (!tower.canAct()) {
                continue;
            }
            s.cooldown -= dt;
            if (s.cooldown > 0f) {
                continue;
            }
            Enemy e = world.targeting.findTarget(tower.x, tower.y, tower.effectiveRange(), tower.targeting, true, true);
            if (e == null) {
                s.cooldown = 0.2f;
                continue;
            }
            s.cooldown = tower.stats.get(Stat.SKULL_COOLDOWN);
            world.projectiles.fireSkullZap(s, e, tower);
        }
    }

    private void updateBarrier(Tower tower, GameWorld world, float dt) {
        float hp = tower.stats.get(Stat.BARRIER_HP);
        if (hp <= 0f) {
            return;
        }
        if (barrier != null && barrier.alive) {
            return;
        }
        barrier = null;
        barrierTimer -= dt;
        if (barrierTimer > 0f || !tower.canAct()) {
            return;
        }
        Enemy threat = world.targeting.findNearest(tower.x, tower.y, tower.effectiveRange(), false, true);
        if (threat == null) {
            barrierTimer = 0.3f;
            return;
        }
        GamePath path = threat.path;
        float along = Math.max(path.project(tower.x, tower.y), threat.distance + BARRIER_LEAD_DISTANCE);
        along = Math.min(along, path.length() - BARRIER_LEAD_DISTANCE);
        path.pointAt(along, tmp);

        Barrier b = new Barrier();
        b.maxHp = hp;
        b.duration = tower.stats.get(Stat.BARRIER_DURATION);
        b.lifetime = b.duration;
        b.owner = tower;
        b.spawn(tmp.x, tmp.y);
        b.guardX = tmp.x;
        b.guardY = tmp.y;
        world.addUnit(b);
        barrier = b;
        barrierTimer = tower.stats.get(Stat.BARRIER_COOLDOWN);
    }

    @Override
    public void onRemoved(Tower tower, GameWorld world) {
        for (ServoSkull s : skulls) {
            s.alive = false;
            world.skulls.removeValue(s, true);
        }
        skulls.clear();
        if (barrier != null && barrier.alive) {
            world.retireUnit(barrier);
        }
        barrier = null;
    }
}
