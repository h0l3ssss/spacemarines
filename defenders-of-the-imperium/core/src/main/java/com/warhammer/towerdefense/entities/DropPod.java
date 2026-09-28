package com.warhammer.towerdefense.entities;

/** Orbital drop pod: falls for {@link #fallTime} seconds, explodes on impact and releases Space Marines. */
public final class DropPod extends Entity {
    public float fallTime;
    public float timer;
    public boolean landed;
    /** Seconds the opened pod stays on the ground after landing (visual). */
    public float remaining;
    public int marines;
    public float marineDuration;
    public float impactDamage;
    public float impactRadius;
    public float stun;
    public float marineHp;
    public float marineDamage;

    public float fallFraction() {
        return fallTime <= 0f ? 1f : Math.min(1f, timer / fallTime);
    }
}
