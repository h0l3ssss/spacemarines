package com.warhammer.towerdefense.abilities;

import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.entities.DropPod;
import com.warhammer.towerdefense.world.GameWorld;

/** ORBITAL DROP: a drop pod slams into the target, stunning enemies, and two Space Marines hold the ground. */
final class OrbitalDrop implements GlobalAbility {
    @Override
    public void cast(GameWorld world, AbilitySystem system, AbilityDefinition def, float x, float y) {
        DropPod pod = new DropPod();
        pod.x = x;
        pod.y = y;
        pod.alive = true;
        pod.fallTime = def.param("fallTime", 1.1f);
        pod.marines = (int) def.param("marines", 2f);
        pod.marineDuration = def.param("duration", 25f);
        pod.impactDamage = def.param("impactDamage", 90f);
        pod.impactRadius = def.param("impactRadius", 70f);
        pod.stun = def.param("stun", 1.5f);
        pod.marineHp = def.param("marineHp", 220f);
        pod.marineDamage = def.param("marineDamage", 16f);
        system.dropPods.add(pod);
    }
}
