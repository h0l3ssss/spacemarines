package com.warhammer.towerdefense.entities;

/**
 * Base of everything that lives on the battlefield. {@link #serial} changes whenever a pooled instance is reused,
 * so holders of a reference can detect that "their" enemy died and the object now represents another one.
 */
public abstract class Entity {
    private static int serialCounter;

    public float x;
    public float y;
    public boolean alive;
    public int serial;

    protected final void assignSerial() {
        serial = ++serialCounter;
    }

    public float dst2(float ox, float oy) {
        float dx = x - ox;
        float dy = y - oy;
        return dx * dx + dy * dy;
    }

    public float dst(float ox, float oy) {
        return (float) Math.sqrt(dst2(ox, oy));
    }

    public float dst(Entity other) {
        return dst(other.x, other.y);
    }
}
