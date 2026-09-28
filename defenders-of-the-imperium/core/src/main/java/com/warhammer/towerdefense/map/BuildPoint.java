package com.warhammer.towerdefense.map;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.JsonUtil;

/** A fixed plot where a tower can be deployed. Barracks send their squad to the rally point. */
public final class BuildPoint {
    public int id;
    public float x;
    public float y;
    /** Default rally point for barracks built here; NaN means "closest point of the nearest path". */
    public float rallyX = Float.NaN;
    public float rallyY = Float.NaN;

    public boolean hasRallyPoint() {
        return !Float.isNaN(rallyX) && !Float.isNaN(rallyY);
    }

    public static BuildPoint parse(JsonValue v, int index, String context) {
        BuildPoint p = new BuildPoint();
        p.id = v.getInt("id", index);
        p.x = JsonUtil.requireFloat(v, "x", context);
        p.y = JsonUtil.requireFloat(v, "y", context);
        JsonValue rally = v.get("rally");
        if (rally != null) {
            p.rallyX = JsonUtil.point(rally, context + ".rally").x;
            p.rallyY = JsonUtil.point(rally, context + ".rally").y;
        }
        return p;
    }
}
