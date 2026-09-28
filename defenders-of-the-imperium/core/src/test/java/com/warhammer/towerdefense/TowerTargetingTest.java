package com.warhammer.towerdefense;

import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.towers.TowerTargeting;
import com.warhammer.towerdefense.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TowerTargetingTest {
    private GameWorld world;
    private Enemy behind;
    private Enemy middle;
    private Enemy ahead;
    private float cx;
    private float cy;

    @BeforeEach
    void setUp() {
        world = TestSupport.world();
        // three enemies along the first straight section of path 0, 60 units apart
        behind = TestSupport.spawn(world, "termagant", 200f);
        middle = TestSupport.spawn(world, "carnifex", 260f);
        ahead = TestSupport.spawn(world, "hormagaunt", 320f);
        middle.hp = 900f;
        behind.hp = 10f;
        cx = behind.x;
        cy = behind.y + 60f;
        world.rebuildGrid();
    }

    private Enemy target(TowerTargeting priority) {
        return world.targeting.findTarget(cx, cy, 400f, priority, true, true);
    }

    @Test
    void firstTargetsTheEnemyClosestToTheFortress() {
        assertSame(ahead, target(TowerTargeting.FIRST));
    }

    @Test
    void lastTargetsTheEnemyFurthestFromTheFortress() {
        assertSame(behind, target(TowerTargeting.LAST));
    }

    @Test
    void strongestAndWeakestUseCurrentHitPoints() {
        assertSame(middle, target(TowerTargeting.STRONGEST));
        assertSame(behind, target(TowerTargeting.WEAKEST));
    }

    @Test
    void closestUsesDistanceToTheTower() {
        assertSame(behind, target(TowerTargeting.CLOSEST));
    }

    @Test
    void fastestPrefersHormagaunts() {
        assertSame(ahead, target(TowerTargeting.FASTEST));
        ahead.applyStatus(StatusType.STUN, 5f, 1f);
        assertSame(behind, target(TowerTargeting.FASTEST), "stunned enemies have speed 0");
    }

    @Test
    void rangeAndAirFilteringAreRespected() {
        assertNull(world.targeting.findTarget(cx, cy + 2000f, 100f, TowerTargeting.FIRST, true, true));
        Enemy flyer = TestSupport.spawn(world, "gargoyle", 330f);
        world.rebuildGrid();
        Enemy groundOnly = world.targeting.findTarget(cx, cy, 400f, TowerTargeting.FIRST, false, true);
        assertSame(ahead, groundOnly, "missiles cannot target the gargoyle even if it is first");
        assertSame(flyer, world.targeting.findTarget(cx, cy, 400f, TowerTargeting.FIRST, true, true));
    }
}
