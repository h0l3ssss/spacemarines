package com.warhammer.towerdefense.save;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.warhammer.towerdefense.data.ArmouryUpgrade;
import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.data.GameData;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.world.Bonuses;

/** Campaign progress saved as JSON: unlocked maps, stars, Armoury upgrades, heroes, seen enemies. */
public class GameSave {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public ObjectMap<String, MapProgress> maps = new ObjectMap<>();
    public ObjectMap<String, HeroProgress> heroes = new ObjectMap<>();
    public ObjectMap<String, Integer> armoury = new ObjectMap<>();
    public Array<String> unlockedHeroes = new Array<>();
    public Array<String> unlockedChapters = new Array<>();
    public Array<String> seenEnemies = new Array<>();
    public String selectedHero;
    public String lastMap;
    public int totalKills;
    public int battlesFought;

    public MapProgress progress(String mapId) {
        MapProgress p = maps.get(mapId);
        if (p == null) {
            p = new MapProgress();
            maps.put(mapId, p);
        }
        return p;
    }

    public int stars(String mapId) {
        MapProgress p = maps.get(mapId);
        return p == null ? 0 : p.stars;
    }

    public boolean isCompleted(String mapId) {
        MapProgress p = maps.get(mapId);
        return p != null && p.completed;
    }

    public boolean isUnlocked(MapDefinition map) {
        return map.requires == null || isCompleted(map.requires);
    }

    public void recordVictory(String mapId, int stars, Difficulty difficulty) {
        MapProgress p = progress(mapId);
        p.completed = true;
        p.victories++;
        p.stars = Math.max(p.stars, stars);
        if (p.bestDifficulty == null || Difficulty.valueOf(p.bestDifficulty).ordinal() < difficulty.ordinal()) {
            p.bestDifficulty = difficulty.name();
        }
        battlesFought++;
        lastMap = mapId;
    }

    public void recordDefeat(String mapId) {
        progress(mapId).defeats++;
        battlesFought++;
        lastMap = mapId;
    }

    public HeroProgress hero(String heroId) {
        HeroProgress h = heroes.get(heroId);
        if (h == null) {
            h = new HeroProgress();
            heroes.put(heroId, h);
        }
        return h;
    }

    public int totalStars() {
        int total = 0;
        for (MapProgress p : maps.values()) {
            total += p.stars;
        }
        return total;
    }

    public int armouryRank(String upgradeId) {
        Integer rank = armoury.get(upgradeId);
        return rank == null ? 0 : rank;
    }

    public int starsSpent(GameData data) {
        int spent = 0;
        for (ArmouryUpgrade u : data.armoury) {
            spent += u.totalCost(armouryRank(u.id));
        }
        return spent;
    }

    public int starsAvailable(GameData data) {
        return Math.max(0, totalStars() - starsSpent(data));
    }

    /** Buys the next rank of an Armoury upgrade if affordable. */
    public boolean buyArmoury(GameData data, ArmouryUpgrade upgrade) {
        int rank = armouryRank(upgrade.id);
        if (rank >= upgrade.maxRank() || upgrade.costs.get(rank) > starsAvailable(data)) {
            return false;
        }
        armoury.put(upgrade.id, rank + 1);
        return true;
    }

    public void resetArmoury() {
        armoury.clear();
    }

    public Bonuses bonuses(GameData data) {
        Bonuses b = new Bonuses();
        for (ArmouryUpgrade u : data.armoury) {
            u.apply(b, armouryRank(u.id));
        }
        return b;
    }

    public boolean markEnemySeen(String enemyId) {
        if (seenEnemies.contains(enemyId, false)) {
            return false;
        }
        seenEnemies.add(enemyId);
        return true;
    }
}
