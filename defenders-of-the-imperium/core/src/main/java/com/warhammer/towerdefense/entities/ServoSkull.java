package com.warhammer.towerdefense.entities;

import com.warhammer.towerdefense.towers.Tower;

/** Servo-skull drone orbiting a Techmarine Fortification; zaps and marks enemies for the other towers. */
public final class ServoSkull extends Entity {
    public Tower owner;
    public float orbitAngle;
    public float cooldown;
    public float zapTimer;
    public float zapX;
    public float zapY;
}
