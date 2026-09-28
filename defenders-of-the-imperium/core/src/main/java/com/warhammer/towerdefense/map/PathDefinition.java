package com.warhammer.towerdefense.map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

/** Control points of an enemy route. The runtime {@link GamePath} smooths them with a centripetal Catmull-Rom spline. */
public final class PathDefinition {
    public int id;
    public float width = 64f;
    public boolean smooth = true;
    public final Array<Vector2> points = new Array<>();

    public static PathDefinition parse(JsonValue v, int index, String context) {
        PathDefinition def = new PathDefinition();
        def.id = v.getInt("id", index);
        def.width = v.getFloat("width", 64f);
        def.smooth = v.getBoolean("smooth", true);
        JsonValue pts = JsonUtil.requireChild(v, "points", context);
        for (JsonValue p = pts.child; p != null; p = p.next) {
            def.points.add(JsonUtil.point(p, context + ".points"));
        }
        if (def.points.size < 2) {
            throw new DataException(context + ": a path needs at least 2 points");
        }
        return def;
    }
}
