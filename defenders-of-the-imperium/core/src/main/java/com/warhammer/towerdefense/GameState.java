package com.warhammer.towerdefense;

/** Scoreboard of a battle: requisition, fortress integrity, outcome and statistics. */
public final class GameState {
    public int requisition;
    public int baseHp;
    public final int baseMaxHp;
    public boolean victory;
    public boolean defeat;
    public float elapsed;
    public int kills;
    public int leaks;
    public int requisitionEarned;

    public GameState(int startingRequisition, int baseHp) {
        this.requisition = startingRequisition;
        this.baseHp = baseHp;
        this.baseMaxHp = baseHp;
    }

    public boolean canAfford(int cost) {
        return requisition >= cost;
    }

    public boolean spend(int cost) {
        if (cost < 0 || cost > requisition) {
            return false;
        }
        requisition -= cost;
        return true;
    }

    /** Income from kills, waves and bonuses (counted in the end-of-battle statistics). */
    public void earn(int amount) {
        if (amount > 0) {
            requisition += amount;
            requisitionEarned += amount;
        }
    }

    /** Money returned by selling a tower (not counted as earned). */
    public void refund(int amount) {
        if (amount > 0) {
            requisition += amount;
        }
    }

    public boolean isOver() {
        return victory || defeat;
    }

    public float baseFraction() {
        return baseMaxHp <= 0 ? 0f : Math.max(0f, baseHp / (float) baseMaxHp);
    }

    /** Campaign stars: 3 with at least 90% integrity left, 2 with 50%, otherwise 1. */
    public int stars() {
        if (!victory) {
            return 0;
        }
        float f = baseFraction();
        if (f >= 0.9f) {
            return 3;
        }
        return f >= 0.5f ? 2 : 1;
    }
}
