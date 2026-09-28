package com.warhammer.towerdefense.entities;

/** A lance strike from the battle barge in orbit: telegraphed for {@link #delay} seconds, then devastating. */
public final class OrbitalStrike extends Entity {
    public float delay;
    public float timer;
    public float radius;
    public float damage;
    public boolean fired;
    /** Seconds the impact glow remains visible after firing. */
    public float afterglow;

    public float chargeFraction() {
        return delay <= 0f ? 1f : Math.min(1f, timer / delay);
    }
}
