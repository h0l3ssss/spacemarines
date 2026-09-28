package com.warhammer.towerdefense.abilities;

import com.warhammer.towerdefense.data.DataException;

/** Maps ids in abilities.json to implementations. */
public final class GlobalAbilities {
    public static final String ORBITAL_DROP = "orbital_drop";
    public static final String ORBITAL_BOMBARDMENT = "orbital_bombardment";
    public static final String AERIAL_SUPPORT = "aerial_support";

    private GlobalAbilities() {
    }

    public static GlobalAbility create(String id) {
        switch (id) {
            case ORBITAL_DROP:
                return new OrbitalDrop();
            case ORBITAL_BOMBARDMENT:
                return new OrbitalBombardment();
            case AERIAL_SUPPORT:
                return new AerialSupport();
            default:
                throw new DataException("abilities.json: unknown ability '" + id + "'");
        }
    }
}
