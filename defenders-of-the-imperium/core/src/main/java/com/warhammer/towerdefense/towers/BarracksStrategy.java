package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.UnitDefinition;
import com.warhammer.towerdefense.entities.SpaceMarine;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Assault Bastion: keeps a squad of Space Marines alive around a rally point on the road. Dead brothers are
 * replaced after the respawn time; upgrades refit the living squad immediately.
 */
public final class BarracksStrategy implements TowerAttackStrategy {
    /** Formation offsets around the rally point, one per squad slot. */
    private static final float[][] FORMATION = {{0f, 12f}, {-16f, -6f}, {16f, -6f}, {0f, -20f}};
    private static final float[] CHAPLAIN_OFFSET = {-26f, 8f};
    /** Unit archetype (units.json) used for the Chaplain Support specialisation. */
    private static final String CHAPLAIN_UNIT = "chaplain";
    private static final float CHAPLAIN_HP_BONUS = 1.4f;
    private static final float CHAPLAIN_STUN_CHANCE = 0.35f;

    private final SpaceMarine[] squad = new SpaceMarine[Constants.MAX_SQUAD_SIZE];
    private final float[] respawn = new float[Constants.MAX_SQUAD_SIZE];
    private SpaceMarine chaplain;
    private float chaplainRespawn;

    @Override
    public void update(Tower tower, GameWorld world, float dt) {
        if (tower.isBuilding()) {
            return;
        }
        int count = Math.min(Constants.MAX_SQUAD_SIZE, tower.stats.getInt(Stat.UNIT_COUNT));
        for (int i = 0; i < Constants.MAX_SQUAD_SIZE; i++) {
            SpaceMarine m = squad[i];
            if (i >= count) {
                if (m != null && m.alive) {
                    world.retireUnit(m);
                }
                squad[i] = null;
                continue;
            }
            if (m == null || !m.alive) {
                if (m != null) {
                    squad[i] = null;
                    respawn[i] = tower.stats.get(Stat.UNIT_RESPAWN);
                }
                respawn[i] -= dt;
                if (respawn[i] <= 0f && !tower.isOffline()) {
                    squad[i] = deploy(tower, world, i);
                }
            }
        }
        boolean wantsChaplain = tower.stats.get(Stat.CHAPLAIN) > 0f;
        if (!wantsChaplain) {
            if (chaplain != null && chaplain.alive) {
                world.retireUnit(chaplain);
            }
            chaplain = null;
        } else if (chaplain == null || !chaplain.alive) {
            if (chaplain != null) {
                chaplain = null;
                chaplainRespawn = tower.stats.get(Stat.UNIT_RESPAWN) * 1.5f;
            }
            chaplainRespawn -= dt;
            if (chaplainRespawn <= 0f && !tower.isOffline()) {
                chaplain = deployChaplain(tower, world);
            }
        }
    }

    private SpaceMarine deploy(Tower tower, GameWorld world, int index) {
        UnitDefinition def = world.data.unit(tower.stats.unitType);
        SpaceMarine m = new SpaceMarine();
        m.configure(def);
        m.owner = tower;
        m.squadIndex = index;
        refit(m, tower);
        m.spawn(tower.x + (index - 1) * 8f, tower.y - 14f);
        m.guardX = tower.rallyX + FORMATION[index][0];
        m.guardY = tower.rallyY + FORMATION[index][1];
        world.addUnit(m);
        return m;
    }

    private SpaceMarine deployChaplain(Tower tower, GameWorld world) {
        SpaceMarine c = new SpaceMarine();
        c.configure(world.data.unit(CHAPLAIN_UNIT));
        c.owner = tower;
        c.squadIndex = 3;
        refit(c, tower);
        c.maxHp *= CHAPLAIN_HP_BONUS;
        c.hp = c.maxHp;
        c.auraHealValue = tower.stats.get(Stat.CHAPLAIN_HEAL);
        c.auraDamageValue = tower.stats.get(Stat.CHAPLAIN_DAMAGE);
        float stun = tower.stats.get(Stat.CHAPLAIN_STUN);
        if (stun > 0f) {
            c.onHitStatus = StatusType.STUN;
            c.onHitChance = CHAPLAIN_STUN_CHANCE;
            c.onHitDuration = stun;
        }
        c.spawn(tower.x, tower.y - 14f);
        c.guardX = tower.rallyX + CHAPLAIN_OFFSET[0];
        c.guardY = tower.rallyY + CHAPLAIN_OFFSET[1];
        world.addUnit(c);
        return c;
    }

    /** Copies the tower's unit stats onto a marine, preserving its current health fraction. */
    private static void refit(SpaceMarine m, Tower tower) {
        float fraction = m.maxHp > 0f ? m.hp / m.maxHp : 1f;
        TowerStats s = tower.stats;
        m.maxHp = s.get(Stat.UNIT_HP) * tower.bonusUnitHpMult;
        m.hp = m.maxHp * fraction;
        m.armor = s.get(Stat.UNIT_ARMOR);
        m.damageMin = s.get(Stat.UNIT_DAMAGE_MIN);
        m.damageMax = s.get(Stat.UNIT_DAMAGE_MAX);
        m.attackCooldown = s.get(Stat.UNIT_COOLDOWN);
        m.regen = s.get(Stat.UNIT_REGEN);
        m.cleave = s.getInt(Stat.UNIT_CLEAVE);
        m.maxEngaged = Math.max(1, s.getInt(Stat.UNIT_MAX_ENGAGED));
        m.rangedDamage = s.get(Stat.UNIT_RANGED_DAMAGE);
        m.rangedRange = s.get(Stat.UNIT_RANGE);
        m.rangedCooldown = s.get(Stat.UNIT_RANGED_COOLDOWN);
        m.rangedWhileMelee = m.rangedDamage > 0f;
        m.engageRadius = Math.max(60f, s.get(Stat.RALLY_RANGE) * 0.45f);
        m.damageMult = tower.bonusDamageMult;
    }

    @Override
    public void onStatsChanged(Tower tower, GameWorld world) {
        String unitType = tower.stats.unitType;
        for (int i = 0; i < squad.length; i++) {
            SpaceMarine m = squad[i];
            if (m == null || !m.alive) {
                respawn[i] = 0f;
                continue;
            }
            if (m.def == null || !m.def.id.equals(unitType)) {
                // The squad is re-equipped (e.g. Terminators teleport in): swap the model in place.
                world.retireUnit(m);
                squad[i] = null;
                respawn[i] = 0f;
            } else {
                refit(m, tower);
            }
        }
        if (chaplain != null && chaplain.alive) {
            chaplain.auraHealValue = tower.stats.get(Stat.CHAPLAIN_HEAL);
            chaplain.auraDamageValue = tower.stats.get(Stat.CHAPLAIN_DAMAGE);
        }
        chaplainRespawn = 0f;
    }

    @Override
    public void onRallyPointChanged(Tower tower, GameWorld world) {
        for (int i = 0; i < squad.length; i++) {
            SpaceMarine m = squad[i];
            if (m != null && m.alive) {
                m.guardX = tower.rallyX + FORMATION[i][0];
                m.guardY = tower.rallyY + FORMATION[i][1];
                m.releaseAll();
            }
        }
        if (chaplain != null && chaplain.alive) {
            chaplain.guardX = tower.rallyX + CHAPLAIN_OFFSET[0];
            chaplain.guardY = tower.rallyY + CHAPLAIN_OFFSET[1];
            chaplain.releaseAll();
        }
    }

    @Override
    public void onRemoved(Tower tower, GameWorld world) {
        for (int i = 0; i < squad.length; i++) {
            if (squad[i] != null && squad[i].alive) {
                world.retireUnit(squad[i]);
            }
            squad[i] = null;
        }
        if (chaplain != null && chaplain.alive) {
            world.retireUnit(chaplain);
        }
        chaplain = null;
    }

    public int aliveCount() {
        int n = 0;
        for (SpaceMarine m : squad) {
            if (m != null && m.alive) {
                n++;
            }
        }
        return n;
    }
}
