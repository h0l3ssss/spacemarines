package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.enemies.EnemyTrait;

import java.util.EnumSet;

/** Static description of an enemy type, loaded from {@code assets/data/enemies.json}. */
public final class EnemyDefinition {
    public enum RangedTarget {
        TOWERS, UNITS, ANY
    }

    public String id;
    public String name;
    public String faction = "tyranids";
    public String sprite;
    public String description = "";
    public String counter = "";
    public float hp;
    public float armor;
    public float speed;
    public float radius = 10f;
    public float scale = 1f;
    public int bounty;
    public int leakDamage = 1;
    public int xp;

    public float meleeDamage;
    public float meleeCooldown = 1f;
    public float meleeRange = 24f;

    public float rangedDamage;
    public float rangedRange;
    public float rangedCooldown = 5f;
    public ProjectileType rangedProjectile = ProjectileType.VENOM;
    public RangedTarget rangedTarget = RangedTarget.TOWERS;

    public boolean flying;
    public float flyHeight = 28f;
    public boolean elite;
    public boolean boss;
    public final EnumSet<EnemyTrait> traits = EnumSet.noneOf(EnemyTrait.class);
    public float synapseRadius = 110f;
    public float synapseSpeedBonus = 0.15f;
    public float synapseDamageBonus = 0.2f;
    public float regen;

    public float healthBarWidth = 24f;
    public float healthBarOffset = 34f;
    public String blood = "TYRANID";
    public String attackSound = "MELEE_HIT";
    public String deathSound = "TYRANID_DEATH";
    public String spawnSound;
    public BossDefinition bossDefinition;

    public boolean has(EnemyTrait trait) {
        return traits.contains(trait);
    }

    public boolean hasRangedAttack() {
        return rangedDamage > 0f && rangedRange > 0f;
    }

    public static EnemyDefinition parse(String id, JsonValue v) {
        String ctx = "enemies.json (" + id + ")";
        EnemyDefinition d = new EnemyDefinition();
        d.id = id;
        d.name = JsonUtil.requireString(v, "name", ctx);
        d.faction = v.getString("faction", "tyranids");
        d.sprite = v.getString("sprite", id);
        d.description = v.getString("description", "");
        d.counter = v.getString("counter", "");
        d.hp = JsonUtil.requireFloat(v, "hp", ctx);
        d.armor = v.getFloat("armor", 0f);
        d.speed = JsonUtil.requireFloat(v, "speed", ctx);
        d.radius = v.getFloat("radius", 10f);
        d.scale = v.getFloat("scale", 1f);
        d.bounty = v.getInt("bounty", 1);
        d.leakDamage = v.getInt("leakDamage", 1);
        d.xp = v.getInt("xp", d.bounty);
        d.flying = v.getBoolean("flying", false);
        d.flyHeight = v.getFloat("flyHeight", 28f);
        d.elite = v.getBoolean("elite", false);
        d.boss = v.has("boss");
        d.regen = v.getFloat("regen", 0f);
        d.blood = v.getString("blood", "TYRANID");

        JsonValue melee = v.get("melee");
        if (melee != null) {
            d.meleeDamage = melee.getFloat("damage", 0f);
            d.meleeCooldown = melee.getFloat("cooldown", 1f);
            d.meleeRange = melee.getFloat("range", 24f);
        }
        JsonValue ranged = v.get("ranged");
        if (ranged != null) {
            d.rangedDamage = ranged.getFloat("damage", 0f);
            d.rangedRange = ranged.getFloat("range", 0f);
            d.rangedCooldown = ranged.getFloat("cooldown", 5f);
            d.rangedProjectile = JsonUtil.enumValue(ranged, "projectile", ProjectileType.class, ProjectileType.VENOM, ctx);
            d.rangedTarget = JsonUtil.enumValue(ranged, "target", RangedTarget.class, RangedTarget.TOWERS, ctx);
        }
        for (String trait : JsonUtil.stringArray(v, "traits")) {
            d.traits.add(JsonUtil.parseEnum(trait, EnemyTrait.class, ctx + ".traits"));
        }
        JsonValue synapse = v.get("synapse");
        if (synapse != null) {
            d.synapseRadius = synapse.getFloat("radius", 110f);
            d.synapseSpeedBonus = synapse.getFloat("speedBonus", 0.15f);
            d.synapseDamageBonus = synapse.getFloat("damageBonus", 0.2f);
        }
        JsonValue bar = v.get("healthBar");
        if (bar != null) {
            d.healthBarWidth = bar.getFloat("width", 24f);
            d.healthBarOffset = bar.getFloat("offset", 34f);
        }
        JsonValue sounds = v.get("sounds");
        if (sounds != null) {
            d.attackSound = sounds.getString("attack", d.attackSound);
            d.deathSound = sounds.getString("death", d.deathSound);
            d.spawnSound = sounds.getString("spawn", null);
        }
        if (d.boss) {
            d.bossDefinition = BossDefinition.parse(v.get("boss"), ctx + ".boss");
        }
        if (d.hp <= 0f || d.speed <= 0f) {
            throw new DataException(ctx + ": hp and speed must be positive");
        }
        if (d.armor < 0f || d.armor > 0.95f) {
            throw new DataException(ctx + ": armor must be between 0 and 0.95");
        }
        return d;
    }
}
