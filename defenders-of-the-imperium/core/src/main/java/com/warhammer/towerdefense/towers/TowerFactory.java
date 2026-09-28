package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.map.BuildPoint;

/** Creates towers and wires the behaviour declared in the data file. */
public final class TowerFactory {
    private TowerFactory() {
    }

    public static Tower create(TowerType type, BuildPoint slot) {
        return new Tower(type, slot, strategyFor(type.behavior));
    }

    static TowerAttackStrategy strategyFor(TowerBehavior behavior) {
        switch (behavior) {
            case RANGED:
                return new RangedAttackStrategy();
            case BARRACKS:
                return new BarracksStrategy();
            case SUPPORT:
                return new SupportStrategy();
            default:
                throw new IllegalArgumentException("Unsupported tower behaviour " + behavior);
        }
    }
}
