package com.warhammer.towerdefense.combat;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.towers.TowerTargeting;

/** Enemy queries used by towers, units, the hero and abilities. All queries go through the {@link SpatialGrid}. */
public final class TargetingSystem {
    private final SpatialGrid grid;
    private final Array<Enemy> candidates = new Array<>(false, 128);

    public TargetingSystem(SpatialGrid grid) {
        this.grid = grid;
    }

    /** Best enemy in range according to the tower's priority, or null. */
    public Enemy findTarget(float x, float y, float range, TowerTargeting priority, boolean hitAir, boolean hitGround) {
        candidates.clear();
        grid.query(x, y, range, candidates);
        float r2 = range * range;
        Enemy best = null;
        float bestScore = -Float.MAX_VALUE;
        for (int i = 0; i < candidates.size; i++) {
            Enemy e = candidates.get(i);
            if (!isCandidate(e, hitAir, hitGround) || e.dst2(x, y) > r2) {
                continue;
            }
            float score = priority.score(e, x, y);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Nearest enemy in range, or null. */
    public Enemy findNearest(float x, float y, float range, boolean hitAir, boolean hitGround) {
        candidates.clear();
        grid.query(x, y, range, candidates);
        float best = range * range;
        Enemy result = null;
        for (int i = 0; i < candidates.size; i++) {
            Enemy e = candidates.get(i);
            if (!isCandidate(e, hitAir, hitGround)) {
                continue;
            }
            float d2 = e.dst2(x, y);
            if (d2 <= best) {
                best = d2;
                result = e;
            }
        }
        return result;
    }

    /** Nearest ground enemy that nobody is holding yet (for blockers). */
    public Enemy findEngageable(float x, float y, float range) {
        candidates.clear();
        grid.query(x, y, range, candidates);
        float best = range * range;
        Enemy result = null;
        for (int i = 0; i < candidates.size; i++) {
            Enemy e = candidates.get(i);
            if (!e.isBlockable() || !e.isOnMap() || e.isBlocked()) {
                continue;
            }
            float d2 = e.dst2(x, y);
            if (d2 <= best) {
                best = d2;
                result = e;
            }
        }
        return result;
    }

    /** Appends all valid enemies inside the circle to {@code out}; returns how many were added. */
    public int collect(float x, float y, float radius, boolean hitAir, boolean hitGround, Array<Enemy> out) {
        candidates.clear();
        grid.query(x, y, radius, candidates);
        float r2 = radius * radius;
        int added = 0;
        for (int i = 0; i < candidates.size; i++) {
            Enemy e = candidates.get(i);
            if (isCandidate(e, hitAir, hitGround) && e.dst2(x, y) <= r2) {
                out.add(e);
                added++;
            }
        }
        return added;
    }

    /** The {@code n} nearest enemies inside the circle, closest first. */
    public int nearest(float x, float y, float radius, int n, boolean hitAir, Array<Enemy> out) {
        out.clear();
        collect(x, y, radius, hitAir, true, out);
        out.sort((a, b) -> Float.compare(a.dst2(x, y), b.dst2(x, y)));
        if (out.size > n) {
            out.truncate(n);
        }
        return out.size;
    }

    private static boolean isCandidate(Enemy e, boolean hitAir, boolean hitGround) {
        if (!e.isTargetable() || !e.isOnMap()) {
            return false;
        }
        return e.flying ? hitAir : hitGround;
    }
}
