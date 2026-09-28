package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Numeric tower attributes. Tiers, specialisations and ranks in {@code towers.json} modify these by name, e.g.
 * {@code "cooldown": "*0.8"} or {@code "damageMax": "+4"}.
 */
public enum Stat {
    // ranged weapons
    DAMAGE_MIN("damageMin", 0f),
    DAMAGE_MAX("damageMax", 0f),
    RANGE("range", 0f),
    COOLDOWN("cooldown", 1f),
    SPLASH_RADIUS("splashRadius", 0f),
    SPLASH_FALLOFF("splashFalloff", 0.5f),
    ARMOR_PIERCE("armorPierce", 0f),
    ELITE_DAMAGE("eliteDamage", 1f),
    BOSS_DAMAGE("bossDamage", 1f),
    EXTRA_SHOT_CHANCE("extraShotChance", 0f),
    MISSILES("missiles", 1f),
    OVERHEAT_SHOTS("overheatShots", 0f),
    OVERHEAT_TIME("overheatTime", 0f),
    STATUS_CHANCE("statusChance", 1f),
    STATUS_DURATION("statusDuration", 0f),
    STATUS_POWER("statusPower", 0f),
    // structure
    CREW_COUNT("crewCount", 1f),
    INTEGRITY("integrity", 200f),
    TOWER_ARMOR("towerArmor", 0f),
    // barracks
    UNIT_COUNT("unitCount", 0f),
    UNIT_HP("unitHp", 100f),
    UNIT_ARMOR("unitArmor", 0f),
    UNIT_DAMAGE_MIN("unitDamageMin", 5f),
    UNIT_DAMAGE_MAX("unitDamageMax", 8f),
    UNIT_COOLDOWN("unitCooldown", 1f),
    UNIT_RESPAWN("unitRespawn", 10f),
    UNIT_REGEN("unitRegen", 4f),
    UNIT_RANGED_DAMAGE("unitRangedDamage", 0f),
    UNIT_RANGE("unitRange", 0f),
    UNIT_RANGED_COOLDOWN("unitRangedCooldown", 1.2f),
    UNIT_CLEAVE("unitCleave", 0f),
    UNIT_MAX_ENGAGED("unitMaxEngaged", 1f),
    RALLY_RANGE("rallyRange", 160f),
    CHAPLAIN("chaplain", 0f),
    CHAPLAIN_HEAL("chaplainHeal", 0f),
    CHAPLAIN_DAMAGE("chaplainDamage", 0f),
    CHAPLAIN_STUN("chaplainStun", 0f),
    // support
    AURA_RADIUS("auraRadius", 0f),
    AURA_RANGE("auraRange", 0f),
    AURA_FIRE_RATE("auraFireRate", 0f),
    AURA_DAMAGE("auraDamage", 0f),
    AURA_ARMOR("auraArmor", 0f),
    REPAIR("repair", 0f),
    SKULLS("skulls", 0f),
    SKULL_DAMAGE("skullDamage", 0f),
    SKULL_COOLDOWN("skullCooldown", 1.5f),
    MARK_DURATION("markDuration", 0f),
    MARK_POWER("markPower", 0f),
    BARRIER_HP("barrierHp", 0f),
    BARRIER_DURATION("barrierDuration", 0f),
    BARRIER_COOLDOWN("barrierCooldown", 15f);

    public static final int COUNT = values().length;
    private static final ObjectMap<String, Stat> BY_KEY = new ObjectMap<>();

    static {
        for (Stat s : values()) {
            BY_KEY.put(s.key, s);
        }
    }

    public final String key;
    public final float defaultValue;

    Stat(String key, float defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /** @return the stat for a JSON key, or null. */
    public static Stat byKey(String key) {
        return BY_KEY.get(key);
    }
}
