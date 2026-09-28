package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectFloatMap;

/**
 * Shared definition for hero abilities and global (orbital) abilities. Tunable numbers live in a free-form
 * {@code params} object so a new ability only needs a new implementation class, never a new data class.
 */
public final class AbilityDefinition {
    public String id;
    public String name;
    public String description = "";
    public String key = "";
    public String icon;
    public int cost;
    public int unlockLevel = 1;
    public float cooldown;
    /** Radius shown while the player chooses where to cast a targeted ability. */
    public float targetRadius;
    public boolean targeted;
    public final ObjectFloatMap<String> params = new ObjectFloatMap<>();

    public float param(String name, float fallback) {
        return params.get(name, fallback);
    }

    public static AbilityDefinition parse(String id, JsonValue v, String context) {
        String ctx = context + " (" + id + ")";
        AbilityDefinition d = new AbilityDefinition();
        d.id = id;
        d.name = JsonUtil.requireString(v, "name", ctx);
        d.description = v.getString("description", "");
        d.key = v.getString("key", "");
        d.icon = v.getString("icon", "icon_ability_" + id);
        d.cost = v.getInt("cost", 0);
        d.unlockLevel = v.getInt("unlockLevel", 1);
        d.cooldown = JsonUtil.requireFloat(v, "cooldown", ctx);
        d.targetRadius = v.getFloat("targetRadius", 0f);
        d.targeted = v.getBoolean("targeted", false);
        d.params.putAll(JsonUtil.floatParams(v.get("params")));
        return d;
    }
}
