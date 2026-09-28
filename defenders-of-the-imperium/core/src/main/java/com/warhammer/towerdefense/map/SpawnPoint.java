package com.warhammer.towerdefense.map;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.JsonUtil;

/** Where a path enters the battlefield. Used for the "incoming wave" indicator shown to the player. */
public final class SpawnPoint {
    public int path;
    public float x;
    public float y;
    public String label = "";

    public static SpawnPoint parse(JsonValue v, String context) {
        SpawnPoint s = new SpawnPoint();
        s.path = v.getInt("path", 0);
        s.x = JsonUtil.requireFloat(v, "x", context);
        s.y = JsonUtil.requireFloat(v, "y", context);
        s.label = v.getString("label", "");
        return s;
    }
}
