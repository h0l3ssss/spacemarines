package com.warhammer.towerdefense.waves;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

/** All waves of one battle plus the pacing rules between them. */
public final class WaveSetDefinition {
    public String id;
    /** Countdown between the end of a wave's spawning and the next wave. */
    public float timeBetweenWaves = 20f;
    /** Requisition granted for calling a wave immediately (scaled by the remaining countdown). */
    public int earlyCallBonusMax = 30;
    /** Bonus for completing a wave without losing base integrity. */
    public int flawlessBonus = 15;
    public final Array<WaveDefinition> waves = new Array<>();

    public int size() {
        return waves.size;
    }

    public static WaveSetDefinition parse(String id, JsonValue v) {
        String ctx = "waves.json (" + id + ")";
        WaveSetDefinition s = new WaveSetDefinition();
        s.id = id;
        s.timeBetweenWaves = v.getFloat("timeBetweenWaves", 20f);
        s.earlyCallBonusMax = v.getInt("earlyCallBonusMax", 30);
        s.flawlessBonus = v.getInt("flawlessBonus", 15);
        JsonValue waves = JsonUtil.requireChild(v, "waves", ctx);
        int n = 1;
        for (JsonValue w = waves.child; w != null; w = w.next, n++) {
            s.waves.add(WaveDefinition.parse(w, n, ctx + ".waves[" + (n - 1) + "]"));
        }
        if (s.waves.isEmpty()) {
            throw new DataException(ctx + ": no waves defined");
        }
        return s;
    }
}
