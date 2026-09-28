package com.warhammer.towerdefense;

/** Global tuning constants that are not per-content data (content lives in assets/data/*.json). */
public final class Constants {
    public static final String GAME_TITLE = "WARHAMMER 40,000";
    public static final String GAME_SUBTITLE = "DEFENDERS OF THE IMPERIUM";
    public static final String DISCLAIMER =
            "Unofficial fan-made project. Not affiliated with, endorsed or sponsored by Games Workshop.";
    public static final String VERSION = "0.1.0 MVP";

    // Virtual resolution: the battlefield and the UI are authored for 1280x720 and scaled to the window.
    public static final float WORLD_WIDTH = 1280f;
    public static final float WORLD_HEIGHT = 720f;
    public static final float UI_WIDTH = 1280f;
    public static final float UI_HEIGHT = 720f;

    // Simulation
    public static final float MAX_FRAME_DELTA = 0.05f;
    public static final float SIM_STEP = 1f / 60f;
    public static final float GRID_CELL = 64f;
    public static final float GRID_MARGIN = 192f;

    // Economy
    public static final float SELL_REFUND = 0.6f;
    public static final float REPAIR_COST_FACTOR = 0.35f;

    // Towers
    public static final float TOWER_BUILD_TIME = 0.7f;
    public static final float TOWER_MUZZLE_HEIGHT = 40f;
    public static final float TOWER_OFFLINE_TIME = 7f;
    public static final float TOWER_RECOVERED_INTEGRITY = 0.4f;
    public static final float TOWER_SELECT_RADIUS = 42f;
    public static final int MAX_SQUAD_SIZE = 4;

    // Units & enemies
    public static final float ENEMY_DEATH_TIME = 1.2f;
    public static final float SYNAPSE_COLLAPSE_STUN = 1.2f;
    public static final float UNIT_ARRIVE_DISTANCE = 3f;

    // Hero
    public static final float HERO_XP_SHARE_RADIUS = 240f;
    public static final float HERO_XP_SHARE = 0.35f;
    public static final int HERO_WAVE_XP = 20;

    // Camera
    public static final float MIN_ZOOM = 0.55f;
    public static final float MAX_ZOOM = 1f;

    private Constants() {
    }
}
