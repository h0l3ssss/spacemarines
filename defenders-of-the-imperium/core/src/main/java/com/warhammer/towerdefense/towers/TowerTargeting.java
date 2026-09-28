package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.entities.Enemy;

/**
 * Target priority of a tower. Every candidate in range gets a score and the highest score wins, so adding a mode is
 * a single enum constant.
 */
public enum TowerTargeting {
    FIRST("FIRST", "Enemy closest to the fortress"),
    LAST("LAST", "Enemy furthest from the fortress"),
    STRONGEST("STRONG", "Enemy with the most hit points"),
    WEAKEST("WEAK", "Enemy with the fewest hit points"),
    CLOSEST("CLOSE", "Enemy closest to the tower"),
    FASTEST("FAST", "Fastest moving enemy");

    private static final TowerTargeting[] VALUES = values();

    public final String label;
    public final String description;

    TowerTargeting(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public float score(Enemy e, float towerX, float towerY) {
        switch (this) {
            case FIRST:
                return -e.remainingDistance();
            case LAST:
                return e.remainingDistance();
            case STRONGEST:
                return e.hp;
            case WEAKEST:
                return -e.hp;
            case CLOSEST: {
                float dx = e.x - towerX;
                float dy = e.y - towerY;
                return -(dx * dx + dy * dy);
            }
            case FASTEST:
                return e.currentSpeed();
            default:
                return 0f;
        }
    }

    public TowerTargeting next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
