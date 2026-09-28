package com.warhammer.towerdefense.map;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.warhammer.towerdefense.data.DataException;
import com.warhammer.towerdefense.data.JsonUtil;

/**
 * A battlefield loaded from {@code assets/data/maps/<id>.json}: paths, build plots, the fortress, props and the
 * campaign metadata (order, unlock requirement, intro transmission).
 */
public final class MapDefinition {
    public String id;
    public String name;
    public String subtitle = "";
    public String description = "";
    public String theme = "agri";
    public String waveSet;
    public String background;
    public String music = "BATTLE";
    public String planetSprite = "planet_agri";
    public int order;
    /** Map id that must be completed before this one unlocks (null = unlocked from the start). */
    public String requires;
    /** False for campaign entries that are announced but not playable yet. */
    public boolean available = true;
    public float width = 1280f;
    public float height = 720f;
    public int startingRequisition = 250;
    public int baseHp = 100;
    public float heroStartX = 200f;
    public float heroStartY = 360f;
    public BasePoint base;
    public final Array<PathDefinition> paths = new Array<>();
    public final Array<BuildPoint> buildPoints = new Array<>();
    public final Array<SpawnPoint> spawnPoints = new Array<>();
    public final Array<TerrainObject> terrain = new Array<>();
    public final Array<String> intro = new Array<>();

    public static MapDefinition parse(JsonValue v, String file) {
        MapDefinition m = new MapDefinition();
        m.id = JsonUtil.requireString(v, "id", file);
        String ctx = file + " (" + m.id + ")";
        m.name = JsonUtil.requireString(v, "name", ctx);
        m.subtitle = v.getString("subtitle", "");
        m.description = v.getString("description", "");
        m.theme = v.getString("theme", "agri");
        m.waveSet = v.getString("waveSet", null);
        m.background = v.getString("background", "maps/" + m.id + ".jpg");
        m.music = v.getString("music", "BATTLE");
        m.planetSprite = v.getString("planetSprite", "planet_agri");
        m.order = v.getInt("order", 0);
        m.requires = v.getString("requires", null);
        m.available = v.getBoolean("available", true);
        m.width = v.getFloat("width", 1280f);
        m.height = v.getFloat("height", 720f);
        m.startingRequisition = v.getInt("startingRequisition", 250);
        m.baseHp = v.getInt("baseHp", 100);
        m.intro.addAll(JsonUtil.stringArray(v, "intro"));

        if (!m.available) {
            return m; // campaign placeholder: only metadata is required
        }

        m.base = BasePoint.parse(JsonUtil.requireChild(v, "base", ctx), ctx + ".base");
        JsonValue hero = v.get("heroStart");
        if (hero != null) {
            m.heroStartX = JsonUtil.point(hero, ctx + ".heroStart").x;
            m.heroStartY = JsonUtil.point(hero, ctx + ".heroStart").y;
        }
        int i = 0;
        for (JsonValue p = JsonUtil.requireChild(v, "paths", ctx).child; p != null; p = p.next) {
            m.paths.add(PathDefinition.parse(p, i, ctx + ".paths[" + i + "]"));
            i++;
        }
        i = 0;
        for (JsonValue b = JsonUtil.requireChild(v, "buildPoints", ctx).child; b != null; b = b.next) {
            m.buildPoints.add(BuildPoint.parse(b, i, ctx + ".buildPoints[" + i + "]"));
            i++;
        }
        JsonValue spawns = v.get("spawns");
        if (spawns != null) {
            for (JsonValue s = spawns.child; s != null; s = s.next) {
                m.spawnPoints.add(SpawnPoint.parse(s, ctx + ".spawns"));
            }
        }
        JsonValue terrain = v.get("terrain");
        if (terrain != null) {
            for (JsonValue t = terrain.child; t != null; t = t.next) {
                m.terrain.add(TerrainObject.parse(t, ctx + ".terrain"));
            }
        }
        m.validate(ctx);
        return m;
    }

    private void validate(String ctx) {
        if (waveSet == null) {
            throw new DataException(ctx + ": playable maps need a 'waveSet'");
        }
        if (paths.isEmpty()) {
            throw new DataException(ctx + ": playable maps need at least one path");
        }
        if (buildPoints.isEmpty()) {
            throw new DataException(ctx + ": playable maps need build points");
        }
        for (SpawnPoint s : spawnPoints) {
            if (s.path < 0 || s.path >= paths.size) {
                throw new DataException(ctx + ": spawn references unknown path " + s.path);
            }
        }
    }
}
