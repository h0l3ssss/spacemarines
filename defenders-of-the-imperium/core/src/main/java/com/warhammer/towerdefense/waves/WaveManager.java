package com.warhammer.towerdefense.waves;

import com.warhammer.towerdefense.entities.Enemy;

/**
 * Wave flow: WAITING (before the first wave) -> SPAWNING -> COUNTDOWN -> SPAWNING ... -> FINAL.
 * The player may call the next wave during WAITING/COUNTDOWN; calling early grants a requisition bonus that shrinks
 * with the remaining countdown. A wave is completed when all its enemies spawned and none are left alive.
 */
public final class WaveManager {
    public enum Phase {
        WAITING, SPAWNING, COUNTDOWN, FINAL
    }

    /** Receiver of wave milestones (the game world, or a stub in tests). */
    public interface WaveListener {
        void onWaveStarted(int wave, int total, WaveDefinition definition);

        void onWaveCompleted(int wave, int reward, boolean flawless);
    }

    private final WaveSetDefinition set;
    private final SpawnManager spawner;
    private final WaveListener listener;
    private final int[] alive;
    private final boolean[] leaked;
    private final boolean[] completed;
    private int current;
    private Phase phase = Phase.WAITING;
    private float countdown;

    public WaveManager(WaveSetDefinition set, SpawnManager spawner, WaveListener listener) {
        this.set = set;
        this.spawner = spawner;
        this.listener = listener;
        int slots = set.size() + 1;
        alive = new int[slots];
        leaked = new boolean[slots];
        completed = new boolean[slots];
        completed[0] = true;
    }

    public void update(float dt) {
        spawner.update(dt);
        switch (phase) {
            case COUNTDOWN:
                countdown -= dt;
                if (countdown <= 0f) {
                    startNextWave();
                }
                break;
            case SPAWNING:
                if (!spawner.isSpawning(current)) {
                    if (current < set.size()) {
                        phase = Phase.COUNTDOWN;
                        countdown = set.timeBetweenWaves;
                    } else {
                        phase = Phase.FINAL;
                    }
                }
                break;
            default:
                break;
        }
        checkCompletions();
    }

    private void checkCompletions() {
        for (int w = 1; w <= current; w++) {
            if (completed[w] || alive[w] > 0 || spawner.isSpawning(w)) {
                continue;
            }
            completed[w] = true;
            boolean flawless = !leaked[w];
            int reward = set.waves.get(w - 1).reward + (flawless ? set.flawlessBonus : 0);
            listener.onWaveCompleted(w, reward, flawless);
        }
    }

    public void onEnemySpawned(int wave) {
        alive[clampWave(wave)]++;
    }

    public void onEnemyRemoved(Enemy enemy, boolean leakedThrough) {
        int w = clampWave(enemy.waveIndex);
        alive[w] = Math.max(0, alive[w] - 1);
        if (leakedThrough) {
            leaked[w] = true;
        }
    }

    private int clampWave(int wave) {
        return Math.max(0, Math.min(alive.length - 1, wave));
    }

    public boolean canCallNextWave() {
        return current < set.size() && (phase == Phase.WAITING || phase == Phase.COUNTDOWN);
    }

    public int earlyCallBonus() {
        if (phase != Phase.COUNTDOWN || set.timeBetweenWaves <= 0f) {
            return 0;
        }
        return Math.round(set.earlyCallBonusMax * Math.max(0f, countdown) / set.timeBetweenWaves);
    }

    /** Starts the next wave now; returns the early-call bonus (0 for the first wave) or -1 if not allowed. */
    public int callNextWave() {
        if (!canCallNextWave()) {
            return -1;
        }
        int bonus = earlyCallBonus();
        startNextWave();
        return bonus;
    }

    private void startNextWave() {
        current++;
        WaveDefinition def = set.waves.get(current - 1);
        spawner.startWave(def);
        phase = Phase.SPAWNING;
        listener.onWaveStarted(current, set.size(), def);
    }

    public boolean allWavesCompleted() {
        if (current < set.size() || !spawner.isIdle()) {
            return false;
        }
        for (int w = 1; w <= set.size(); w++) {
            if (!completed[w]) {
                return false;
            }
        }
        return true;
    }

    public int currentWave() {
        return current;
    }

    public int totalWaves() {
        return set.size();
    }

    public Phase phase() {
        return phase;
    }

    public float countdown() {
        return countdown;
    }

    public float countdownFraction() {
        return set.timeBetweenWaves <= 0f ? 0f : Math.max(0f, countdown / set.timeBetweenWaves);
    }

    /** Definition of the wave that will start next, or null after the last one. */
    public WaveDefinition nextWave() {
        return current < set.size() ? set.waves.get(current) : null;
    }

    public WaveSetDefinition set() {
        return set;
    }

    public int aliveInWave(int wave) {
        return alive[clampWave(wave)];
    }
}
