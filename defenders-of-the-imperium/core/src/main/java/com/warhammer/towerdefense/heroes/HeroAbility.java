package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.world.GameWorld;

/** An active hero power. Numbers come from the ability's {@code params} in heroes.json. */
public interface HeroAbility {
    /** @return false if the ability could not be used now (no target in range...), so no cooldown is spent. */
    boolean activate(Hero hero, AbilityDefinition definition, GameWorld world);
}
