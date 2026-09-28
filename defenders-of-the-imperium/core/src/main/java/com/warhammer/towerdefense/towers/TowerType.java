package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

/**
 * Static definition of a tower: its tiers and specialisation trees, loaded from {@code assets/data/towers.json}.
 * Tier 1 is the construction itself; its cost is the build cost.
 */
public final class TowerType {
    public String id;
    public String name;
    public String role = "";
    public String description = "";
    public String icon;
    public TowerBehavior behavior;
    public final Array<TowerUpgrade> tiers = new Array<>();
    public final Array<Specialization> specializations = new Array<>();

    public int buildCost() {
        return tiers.first().cost;
    }

    public int maxTier() {
        return tiers.size;
    }

    public static TowerType parse(JsonValue v, int index) {
        String ctx = "towers.json [" + index + "]";
        TowerType t = new TowerType();
        t.id = JsonUtil.requireString(v, "id", ctx);
        ctx = "towers.json (" + t.id + ")";
        t.name = JsonUtil.requireString(v, "name", ctx);
        t.role = v.getString("role", "");
        t.description = v.getString("description", "");
        t.icon = v.getString("icon", "icon_tower_" + t.id);
        t.behavior = JsonUtil.enumValue(v, "behavior", TowerBehavior.class, null, ctx);
        if (t.behavior == null) {
            throw new DataException(ctx + ": missing 'behavior' (RANGED, BARRACKS or SUPPORT)");
        }
        int i = 0;
        for (JsonValue tier = JsonUtil.requireChild(v, "tiers", ctx).child; tier != null; tier = tier.next, i++) {
            t.tiers.add(TowerUpgrade.parse(tier, ctx + ".tiers[" + i + "]"));
        }
        if (t.tiers.isEmpty()) {
            throw new DataException(ctx + ": a tower needs at least one tier");
        }
        JsonValue specs = v.get("specializations");
        if (specs != null) {
            i = 0;
            for (JsonValue s = specs.child; s != null; s = s.next, i++) {
                t.specializations.add(Specialization.parse(s, ctx + ".specializations[" + i + "]"));
            }
        }
        return t;
    }
}
