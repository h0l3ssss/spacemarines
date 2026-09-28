package com.warhammer.towerdefense.tools.art;

/**
 * Parameters of one animation frame. Rigs read these to move limbs, weapons and bodies, so a sprite sheet is just
 * the same drawing routine evaluated with different poses.
 */
public final class Pose {
    public enum Kind {
        IDLE, WALK, ATTACK, DEATH
    }

    public final Kind kind;
    /** Progress of the animation cycle in [0, 1). */
    public final float t;
    public final int frame;

    private Pose(Kind kind, float t, int frame) {
        this.kind = kind;
        this.t = t;
        this.frame = frame;
    }

    public static Pose of(Kind kind, int frame, int frames) {
        return new Pose(kind, frames <= 1 ? 0f : frame / (float) frames, frame);
    }

    public boolean walking() {
        return kind == Kind.WALK;
    }

    public boolean attacking() {
        return kind == Kind.ATTACK;
    }

    public boolean dying() {
        return kind == Kind.DEATH;
    }

    /** Leg swing in [-1, 1] for walk cycles. */
    public float stride() {
        return walking() ? (float) Math.sin(t * Math.PI * 2) : 0f;
    }

    /** Vertical bob (negative = up) for walk and idle breathing. */
    public float bob() {
        if (walking()) {
            return -(float) Math.abs(Math.sin(t * Math.PI * 2)) * 1.2f;
        }
        if (kind == Kind.IDLE) {
            return frame == 1 ? -0.5f : 0f;
        }
        return 0f;
    }

    /** Attack swing: 0 wind-up, rising to 1 at the strike (frame 2), back down afterwards. */
    public float swing() {
        if (!attacking()) {
            return 0f;
        }
        switch (frame) {
            case 0:
                return -0.6f;
            case 1:
                return 0.3f;
            case 2:
                return 1f;
            default:
                return 0.5f;
        }
    }

    /** Muzzle flash on the first two attack frames. */
    public boolean flash() {
        return attacking() && frame <= 1;
    }

    /** Weapon recoil distance (art units). */
    public float recoil() {
        if (!attacking()) {
            return 0f;
        }
        return frame == 0 ? 2.2f : frame == 1 ? 1.2f : 0.4f;
    }

    /** Death progress in [0, 1]: 0 = hit, 1 = on the ground. */
    public float death() {
        return dying() ? Math.min(1f, (frame + 1) / 3f) : 0f;
    }

    /** Wing beat for flyers in [-1, 1] (flaps in every animation). */
    public float wing() {
        return (float) Math.sin(t * Math.PI * 2 + (kind == Kind.IDLE ? 0.8 : 0));
    }
}
