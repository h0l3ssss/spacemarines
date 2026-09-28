package com.warhammer.towerdefense.entities;

public enum EnemyState {
    WALKING,
    /** Held in place by a defender (Space Marine, hero, barrier). */
    FIGHTING,
    /** Short pause while performing a ranged attack or boss ability. */
    CASTING,
    DYING,
    LEAKED
}
