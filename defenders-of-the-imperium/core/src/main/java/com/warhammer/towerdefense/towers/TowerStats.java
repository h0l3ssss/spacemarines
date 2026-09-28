package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

import java.util.Arrays;

/**
 * The fully resolved attributes of a tower at its current tier/specialisation/rank. Numeric values are stored in a
 * flat array indexed by {@link Stat}; a handful of non numeric properties (projectile, sprite, unit type...) are
 * plain fields. Instances are recomputed only when the tower changes, never per frame.
 */
public final class TowerStats {
    public static final String PROP_PROJECTILE = "projectile";
    public static final String PROP_DAMAGE_TYPE = "damageType";
    public static final String PROP_STATUS = "status";
    public static final String PROP_CAN_HIT_AIR = "canHitAir";
    public static final String PROP_UNIT_TYPE = "unitType";
    public static final String PROP_SPRITE = "sprite";
    public static final String PROP_CREW = "crew";

    private final float[] values = new float[Stat.COUNT];
    public ProjectileType projectile;
    public DamageType damageType;
    public StatusType status;
    public boolean canHitAir;
    public String unitType;
    public String sprite;
    public String crew;

    public TowerStats() {
        reset();
    }

    public void reset() {
        for (Stat s : Stat.values()) {
            values[s.ordinal()] = s.defaultValue;
        }
        projectile = ProjectileType.BOLT;
        damageType = DamageType.KINETIC;
        status = null;
        canHitAir = true;
        unitType = null;
        sprite = null;
        crew = null;
    }

    public float get(Stat stat) {
        return values[stat.ordinal()];
    }

    public int getInt(Stat stat) {
        return Math.round(values[stat.ordinal()]);
    }

    public void set(Stat stat, float value) {
        values[stat.ordinal()] = value;
    }

    public void copyFrom(TowerStats other) {
        System.arraycopy(other.values, 0, values, 0, values.length);
        projectile = other.projectile;
        damageType = other.damageType;
        status = other.status;
        canHitAir = other.canHitAir;
        unitType = other.unitType;
        sprite = other.sprite;
        crew = other.crew;
    }

    public float averageDamage() {
        return (get(Stat.DAMAGE_MIN) + get(Stat.DAMAGE_MAX)) * 0.5f;
    }

    /** Applies a non numeric property. Values were validated when the data was loaded. */
    public void applyProperty(String key, String value) {
        switch (key) {
            case PROP_PROJECTILE:
                projectile = ProjectileType.valueOf(value);
                break;
            case PROP_DAMAGE_TYPE:
                damageType = DamageType.valueOf(value);
                break;
            case PROP_STATUS:
                status = "NONE".equals(value) ? null : StatusType.valueOf(value);
                break;
            case PROP_CAN_HIT_AIR:
                canHitAir = Boolean.parseBoolean(value);
                break;
            case PROP_UNIT_TYPE:
                unitType = value;
                break;
            case PROP_SPRITE:
                sprite = value;
                break;
            case PROP_CREW:
                crew = value;
                break;
            default:
                throw new IllegalArgumentException("Unknown tower property " + key);
        }
    }

    public static boolean isProperty(String key) {
        return Arrays.asList(PROP_PROJECTILE, PROP_DAMAGE_TYPE, PROP_STATUS, PROP_CAN_HIT_AIR, PROP_UNIT_TYPE,
                PROP_SPRITE, PROP_CREW).contains(key);
    }

    /** Validates and normalises a property value at load time so typos fail fast with a clear message. */
    public static String normaliseProperty(String key, String raw, String context) {
        switch (key) {
            case PROP_PROJECTILE:
                return JsonUtil.parseEnum(raw, ProjectileType.class, context).name();
            case PROP_DAMAGE_TYPE:
                return JsonUtil.parseEnum(raw, DamageType.class, context).name();
            case PROP_STATUS:
                return "NONE".equalsIgnoreCase(raw) ? "NONE" : JsonUtil.parseEnum(raw, StatusType.class, context).name();
            case PROP_CAN_HIT_AIR:
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new DataException(context + ": canHitAir must be true or false");
                }
                return raw.toLowerCase();
            default:
                return raw;
        }
    }
}
