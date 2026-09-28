package com.warhammer.towerdefense.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.entities.Enemy;

/**
 * Uniform grid over the battlefield, rebuilt every simulation step. Range queries only visit the cells overlapping
 * the query circle, which keeps targeting and splash damage far from O(towers x enemies) with 300+ enemies.
 */
public final class SpatialGrid {
    private final float originX;
    private final float originY;
    private final float cellSize;
    private final int cols;
    private final int rows;
    private final Array<Enemy>[] cells;

    @SuppressWarnings("unchecked")
    public SpatialGrid(float originX, float originY, float width, float height, float cellSize) {
        this.originX = originX;
        this.originY = originY;
        this.cellSize = cellSize;
        this.cols = Math.max(1, (int) Math.ceil(width / cellSize));
        this.rows = Math.max(1, (int) Math.ceil(height / cellSize));
        this.cells = new Array[cols * rows];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Array<>(false, 16);
        }
    }

    public void clear() {
        for (Array<Enemy> cell : cells) {
            cell.clear();
        }
    }

    public void insert(Enemy e) {
        cells[row(e.y) * cols + col(e.x)].add(e);
    }

    /** Appends every enemy stored in cells overlapping the circle; callers must still check the exact distance. */
    public void query(float x, float y, float radius, Array<Enemy> out) {
        int c0 = col(x - radius);
        int c1 = col(x + radius);
        int r0 = row(y - radius);
        int r1 = row(y + radius);
        for (int r = r0; r <= r1; r++) {
            int base = r * cols;
            for (int c = c0; c <= c1; c++) {
                out.addAll(cells[base + c]);
            }
        }
    }

    private int col(float x) {
        return MathUtils.clamp((int) ((x - originX) / cellSize), 0, cols - 1);
    }

    private int row(float y) {
        return MathUtils.clamp((int) ((y - originY) / cellSize), 0, rows - 1);
    }
}
