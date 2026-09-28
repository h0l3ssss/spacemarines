package com.warhammer.towerdefense.waves;

import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

/** One spawn group inside a wave: "20 Termagants, one every 0.9 s, starting 4 s into the wave, on path 1". */
public final class EnemySpawnDefinition {
    /** Path index, or -1 to alternate between all paths. */
    public static final int ALTERNATE_PATHS = -1;

    public String enemy;
    public int count = 1;
    public float interval = 1f;
    public float delay;
    public int path = ALTERNATE_PATHS;
    /** Extra hp multiplier for "elite" versions of a group. */
    public float hpMult = 1f;

    public float duration() {
        return delay + Math.max(0, count - 1) * interval;
    }

    public static EnemySpawnDefinition parse(JsonValue v, String context) {
        EnemySpawnDefinition d = new EnemySpawnDefinition();
        d.enemy = JsonUtil.requireString(v, "enemy", context);
        d.count = v.getInt("count", 1);
        d.interval = v.getFloat("interval", 1f);
        d.delay = v.getFloat("delay", 0f);
        d.path = v.getInt("path", ALTERNATE_PATHS);
        d.hpMult = v.getFloat("hpMult", 1f);
        if (d.count <= 0 || d.interval < 0f || d.delay < 0f) {
            throw new DataException(context + ": count must be > 0, interval and delay >= 0");
        }
        return d;
    }
}
