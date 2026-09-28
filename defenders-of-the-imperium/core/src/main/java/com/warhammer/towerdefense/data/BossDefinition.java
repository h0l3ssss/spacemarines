package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Multi-phase boss behaviour. A boss enters phase {@code i} when its hp fraction drops to the phase threshold;
 * every phase has its own set of timed abilities (spawning minions, psychic screams, leaps, shields...).
 */
public final class BossDefinition {
    public enum AbilityType {
        SPAWN, PSYCHIC_SCREAM, LEAP, BIO_PLASMA, WARP_SHIELD, REGENERATE
    }

    public final Array<Phase> phases = new Array<>();

    public static final class Phase {
        public float threshold = 1f;
        public String announcement = "";
        public float speedMult = 1f;
        public float damageMult = 1f;
        public final Array<Ability> abilities = new Array<>();
    }

    public static final class Ability {
        public AbilityType type;
        public float cooldown = 10f;
        public float initialDelay = 3f;
        public String enemy;
        public int count = 1;
        public float radius;
        public float range;
        public float damage;
        public float duration;
        public float amount;
    }

    public static BossDefinition parse(JsonValue v, String context) {
        BossDefinition def = new BossDefinition();
        JsonValue phases = JsonUtil.requireChild(v, "phases", context);
        int i = 0;
        for (JsonValue p = phases.child; p != null; p = p.next, i++) {
            String ctx = context + ".phases[" + i + "]";
            Phase phase = new Phase();
            phase.threshold = p.getFloat("threshold", i == 0 ? 1f : 0.5f);
            phase.announcement = p.getString("announcement", "");
            phase.speedMult = p.getFloat("speedMult", 1f);
            phase.damageMult = p.getFloat("damageMult", 1f);
            JsonValue abilities = p.get("abilities");
            if (abilities != null) {
                for (JsonValue a = abilities.child; a != null; a = a.next) {
                    Ability ab = new Ability();
                    ab.type = JsonUtil.enumValue(a, "type", AbilityType.class, null, ctx);
                    if (ab.type == null) {
                        throw new DataException(ctx + ": boss ability without 'type'");
                    }
                    ab.cooldown = a.getFloat("cooldown", 10f);
                    ab.initialDelay = a.getFloat("initialDelay", Math.min(3f, ab.cooldown));
                    ab.enemy = a.getString("enemy", null);
                    ab.count = a.getInt("count", 1);
                    ab.radius = a.getFloat("radius", 0f);
                    ab.range = a.getFloat("range", 0f);
                    ab.damage = a.getFloat("damage", 0f);
                    ab.duration = a.getFloat("duration", 0f);
                    ab.amount = a.getFloat("amount", 0f);
                    if (ab.type == AbilityType.SPAWN && ab.enemy == null) {
                        throw new DataException(ctx + ": SPAWN ability needs an 'enemy'");
                    }
                    phase.abilities.add(ab);
                }
            }
            def.phases.add(phase);
        }
        if (def.phases.isEmpty()) {
            throw new DataException(context + ": a boss needs at least one phase");
        }
        return def;
    }
}
