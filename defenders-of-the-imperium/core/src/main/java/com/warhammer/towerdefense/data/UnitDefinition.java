package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;

/**
 * Visual and behavioural archetype of an allied infantry unit (Assault Intercessor, Terminator, Chaplain...).
 * Numbers such as hp and damage come from whoever deploys the unit (barracks tower stats or an ability), so the
 * same archetype scales with upgrades.
 */
public final class UnitDefinition {
    public String id;
    public String name;
    public String sprite;
    public float speed = 60f;
    public float meleeRange = 22f;
    public float radius = 10f;
    public DamageType damageType = DamageType.KINETIC;
    public ProjectileType rangedProjectile;
    public StatusType onHitStatus;
    public float onHitChance;
    public float onHitDuration;
    public float onHitPower;
    public float auraRadius;
    public String attackSound = "CHAINSWORD";

    public static UnitDefinition parse(String id, JsonValue v) {
        String ctx = "units.json (" + id + ")";
        UnitDefinition d = new UnitDefinition();
        d.id = id;
        d.name = JsonUtil.requireString(v, "name", ctx);
        d.sprite = JsonUtil.requireString(v, "sprite", ctx);
        d.speed = v.getFloat("speed", 60f);
        d.meleeRange = v.getFloat("meleeRange", 22f);
        d.radius = v.getFloat("radius", 10f);
        d.damageType = JsonUtil.enumValue(v, "damageType", DamageType.class, DamageType.KINETIC, ctx);
        d.rangedProjectile = JsonUtil.enumValue(v, "rangedProjectile", ProjectileType.class, null, ctx);
        d.attackSound = v.getString("attackSound", "CHAINSWORD");
        JsonValue onHit = v.get("onHit");
        if (onHit != null) {
            d.onHitStatus = JsonUtil.enumValue(onHit, "status", StatusType.class, null, ctx + ".onHit");
            d.onHitChance = onHit.getFloat("chance", 1f);
            d.onHitDuration = onHit.getFloat("duration", 1f);
            d.onHitPower = onHit.getFloat("power", 0f);
        }
        JsonValue aura = v.get("aura");
        if (aura != null) {
            d.auraRadius = aura.getFloat("radius", 90f);
        }
        return d;
    }
}
