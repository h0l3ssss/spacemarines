package com.warhammer.towerdefense.entities;

/** Thunderhawk gunship strafing run across the battlefield along a horizontal line. */
public final class Thunderhawk extends Entity {
    public float vx;
    public float lineY;
    public float fireTimer;
    public float fireInterval = 0.12f;
    public float strafeDamage;
    public float strafeWidth;
    public float bombX;
    public int bombs;
    public float bombDamage;
    public float bombRadius;
    public float bombTimer;
    public boolean bombing;
    public float exitX;
}
