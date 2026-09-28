package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;

/** Hero stats, level curve and abilities loaded from {@code assets/data/heroes.json}. */
public final class HeroDefinition {
    public String id;
    public String name;
    public String title = "";
    public String description = "";
    public String sprite;
    public String portrait;
    public boolean unlockedByDefault = true;

    public float hp;
    public float hpPerLevel;
    public float armor;
    public float speed;
    public float regen;
    public float respawnTime = 15f;
    public float engageRadius = 70f;
    public int maxEngaged = 2;

    public float damageMin;
    public float damageMax;
    public float damagePerLevel;
    public float attackCooldown = 1f;
    public float meleeRange = 26f;
    public DamageType damageType = DamageType.KINETIC;

    public float rangedDamage;
    public float rangedDamagePerLevel;
    public float rangedRange;
    public float rangedCooldown = 1.2f;
    public ProjectileType rangedProjectile = ProjectileType.PISTOL_BOLT;

    /** Cumulative XP required to reach level index+1 (entry 0 is always 0). */
    public final IntArray xpLevels = new IntArray();
    public final Array<AbilityDefinition> abilities = new Array<>();

    public int maxLevel() {
        return xpLevels.size;
    }

    public static HeroDefinition parse(String id, JsonValue v) {
        String ctx = "heroes.json (" + id + ")";
        HeroDefinition d = new HeroDefinition();
        d.id = id;
        d.name = JsonUtil.requireString(v, "name", ctx);
        d.title = v.getString("title", "");
        d.description = v.getString("description", "");
        d.sprite = JsonUtil.requireString(v, "sprite", ctx);
        d.portrait = v.getString("portrait", "portrait_" + id);
        d.unlockedByDefault = v.getBoolean("unlockedByDefault", true);
        d.hp = JsonUtil.requireFloat(v, "hp", ctx);
        d.hpPerLevel = v.getFloat("hpPerLevel", 0f);
        d.armor = v.getFloat("armor", 0f);
        d.speed = JsonUtil.requireFloat(v, "speed", ctx);
        d.regen = v.getFloat("regen", 0f);
        d.respawnTime = v.getFloat("respawnTime", 15f);
        d.engageRadius = v.getFloat("engageRadius", 70f);
        d.maxEngaged = v.getInt("maxEngaged", 2);

        JsonValue melee = JsonUtil.requireChild(v, "melee", ctx);
        d.damageMin = melee.getFloat("damageMin", 10f);
        d.damageMax = melee.getFloat("damageMax", d.damageMin);
        d.damagePerLevel = melee.getFloat("perLevel", 0f);
        d.attackCooldown = melee.getFloat("cooldown", 1f);
        d.meleeRange = melee.getFloat("range", 26f);
        d.damageType = JsonUtil.enumValue(melee, "damageType", DamageType.class, DamageType.KINETIC, ctx);

        JsonValue ranged = v.get("ranged");
        if (ranged != null) {
            d.rangedDamage = ranged.getFloat("damage", 0f);
            d.rangedDamagePerLevel = ranged.getFloat("perLevel", 0f);
            d.rangedRange = ranged.getFloat("range", 0f);
            d.rangedCooldown = ranged.getFloat("cooldown", 1.2f);
            d.rangedProjectile = JsonUtil.enumValue(ranged, "projectile", ProjectileType.class,
                    ProjectileType.PISTOL_BOLT, ctx);
        }

        JsonValue levels = JsonUtil.requireChild(v, "xpLevels", ctx);
        for (JsonValue l = levels.child; l != null; l = l.next) {
            d.xpLevels.add(l.asInt());
        }
        if (d.xpLevels.isEmpty() || d.xpLevels.first() != 0) {
            throw new DataException(ctx + ": xpLevels must start with 0");
        }
        for (int i = 1; i < d.xpLevels.size; i++) {
            if (d.xpLevels.get(i) <= d.xpLevels.get(i - 1)) {
                throw new DataException(ctx + ": xpLevels must be strictly increasing");
            }
        }
        JsonValue abilities = JsonUtil.requireChild(v, "abilities", ctx);
        for (JsonValue a = abilities.child; a != null; a = a.next) {
            d.abilities.add(AbilityDefinition.parse(JsonUtil.requireString(a, "id", ctx), a, ctx + ".abilities"));
        }
        return d;
    }
}
