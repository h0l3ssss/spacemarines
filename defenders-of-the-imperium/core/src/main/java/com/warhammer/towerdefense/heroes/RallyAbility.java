package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/** RALLY THE BROTHERS: nearby Space Marines (and tower crews) fight harder for a while. */
final class RallyAbility implements HeroAbility {
    @Override
    public boolean activate(Hero hero, AbilityDefinition def, GameWorld world) {
        float radius = def.param("radius", 180f);
        float duration = def.param("duration", 8f);
        float bonus = def.param("damageBonus", 0.35f);
        float towerBonus = def.param("towerDamageBonus", 0.2f);
        float r2 = radius * radius;
        for (AlliedUnit u : world.units) {
            if (u.alive && u.dst2(hero.x, hero.y) <= r2) {
                u.buffDamage = Math.max(u.buffDamage, bonus);
                u.buffTimer = Math.max(u.buffTimer, duration);
            }
        }
        hero.buffDamage = Math.max(hero.buffDamage, bonus);
        hero.buffTimer = Math.max(hero.buffTimer, duration);
        for (Tower t : world.towers) {
            if (t.dst2(hero.x, hero.y) <= r2) {
                t.rallyBuff = Math.max(t.rallyBuff, towerBonus);
                t.rallyTimer = Math.max(t.rallyTimer, duration);
            }
        }
        return true;
    }
}
