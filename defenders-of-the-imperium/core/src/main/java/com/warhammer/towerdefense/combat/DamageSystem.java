package com.warhammer.towerdefense.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.enemies.FactionMechanic;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/** Resolves every hit in the game: armour, elite/boss multipliers, marks, shields, statuses, kills and rewards. */
public final class DamageSystem {
    private static final float MAX_ARMOR = 0.9f;
    private static final float MIN_SPLASH_FACTOR = 0.15f;
    private static final float CLEAVE_RADIUS = 36f;

    private final GameWorld world;
    private final DamageInfo meleeInfo = new DamageInfo();
    private final DamageInfo dotInfo = new DamageInfo();
    private final Array<Enemy> splashTargets = new Array<>(false, 64);

    public DamageSystem(GameWorld world) {
        this.world = world;
        dotInfo.type = DamageType.TRUE;
        dotInfo.source = DamageInfo.Source.DOT;
    }

    /** Damage left after armour: effective armour = armour * typeFactor - pierce, clamped to [0, 0.9]. */
    public static float mitigate(float amount, float armor, DamageType type, float armorPierce) {
        float effective = MathUtils.clamp(armor * type.armorFactor - armorPierce, 0f, MAX_ARMOR);
        return amount * (1f - effective);
    }

    /** Final damage of a hit before shields, including elite/boss bonuses and the MARKED status. */
    public static float computeDamage(Enemy e, DamageInfo info, float amount) {
        float mult = 1f;
        if (e.def.elite) {
            mult *= info.eliteMult;
        }
        if (e.def.boss) {
            mult *= info.bossMult;
        }
        mult *= 1f + e.power(StatusType.MARKED);
        return mitigate(amount * mult, e.effectiveArmor(), info.type, info.armorPierce);
    }

    public float damageEnemy(Enemy e, DamageInfo info) {
        return damageEnemy(e, info, info.amount);
    }

    public float damageEnemy(Enemy e, DamageInfo info, float amount) {
        if (!e.isTargetable()) {
            return 0f;
        }
        float dealt = absorbShield(e, computeDamage(e, info, amount));
        e.hp -= dealt;
        e.hitFlash = 1f;
        if (info.tower != null) {
            info.tower.damageDealt += dealt;
        }
        if (dealt > 0f && info.source != DamageInfo.Source.DOT) {
            world.events.enemyHit(e, dealt, info.type);
        }
        if (info.status != null && info.statusDuration > 0f
                && (info.statusChance >= 1f || world.random.nextFloat() < info.statusChance)) {
            e.applyStatus(info.status, info.statusDuration, info.statusPower);
            world.events.statusApplied(e, info.status);
        }
        if (e.hp <= 0f) {
            if (info.tower != null) {
                info.tower.kills++;
            }
            kill(e, info);
        }
        return dealt;
    }

    private static float absorbShield(Enemy e, float damage) {
        float shield = e.power(StatusType.SHIELD);
        if (shield <= 0f) {
            return damage;
        }
        int i = StatusType.SHIELD.ordinal();
        float absorbed = Math.min(shield, damage);
        e.statusPower[i] -= absorbed;
        if (e.statusPower[i] <= 0.01f) {
            e.clearStatus(StatusType.SHIELD);
        }
        return damage - absorbed;
    }

    /** Damage over time (burn, bleed): true damage, no hit events. */
    public void damageOverTime(Enemy e, float amount) {
        dotInfo.amount = amount;
        damageEnemy(e, dotInfo, amount);
    }

    /** Area damage with linear falloff; {@code exclude} already took the direct hit. */
    public void splash(float x, float y, DamageInfo info, Enemy exclude) {
        if (info.splashRadius <= 0f) {
            return;
        }
        splashTargets.clear();
        world.targeting.collect(x, y, info.splashRadius, info.hitsAir, info.hitsGround, splashTargets);
        for (int i = 0; i < splashTargets.size; i++) {
            Enemy e = splashTargets.get(i);
            if (e == exclude) {
                continue;
            }
            float falloff = 1f - info.splashFalloff * (e.dst(x, y) / info.splashRadius);
            damageEnemy(e, info, info.amount * Math.max(MIN_SPLASH_FACTOR, falloff));
        }
    }

    public void unitMelee(AlliedUnit attacker, Enemy target, float amount) {
        meleeInfo.reset();
        meleeInfo.amount = amount;
        meleeInfo.type = attacker.damageType;
        meleeInfo.source = attacker.isHero() ? DamageInfo.Source.HERO : DamageInfo.Source.UNIT;
        meleeInfo.heroCredit = attacker.isHero();
        meleeInfo.tower = attacker.owner;
        if (attacker.onHitStatus != null) {
            meleeInfo.withStatus(attacker.onHitStatus, attacker.onHitChance, attacker.onHitDuration, attacker.onHitPower);
        }
        world.events.unitMelee(attacker, target, amount);
        damageEnemy(target, meleeInfo);
    }

    /** Extra hits on up to {@code count} ground enemies around the primary target. */
    public void cleave(AlliedUnit attacker, Enemy primary, float amount, int count) {
        splashTargets.clear();
        world.targeting.collect(primary.x, primary.y, CLEAVE_RADIUS, false, true, splashTargets);
        int hits = 0;
        for (int i = 0; i < splashTargets.size && hits < count; i++) {
            Enemy e = splashTargets.get(i);
            if (e != primary) {
                unitMelee(attacker, e, amount);
                hits++;
            }
        }
    }

    public void damageUnit(AlliedUnit unit, float amount, float armorIgnore) {
        unit.takeDamage(world, amount, armorIgnore);
    }

    /** Enemy fire against a structure; at zero integrity the tower goes offline for a while. */
    public void damageTower(Tower tower, float amount) {
        if (tower.isOffline() || tower.isBuilding()) {
            return;
        }
        float armor = Math.min(0.8f, tower.stats.get(Stat.TOWER_ARMOR) + tower.buffArmor);
        tower.integrity -= amount * (1f - armor);
        if (tower.integrity <= 0f) {
            tower.integrity = 0f;
            suppressTower(tower, Constants.TOWER_OFFLINE_TIME);
        }
    }

    public void suppressTower(Tower tower, float seconds) {
        boolean wasOnline = !tower.isOffline();
        tower.offlineTimer = Math.max(tower.offlineTimer, seconds);
        if (wasOnline) {
            world.events.towerOffline(tower);
        }
    }

    public void kill(Enemy e, DamageInfo info) {
        if (e.state == EnemyState.DYING || e.state == EnemyState.LEAKED) {
            return;
        }
        e.hp = 0f;
        e.state = EnemyState.DYING;
        e.deathTimer = 0f;
        e.stateTime = 0f;
        if (e.blocker != null) {
            e.blocker.engaged.removeValue(e, true);
            e.blocker = null;
        }
        int bounty = world.bounty(e);
        world.state.earn(bounty);
        world.state.kills++;
        world.grantKillXp(e, info.heroCredit);
        for (FactionMechanic mechanic : world.mechanics) {
            mechanic.onEnemyKilled(world, e);
        }
        world.waves.onEnemyRemoved(e, false);
        world.events.enemyKilled(e, bounty);
        if (bounty > 0) {
            world.events.requisitionGained(bounty, e.x, e.y + e.def.radius * 2f);
        }
    }
}
