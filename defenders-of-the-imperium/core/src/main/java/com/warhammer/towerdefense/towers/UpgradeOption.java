package com.warhammer.towerdefense.towers;

/** A purchase currently available for a specific tower (shown as a button in the tower panel). */
public final class UpgradeOption {
    public enum Kind {
        TIER, SPECIALIZATION, RANK
    }

    public final Kind kind;
    /** Tier index, specialisation index or rank index depending on {@link #kind}. */
    public final int index;
    public final TowerUpgrade upgrade;
    public final int cost;

    public UpgradeOption(Kind kind, int index, TowerUpgrade upgrade, int cost) {
        this.kind = kind;
        this.index = index;
        this.upgrade = upgrade;
        this.cost = cost;
    }
}
