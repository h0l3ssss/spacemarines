package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.JsonUtil;

/** An upgrade tree unlocked once a tower reaches its last tier (e.g. BOLTER DISCIPLINE). */
public final class Specialization {
    public String id;
    public String name;
    public String description = "";
    public String icon;
    /** Buying the specialisation itself (rank 1). */
    public TowerUpgrade unlock;
    /** Further ranks (rank 2, rank 3, ...). */
    public final Array<TowerUpgrade> ranks = new Array<>();

    public int maxRank() {
        return 1 + ranks.size;
    }

    public static Specialization parse(JsonValue v, String context) {
        Specialization s = new Specialization();
        s.id = JsonUtil.requireString(v, "id", context);
        String ctx = context + " (" + s.id + ")";
        s.name = JsonUtil.requireString(v, "name", ctx);
        s.description = v.getString("description", "");
        s.icon = v.getString("icon", "icon_spec_" + s.id);
        s.unlock = TowerUpgrade.parse(v, ctx);
        s.unlock.name = s.name;
        s.unlock.icon = s.icon;
        JsonValue ranks = v.get("ranks");
        if (ranks != null) {
            int i = 0;
            for (JsonValue r = ranks.child; r != null; r = r.next, i++) {
                TowerUpgrade rank = TowerUpgrade.parse(r, ctx + ".ranks[" + i + "]");
                if (rank.icon == null) {
                    rank.icon = s.icon;
                }
                s.ranks.add(rank);
            }
        }
        return s;
    }
}
