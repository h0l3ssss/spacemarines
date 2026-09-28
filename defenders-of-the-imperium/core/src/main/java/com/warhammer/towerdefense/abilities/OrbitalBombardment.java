package com.warhammer.towerdefense.abilities;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.OrbitalStrike;
import com.warhammer.towerdefense.world.GameWorld;

/** ORBITAL BOMBARDMENT: after a short targeting delay a lance strike obliterates the area. */
final class OrbitalBombardment implements GlobalAbility {
    @Override
    public void cast(GameWorld world, AbilitySystem system, AbilityDefinition def, float x, float y) {
        OrbitalStrike strike = new OrbitalStrike();
        strike.x = x;
        strike.y = y;
        strike.alive = true;
        strike.delay = def.param("delay", 2.2f);
        strike.radius = def.param("radius", 110f);
        strike.damage = def.param("damage", 500f);
        system.strikes.add(strike);
    }
}
