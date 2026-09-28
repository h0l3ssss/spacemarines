package com.warhammer.towerdefense.enemies;

import com.warhammer.towerdefense.data.BossDefinition;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Drives a boss through its phases. Phase changes happen when hp crosses the thresholds defined in data; each
 * phase runs its own ability timers.
 */
public final class BossController {
    private static final float RETRY_DELAY = 0.5f;

    private final Enemy boss;
    private final BossDefinition definition;
    private final float cooldownMultiplier;
    private int phase = -1;
    private float[] timers = new float[0];

    public BossController(Enemy boss, BossDefinition definition, float cooldownMultiplier) {
        this.boss = boss;
        this.definition = definition;
        this.cooldownMultiplier = cooldownMultiplier;
    }

    public void update(GameWorld world, float dt) {
        if (phase < 0) {
            enterPhase(0, world);
        }
        float fraction = boss.hpFraction();
        while (phase + 1 < definition.phases.size && fraction <= definition.phases.get(phase + 1).threshold) {
            enterPhase(phase + 1, world);
        }
        BossDefinition.Phase current = definition.phases.get(phase);
        for (int i = 0; i < current.abilities.size; i++) {
            timers[i] -= dt;
            if (timers[i] > 0f) {
                continue;
            }
            BossDefinition.Ability ability = current.abilities.get(i);
            boolean used = BossAbilities.execute(ability, boss, world);
            timers[i] = used ? ability.cooldown * cooldownMultiplier : RETRY_DELAY;
            if (!boss.isTargetable()) {
                return;
            }
        }
    }

    private void enterPhase(int index, GameWorld world) {
        phase = index;
        BossDefinition.Phase p = definition.phases.get(index);
        timers = new float[p.abilities.size];
        for (int i = 0; i < timers.length; i++) {
            timers[i] = p.abilities.get(i).initialDelay;
        }
        boss.bossSpeedMult = p.speedMult;
        boss.bossDamageMult = p.damageMult;
        world.events.bossPhase(boss, index, p.announcement);
    }

    public int phase() {
        return Math.max(0, phase);
    }

    public int phaseCount() {
        return definition.phases.size;
    }

    /** Hp fractions at which later phases start (for the boss bar notches). */
    public float threshold(int phaseIndex) {
        return definition.phases.get(phaseIndex).threshold;
    }
}
