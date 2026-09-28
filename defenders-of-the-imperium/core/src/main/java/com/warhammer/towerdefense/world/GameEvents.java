package com.warhammer.towerdefense.world;

import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.data.BossDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.DropPod;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.heroes.Hero;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.waves.WaveDefinition;

/**
 * Everything the simulation reports to the presentation layer (particles, audio, camera shake, HUD banners).
 * The model never touches rendering or sound directly; it only calls these hooks. All methods default to no-ops.
 */
public interface GameEvents {
    GameEvents NONE = new GameEvents() {
    };

    default void towerBuilt(Tower tower) {
    }

    default void towerUpgraded(Tower tower, UpgradeOption option) {
    }

    default void towerSold(Tower tower, int refund) {
    }

    default void towerFired(Tower tower, ProjectileType type, float fromX, float fromY, float toX, float toY) {
    }

    default void towerOverheated(Tower tower) {
    }

    default void towerOffline(Tower tower) {
    }

    default void towerOnline(Tower tower) {
    }

    default void shotFired(ProjectileType type, float fromX, float fromY, float toX, float toY) {
    }

    default void projectileImpact(ProjectileType type, float x, float y, float splashRadius, boolean hostile) {
    }

    default void enemySpawned(Enemy enemy) {
    }

    default void enemyHit(Enemy enemy, float damage, DamageType type) {
    }

    default void statusApplied(Enemy enemy, StatusType status) {
    }

    default void enemyKilled(Enemy enemy, int bounty) {
    }

    default void enemyLeaked(Enemy enemy, int damage) {
    }

    default void enemyMelee(Enemy attacker, AlliedUnit target, float damage) {
    }

    default void enemyRanged(Enemy attacker) {
    }

    default void unitMelee(AlliedUnit attacker, Enemy target, float damage) {
    }

    default void unitSpawned(AlliedUnit unit) {
    }

    default void unitDied(AlliedUnit unit) {
    }

    default void unitRetired(AlliedUnit unit) {
    }

    default void heroLevelUp(Hero hero) {
    }

    default void heroAbility(Hero hero, AbilityDefinition ability, float x, float y) {
    }

    default void heroDied(Hero hero) {
    }

    default void heroRespawned(Hero hero) {
    }

    default void globalAbilityCast(AbilityDefinition ability, float x, float y) {
    }

    default void dropPodLanded(DropPod pod) {
    }

    default void orbitalStrike(float x, float y, float radius) {
    }

    default void gunshipBomb(float x, float y, float radius) {
    }

    default void waveStarted(int wave, int total, WaveDefinition definition) {
    }

    default void waveCompleted(int wave, int reward, boolean flawless) {
    }

    default void earlyCall(int bonus) {
    }

    default void requisitionGained(int amount, float x, float y) {
    }

    default void baseDamaged(int damage, int hpLeft) {
    }

    default void bossSpawned(Enemy boss) {
    }

    default void bossPhase(Enemy boss, int phase, String announcement) {
    }

    default void bossAbility(Enemy boss, BossDefinition.AbilityType type, float x, float y, float radius) {
    }

    default void synapseCollapse(float x, float y, float radius) {
    }

    default void victory() {
    }

    default void defeat() {
    }
}
