package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * ANGEL OF DEATH (ultimate): the Captain becomes a paragon of war for a few seconds - more damage, more speed, less
 * damage taken - and every battle-brother and tower nearby shares in his fury.
 */
final class AngelOfDeathAbility implements HeroAbility {
    @Override
    public boolean activate(Hero hero, AbilityDefinition def, GameWorld world) {
        float duration = def.param("duration", 10f);
        hero.empower(duration, def.param("damageMult", 1.8f), def.param("speedMult", 1.4f),
                def.param("damageTaken", 0.5f));
        hero.hp = Math.min(hero.maxHp, hero.hp + hero.maxHp * def.param("heal", 0.25f));
        float radius = def.param("allyRadius", 220f);
        float allyBonus = def.param("allyDamageBonus", 0.3f);
        float r2 = radius * radius;
        for (AlliedUnit u : world.units) {
            if (u.alive && u.dst2(hero.x, hero.y) <= r2) {
                u.buffDamage = Math.max(u.buffDamage, allyBonus);
                u.buffTimer = Math.max(u.buffTimer, duration);
            }
        }
        for (Tower t : world.towers) {
            if (t.dst2(hero.x, hero.y) <= r2) {
                t.rallyBuff = Math.max(t.rallyBuff, allyBonus);
                t.rallyTimer = Math.max(t.rallyTimer, duration);
            }
        }
        return true;
    }
}
