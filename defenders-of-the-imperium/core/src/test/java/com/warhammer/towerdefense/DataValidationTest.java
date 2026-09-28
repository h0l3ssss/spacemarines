package com.warhammer.towerdefense;

import com.warhammer.towerdefense.abilities.AbilitySystem;
import com.warhammer.towerdefense.abilities.GlobalAbilities;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.GameData;
import com.warhammer.towerdefense.data.HeroDefinition;
import com.warhammer.towerdefense.heroes.HeroAbilities;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.waves.WaveSetDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataValidationTest {
    @Test
    void allDataFilesLoadAndCrossReferencesResolve() {
        GameData data = TestSupport.data();
        assertEquals(4, data.towers.size, "four Space Marine towers");
        for (TowerType t : data.towers) {
            assertEquals(3, t.tiers.size, t.id + " tiers");
            assertEquals(3, t.specializations.size, t.id + " specialisations");
            assertTrue(t.buildCost() > 0);
        }
        assertTrue(data.enemies.size >= 7, "six Tyranid types plus the Hive Tyrant");
        assertTrue(data.enemy("hive_tyrant").boss);
        assertEquals(3, data.difficulties.size);
        assertFalse(data.armoury.isEmpty());
    }

    @Test
    void mvpCampaignHasTenWavesEndingWithTheBoss() {
        GameData data = TestSupport.data();
        MapDefinition map = data.map(TestSupport.MAP);
        assertNotNull(map);
        assertTrue(map.available);
        assertTrue(map.buildPoints.size >= 10);
        WaveSetDefinition waves = data.waveSet(map.waveSet);
        assertEquals(10, waves.size());
        assertTrue(waves.waves.peek().boss, "wave 10 is the boss wave");
        assertTrue(waves.waves.peek().enemyCounts().containsKey("hive_tyrant"));
        assertFalse(data.map("necron_tomb_world").available, "future maps are announced but locked");
    }

    @Test
    void everyAbilityIdHasAnImplementation() {
        GameData data = TestSupport.data();
        for (HeroDefinition hero : data.heroes.values()) {
            for (AbilityDefinition a : hero.abilities) {
                assertNotNull(HeroAbilities.create(a.id));
            }
        }
        for (AbilityDefinition a : data.globalAbilities) {
            assertNotNull(GlobalAbilities.create(a.id));
        }
        assertThrows(DataException.class, () -> HeroAbilities.create("does_not_exist"));
    }

    @Test
    void unitArchetypesUsedByCodeExist() {
        GameData data = TestSupport.data();
        assertNotNull(data.unit(AbilitySystem.DROP_UNIT));
        assertNotNull(data.unit("chaplain"));
    }
}
