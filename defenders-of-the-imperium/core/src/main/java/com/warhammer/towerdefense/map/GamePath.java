package com.warhammer.towerdefense.map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

/**
 * Runtime representation of a route: the control points are smoothed with a centripetal Catmull-Rom spline and
 * resampled into a dense polyline with cumulative distances. Enemies move by distance, so movement speed is exact
 * regardless of curvature, and lookups are O(1) amortised thanks to a per-walker segment hint.
 */
public final class GamePath {
    private static final float SAMPLE_SPACING = 6f;

    public final int id;
    public final float width;
    private final float[] xs;
    private final float[] ys;
    private final float[] cumulative;
    private final int count;
    private final float length;

    public GamePath(PathDefinition def) {
        this(def.id, def.points, def.smooth, def.width);
    }

    public GamePath(int id, Array<Vector2> controlPoints, boolean smooth, float width) {
        if (controlPoints.size < 2) {
            throw new IllegalArgumentException("A path needs at least two points");
        }
        this.id = id;
        this.width = width;
        FloatArray px = new FloatArray();
        FloatArray py = new FloatArray();
        if (smooth && controlPoints.size >= 3) {
            sampleSpline(controlPoints, px, py);
        } else {
            sampleLinear(controlPoints, px, py);
        }
        // Remove duplicated points so every segment has a positive length.
        FloatArray cx = new FloatArray();
        FloatArray cy = new FloatArray();
        cx.add(px.get(0));
        cy.add(py.get(0));
        for (int i = 1; i < px.size; i++) {
            float dx = px.get(i) - cx.peek();
            float dy = py.get(i) - cy.peek();
            if (dx * dx + dy * dy > 0.01f) {
                cx.add(px.get(i));
                cy.add(py.get(i));
            }
        }
        count = cx.size;
        xs = cx.toArray();
        ys = cy.toArray();
        cumulative = new float[count];
        float total = 0f;
        for (int i = 1; i < count; i++) {
            total += (float) Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
            cumulative[i] = total;
        }
        length = total;
    }

    public float length() {
        return length;
    }

    public int pointCount() {
        return count;
    }

    public float pointX(int index) {
        return xs[index];
    }

    public float pointY(int index) {
        return ys[index];
    }

    /**
     * Samples position and heading at {@code distance}. {@code hint} is the segment index returned by a previous
     * call (0 if unknown); walkers moving forward get constant time lookups.
     *
     * @return the segment index to pass as hint next time
     */
    public int sample(float distance, int hint, Vector2 outPos, Vector2 outDir) {
        if (distance <= 0f) {
            outPos.set(xs[0], ys[0]);
            setDirection(0, outDir);
            return 0;
        }
        if (distance >= length) {
            outPos.set(xs[count - 1], ys[count - 1]);
            setDirection(count - 2, outDir);
            return count - 2;
        }
        int i = hint;
        if (i < 0 || i >= count - 1 || cumulative[i] > distance) {
            i = binarySearch(distance);
        } else {
            while (i < count - 2 && cumulative[i + 1] < distance) {
                i++;
            }
        }
        float segLen = cumulative[i + 1] - cumulative[i];
        float t = segLen <= 0f ? 0f : (distance - cumulative[i]) / segLen;
        outPos.set(xs[i] + (xs[i + 1] - xs[i]) * t, ys[i] + (ys[i + 1] - ys[i]) * t);
        setDirection(i, outDir);
        return i;
    }

    public void pointAt(float distance, Vector2 out) {
        sample(distance, 0, out, TMP_DIR.get());
    }

    /** Distance along the path of the point closest to (x, y). */
    public float project(float x, float y) {
        float bestDist2 = Float.MAX_VALUE;
        float bestAlong = 0f;
        for (int i = 0; i < count - 1; i++) {
            float ax = xs[i];
            float ay = ys[i];
            float dx = xs[i + 1] - ax;
            float dy = ys[i + 1] - ay;
            float len2 = dx * dx + dy * dy;
            float t = len2 <= 0f ? 0f : ((x - ax) * dx + (y - ay) * dy) / len2;
            t = Math.max(0f, Math.min(1f, t));
            float cx = ax + dx * t;
            float cy = ay + dy * t;
            float d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                bestAlong = cumulative[i] + (float) Math.sqrt(len2) * t;
            }
        }
        return bestAlong;
    }

    /** Shortest distance from (x, y) to the path centre line. */
    public float distanceTo(float x, float y) {
        Vector2 p = TMP_POS.get();
        pointAt(project(x, y), p);
        return (float) Math.hypot(p.x - x, p.y - y);
    }

    private void setDirection(int segment, Vector2 outDir) {
        int i = Math.max(0, Math.min(count - 2, segment));
        float dx = xs[i + 1] - xs[i];
        float dy = ys[i + 1] - ys[i];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0f) {
            outDir.set(1f, 0f);
        } else {
            outDir.set(dx / len, dy / len);
        }
    }

    private int binarySearch(float distance) {
        int lo = 0;
        int hi = count - 2;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (cumulative[mid] <= distance) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    private static void sampleLinear(Array<Vector2> pts, FloatArray outX, FloatArray outY) {
        for (int i = 0; i < pts.size - 1; i++) {
            Vector2 a = pts.get(i);
            Vector2 b = pts.get(i + 1);
            int steps = Math.max(1, (int) Math.ceil(a.dst(b) / SAMPLE_SPACING));
            for (int s = 0; s < steps; s++) {
                float t = s / (float) steps;
                outX.add(a.x + (b.x - a.x) * t);
                outY.add(a.y + (b.y - a.y) * t);
            }
        }
        outX.add(pts.peek().x);
        outY.add(pts.peek().y);
    }

    private static void sampleSpline(Array<Vector2> pts, FloatArray outX, FloatArray outY) {
        int n = pts.size;
        Vector2 out = new Vector2();
        for (int i = 0; i < n - 1; i++) {
            Vector2 p1 = pts.get(i);
            Vector2 p2 = pts.get(i + 1);
            Vector2 p0 = i > 0 ? pts.get(i - 1) : new Vector2(2 * p1.x - p2.x, 2 * p1.y - p2.y);
            Vector2 p3 = i + 2 < n ? pts.get(i + 2) : new Vector2(2 * p2.x - p1.x, 2 * p2.y - p1.y);
            int steps = Math.max(2, (int) Math.ceil(p1.dst(p2) / SAMPLE_SPACING));
            for (int s = 0; s < steps; s++) {
                centripetal(p0, p1, p2, p3, s / (float) steps, out);
                outX.add(out.x);
                outY.add(out.y);
            }
        }
        outX.add(pts.peek().x);
        outY.add(pts.peek().y);
    }

    /** Barry-Goldman pyramidal evaluation of a centripetal Catmull-Rom segment between p1 and p2. */
    static void centripetal(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, float u, Vector2 out) {
        float t0 = 0f;
        float t1 = t0 + knot(p0, p1);
        float t2 = t1 + knot(p1, p2);
        float t3 = t2 + knot(p2, p3);
        float t = t1 + (t2 - t1) * u;

        float a1x = lerp(p0.x, p1.x, t0, t1, t);
        float a1y = lerp(p0.y, p1.y, t0, t1, t);
        float a2x = lerp(p1.x, p2.x, t1, t2, t);
        float a2y = lerp(p1.y, p2.y, t1, t2, t);
        float a3x = lerp(p2.x, p3.x, t2, t3, t);
        float a3y = lerp(p2.y, p3.y, t2, t3, t);

        float b1x = lerp(a1x, a2x, t0, t2, t);
        float b1y = lerp(a1y, a2y, t0, t2, t);
        float b2x = lerp(a2x, a3x, t1, t3, t);
        float b2y = lerp(a2y, a3y, t1, t3, t);

        out.set(lerp(b1x, b2x, t1, t2, t), lerp(b1y, b2y, t1, t2, t));
    }

    private static float lerp(float a, float b, float ta, float tb, float t) {
        float span = tb - ta;
        if (span <= 1e-6f) {
            return a;
        }
        return (tb - t) / span * a + (t - ta) / span * b;
    }

    private static float knot(Vector2 a, Vector2 b) {
        return Math.max((float) Math.sqrt(a.dst(b)), 1e-3f);
    }

    private static final ThreadLocal<Vector2> TMP_POS = ThreadLocal.withInitial(Vector2::new);
    private static final ThreadLocal<Vector2> TMP_DIR = ThreadLocal.withInitial(Vector2::new);
}
