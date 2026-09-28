package com.warhammer.towerdefense.heroes;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/** BOLTSTORM: a rapid volley of bolt pistol rounds spread over the nearest enemies (air included). */
final class BoltstormAbility implements HeroAbility {
    private final Array<Enemy> targets = new Array<>(false, 16);
    private final DamageInfo payload = new DamageInfo();

    @Override
    public boolean activate(Hero hero, AbilityDefinition def, GameWorld world) {
        float range = def.param("range", 200f);
        int shots = Math.max(1, (int) def.param("shots", 8f));
        int found = world.targeting.nearest(hero.x, hero.y, range, shots, true, targets);
        if (found == 0) {
            return false;
        }
        payload.reset();
        payload.amount = (def.param("damage", 40f) + def.param("damagePerLevel", 5f) * (hero.level - 1))
                * hero.damageMultiplier();
        payload.type = DamageType.KINETIC;
        payload.armorPierce = def.param("armorPierce", 0.2f);
        payload.source = DamageInfo.Source.HERO;
        payload.heroCredit = true;
        for (int i = 0; i < shots; i++) {
            Enemy e = targets.get(i % found);
            world.projectiles.fire(ProjectileType.PISTOL_BOLT, hero.x + hero.facing * 10f, hero.y + 22f, e, payload);
        }
        hero.facing = targets.first().x >= hero.x ? 1f : -1f;
        hero.attackAnim = 0.5f;
        return true;
    }
}
