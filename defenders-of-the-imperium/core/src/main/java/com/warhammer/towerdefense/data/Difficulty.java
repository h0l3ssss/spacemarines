package com.warhammer.towerdefense.data;

/** Difficulty levels. Their numbers live in {@code assets/data/difficulty.json}. */
public enum Difficulty {
    CASUAL, VETERAN, HARD;

    public Difficulty next() {
        Difficulty[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public Difficulty previous() {
        Difficulty[] all = values();
        return all[(ordinal() + all.length - 1) % all.length];
    }
}
