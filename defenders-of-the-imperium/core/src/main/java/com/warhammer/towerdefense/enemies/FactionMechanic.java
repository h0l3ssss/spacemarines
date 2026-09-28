package com.warhammer.towerdefense.enemies;

import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Faction-wide rule that runs every simulation step (Tyranid synapse today; Necron reanimation, Ork WAAAGH!,
 * Chaos Dark Pact or T'au markerlights are meant to be added as further implementations).
 */
public interface FactionMechanic {
    void update(GameWorld world, float dt);

    default void onEnemyKilled(GameWorld world, Enemy enemy) {
    }
}
