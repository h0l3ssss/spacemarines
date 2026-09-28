package com.warhammer.towerdefense.combat;

/**
 * How damage interacts with armour. Armour is a 0..1 fraction; the effective armour an attack faces is
 * {@code armour * armorFactor - armourPierce}. Energy weapons (plasma, lascannon) mostly ignore armour, which
 * makes them the natural counter to elites such as Tyranid Warriors and the Carnifex.
 */
public enum DamageType {
    KINETIC(1f),
    EXPLOSIVE(0.6f),
    ENERGY(0.3f),
    TRUE(0f);

    public final float armorFactor;

    DamageType(float armorFactor) {
        this.armorFactor = armorFactor;
    }
}
