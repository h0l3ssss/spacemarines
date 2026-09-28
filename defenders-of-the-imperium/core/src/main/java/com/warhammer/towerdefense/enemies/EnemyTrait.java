package com.warhammer.towerdefense.enemies;

/** Behaviour flags attached to enemy definitions in {@code enemies.json}. */
public enum EnemyTrait {
    /** Projects the Hive Mind's will: nearby INSTINCTIVE creatures move and strike harder. */
    SYNAPSE,
    /** Benefits from synapse, and is briefly stunned when the synapse creature controlling it dies. */
    INSTINCTIVE,
    /** Slowly regenerates hit points. */
    REGENERATE,
    /** Melee attacks ignore half of the defender's armour. */
    RENDING,
    /** Melee attacks hit every defender engaging it. */
    CLEAVE,
    /** The first strike after being engaged is a pouncing leap that deals double damage. */
    LEAPER,
    /** Ranged attacks target the player's towers (suppressing them). */
    TOWER_HUNTER
}
