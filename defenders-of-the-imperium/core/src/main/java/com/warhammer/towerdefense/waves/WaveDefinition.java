package com.warhammer.towerdefense.waves;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.warhammer.towerdefense.data.DataException;

/** A wave: a list of spawn groups plus its reward and banner text. */
public final class WaveDefinition {
    public int number;
    public String title = "";
    public int reward;
    public boolean boss;
    public final Array<EnemySpawnDefinition> groups = new Array<>();

    /** Seconds from the wave start until its last enemy has spawned. */
    public float duration() {
        float max = 0f;
        for (EnemySpawnDefinition g : groups) {
            max = Math.max(max, g.duration());
        }
        return max;
    }

    public int totalEnemies() {
        int total = 0;
        for (EnemySpawnDefinition g : groups) {
            total += g.count;
        }
        return total;
    }

    /** Enemy id -> count, in first-appearance order (for the "incoming" preview). */
    public ObjectIntMap<String> enemyCounts() {
        ObjectIntMap<String> counts = new ObjectIntMap<>();
        for (EnemySpawnDefinition g : groups) {
            counts.getAndIncrement(g.enemy, 0, g.count);
        }
        return counts;
    }

    public static WaveDefinition parse(JsonValue v, int number, String context) {
        WaveDefinition w = new WaveDefinition();
        w.number = number;
        w.title = v.getString("title", "");
        w.reward = v.getInt("reward", 0);
        w.boss = v.getBoolean("boss", false);
        JsonValue groups = v.get("groups");
        if (groups == null || groups.size == 0) {
            throw new DataException(context + ": wave " + number + " has no spawn groups");
        }
        int i = 0;
        for (JsonValue g = groups.child; g != null; g = g.next, i++) {
            w.groups.add(EnemySpawnDefinition.parse(g, context + ".groups[" + i + "]"));
        }
        return w;
    }
}
