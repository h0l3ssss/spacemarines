package com.warhammer.towerdefense.heroes;

import com.warhammer.towerdefense.data.DataException;

/**
 * Maps ability ids used in heroes.json to implementations. The Captain of the Ultramarines is pure data (see
 * heroes.json); his four powers are the classes registered here, so new heroes can mix and match them.
 */
public final class HeroAbilities {
    public static final String BOLTSTORM = "boltstorm";
    public static final String RALLY = "rally";
    public static final String POWER_SWORD = "power_sword";
    public static final String ANGEL_OF_DEATH = "angel_of_death";

    private HeroAbilities() {
    }

    public static HeroAbility create(String id) {
        switch (id) {
            case BOLTSTORM:
                return new BoltstormAbility();
            case RALLY:
                return new RallyAbility();
            case POWER_SWORD:
                return new PowerSwordAbility();
            case ANGEL_OF_DEATH:
                return new AngelOfDeathAbility();
            default:
                throw new DataException("heroes.json: unknown hero ability '" + id + "'");
        }
    }
}
