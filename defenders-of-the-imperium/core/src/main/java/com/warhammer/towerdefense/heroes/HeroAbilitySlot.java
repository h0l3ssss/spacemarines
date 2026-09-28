package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.data.AbilityDefinition;

/** Runtime state (cooldown) of one hero ability. */
public final class HeroAbilitySlot {
    public final AbilityDefinition definition;
    public final HeroAbility ability;
    public float cooldown;

    public HeroAbilitySlot(AbilityDefinition definition, HeroAbility ability) {
        this.definition = definition;
        this.ability = ability;
    }

    public void tick(float dt) {
        if (cooldown > 0f) {
            cooldown = Math.max(0f, cooldown - dt);
        }
    }

    public boolean isReady() {
        return cooldown <= 0f;
    }

    public float cooldownFraction() {
        return definition.cooldown <= 0f ? 0f : cooldown / definition.cooldown;
    }
}
