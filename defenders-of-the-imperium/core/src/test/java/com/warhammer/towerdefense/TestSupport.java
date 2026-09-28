package com.warhammer.towerdefense;

import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.data.FileSystemDataSource;
import com.warhammer.towerdefense.data.GameData;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.world.GameWorld;
import com.warhammer.towerdefense.world.MatchSetup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Shared fixtures: the real game data (tests run with /assets as working directory) and world factories. */
public final class TestSupport {
    public static final String MAP = "agri_world_vigilus";
    private static GameData data;

    private TestSupport() {
    }

    public static Path assetsRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("data"))) {
            return cwd;
        }
        return cwd.resolve("../assets").normalize();
    }

    public static synchronized GameData data() {
        if (data == null) {
            data = GameData.load(new FileSystemDataSource(assetsRoot()));
        }
        return data;
    }

    public static GameWorld world(Difficulty difficulty, boolean withHero) {
        MatchSetup setup = new MatchSetup(data().map(MAP));
        setup.difficulty = difficulty;
        setup.heroId = withHero ? data().defaultHero : null;
        setup.seed = 42L;
        return new GameWorld(data(), setup);
    }

    public static GameWorld world() {
        return world(Difficulty.VETERAN, false);
    }

    /** Spawns an enemy at a given distance along path 0 with no lateral offset. */
    public static Enemy spawn(GameWorld world, String enemyId, float distance) {
        GamePath path = world.paths.first();
        Enemy e = world.spawnOnPath(world.data.enemy(enemyId), path, Math.max(0.01f, distance), 0f, 0, 1f);
        world.rebuildGrid();
        return e;
    }
}
