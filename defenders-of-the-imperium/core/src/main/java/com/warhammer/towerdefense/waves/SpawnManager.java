package com.warhammer.towerdefense.waves;

import com.badlogic.gdx.utils.Array;

/**
 * Executes the spawn groups of started waves over time. Several waves may be spawning at once (when the player
 * calls the next wave early), each group keeps its own timer.
 */
public final class SpawnManager {
    /** Receiver of spawn requests (the game world, or a stub in tests). */
    public interface SpawnTarget {
        void spawnEnemy(String enemyId, int pathIndex, int wave, float hpMult);

        int pathCount();
    }

    private static final class ActiveGroup {
        EnemySpawnDefinition def;
        int wave;
        float timer;
        int spawned;
    }

    private final SpawnTarget target;
    private final Array<ActiveGroup> groups = new Array<>();

    public SpawnManager(SpawnTarget target) {
        this.target = target;
    }

    public void startWave(WaveDefinition wave) {
        for (EnemySpawnDefinition def : wave.groups) {
            ActiveGroup g = new ActiveGroup();
            g.def = def;
            g.wave = wave.number;
            g.timer = -def.delay;
            groups.add(g);
        }
    }

    public void update(float dt) {
        for (int i = groups.size - 1; i >= 0; i--) {
            ActiveGroup g = groups.get(i);
            g.timer += dt;
            while (g.timer >= 0f && g.spawned < g.def.count) {
                int paths = Math.max(1, target.pathCount());
                int path = g.def.path >= 0 ? Math.min(g.def.path, paths - 1) : g.spawned % paths;
                target.spawnEnemy(g.def.enemy, path, g.wave, g.def.hpMult);
                g.spawned++;
                g.timer -= g.def.interval;
            }
            if (g.spawned >= g.def.count) {
                groups.removeIndex(i);
            }
        }
    }

    public boolean isSpawning(int wave) {
        for (ActiveGroup g : groups) {
            if (g.wave == wave) {
                return true;
            }
        }
        return false;
    }

    public boolean isIdle() {
        return groups.isEmpty();
    }

    /** Enemies of this wave still waiting to spawn. */
    public int pending(int wave) {
        int n = 0;
        for (ActiveGroup g : groups) {
            if (g.wave == wave) {
                n += g.def.count - g.spawned;
            }
        }
        return n;
    }

    public void clear() {
        groups.clear();
    }
}
