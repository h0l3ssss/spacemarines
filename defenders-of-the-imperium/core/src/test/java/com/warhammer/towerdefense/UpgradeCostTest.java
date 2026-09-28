package com.warhammer.towerdefense;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.world.GameWorld;
import com.warhammer.towerdefense.world.MatchSetup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeCostTest {
    private final Array<UpgradeOption> options = new Array<>();

    private static Tower build(GameWorld world, String type) {
        world.state.requisition = 100_000;
        Tower t = world.build(world.map.buildPoints.first(), world.data.tower(type));
        t.buildTimer = 0f;
        return t;
    }

    private UpgradeOption only(GameWorld world, Tower t) {
        world.upgradeOptions(t, options);
        assertEquals(1, options.size);
        return options.first();
    }

    @Test
    void tierUpgradesFollowTheDataFile() {
        GameWorld world = TestSupport.world();
        Tower t = build(world, "tactical");
        assertEquals(70, t.totalSpent);
        UpgradeOption tier2 = only(world, t);
        assertEquals(UpgradeOption.Kind.TIER, tier2.kind);
        assertEquals(110, tier2.cost);
        world.upgrade(t, tier2);
        t.buildTimer = 0f;
        UpgradeOption tier3 = only(world, t);
        assertEquals(160, tier3.cost);
        world.upgrade(t, tier3);
        t.buildTimer = 0f;
        assertEquals(340, t.totalSpent);
        assertEquals(3, t.tier);
        assertEquals(3, t.stats.getInt(Stat.CREW_COUNT));
    }

    @Test
    void specialisationsAndRanksApplyModifiers() {
        GameWorld world = TestSupport.world();
        Tower t = build(world, "tactical");
        t.tier = 3;
        t.recomputeStats();
        world.upgradeOptions(t, options);
        assertEquals(3, options.size, "three specialisations offered at tier 3");
        assertEquals(230, options.get(0).cost);
        assertEquals(260, options.get(2).cost);

        world.upgrade(t, options.get(0));
        t.buildTimer = 0f;
        assertEquals(0.5f * 0.8f, t.stats.get(Stat.COOLDOWN), 0.0001f, "Bolter Discipline: +25% fire rate");
        assertEquals(0.15f, t.stats.get(Stat.EXTRA_SHOT_CHANCE), 0.0001f);

        UpgradeOption rank2 = only(world, t);
        assertEquals(UpgradeOption.Kind.RANK, rank2.kind);
        world.upgrade(t, rank2);
        t.buildTimer = 0f;
        assertEquals(0.5f * 0.8f * 0.8f, t.stats.get(Stat.COOLDOWN), 0.0001f);
        world.upgrade(t, only(world, t));
        assertEquals(0.35f, t.stats.get(Stat.EXTRA_SHOT_CHANCE), 0.0001f);
        world.upgradeOptions(t, options);
        assertTrue(options.isEmpty(), "fully upgraded");
        assertTrue(t.isMaxed());
        assertEquals(6, t.level());
    }

    @Test
    void heavyBolterSwapsTheWeapon() {
        GameWorld world = TestSupport.world();
        Tower t = build(world, "tactical");
        t.tier = 3;
        t.recomputeStats();
        world.upgradeOptions(t, options);
        world.upgrade(t, options.get(2));
        assertEquals(ProjectileType.HEAVY_BOLT, t.stats.projectile);
        assertEquals(1, t.stats.getInt(Stat.CREW_COUNT));
        assertTrue(t.stats.get(Stat.SPLASH_RADIUS) > 0f);
    }

    @Test
    void lascannonHitsFlyersWhileMissilesCannot() {
        GameWorld world = TestSupport.world();
        Tower t = build(world, "devastator");
        assertTrue(!t.stats.canHitAir);
        t.tier = 3;
        t.recomputeStats();
        world.upgradeOptions(t, options);
        world.upgrade(t, options.get(0));
        assertEquals(ProjectileType.LASER, t.stats.projectile);
        assertTrue(t.stats.canHitAir);
    }

    @Test
    void armouryDiscountReducesEveryPurchase() {
        MapDefinition map = TestSupport.data().map(TestSupport.MAP);
        MatchSetup setup = new MatchSetup(map);
        setup.difficulty = Difficulty.VETERAN;
        setup.bonuses.upgradeCostMult = 0.9f;
        GameWorld world = new GameWorld(TestSupport.data(), setup);
        assertEquals(Math.round(70 * 0.9f), world.buildCost(world.data.tower("tactical")));
        Tower t = build(world, "tactical");
        assertEquals(Math.round(110 * 0.9f), only(world, t).cost);
    }
}
