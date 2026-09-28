package com.warhammer.towerdefense;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.entities.SpaceMarine;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.world.GameWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyMovementTest {
    @Test
    void straightPathHasExactLengthAndSampling() {
        Array<Vector2> pts = new Array<>();
        pts.add(new Vector2(0, 0));
        pts.add(new Vector2(300, 0));
        pts.add(new Vector2(300, 400));
        GamePath path = new GamePath(0, pts, false, 60f);
        assertEquals(700f, path.length(), 0.01f);
        Vector2 pos = new Vector2();
        Vector2 dir = new Vector2();
        path.sample(500f, 0, pos, dir);
        assertEquals(300f, pos.x, 0.01f);
        assertEquals(200f, pos.y, 0.01f);
        assertEquals(1f, dir.y, 0.01f);
        assertEquals(350f, path.project(310f, 50f), 0.5f);
    }

    @Test
    void smoothPathPassesThroughControlPoints() {
        Array<Vector2> pts = new Array<>();
        pts.add(new Vector2(0, 0));
        pts.add(new Vector2(200, 100));
        pts.add(new Vector2(400, 0));
        GamePath path = new GamePath(0, pts, true, 60f);
        assertTrue(path.distanceTo(200f, 100f) < 1f, "spline interpolates control points");
        assertTrue(path.length() > 447f, "curve is at least as long as the polyline");
    }

    @Test
    void enemyMovesAtItsSpeedAlongThePath() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "termagant", 100f);
        float speed = e.currentSpeed();
        for (int i = 0; i < 60; i++) {
            world.enemySystem.update(1f / 60f);
        }
        assertEquals(100f + speed, e.distance, 0.05f);
    }

    @Test
    void slowAndStunReduceMovement() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "termagant", 100f);
        float base = e.currentSpeed();
        e.applyStatus(StatusType.SLOW, 5f, 0.3f);
        assertEquals(base * 0.7f, e.currentSpeed(), 0.01f);
        e.applyStatus(StatusType.STUN, 1f, 1f);
        float before = e.distance;
        world.enemySystem.update(0.5f);
        assertEquals(before, e.distance, 0.0001f);
    }

    @Test
    void enemyReachingTheFortressDamagesIt() {
        GameWorld world = TestSupport.world();
        GamePath path = world.paths.first();
        Enemy e = TestSupport.spawn(world, "termagant", path.length() - 1f);
        int leakDamage = e.def.leakDamage;
        int hp = world.state.baseHp;
        world.enemySystem.update(0.5f);
        assertEquals(hp - leakDamage, world.state.baseHp);
        assertEquals(0, world.enemies.size, "leaked enemies are removed");
        assertEquals(1, world.state.leaks);
    }

    @Test
    void blockedEnemyStopsAndFights() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "termagant", 600f);
        SpaceMarine m = new SpaceMarine();
        m.configure(world.data.unit("assault_marine"));
        m.maxHp = 100f;
        m.damageMin = m.damageMax = 1f;
        m.spawn(e.x + 5f, e.y);
        m.guardX = m.x;
        m.guardY = m.y;
        world.addUnit(m);
        m.engage(e);
        float before = e.distance;
        for (int i = 0; i < 120; i++) {
            world.enemySystem.update(1f / 60f);
        }
        assertEquals(before, e.distance, 0.0001f);
        assertEquals(EnemyState.FIGHTING, e.state);
        assertTrue(m.hp < m.maxHp, "the termagant attacked the marine");
    }
}
