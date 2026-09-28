package com.warhammer.towerdefense;

import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.debug.AutoCommander;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays the whole Agri-World Vigilus campaign headlessly with the scripted {@link AutoCommander} to check that the
 * wave design is winnable with a sensible defence, and that doing nothing loses. Prints a short battle report.
 */
class GameplaySimulationTest {
    private static final float STEP = 1f / 60f;
    private static final float MAX_BATTLE_SECONDS = 1500f;

    private static GameWorld play(Difficulty difficulty, boolean commander) {
        GameWorld world = TestSupport.world(difficulty, commander);
        StringBuilder trace = new StringBuilder();
        world.events = new com.warhammer.towerdefense.world.GameEvents() {
            @Override
            public void waveCompleted(int wave, int reward, boolean flawless) {
                trace.append(" w").append(wave).append(':').append(world.state.baseHp);
            }

            @Override
            public void enemyLeaked(com.warhammer.towerdefense.entities.Enemy enemy, int damage) {
                if (enemy.def.boss) {
                    trace.append(" BOSS-LEAKED(hp=").append(Math.round(enemy.hp)).append(')');
                }
            }

            @Override
            public void bossPhase(com.warhammer.towerdefense.entities.Enemy boss, int phase, String announcement) {
                trace.append(" [boss phase ").append(phase).append(" t=").append(Math.round(world.time)).append(']');
            }
        };
        AutoCommander ai = commander ? new AutoCommander(world, false) : null;
        if (!commander) {
            world.callNextWave();
        }
        float t = 0f;
        int peakEnemies = 0;
        while (!world.state.isOver() && t < MAX_BATTLE_SECONDS) {
            if (ai != null) {
                ai.update(STEP);
            } else if (world.waves.canCallNextWave() && world.activeEnemyCount() == 0) {
                world.callNextWave();
            }
            world.update(STEP);
            peakEnemies = Math.max(peakEnemies, world.enemies.size);
            t += STEP;
        }
        StringBuilder report = new StringBuilder();
        report.append(String.format("[SIM] %-7s commander=%-5s result=%-7s wave=%d/%d baseHp=%d/%d time=%.0fs kills=%d "
                        + "leaks=%d earned=%d peakEnemies=%d stars=%d",
                difficulty, commander, world.state.victory ? "VICTORY" : world.state.defeat ? "DEFEAT" : "TIMEOUT",
                world.waves.currentWave(), world.waves.totalWaves(), world.state.baseHp, world.state.baseMaxHp, t,
                world.state.kills, world.state.leaks, world.state.requisitionEarned, peakEnemies, world.state.stars()));
        if (world.hero != null) {
            report.append(" heroLevel=").append(world.hero.level);
        }
        report.append(System.lineSeparator()).append("      trace:").append(trace);
        for (Tower tower : world.towers) {
            report.append("\n      ").append(String.format("%-28s lvl%d kills=%-4d dmg=%.0f", tower.displayName(),
                    tower.level(), tower.kills, tower.damageDealt));
        }
        System.out.println(report);
        return world;
    }

    @Test
    void casualIsWinnableWithAReasonableDefence() {
        assertTrue(play(Difficulty.CASUAL, true).state.victory);
    }

    @Test
    void veteranIsWinnableWithAReasonableDefence() {
        assertTrue(play(Difficulty.VETERAN, true).state.victory);
    }

    @Test
    void hardReportOnly() {
        GameWorld world = play(Difficulty.HARD, true);
        assertTrue(world.state.isOver(), "the battle must end");
    }

    @Test
    void doingNothingLoses() {
        assertTrue(play(Difficulty.VETERAN, false).state.defeat);
    }
}
