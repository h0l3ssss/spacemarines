package com.warhammer.towerdefense.combat;

/**
 * Every projectile in the game. The type decides motion, speed and the sprite used by the renderer; damage comes from
 * the weapon that fired it. This replaces a class hierarchy (BoltProjectile, PlasmaProjectile, ...) with data, which
 * keeps projectiles poolable in a single {@code Pool<Projectile>}.
 */
public enum ProjectileType {
    BOLT(ProjectileMotion.DIRECT, 950f, 7f, "proj_bolt", 0f),
    PISTOL_BOLT(ProjectileMotion.DIRECT, 900f, 7f, "proj_bolt", 0f),
    STORM_BOLT(ProjectileMotion.DIRECT, 950f, 7f, "proj_bolt", 0f),
    HEAVY_BOLT(ProjectileMotion.DIRECT, 880f, 9f, "proj_heavybolt", 0f),
    PLASMA(ProjectileMotion.DIRECT, 540f, 11f, "proj_plasma", 0f),
    MISSILE(ProjectileMotion.BALLISTIC, 330f, 0f, "proj_missile", 0f),
    ROCKET(ProjectileMotion.SEEKING, 240f, 12f, "proj_rocket", 0f),
    LASER(ProjectileMotion.BEAM, 0f, 0f, "beam_laser", 0.22f),
    SKULL_ZAP(ProjectileMotion.BEAM, 0f, 0f, "beam_zap", 0.12f),
    VENOM(ProjectileMotion.DIRECT, 420f, 10f, "proj_venom", 0f),
    BIO_PLASMA(ProjectileMotion.BALLISTIC, 300f, 0f, "proj_bioplasma", 0f);

    public final ProjectileMotion motion;
    /** World units per second (for BALLISTIC it defines the flight time: distance / speed). */
    public final float speed;
    public final float hitRadius;
    public final String sprite;
    /** Visual lifetime of beam projectiles. */
    public final float beamDuration;

    ProjectileType(ProjectileMotion motion, float speed, float hitRadius, String sprite, float beamDuration) {
        this.motion = motion;
        this.speed = speed;
        this.hitRadius = hitRadius;
        this.sprite = sprite;
        this.beamDuration = beamDuration;
    }
}
