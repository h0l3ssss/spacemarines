package com.warhammer.towerdefense.combat;

/** Movement model of a projectile. Each {@link ProjectileType} picks one. */
public enum ProjectileMotion {
    /** Fast shot that homes on its target (bolts, plasma, venom). */
    DIRECT,
    /** Lobbed shell that flies to a predicted ground point and explodes (missiles, bio-plasma). */
    BALLISTIC,
    /** Accelerating rocket that steers towards its target. */
    SEEKING,
    /** Instant hit; the projectile only lives briefly as a visual beam (lascannon, servo-skull zap). */
    BEAM
}
