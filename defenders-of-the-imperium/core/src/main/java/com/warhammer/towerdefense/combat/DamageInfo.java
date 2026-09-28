package com.warhammer.towerdefense.combat;

import com.warhammer.towerdefense.towers.Tower;

/** Everything needed to resolve a hit. Instances are reused (projectiles own one each) to avoid allocations. */
public final class DamageInfo {
    public enum Source {
        TOWER, UNIT, HERO, ABILITY, DOT, ENEMY
    }

    public float amount;
    public DamageType type = DamageType.KINETIC;
    public float armorPierce;
    public float eliteMult = 1f;
    public float bossMult = 1f;
    public float splashRadius;
    public float splashFalloff = 0.5f;
    public boolean hitsAir = true;
    public boolean hitsGround = true;
    public StatusType status;
    public float statusChance;
    public float statusDuration;
    public float statusPower;
    public Source source = Source.TOWER;
    /** Kills with this hit grant full XP to the hero. */
    public boolean heroCredit;
    /** Tower credited with damage and kills (tower panel statistics), may be null. */
    public Tower tower;

    public DamageInfo reset() {
        amount = 0f;
        type = DamageType.KINETIC;
        armorPierce = 0f;
        eliteMult = 1f;
        bossMult = 1f;
        splashRadius = 0f;
        splashFalloff = 0.5f;
        hitsAir = true;
        hitsGround = true;
        status = null;
        statusChance = 0f;
        statusDuration = 0f;
        statusPower = 0f;
        source = Source.TOWER;
        heroCredit = false;
        tower = null;
        return this;
    }

    public DamageInfo set(DamageInfo o) {
        amount = o.amount;
        type = o.type;
        armorPierce = o.armorPierce;
        eliteMult = o.eliteMult;
        bossMult = o.bossMult;
        splashRadius = o.splashRadius;
        splashFalloff = o.splashFalloff;
        hitsAir = o.hitsAir;
        hitsGround = o.hitsGround;
        status = o.status;
        statusChance = o.statusChance;
        statusDuration = o.statusDuration;
        statusPower = o.statusPower;
        source = o.source;
        heroCredit = o.heroCredit;
        tower = o.tower;
        return this;
    }

    public DamageInfo withStatus(StatusType s, float chance, float duration, float power) {
        status = s;
        statusChance = chance;
        statusDuration = duration;
        statusPower = power;
        return this;
    }
}
