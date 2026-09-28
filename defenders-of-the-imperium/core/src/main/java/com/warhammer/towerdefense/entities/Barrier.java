package com.warhammer.towerdefense.entities;

import com.warhammer.towerdefense.world.GameWorld;

/**
 * Temporary void-shield wall projected by a Techmarine Fortress Node. It does not fight, but holds up to
 * {@link #maxEngaged} enemies on the road until it collapses or expires.
 */
public final class Barrier extends AlliedUnit {
    private static final float CATCH_RADIUS = 34f;

    public float duration;

    public Barrier() {
        sprite = "barrier";
        name = "Void Barrier";
        mobile = false;
        canAttack = false;
        blocksEnemies = true;
        maxEngaged = 8;
        radius = 18f;
    }

    @Override
    public void update(GameWorld w, float dt) {
        super.update(w, dt);
        if (!alive) {
            return;
        }
        while (engaged.size < maxEngaged) {
            Enemy e = w.targeting.findEngageable(x, y, CATCH_RADIUS);
            if (e == null) {
                break;
            }
            engage(e);
        }
    }

    public float lifeFraction() {
        return duration <= 0f ? 0f : Math.max(0f, lifetime / duration);
    }
}
