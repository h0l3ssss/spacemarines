package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.world.Bonuses;

/** A permanent campaign upgrade bought with stars in the Armoury (armoury.json). */
public final class ArmouryUpgrade {
    public enum Effect {
        STARTING_REQUISITION, INCOME, TOWER_DAMAGE, TOWER_RANGE, UPGRADE_COST, ABILITY_COOLDOWN, UNIT_HP, HERO_XP,
        BASE_HP
    }

    public String id;
    public String name;
    public String description = "";
    public String icon;
    public Effect effect;
    public float perRank;
    /** Star cost of each rank; its size is the maximum rank. */
    public final IntArray costs = new IntArray();

    public int maxRank() {
        return costs.size;
    }

    /** Total stars spent to reach {@code rank}. */
    public int totalCost(int rank) {
        int total = 0;
        for (int i = 0; i < Math.min(rank, costs.size); i++) {
            total += costs.get(i);
        }
        return total;
    }

    public void apply(Bonuses bonuses, int rank) {
        if (rank <= 0) {
            return;
        }
        float amount = perRank * Math.min(rank, maxRank());
        switch (effect) {
            case STARTING_REQUISITION:
                bonuses.startingRequisitionMult += amount;
                break;
            case INCOME:
                bonuses.incomeMult += amount;
                break;
            case TOWER_DAMAGE:
                bonuses.towerDamageMult += amount;
                break;
            case TOWER_RANGE:
                bonuses.towerRangeMult += amount;
                break;
            case UPGRADE_COST:
                bonuses.upgradeCostMult = Math.max(0.5f, bonuses.upgradeCostMult + amount);
                break;
            case ABILITY_COOLDOWN:
                bonuses.abilityCooldownMult = Math.max(0.4f, bonuses.abilityCooldownMult + amount);
                break;
            case UNIT_HP:
                bonuses.unitHpMult += amount;
                break;
            case HERO_XP:
                bonuses.heroXpMult += amount;
                break;
            case BASE_HP:
                bonuses.baseHpBonus += Math.round(amount);
                break;
            default:
                throw new IllegalStateException("Unknown effect " + effect);
        }
    }

    public static ArmouryUpgrade parse(JsonValue v, String context) {
        ArmouryUpgrade u = new ArmouryUpgrade();
        u.id = JsonUtil.requireString(v, "id", context);
        String ctx = context + " (" + u.id + ")";
        u.name = JsonUtil.requireString(v, "name", ctx);
        u.description = v.getString("description", "");
        u.icon = v.getString("icon", "icon_star");
        u.effect = JsonUtil.enumValue(v, "effect", Effect.class, null, ctx);
        if (u.effect == null) {
            throw new DataException(ctx + ": missing 'effect'");
        }
        u.perRank = JsonUtil.requireFloat(v, "perRank", ctx);
        JsonValue costs = JsonUtil.requireChild(v, "costs", ctx);
        for (JsonValue c = costs.child; c != null; c = c.next) {
            u.costs.add(c.asInt());
        }
        if (u.costs.isEmpty()) {
            throw new DataException(ctx + ": 'costs' must list at least one rank");
        }
        return u;
    }
}
