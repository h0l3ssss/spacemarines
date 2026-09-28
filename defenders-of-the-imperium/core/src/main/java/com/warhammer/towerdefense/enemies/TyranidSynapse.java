package com.warhammer.towerdefense.enemies;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * SYNAPTIC CONTROL: creatures with the SYNAPSE trait (Tyranid Warriors, the Hive Tyrant) drive nearby INSTINCTIVE
 * gaunts to move and strike harder. Killing a synapse creature severs the link and briefly stuns the brood,
 * rewarding players who focus elites with lascannons.
 */
public final class TyranidSynapse implements FactionMechanic {
    private final Array<Enemy> nearby = new Array<>(false, 64);

    @Override
    public void update(GameWorld world, float dt) {
        Array<Enemy> enemies = world.enemies;
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).synapseBuffed = false;
        }
        for (int i = 0; i < enemies.size; i++) {
            Enemy source = enemies.get(i);
            if (!source.isTargetable() || !source.def.has(EnemyTrait.SYNAPSE)) {
                continue;
            }
            nearby.clear();
            world.targeting.collect(source.x, source.y, source.def.synapseRadius, true, true, nearby);
            for (int j = 0; j < nearby.size; j++) {
                Enemy e = nearby.get(j);
                if (e != source && e.def.has(EnemyTrait.INSTINCTIVE)) {
                    e.synapseBuffed = true;
                }
            }
        }
    }

    @Override
    public void onEnemyKilled(GameWorld world, Enemy dead) {
        if (!dead.def.has(EnemyTrait.SYNAPSE)) {
            return;
        }
        nearby.clear();
        world.targeting.collect(dead.x, dead.y, dead.def.synapseRadius, true, true, nearby);
        for (int i = 0; i < nearby.size; i++) {
            Enemy e = nearby.get(i);
            if (e.def.has(EnemyTrait.INSTINCTIVE)) {
                e.applyStatus(StatusType.STUN, Constants.SYNAPSE_COLLAPSE_STUN, 1f);
                e.synapseBuffed = false;
            }
        }
        world.events.synapseCollapse(dead.x, dead.y, dead.def.synapseRadius);
    }
}
