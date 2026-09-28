package com.warhammer.towerdefense.abilities;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.world.GameWorld;

/** A reinforcement / fire-support ability the player casts on a point of the battlefield. */
public interface GlobalAbility {
    void cast(GameWorld world, AbilitySystem system, AbilityDefinition definition, float x, float y);
}
