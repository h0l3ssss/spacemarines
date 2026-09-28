package com.warhammer.towerdefense.abilities;

import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.Thunderhawk;
import com.warhammer.towerdefense.world.GameWorld;

/** AERIAL SUPPORT: a Thunderhawk gunship strafes along a line and carpet-bombs around the chosen point. */
final class AerialSupport implements GlobalAbility {
    private static final float ENTRY_MARGIN = 220f;

    @Override
    public void cast(GameWorld world, AbilitySystem system, AbilityDefinition def, float x, float y) {
        Thunderhawk ship = new Thunderhawk();
        ship.alive = true;
        ship.x = -ENTRY_MARGIN;
        ship.y = y;
        ship.lineY = y;
        ship.vx = def.param("speed", 520f);
        ship.exitX = Constants.WORLD_WIDTH + ENTRY_MARGIN;
        ship.fireInterval = def.param("fireInterval", 0.09f);
        ship.strafeDamage = def.param("strafeDamage", 26f);
        ship.strafeWidth = def.param("strafeWidth", 75f);
        ship.bombX = x - def.param("bombLead", 80f);
        ship.bombs = (int) def.param("bombs", 6f);
        ship.bombDamage = def.param("bombDamage", 120f);
        ship.bombRadius = def.param("bombRadius", 55f);
        system.gunships.add(ship);
    }
}
