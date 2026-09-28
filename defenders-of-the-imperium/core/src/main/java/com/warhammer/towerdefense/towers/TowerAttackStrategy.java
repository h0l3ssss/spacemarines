package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.world.GameWorld;

/** Per-tower behaviour. One instance per tower, so strategies may keep private state (squads, heat, skulls). */
public interface TowerAttackStrategy {
    void update(Tower tower, GameWorld world, float dt);

    /** Called for every tower before {@link #update}; support towers push their buffs here. */
    default void applyAura(Tower tower, GameWorld world) {
    }

    default void onStatsChanged(Tower tower, GameWorld world) {
    }

    default void onRallyPointChanged(Tower tower, GameWorld world) {
    }

    default void onRemoved(Tower tower, GameWorld world) {
    }
}
