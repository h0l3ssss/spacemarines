package com.warhammer.towerdefense.world;

import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.map.MapDefinition;

/** Everything needed to start a battle. */
public final class MatchSetup {
    public MapDefinition map;
    public Difficulty difficulty = Difficulty.VETERAN;
    /** Hero to deploy, or null for none. */
    public String heroId;
    public int heroLevel = 1;
    public int heroXp;
    public long seed = System.nanoTime();
    public final Bonuses bonuses = new Bonuses();

    public MatchSetup(MapDefinition map) {
        this.map = map;
    }
}
