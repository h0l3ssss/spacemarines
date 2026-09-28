package com.warhammer.towerdefense.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.Projectile;
import com.warhammer.towerdefense.entities.ServoSkull;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Launches, moves and resolves every projectile. Projectiles come from a {@link Pool}; the active list is iterated
 * backwards and finished projectiles are swap-removed, so a heavy firefight allocates nothing.
 */
public final class ProjectileSystem {
    private static final float MAX_LIFETIME = 3f;
    private static final float ROCKET_ACCELERATION = 900f;
    private static final float ROCKET_MAX_SPEED = 720f;
    private static final float ROCKET_TURN_RATE = 7f;
    private static final float VOLLEY_SPREAD = 9f;
    private static final float MIN_ARC = 30f;
    private static final float MAX_ARC = 150f;
    private static final float ARC_FACTOR = 0.35f;
    private static final float DIRECT_HIT_RADIUS = 18f;

    public final Array<Projectile> active = new Array<>(false, 256);
    private final GameWorld world;
    private final Pool<Projectile> pool = new Pool<Projectile>(256, 8192) {
        @Override
        protected Projectile newObject() {
            return new Projectile();
        }
    };
    private final Vector2 predicted = new Vector2();

    public ProjectileSystem(GameWorld world) {
        this.world = world;
    }

    /** Height above the enemy's feet that projectiles aim at (its body, or its wings for flyers). */
    public static float hitHeight(Enemy e) {
        return e.def.radius * 1.2f + (e.flying ? e.def.flyHeight : 0f);
    }

    public void fireFromTower(Tower tower, Enemy target, int volleyIndex, int volleySize) {
        ProjectileType type = tower.stats.projectile;
        float offset = volleySize > 1 ? (volleyIndex - (volleySize - 1) * 0.5f) * VOLLEY_SPREAD : 0f;
        Projectile p = obtain(type, tower.muzzleX() + offset, tower.muzzleY());
        tower.buildDamage(p.damage, world.random.nextFloat());
        launchAt(p, target, volleyIndex * 14f);
        world.events.towerFired(tower, type, p.startX, p.startY, p.targetX, p.targetY);
    }

    public void fireFromUnit(AlliedUnit unit, Enemy target, float damage) {
        Projectile p = obtain(unit.rangedProjectile, unit.x + unit.facing * 10f, unit.y + 18f);
        p.damage.amount = damage;
        p.damage.type = DamageType.KINETIC;
        p.damage.source = unit.isHero() ? DamageInfo.Source.HERO : DamageInfo.Source.UNIT;
        p.damage.heroCredit = unit.isHero();
        launchAt(p, target, 0f);
        world.events.shotFired(p.type, p.startX, p.startY, p.targetX, p.targetY);
    }

    /** Generic friendly shot (abilities, gunship). The payload is copied. */
    public void fire(ProjectileType type, float fromX, float fromY, Enemy target, DamageInfo payload) {
        Projectile p = obtain(type, fromX, fromY);
        p.damage.set(payload);
        launchAt(p, target, 0f);
        world.events.shotFired(type, p.startX, p.startY, p.targetX, p.targetY);
    }

    public void fireSkullZap(ServoSkull skull, Enemy target, Tower owner) {
        Projectile p = obtain(ProjectileType.SKULL_ZAP, skull.x, skull.y);
        DamageInfo d = p.damage;
        d.amount = owner.stats.get(Stat.SKULL_DAMAGE) * owner.damageMultiplier();
        d.type = DamageType.ENERGY;
        d.tower = owner;
        d.withStatus(StatusType.MARKED, 1f, owner.stats.get(Stat.MARK_DURATION), owner.stats.get(Stat.MARK_POWER));
        launchAt(p, target, 0f);
        skull.zapTimer = ProjectileType.SKULL_ZAP.beamDuration;
        skull.zapX = p.targetX;
        skull.zapY = p.targetY;
        world.events.shotFired(p.type, p.startX, p.startY, p.targetX, p.targetY);
    }

    /** Hostile shot at a tower (venom cannon, bio-plasma). */
    public void fireAtTower(Enemy source, Tower tower, ProjectileType type, float damage, float splashRadius) {
        Projectile p = obtain(type, source.x + source.facing * source.def.radius, source.y + source.def.radius * 1.6f);
        p.hostile = true;
        p.towerTarget = tower;
        p.damage.amount = damage;
        p.damage.splashRadius = splashRadius;
        p.damage.source = DamageInfo.Source.ENEMY;
        aimAtPoint(p, tower.x, tower.y + 22f);
        world.events.shotFired(type, p.startX, p.startY, p.targetX, p.targetY);
    }

    /** Hostile shot at a defender. */
    public void fireAtUnit(Enemy source, AlliedUnit unit, ProjectileType type, float damage) {
        Projectile p = obtain(type, source.x + source.facing * source.def.radius, source.y + source.def.radius * 1.6f);
        p.hostile = true;
        p.unitTarget = unit;
        p.unitTargetSerial = unit.serial;
        p.damage.amount = damage;
        p.damage.source = DamageInfo.Source.ENEMY;
        aimAtPoint(p, unit.x, unit.y + 16f);
        world.events.shotFired(type, p.startX, p.startY, p.targetX, p.targetY);
    }

    private Projectile obtain(ProjectileType type, float x, float y) {
        Projectile p = pool.obtain();
        p.type = type;
        p.active = true;
        p.x = p.startX = x;
        p.y = p.startY = y;
        p.speed = type.speed;
        p.lifetime = MAX_LIFETIME;
        active.add(p);
        return p;
    }

    private void launchAt(Projectile p, Enemy target, float extraArcDelay) {
        p.target = target;
        p.targetSerial = target.serial;
        float tx = target.x;
        float ty = target.y + hitHeight(target);
        switch (p.type.motion) {
            case BALLISTIC: {
                float dist = Vector2.dst(p.x, p.y, tx, target.y);
                float flight = Math.max(0.35f, dist / p.type.speed) + extraArcDelay / p.type.speed;
                predictPosition(target, flight, predicted);
                p.duration = flight;
                p.arc = MathUtils.clamp(dist * ARC_FACTOR, MIN_ARC, MAX_ARC);
                p.targetX = predicted.x;
                p.targetY = predicted.y;
                break;
            }
            case BEAM:
                p.targetX = tx;
                p.targetY = ty;
                p.duration = p.type.beamDuration;
                resolveBeam(p);
                break;
            case SEEKING: {
                p.targetX = tx;
                p.targetY = ty;
                float angle = MathUtils.atan2(ty - p.y, tx - p.x) + (world.random.nextFloat() - 0.5f) * 1.8f;
                p.speed = p.type.speed;
                p.vx = MathUtils.cos(angle) * p.speed;
                p.vy = MathUtils.sin(angle) * p.speed;
                p.angle = angle * MathUtils.radiansToDegrees;
                break;
            }
            default:
                aimAtPoint(p, tx, ty);
                break;
        }
    }

    private void aimAtPoint(Projectile p, float tx, float ty) {
        p.targetX = tx;
        p.targetY = ty;
        if (p.type.motion == ProjectileMotion.BALLISTIC) {
            float dist = Vector2.dst(p.x, p.y, tx, ty);
            p.duration = Math.max(0.4f, dist / p.type.speed);
            p.arc = MathUtils.clamp(dist * ARC_FACTOR, MIN_ARC, MAX_ARC);
            return;
        }
        float dx = tx - p.x;
        float dy = ty - p.y;
        float len = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy));
        p.vx = dx / len * p.speed;
        p.vy = dy / len * p.speed;
        p.angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
    }

    /** Where a walking enemy will be after {@code seconds} (blocked enemies stay put). */
    private void predictPosition(Enemy e, float seconds, Vector2 out) {
        if (e.isBlocked() || e.flying) {
            out.set(e.x, e.y);
            return;
        }
        float future = Math.min(e.path.length(), e.distance + e.currentSpeed() * seconds);
        e.path.pointAt(future, out);
        out.x -= e.dirY * e.lateral;
        out.y += e.dirX * e.lateral;
    }

    public void update(float dt) {
        for (int i = active.size - 1; i >= 0; i--) {
            Projectile p = active.get(i);
            if (step(p, dt)) {
                active.removeIndex(i);
                pool.free(p);
            }
        }
    }

    /** @return true when the projectile is finished. */
    private boolean step(Projectile p, float dt) {
        p.age += dt;
        switch (p.type.motion) {
            case BEAM:
                return p.age >= p.duration;
            case BALLISTIC: {
                float t = Math.min(1f, p.age / p.duration);
                float prevX = p.x;
                float prevY = p.y + p.height;
                p.x = MathUtils.lerp(p.startX, p.targetX, t);
                p.y = MathUtils.lerp(p.startY, p.targetY, t);
                p.height = 4f * p.arc * t * (1f - t);
                p.angle = MathUtils.atan2(p.y + p.height - prevY, p.x - prevX) * MathUtils.radiansToDegrees;
                if (t >= 1f) {
                    impact(p);
                    return true;
                }
                return false;
            }
            case SEEKING:
                return stepSeeking(p, dt);
            default:
                return stepDirect(p, dt);
        }
    }

    private boolean stepDirect(Projectile p, float dt) {
        refreshTargetPoint(p);
        float dx = p.targetX - p.x;
        float dy = p.targetY - p.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float travel = p.speed * dt;
        if (dist <= travel + p.type.hitRadius) {
            p.x = p.targetX;
            p.y = p.targetY;
            impact(p);
            return true;
        }
        p.vx = dx / dist * p.speed;
        p.vy = dy / dist * p.speed;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        return p.age >= p.lifetime;
    }

    private boolean stepSeeking(Projectile p, float dt) {
        refreshTargetPoint(p);
        p.speed = Math.min(ROCKET_MAX_SPEED, p.speed + ROCKET_ACCELERATION * dt);
        float desired = MathUtils.atan2(p.targetY - p.y, p.targetX - p.x);
        float current = MathUtils.atan2(p.vy, p.vx);
        float diff = desired - current;
        while (diff > MathUtils.PI) {
            diff -= MathUtils.PI2;
        }
        while (diff < -MathUtils.PI) {
            diff += MathUtils.PI2;
        }
        current += MathUtils.clamp(diff, -ROCKET_TURN_RATE * dt, ROCKET_TURN_RATE * dt);
        p.vx = MathUtils.cos(current) * p.speed;
        p.vy = MathUtils.sin(current) * p.speed;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.angle = current * MathUtils.radiansToDegrees;
        if (Vector2.dst2(p.x, p.y, p.targetX, p.targetY) <= p.type.hitRadius * p.type.hitRadius) {
            impact(p);
            return true;
        }
        return p.age >= p.lifetime;
    }

    private void refreshTargetPoint(Projectile p) {
        if (p.hostile) {
            if (p.hasValidUnitTarget()) {
                p.targetX = p.unitTarget.x;
                p.targetY = p.unitTarget.y + 16f;
            }
        } else if (p.hasValidTarget()) {
            p.targetX = p.target.x;
            p.targetY = p.target.y + hitHeight(p.target);
        }
    }

    private void resolveBeam(Projectile p) {
        if (p.hasValidTarget()) {
            world.damage.damageEnemy(p.target, p.damage);
        }
        world.damage.splash(p.targetX, p.targetY, p.damage, p.target);
        world.events.projectileImpact(p.type, p.targetX, p.targetY, p.damage.splashRadius, false);
    }

    private void impact(Projectile p) {
        if (p.hostile) {
            impactHostile(p);
            return;
        }
        if (p.type.motion == ProjectileMotion.BALLISTIC) {
            if (p.damage.splashRadius <= 0f && p.hasValidTarget()
                    && p.target.dst(p.x, p.y) <= DIRECT_HIT_RADIUS + p.target.def.radius) {
                world.damage.damageEnemy(p.target, p.damage);
            } else {
                world.damage.splash(p.x, p.y, p.damage, null);
            }
        } else {
            Enemy direct = p.hasValidTarget() ? p.target : null;
            if (direct != null) {
                world.damage.damageEnemy(direct, p.damage);
            }
            world.damage.splash(p.x, p.y, p.damage, direct);
        }
        world.events.projectileImpact(p.type, p.x, p.y, p.damage.splashRadius, false);
    }

    private void impactHostile(Projectile p) {
        if (p.towerTarget != null && p.towerTarget.alive) {
            world.damage.damageTower(p.towerTarget, p.damage.amount);
        } else if (p.hasValidUnitTarget()) {
            world.damage.damageUnit(p.unitTarget, p.damage.amount, 0f);
        }
        if (p.damage.splashRadius > 0f) {
            world.damageUnitsInRadius(p.x, p.y, p.damage.splashRadius, p.damage.amount * 0.5f);
        }
        world.events.projectileImpact(p.type, p.x, p.y, p.damage.splashRadius, true);
    }

    public int activeCount() {
        return active.size;
    }

    public void clear() {
        pool.freeAll(active);
        active.clear();
    }
}
