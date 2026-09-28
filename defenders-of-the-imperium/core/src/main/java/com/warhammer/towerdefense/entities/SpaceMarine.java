package com.warhammer.towerdefense.entities;

import com.warhammer.towerdefense.data.UnitDefinition;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Infantry deployed by an Assault Bastion or an Orbital Drop. A Chaplain is a Space Marine with an aura that heals
 * and inspires nearby battle-brothers.
 */
public class SpaceMarine extends AlliedUnit {
    public UnitDefinition def;
    public float auraRadius;
    public float auraHealValue;
    public float auraDamageValue;

    public void configure(UnitDefinition def) {
        this.def = def;
        this.sprite = def.sprite;
        this.name = def.name;
        this.speed = def.speed;
        this.meleeRange = def.meleeRange;
        this.radius = def.radius;
        this.damageType = def.damageType;
        this.onHitStatus = def.onHitStatus;
        this.onHitChance = def.onHitChance;
        this.onHitDuration = def.onHitDuration;
        this.onHitPower = def.onHitPower;
        this.auraRadius = def.auraRadius;
        if (def.rangedProjectile != null) {
            this.rangedProjectile = def.rangedProjectile;
        }
    }

    public boolean hasAura() {
        return auraRadius > 0f && (auraHealValue > 0f || auraDamageValue > 0f);
    }

    /** Pushes this unit's aura onto nearby allies (including itself and the hero). */
    public void applyAura(GameWorld w) {
        if (!alive || !hasAura()) {
            return;
        }
        float r2 = auraRadius * auraRadius;
        for (AlliedUnit u : w.units) {
            if (u.alive && u.dst2(x, y) <= r2) {
                u.auraHeal = Math.max(u.auraHeal, auraHealValue);
                u.auraDamage = Math.max(u.auraDamage, auraDamageValue);
            }
        }
        if (w.hero != null && w.hero.alive && w.hero.dst2(x, y) <= r2) {
            w.hero.auraHeal = Math.max(w.hero.auraHeal, auraHealValue);
            w.hero.auraDamage = Math.max(w.hero.auraDamage, auraDamageValue);
        }
    }
}
