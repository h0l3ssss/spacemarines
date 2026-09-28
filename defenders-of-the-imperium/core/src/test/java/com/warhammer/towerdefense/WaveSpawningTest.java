package com.warhammer.towerdefense;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.waves.SpawnManager;
import com.warhammer.towerdefense.waves.WaveDefinition;
import com.warhammer.towerdefense.waves.WaveManager;
import com.warhammer.towerdefense.waves.WaveSetDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveSpawningTest {
    /** Records spawns instead of creating real enemies. */
    private static final class Recorder implements SpawnManager.SpawnTarget, WaveManager.WaveListener {
        final Array<String> spawned = new Array<>();
        final Array<Integer> paths = new Array<>();
        int started;
        int completed;
        int lastReward;
        boolean lastFlawless;

        @Override
        public void spawnEnemy(String enemyId, int pathIndex, int wave, float hpMult) {
            spawned.add(enemyId);
            paths.add(pathIndex);
        }

        @Override
        public int pathCount() {
            return 2;
        }

        @Override
        public void onWaveStarted(int wave, int total, WaveDefinition definition) {
            started = wave;
        }

        @Override
        public void onWaveCompleted(int wave, int reward, boolean flawless) {
            completed = wave;
            lastReward = reward;
            lastFlawless = flawless;
        }
    }

    private static WaveSetDefinition set(String json) {
        return WaveSetDefinition.parse("test", new JsonReader().parse(json));
    }

    private static final String TWO_WAVES = "{ timeBetweenWaves: 10, earlyCallBonusMax: 20, flawlessBonus: 5, waves: ["
            + "{ reward: 30, groups: [ { enemy: termagant, count: 10, interval: 1.0 } ] },"
            + "{ reward: 40, groups: [ { enemy: termagant, count: 2, interval: 1.0 },"
            + "                        { enemy: hormagaunt, count: 3, interval: 0.5, delay: 4, path: 1 } ] } ] }";

    @Test
    void groupsSpawnAtTheirIntervalAndAlternatePaths() {
        Recorder r = new Recorder();
        SpawnManager spawner = new SpawnManager(r);
        spawner.startWave(set(TWO_WAVES).waves.first());
        spawner.update(0f);
        assertEquals(1, r.spawned.size, "first enemy spawns immediately");
        for (int i = 0; i < 90; i++) {
            spawner.update(0.1f);
        }
        assertEquals(10, r.spawned.size);
        assertEquals(0, (int) r.paths.get(0));
        assertEquals(1, (int) r.paths.get(1));
        assertTrue(spawner.isIdle());
    }

    @Test
    void delayedGroupWaitsForItsDelay() {
        Recorder r = new Recorder();
        SpawnManager spawner = new SpawnManager(r);
        spawner.startWave(set(TWO_WAVES).waves.get(1));
        spawner.update(3.9f);
        assertFalse(r.spawned.contains("hormagaunt", false));
        spawner.update(0.2f);
        assertTrue(r.spawned.contains("hormagaunt", false));
        assertEquals(1, (int) r.paths.peek(), "fixed path honoured");
    }

    @Test
    void waveFlowCountdownEarlyCallAndCompletion() {
        Recorder r = new Recorder();
        SpawnManager spawner = new SpawnManager(r);
        WaveManager waves = new WaveManager(set(TWO_WAVES), spawner, r);
        assertEquals(WaveManager.Phase.WAITING, waves.phase());
        assertEquals(0, waves.callNextWave(), "no bonus for the first wave");
        assertEquals(1, r.started);
        for (int i = 0; i < 10; i++) {
            waves.onEnemySpawned(1);
        }
        for (int i = 0; i < 100; i++) {
            waves.update(0.1f);
        }
        assertEquals(WaveManager.Phase.COUNTDOWN, waves.phase());
        waves.update(5f);
        int bonus = waves.earlyCallBonus();
        assertEquals(Math.round(20 * waves.countdown() / 10f), bonus, "bonus proportional to the countdown left");
        assertTrue(bonus > 0 && bonus < 20);

        Enemy dead = new Enemy();
        dead.waveIndex = 1;
        for (int i = 0; i < 10; i++) {
            waves.onEnemyRemoved(dead, false);
        }
        waves.update(0.01f);
        assertEquals(1, r.completed);
        assertEquals(35, r.lastReward, "reward + flawless bonus");
        assertTrue(r.lastFlawless);

        waves.update(10f);
        assertEquals(2, r.started, "countdown expiry starts the next wave");
        assertFalse(waves.canCallNextWave());
    }

    @Test
    void leakingRemovesFlawlessBonus() {
        Recorder r = new Recorder();
        WaveManager waves = new WaveManager(set(TWO_WAVES), new SpawnManager(r), r);
        waves.callNextWave();
        waves.onEnemySpawned(1);
        waves.update(20f);
        Enemy leaker = new Enemy();
        leaker.waveIndex = 1;
        waves.onEnemyRemoved(leaker, true);
        waves.update(0.01f);
        assertEquals(1, r.completed);
        assertFalse(r.lastFlawless);
        assertEquals(30, r.lastReward);
    }
}
