package com.warhammer.towerdefense.map;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.JsonUtil;

/**
 * A decorative prop (tree, ruin, crater, wreck...). Props are painted into the map background by the asset
 * generator, so adding a prop to the map JSON and re-running {@code gradlew generateAssets} is enough.
 */
public final class TerrainObject {
    public String type;
    public float x;
    public float y;
    public float scale = 1f;
    public float rotation;
    public boolean flip;
    public int variant;

    public static TerrainObject parse(JsonValue v, String context) {
        TerrainObject t = new TerrainObject();
        t.type = JsonUtil.requireString(v, "type", context);
        t.x = JsonUtil.requireFloat(v, "x", context);
        t.y = JsonUtil.requireFloat(v, "y", context);
        t.scale = v.getFloat("scale", 1f);
        t.rotation = v.getFloat("rotation", 0f);
        t.flip = v.getBoolean("flip", false);
        t.variant = v.getInt("variant", 0);
        return t;
    }
}
