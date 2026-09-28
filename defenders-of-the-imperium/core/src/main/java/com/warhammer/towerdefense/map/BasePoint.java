package com.warhammer.towerdefense.map;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.JsonUtil;

/** The Imperial Fortress the player defends. Enemies that finish their path damage it. */
public final class BasePoint {
    public float x;
    public float y;
    public float radius = 60f;
    public String sprite = "fortress";

    public static BasePoint parse(JsonValue v, String context) {
        BasePoint b = new BasePoint();
        b.x = JsonUtil.requireFloat(v, "x", context);
        b.y = JsonUtil.requireFloat(v, "y", context);
        b.radius = v.getFloat("radius", 60f);
        b.sprite = v.getString("sprite", "fortress");
        return b;
    }
}
