package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.JsonValue;

/**
 * Per-difficulty tuning. Instead of blindly multiplying everything, each axis is adjusted moderately and HARD adds
 * qualitative pressure (extra armour, faster boss abilities, lower income).
 */
public final class DifficultyDefinition {
    public Difficulty id = Difficulty.VETERAN;
    public String name = "VETERAN";
    public String description = "";
    public float enemyHp = 1f;
    public float enemyDamage = 1f;
    public float enemySpeed = 1f;
    public float enemyArmorBonus;
    public float leakDamage = 1f;
    public float income = 1f;
    public int startingRequisitionBonus;
    public float bossHp = 1f;
    /** Multiplier applied to boss ability cooldowns (lower = more frequent). */
    public float bossCooldown = 1f;

    public static DifficultyDefinition defaults(Difficulty id) {
        DifficultyDefinition d = new DifficultyDefinition();
        d.id = id;
        d.name = id.name();
        return d;
    }

    public static DifficultyDefinition parse(Difficulty id, JsonValue v) {
        DifficultyDefinition d = defaults(id);
        d.name = v.getString("name", id.name());
        d.description = v.getString("description", "");
        d.enemyHp = v.getFloat("enemyHp", 1f);
        d.enemyDamage = v.getFloat("enemyDamage", 1f);
        d.enemySpeed = v.getFloat("enemySpeed", 1f);
        d.enemyArmorBonus = v.getFloat("enemyArmorBonus", 0f);
        d.leakDamage = v.getFloat("leakDamage", 1f);
        d.income = v.getFloat("income", 1f);
        d.startingRequisitionBonus = v.getInt("startingRequisitionBonus", 0);
        d.bossHp = v.getFloat("bossHp", 1f);
        d.bossCooldown = v.getFloat("bossCooldown", 1f);
        return d;
    }
}
