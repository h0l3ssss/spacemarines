package com.warhammer.towerdefense.data;

/** Abstraction over where data files come from (LibGDX internal files in game, plain files in tests/tools). */
public interface DataSource {
    /** Reads a text file relative to the assets root, e.g. {@code data/towers.json}. */
    String read(String path);

    boolean exists(String path);
}
