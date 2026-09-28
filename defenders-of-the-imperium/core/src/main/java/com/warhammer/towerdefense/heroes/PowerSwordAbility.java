package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/** POWER SWORD: a crackling sweep that cleaves the target and everything around it, stunning the survivors. */
final class PowerSwordAbility implements HeroAbility {
    private final DamageInfo payload = new DamageInfo();

    @Override
    public boolean activate(Hero hero, AbilityDefinition def, GameWorld world) {
        Enemy target = world.targeting.findNearest(hero.x, hero.y, def.param("reach", 80f), false, true);
        if (target == null) {
            return false;
        }
        payload.reset();
        payload.amount = (def.param("damage", 180f) + def.param("damagePerLevel", 20f) * (hero.level - 1))
                * hero.damageMultiplier();
        payload.type = DamageType.ENERGY;
        payload.armorPierce = def.param("armorPierce", 0.3f);
        payload.splashRadius = def.param("radius", 55f);
        payload.splashFalloff = 0.4f;
        payload.hitsAir = false;
        payload.source = DamageInfo.Source.HERO;
        payload.heroCredit = true;
        payload.withStatus(StatusType.STUN, 1f, def.param("stun", 1.2f), 1f);
        world.damage.damageEnemy(target, payload);
        world.damage.splash(target.x, target.y, payload, target);
        hero.facing = target.x >= hero.x ? 1f : -1f;
        hero.attackAnim = 0.5f;
        return true;
    }
}
