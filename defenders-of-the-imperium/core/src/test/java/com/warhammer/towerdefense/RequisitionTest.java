package com.warhammer.towerdefense;

import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.world.GameWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequisitionTest {
    @Test
    void startingRequisitionDependsOnDifficulty() {
        int base = TestSupport.data().map(TestSupport.MAP).startingRequisition;
        assertEquals(base, TestSupport.world(Difficulty.VETERAN, false).state.requisition);
        assertEquals(base + 60, TestSupport.world(Difficulty.CASUAL, false).state.requisition);
        assertEquals(base - 20, TestSupport.world(Difficulty.HARD, false).state.requisition);
    }

    @Test
    void buildingSpendsAndRequiresFunds() {
        GameWorld world = TestSupport.world();
        TowerType tactical = world.data.tower("tactical");
        BuildPoint slot = world.map.buildPoints.first();
        int before = world.state.requisition;
        Tower t = world.build(slot, tactical);
        assertNotNull(t);
        assertEquals(before - tactical.buildCost(), world.state.requisition);
        assertNull(world.build(slot, tactical), "slot already occupied");

        world.state.requisition = 10;
        assertFalse(world.canBuild(world.map.buildPoints.get(1), tactical));
        assertNull(world.build(world.map.buildPoints.get(1), tactical));
        assertEquals(10, world.state.requisition, "nothing spent on failure");
    }

    @Test
    void sellingRefundsSixtyPercentOfEverythingSpent() {
        GameWorld world = TestSupport.world();
        world.state.requisition = 1000;
        Tower t = world.build(world.map.buildPoints.first(), world.data.tower("devastator"));
        t.buildTimer = 0f;
        world.upgrade(t, world.upgradeOptions(t, new com.badlogic.gdx.utils.Array<>()).first());
        int spent = t.totalSpent;
        int before = world.state.requisition;
        int refund = world.sell(t);
        assertEquals(Math.round(spent * 0.6f), refund);
        assertEquals(before + refund, world.state.requisition);
        assertTrue(world.towers.isEmpty());
    }

    @Test
    void killsPayBountyScaledByDifficultyIncome() {
        GameWorld casual = TestSupport.world(Difficulty.CASUAL, false);
        Enemy e = TestSupport.spawn(casual, "tyranid_warrior", 300f);
        int before = casual.state.requisition;
        DamageInfo lethal = new DamageInfo();
        lethal.amount = 1e6f;
        casual.damage.damageEnemy(e, lethal);
        int expected = Math.round(e.def.bounty * casual.difficulty.income);
        assertEquals(before + expected, casual.state.requisition);
        assertEquals(expected, casual.state.requisitionEarned);
    }

    @Test
    void callingAWaveEarlyGrantsABonus() {
        GameWorld world = TestSupport.world();
        assertEquals(0, world.callNextWave(), "first wave has no bonus");
        for (int i = 0; i < 60 * 20; i++) {
            world.update(1f / 60f);
        }
        assertTrue(world.waves.canCallNextWave(), "wave 1 finished spawning, countdown running");
        int before = world.state.requisition;
        int bonus = world.callNextWave();
        assertTrue(bonus > 0);
        assertEquals(before + bonus, world.state.requisition);
        assertEquals(2, world.waves.currentWave());
    }
}
