package com.warhammer.towerdefense;

import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.DamageSystem;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.world.GameWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSystemTest {
    private static final float EPS = 0.001f;

    @Test
    void armourReducesKineticFullyAndEnergyMostlyIgnoresIt() {
        assertEquals(50f, DamageSystem.mitigate(100f, 0.5f, DamageType.KINETIC, 0f), EPS);
        assertEquals(70f, DamageSystem.mitigate(100f, 0.5f, DamageType.EXPLOSIVE, 0f), EPS);
        assertEquals(85f, DamageSystem.mitigate(100f, 0.5f, DamageType.ENERGY, 0f), EPS);
        assertEquals(100f, DamageSystem.mitigate(100f, 0.5f, DamageType.TRUE, 0f), EPS);
    }

    @Test
    void armourPierceAndClampWork() {
        assertEquals(70f, DamageSystem.mitigate(100f, 0.5f, DamageType.KINETIC, 0.2f), EPS);
        assertEquals(100f, DamageSystem.mitigate(100f, 0.1f, DamageType.KINETIC, 0.5f), EPS, "pierce never heals");
        assertEquals(10f, DamageSystem.mitigate(100f, 5f, DamageType.KINETIC, 0f), EPS, "armour capped at 90%");
    }

    @Test
    void eliteBossAndMarkMultipliersApply() {
        GameWorld world = TestSupport.world();
        Enemy warrior = TestSupport.spawn(world, "tyranid_warrior", 400f);
        DamageInfo info = new DamageInfo();
        info.amount = 100f;
        info.type = DamageType.TRUE;
        info.eliteMult = 2f;
        assertEquals(200f, DamageSystem.computeDamage(warrior, info, 100f), EPS);
        warrior.applyStatus(StatusType.MARKED, 5f, 0.25f);
        assertEquals(250f, DamageSystem.computeDamage(warrior, info, 100f), EPS);

        Enemy gaunt = TestSupport.spawn(world, "termagant", 300f);
        assertEquals(100f, DamageSystem.computeDamage(gaunt, info, 100f), EPS, "elite bonus ignored on gaunts");
    }

    @Test
    void shieldAbsorbsBeforeHitPoints() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "carnifex", 300f);
        float hp = e.hp;
        e.applyStatus(StatusType.SHIELD, 10f, 150f);
        DamageInfo info = new DamageInfo();
        info.amount = 100f;
        info.type = DamageType.TRUE;
        world.damage.damageEnemy(e, info);
        assertEquals(hp, e.hp, EPS, "fully absorbed");
        world.damage.damageEnemy(e, info);
        assertEquals(hp - 50f, e.hp, EPS, "50 left over after the shield broke");
        assertFalse(e.has(StatusType.SHIELD));
    }

    @Test
    void killingAwardsBountyExactlyOnce() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "termagant", 300f);
        int before = world.state.requisition;
        DamageInfo info = new DamageInfo();
        info.amount = 10_000f;
        info.type = DamageType.TRUE;
        world.damage.damageEnemy(e, info);
        assertEquals(EnemyState.DYING, e.state);
        world.damage.damageEnemy(e, info);
        assertEquals(before + e.def.bounty, world.state.requisition);
        assertEquals(1, world.state.kills);
        assertFalse(e.isTargetable());
    }

    @Test
    void burnDamagesOverTime() {
        GameWorld world = TestSupport.world();
        Enemy e = TestSupport.spawn(world, "carnifex", 300f);
        float hp = e.hp;
        e.applyStatus(StatusType.BURN, 2f, 20f);
        for (int i = 0; i < 60; i++) {
            world.enemySystem.update(1f / 60f);
        }
        assertEquals(hp - 20f, e.hp, 0.5f);
        assertTrue(e.has(StatusType.BURN));
    }
}
