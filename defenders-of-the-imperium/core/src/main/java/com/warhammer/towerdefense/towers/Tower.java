package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.entities.Entity;
import com.warhammer.towerdefense.map.BuildPoint;

/**
 * A deployed tower. Holds its progression (tier, specialisation, rank), resolved {@link TowerStats}, runtime
 * buffs and a small amount of presentation state (build/upgrade/fire timers) that the renderer reads.
 */
public final class Tower extends Entity {
    public final TowerType type;
    public final BuildPoint slot;
    public final TowerStats stats = new TowerStats();
    public final TowerAttackStrategy strategy;

    public int tier = 1;
    public int specIndex = -1;
    /** 0 = no specialisation, 1 = specialisation unlocked, 2.. = extra ranks. */
    public int specRank;
    public TowerTargeting targeting = TowerTargeting.FIRST;
    public int totalSpent;

    public float cooldown;
    public float integrity;
    public float maxIntegrity;
    public float offlineTimer;

    // Aura buffs from Techmarine towers (reset every frame before auras are applied).
    public float buffRange;
    public float buffFireRate;
    public float buffDamage;
    public float buffArmor;
    // Temporary buff from the hero's "Rally the Brothers".
    public float rallyBuff;
    public float rallyTimer;
    /** Permanent multiplier from Armoury upgrades. */
    public float bonusDamageMult = 1f;
    public float bonusRangeMult = 1f;
    public float bonusUnitHpMult = 1f;

    public float rallyX;
    public float rallyY;

    // Presentation state.
    public float buildTimer = Constants.TOWER_BUILD_TIME;
    public float upgradeFlash;
    public float fireAnim;
    public float aimX;
    public float aimY;
    public int shotIndex;
    public boolean overheated;
    public float heat;

    public int kills;
    public float damageDealt;

    public Tower(TowerType type, BuildPoint slot, TowerAttackStrategy strategy) {
        this.type = type;
        this.slot = slot;
        this.strategy = strategy;
        this.x = slot.x;
        this.y = slot.y;
        this.alive = true;
        assignSerial();
        recomputeStats();
        integrity = maxIntegrity;
        aimX = x - 1f;
        aimY = y;
    }

    public void recomputeStats() {
        stats.reset();
        for (int i = 0; i < tier && i < type.tiers.size; i++) {
            type.tiers.get(i).applyTo(stats);
        }
        Specialization spec = specialization();
        if (spec != null) {
            spec.unlock.applyTo(stats);
            for (int r = 0; r < specRank - 1 && r < spec.ranks.size; r++) {
                spec.ranks.get(r).applyTo(stats);
            }
        }
        float oldMax = maxIntegrity;
        maxIntegrity = stats.get(Stat.INTEGRITY);
        if (oldMax > 0f) {
            integrity = Math.min(maxIntegrity, integrity + (maxIntegrity - oldMax));
        }
    }

    public Specialization specialization() {
        return specIndex >= 0 && specIndex < type.specializations.size ? type.specializations.get(specIndex) : null;
    }

    public String displayName() {
        Specialization spec = specialization();
        if (spec != null) {
            return spec.name;
        }
        return type.tiers.get(tier - 1).name;
    }

    /** Level shown to the player: tiers 1..3 then specialisation ranks 4..6. */
    public int level() {
        return tier + specRank;
    }

    public String sprite() {
        return stats.sprite != null ? stats.sprite : "tower_" + type.id + "_" + tier;
    }

    public float effectiveRange() {
        return stats.get(Stat.RANGE) * bonusRangeMult * (1f + buffRange);
    }

    public float effectiveCooldown() {
        return stats.get(Stat.COOLDOWN) / (1f + buffFireRate);
    }

    public float damageMultiplier() {
        return bonusDamageMult * (1f + buffDamage + (rallyTimer > 0f ? rallyBuff : 0f));
    }

    public boolean isOffline() {
        return offlineTimer > 0f;
    }

    public boolean isBuilding() {
        return buildTimer > 0f;
    }

    public boolean canAct() {
        return !isOffline() && !isBuilding();
    }

    public float muzzleX() {
        return x;
    }

    public float muzzleY() {
        return y + Constants.TOWER_MUZZLE_HEIGHT;
    }

    public void resetBuffs() {
        buffRange = 0f;
        buffFireRate = 0f;
        buffDamage = 0f;
        buffArmor = 0f;
    }

    /** Fills {@code out} with this tower's ranged damage payload (randomised later per shot). */
    public DamageInfo buildDamage(DamageInfo out, float roll) {
        out.reset();
        float min = stats.get(Stat.DAMAGE_MIN);
        float max = stats.get(Stat.DAMAGE_MAX);
        out.amount = (min + (max - min) * roll) * damageMultiplier();
        out.type = stats.damageType;
        out.armorPierce = stats.get(Stat.ARMOR_PIERCE);
        out.eliteMult = stats.get(Stat.ELITE_DAMAGE);
        out.bossMult = stats.get(Stat.BOSS_DAMAGE);
        out.splashRadius = stats.get(Stat.SPLASH_RADIUS);
        out.splashFalloff = stats.get(Stat.SPLASH_FALLOFF);
        out.hitsAir = stats.canHitAir;
        out.hitsGround = true;
        out.source = DamageInfo.Source.TOWER;
        out.tower = this;
        if (stats.status != null && stats.get(Stat.STATUS_DURATION) > 0f) {
            out.withStatus(stats.status, stats.get(Stat.STATUS_CHANCE), stats.get(Stat.STATUS_DURATION),
                    stats.get(Stat.STATUS_POWER));
        }
        return out;
    }

    /** Collects the purchases currently available to this tower. */
    public Array<UpgradeOption> upgradeOptions(Array<UpgradeOption> out, float costMultiplier) {
        out.clear();
        if (specIndex < 0) {
            if (tier < type.maxTier()) {
                TowerUpgrade next = type.tiers.get(tier);
                out.add(new UpgradeOption(UpgradeOption.Kind.TIER, tier, next, cost(next, costMultiplier)));
            } else {
                for (int i = 0; i < type.specializations.size; i++) {
                    TowerUpgrade unlock = type.specializations.get(i).unlock;
                    out.add(new UpgradeOption(UpgradeOption.Kind.SPECIALIZATION, i, unlock, cost(unlock, costMultiplier)));
                }
            }
        } else {
            Specialization spec = specialization();
            int rankIndex = specRank - 1;
            if (spec != null && rankIndex < spec.ranks.size) {
                TowerUpgrade rank = spec.ranks.get(rankIndex);
                out.add(new UpgradeOption(UpgradeOption.Kind.RANK, rankIndex, rank, cost(rank, costMultiplier)));
            }
        }
        return out;
    }

    private static int cost(TowerUpgrade upgrade, float multiplier) {
        return Math.max(0, Math.round(upgrade.cost * multiplier));
    }

    /** Applies a purchase that has already been paid for. */
    public void applyUpgrade(UpgradeOption option) {
        switch (option.kind) {
            case TIER:
                tier = option.index + 1;
                break;
            case SPECIALIZATION:
                specIndex = option.index;
                specRank = 1;
                break;
            case RANK:
                specRank = option.index + 2;
                break;
            default:
                throw new IllegalStateException("Unknown upgrade kind " + option.kind);
        }
        totalSpent += option.cost;
        recomputeStats();
        upgradeFlash = 1f;
    }

    public boolean isMaxed() {
        if (specIndex < 0) {
            return tier >= type.maxTier() && type.specializations.size == 0;
        }
        Specialization spec = specialization();
        return spec == null || specRank >= spec.maxRank();
    }

    public int sellValue() {
        return Math.round(totalSpent * Constants.SELL_REFUND);
    }

    public int repairCost() {
        if (maxIntegrity <= 0f) {
            return 0;
        }
        float missing = 1f - integrity / maxIntegrity;
        if (isOffline()) {
            missing = Math.max(missing, 0.6f);
        }
        return Math.round(missing * totalSpent * Constants.REPAIR_COST_FACTOR);
    }

    public boolean needsRepair() {
        return isOffline() || integrity < maxIntegrity - 0.5f;
    }
}
