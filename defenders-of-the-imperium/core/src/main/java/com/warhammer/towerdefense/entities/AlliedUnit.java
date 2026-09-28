package com.warhammer.towerdefense.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * A defender that can block enemies on the road (Kingdom Rush style): Space Marines from barracks or drop pods, the
 * hero and Techmarine void barriers. Blocked enemies stop walking and fight the unit that holds them.
 */
public class AlliedUnit extends Entity {
    public String sprite;
    public String name = "";
    public float hp;
    public float maxHp;
    public float armor;
    public float speed = 60f;
    public float regen;
    public float radius = 10f;

    public float damageMin;
    public float damageMax;
    public float attackCooldown = 1f;
    public float meleeRange = 22f;
    public DamageType damageType = DamageType.KINETIC;
    public int cleave;
    public int maxEngaged = 1;
    public StatusType onHitStatus;
    public float onHitChance;
    public float onHitDuration;
    public float onHitPower;

    public float rangedDamage;
    public float rangedRange;
    public float rangedCooldown = 1.2f;
    public ProjectileType rangedProjectile = ProjectileType.BOLT;
    public boolean rangedWhileMelee;

    public float guardX;
    public float guardY;
    public float engageRadius = 70f;
    public final Array<Enemy> engaged = new Array<>(false, 4);
    public Enemy target;
    public int targetSerial;

    public UnitState state = UnitState.IDLE;
    public float stateTime;
    public float facing = 1f;
    public float attackTimer;
    public float rangedTimer;
    public float hitFlash;
    public float attackAnim;
    public float stunTimer;
    /** Seconds until the unit leaves the battlefield (-1 = permanent). */
    public float lifetime = -1f;

    public float damageMult = 1f;
    public float damageTakenMult = 1f;
    public float speedMult = 1f;
    public float buffDamage;
    public float buffTimer;
    // Aura contributions, reset every frame by the world and re-applied by chaplains / techmarines.
    public float auraDamage;
    public float auraHeal;
    public float auraArmor;

    public boolean blocksEnemies = true;
    public boolean mobile = true;
    public boolean canAttack = true;
    public Tower owner;
    public int squadIndex;

    public boolean moveOrder;
    public float moveX;
    public float moveY;

    public void spawn(float px, float py) {
        assignSerial();
        alive = true;
        x = px;
        y = py;
        hp = maxHp;
        state = UnitState.IDLE;
        stateTime = 0f;
        engaged.clear();
        target = null;
    }

    public void update(GameWorld w, float dt) {
        if (!alive) {
            return;
        }
        stateTime += dt;
        attackTimer -= dt;
        rangedTimer -= dt;
        hitFlash = Math.max(0f, hitFlash - dt * 4f);
        attackAnim = Math.max(0f, attackAnim - dt);
        if (buffTimer > 0f) {
            buffTimer -= dt;
            if (buffTimer <= 0f) {
                buffDamage = 0f;
            }
        }
        if (auraHeal > 0f && hp < maxHp) {
            hp = Math.min(maxHp, hp + auraHeal * dt);
        }
        if (lifetime > 0f) {
            lifetime -= dt;
            if (lifetime <= 0f) {
                w.retireUnit(this);
                return;
            }
        }
        pruneEngaged();
        if (stunTimer > 0f) {
            stunTimer -= dt;
            return;
        }
        if (moveOrder) {
            if (moveTowards(moveX, moveY, dt)) {
                moveOrder = false;
                guardX = moveX;
                guardY = moveY;
                state = UnitState.IDLE;
            } else {
                state = UnitState.MOVING;
            }
            return;
        }
        if (!canAttack) {
            return;
        }
        if (!targetValid() || leashBroken()) {
            target = null;
        }
        if (target == null) {
            acquireTarget(w);
        }
        if (target != null) {
            fight(w, dt);
        } else if (mobile) {
            if (moveTowards(guardX, guardY, dt)) {
                state = UnitState.IDLE;
                if (hp < maxHp) {
                    hp = Math.min(maxHp, hp + regen * dt);
                }
            } else {
                state = UnitState.MOVING;
            }
        } else {
            state = UnitState.IDLE;
        }
        tryRangedAttack(w);
    }

    protected void acquireTarget(GameWorld w) {
        if (engaged.size > 0) {
            setTarget(engaged.first());
            return;
        }
        Enemy free = w.targeting.findEngageable(guardX, guardY, engageRadius);
        if (free != null) {
            engage(free);
            return;
        }
        Enemy assist = w.targeting.findNearest(guardX, guardY, engageRadius, false, true);
        if (assist != null) {
            setTarget(assist);
        }
    }

    public void engage(Enemy e) {
        if (blocksEnemies && engaged.size < maxEngaged && !e.isBlocked()) {
            e.blocker = this;
            e.blockerSerial = serial;
            engaged.add(e);
        }
        if (target == null) {
            setTarget(e);
        }
    }

    protected void setTarget(Enemy e) {
        target = e;
        targetSerial = e.serial;
    }

    protected boolean targetValid() {
        return target != null && target.serial == targetSerial && target.isTargetable() && !target.flying;
    }

    private boolean leashBroken() {
        if (target == null || engaged.contains(target, true)) {
            return false;
        }
        return target.dst(guardX, guardY) > engageRadius + 40f;
    }

    protected void fight(GameWorld w, float dt) {
        Enemy t = target;
        if (blocksEnemies && engaged.size < maxEngaged) {
            Enemy extra = w.targeting.findEngageable(x, y, meleeRange + 24f);
            if (extra != null) {
                engage(extra);
            }
        }
        float reach = t.def.radius + radius;
        float side = (squadIndex % 3) - 1f;
        float standX = t.x + t.dirX * reach - t.dirY * side * 12f;
        float standY = t.y + t.dirY * reach + t.dirX * side * 12f;
        float dist = t.dst(x, y);
        if (dist > meleeRange + t.def.radius) {
            if (mobile) {
                moveTowards(standX, standY, dt);
                state = UnitState.MOVING;
            }
            return;
        }
        state = UnitState.FIGHTING;
        facing = t.x >= x ? 1f : -1f;
        if (attackTimer <= 0f) {
            attackTimer = attackCooldown;
            attackAnim = 0.3f;
            strike(w, t);
        }
    }

    protected void strike(GameWorld w, Enemy t) {
        float dmg = MathUtils.lerp(damageMin, damageMax, w.random.nextFloat()) * damageMultiplier();
        w.damage.unitMelee(this, t, dmg);
        if (cleave > 0) {
            w.damage.cleave(this, t, dmg * 0.6f, cleave);
        }
    }

    protected void tryRangedAttack(GameWorld w) {
        if (rangedDamage <= 0f || rangedTimer > 0f) {
            return;
        }
        if (state == UnitState.FIGHTING && !rangedWhileMelee) {
            return;
        }
        Enemy e = w.targeting.findNearest(x, y, rangedRange, true, true);
        if (e == null) {
            return;
        }
        rangedTimer = rangedCooldown;
        attackAnim = Math.max(attackAnim, 0.2f);
        if (state != UnitState.FIGHTING) {
            facing = e.x >= x ? 1f : -1f;
        }
        w.projectiles.fireFromUnit(this, e, rangedDamage * damageMultiplier());
    }

    /** @return true when the destination was reached. */
    protected boolean moveTowards(float tx, float ty, float dt) {
        float dx = tx - x;
        float dy = ty - y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d <= Constants.UNIT_ARRIVE_DISTANCE) {
            return true;
        }
        float step = speed * speedMult * dt;
        if (Math.abs(dx) > 1f) {
            facing = dx > 0f ? 1f : -1f;
        }
        if (step >= d) {
            x = tx;
            y = ty;
            return true;
        }
        x += dx / d * step;
        y += dy / d * step;
        return false;
    }

    protected void pruneEngaged() {
        for (int i = engaged.size - 1; i >= 0; i--) {
            Enemy e = engaged.get(i);
            if (!e.isTargetable() || e.blocker != this || e.blockerSerial != serial) {
                engaged.removeIndex(i);
            }
        }
    }

    public float damageMultiplier() {
        return damageMult * (1f + buffDamage + auraDamage);
    }

    public void takeDamage(GameWorld w, float amount, float armorIgnore) {
        if (!alive) {
            return;
        }
        float effectiveArmor = Math.min(0.85f, (armor + auraArmor) * (1f - armorIgnore));
        hp -= amount * (1f - effectiveArmor) * damageTakenMult;
        hitFlash = 1f;
        if (hp <= 0f) {
            die(w);
        }
    }

    protected void die(GameWorld w) {
        hp = 0f;
        alive = false;
        state = UnitState.DEAD;
        releaseAll();
        w.onUnitDied(this);
    }

    public void releaseAll() {
        for (Enemy e : engaged) {
            if (e.blocker == this) {
                e.blocker = null;
            }
        }
        engaged.clear();
        target = null;
    }

    public float hpFraction() {
        return maxHp <= 0f ? 0f : Math.max(0f, hp / maxHp);
    }

    public boolean isHero() {
        return false;
    }

    public void resetAuras() {
        auraDamage = 0f;
        auraHeal = 0f;
        auraArmor = 0f;
    }
}
