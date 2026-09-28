package com.warhammer.towerdefense.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.EnemyDefinition;
import com.warhammer.towerdefense.enemies.BossController;
import com.warhammer.towerdefense.map.GamePath;

import java.util.Arrays;

/** A hostile unit walking a path. Pooled: {@link #reset()} clears every field before reuse. */
public final class Enemy extends Entity implements Pool.Poolable {
    public EnemyDefinition def;
    public GamePath path;
    public int pathHint;
    /** Distance travelled along {@link #path}. */
    public float distance;
    /** Sideways offset from the path centre line, so hordes spread across the road. */
    public float lateral;
    /** Wave number that spawned this enemy (used to detect wave completion). */
    public int waveIndex;

    public float hp;
    public float maxHp;
    public float armor;
    public float baseSpeed;
    public float damageMult = 1f;
    public boolean flying;

    public EnemyState state = EnemyState.WALKING;
    public float stateTime;
    public float animTime;
    public float facing = -1f;
    public float dirX = -1f;
    public float dirY;

    public AlliedUnit blocker;
    public int blockerSerial;
    public float attackTimer;
    public float rangedTimer;
    public float castTimer;
    public boolean pounceReady;

    public final float[] statusTime = new float[StatusType.COUNT];
    public final float[] statusPower = new float[StatusType.COUNT];
    public boolean synapseBuffed;

    public float hitFlash;
    public float deathTimer;
    public BossController boss;
    public float bossSpeedMult = 1f;
    public float bossDamageMult = 1f;

    private final Vector2 tmpPos = new Vector2();
    private final Vector2 tmpDir = new Vector2();

    public void init(EnemyDefinition def, GamePath path, float lateral, int waveIndex, float hp, float speed,
                     float damageMult, float armor) {
        assignSerial();
        this.def = def;
        this.path = path;
        this.lateral = lateral;
        this.waveIndex = waveIndex;
        this.hp = hp;
        this.maxHp = hp;
        this.baseSpeed = speed;
        this.damageMult = damageMult;
        this.armor = armor;
        this.flying = def.flying;
        this.alive = true;
        this.state = EnemyState.WALKING;
        this.pounceReady = true;
        this.distance = 0f;
        this.pathHint = 0;
        this.attackTimer = def.meleeCooldown * 0.5f;
        this.rangedTimer = def.rangedCooldown * 0.5f;
        placeOnPath();
    }

    @Override
    public void reset() {
        def = null;
        path = null;
        blocker = null;
        boss = null;
        alive = false;
        state = EnemyState.WALKING;
        distance = 0f;
        pathHint = 0;
        lateral = 0f;
        hp = maxHp = 0f;
        stateTime = animTime = 0f;
        attackTimer = rangedTimer = castTimer = 0f;
        hitFlash = deathTimer = 0f;
        synapseBuffed = false;
        pounceReady = false;
        bossSpeedMult = bossDamageMult = 1f;
        facing = -1f;
        Arrays.fill(statusTime, 0f);
        Arrays.fill(statusPower, 0f);
    }

    /** Recomputes the world position from the distance travelled and lateral offset. */
    public void placeOnPath() {
        pathHint = path.sample(distance, pathHint, tmpPos, tmpDir);
        dirX = tmpDir.x;
        dirY = tmpDir.y;
        x = tmpPos.x - tmpDir.y * lateral;
        y = tmpPos.y + tmpDir.x * lateral;
        if (Math.abs(dirX) > 0.2f) {
            facing = dirX > 0f ? 1f : -1f;
        }
    }

    public boolean isTargetable() {
        return alive && state != EnemyState.DYING && state != EnemyState.LEAKED;
    }

    public boolean isOnMap() {
        return x >= 0f && x <= Constants.WORLD_WIDTH && y >= 0f && y <= Constants.WORLD_HEIGHT;
    }

    public boolean isBlocked() {
        return blocker != null && blocker.alive && blocker.serial == blockerSerial;
    }

    public boolean isBlockable() {
        return !flying && isTargetable();
    }

    public float remainingDistance() {
        return path.length() - distance;
    }

    public boolean has(StatusType status) {
        return statusTime[status.ordinal()] > 0f;
    }

    public float power(StatusType status) {
        return statusTime[status.ordinal()] > 0f ? statusPower[status.ordinal()] : 0f;
    }

    /** Applies or refreshes a status, keeping the longest duration and the strongest power. */
    public void applyStatus(StatusType status, float duration, float power) {
        int i = status.ordinal();
        statusTime[i] = Math.max(statusTime[i], duration);
        statusPower[i] = Math.max(statusPower[i], power);
    }

    public void clearStatus(StatusType status) {
        statusTime[status.ordinal()] = 0f;
        statusPower[status.ordinal()] = 0f;
    }

    public float currentSpeed() {
        if (has(StatusType.STUN)) {
            return 0f;
        }
        float speed = baseSpeed * bossSpeedMult;
        speed *= 1f - Math.min(0.75f, power(StatusType.SLOW));
        if (synapseBuffed) {
            speed *= 1f + def.synapseSpeedBonus;
        }
        return speed;
    }

    public float currentDamageMultiplier() {
        float mult = damageMult * bossDamageMult;
        if (synapseBuffed) {
            mult *= 1f + def.synapseDamageBonus;
        }
        return mult;
    }

    public float effectiveArmor() {
        return Math.max(0f, armor - power(StatusType.ARMOR_BREAK));
    }

    public float hpFraction() {
        return maxHp <= 0f ? 0f : Math.max(0f, hp / maxHp);
    }
}
