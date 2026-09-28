package com.warhammer.towerdefense.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.towers.TowerBehavior;
import com.warhammer.towerdefense.towers.TowerStats;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.towers.TowerUpgrade;
import com.warhammer.towerdefense.towers.Specialization;
import com.warhammer.towerdefense.waves.EnemySpawnDefinition;
import com.warhammer.towerdefense.waves.WaveDefinition;
import com.warhammer.towerdefense.waves.WaveSetDefinition;

/**
 * All game content, loaded from {@code assets/data}. Cross references (waves -> enemies, towers -> units, maps ->
 * wave sets...) are validated at load time so a typo in a JSON file produces a clear error instead of a crash
 * in the middle of a battle.
 */
public final class GameData {
    public static final String ENEMIES = "data/enemies.json";
    public static final String UNITS = "data/units.json";
    public static final String TOWERS = "data/towers.json";
    public static final String HEROES = "data/heroes.json";
    public static final String ABILITIES = "data/abilities.json";
    public static final String WAVES = "data/waves.json";
    public static final String DIFFICULTY = "data/difficulty.json";
    public static final String CAMPAIGN = "data/campaign.json";
    public static final String ARMOURY = "data/armoury.json";
    public static final String MAPS_DIR = "data/maps/";

    public final OrderedMap<String, EnemyDefinition> enemies = new OrderedMap<>();
    public final OrderedMap<String, UnitDefinition> units = new OrderedMap<>();
    public final Array<TowerType> towers = new Array<>();
    public final OrderedMap<String, HeroDefinition> heroes = new OrderedMap<>();
    public final Array<AbilityDefinition> globalAbilities = new Array<>();
    public final OrderedMap<String, WaveSetDefinition> waveSets = new OrderedMap<>();
    public final Array<MapDefinition> maps = new Array<>();
    public final ObjectMap<Difficulty, DifficultyDefinition> difficulties = new ObjectMap<>();
    public final Array<ArmouryUpgrade> armoury = new Array<>();
    public String defaultHero;

    public static GameData load(DataSource source) {
        GameData d = new GameData();
        for (JsonValue v = JsonUtil.parse(source, ENEMIES).child; v != null; v = v.next) {
            d.enemies.put(v.name, EnemyDefinition.parse(v.name, v));
        }
        for (JsonValue v = JsonUtil.parse(source, UNITS).child; v != null; v = v.next) {
            d.units.put(v.name, UnitDefinition.parse(v.name, v));
        }
        JsonValue towers = JsonUtil.requireChild(JsonUtil.parse(source, TOWERS), "towers", TOWERS);
        int i = 0;
        for (JsonValue v = towers.child; v != null; v = v.next, i++) {
            d.towers.add(TowerType.parse(v, i));
        }
        for (JsonValue v = JsonUtil.parse(source, HEROES).child; v != null; v = v.next) {
            d.heroes.put(v.name, HeroDefinition.parse(v.name, v));
        }
        JsonValue abilities = JsonUtil.requireChild(JsonUtil.parse(source, ABILITIES), "abilities", ABILITIES);
        for (JsonValue v = abilities.child; v != null; v = v.next) {
            d.globalAbilities.add(AbilityDefinition.parse(JsonUtil.requireString(v, "id", ABILITIES), v, ABILITIES));
        }
        for (JsonValue v = JsonUtil.parse(source, WAVES).child; v != null; v = v.next) {
            d.waveSets.put(v.name, WaveSetDefinition.parse(v.name, v));
        }
        JsonValue difficulty = JsonUtil.parse(source, DIFFICULTY);
        for (Difficulty level : Difficulty.values()) {
            JsonValue v = difficulty.get(level.name());
            d.difficulties.put(level, v == null ? DifficultyDefinition.defaults(level) : DifficultyDefinition.parse(level, v));
        }
        JsonValue campaign = JsonUtil.parse(source, CAMPAIGN);
        d.defaultHero = campaign.getString("defaultHero", null);
        for (String mapId : JsonUtil.stringArray(campaign, "maps")) {
            String file = MAPS_DIR + mapId + ".json";
            d.maps.add(MapDefinition.parse(JsonUtil.parse(source, file), file));
        }
        if (source.exists(ARMOURY)) {
            JsonValue upgrades = JsonUtil.requireChild(JsonUtil.parse(source, ARMOURY), "upgrades", ARMOURY);
            for (JsonValue v = upgrades.child; v != null; v = v.next) {
                d.armoury.add(ArmouryUpgrade.parse(v, ARMOURY));
            }
        }
        d.validate();
        return d;
    }

    private void validate() {
        if (towers.isEmpty()) {
            throw new DataException(TOWERS + ": no towers defined");
        }
        for (WaveSetDefinition set : waveSets.values()) {
            for (WaveDefinition wave : set.waves) {
                for (EnemySpawnDefinition group : wave.groups) {
                    if (!enemies.containsKey(group.enemy)) {
                        throw new DataException(WAVES + " (" + set.id + "): wave " + wave.number
                                + " references unknown enemy '" + group.enemy + "'");
                    }
                }
            }
        }
        for (EnemyDefinition e : enemies.values()) {
            if (e.bossDefinition == null) {
                continue;
            }
            for (BossDefinition.Phase phase : e.bossDefinition.phases) {
                for (BossDefinition.Ability a : phase.abilities) {
                    if (a.enemy != null && !enemies.containsKey(a.enemy)) {
                        throw new DataException(ENEMIES + " (" + e.id + "): boss spawns unknown enemy '" + a.enemy + "'");
                    }
                }
            }
        }
        for (TowerType t : towers) {
            validateTower(t);
        }
        for (MapDefinition m : maps) {
            validateMap(m);
        }
        if (defaultHero != null && !heroes.containsKey(defaultHero)) {
            throw new DataException(CAMPAIGN + ": unknown defaultHero '" + defaultHero + "'");
        }
    }

    private void validateTower(TowerType t) {
        Array<TowerUpgrade> all = new Array<>(t.tiers);
        for (Specialization s : t.specializations) {
            all.add(s.unlock);
            all.addAll(s.ranks);
        }
        for (TowerUpgrade u : all) {
            String unit = u.properties.get(TowerStats.PROP_UNIT_TYPE);
            if (unit != null && !units.containsKey(unit)) {
                throw new DataException(TOWERS + " (" + t.id + "): unknown unitType '" + unit + "'");
            }
        }
        if (t.behavior == TowerBehavior.BARRACKS) {
            TowerStats base = new TowerStats();
            t.tiers.first().applyTo(base);
            if (base.unitType == null) {
                throw new DataException(TOWERS + " (" + t.id + "): barracks towers need a unitType on their first tier");
            }
        }
    }

    private void validateMap(MapDefinition m) {
        if (m.requires != null && map(m.requires) == null) {
            throw new DataException(MAPS_DIR + m.id + ".json: requires unknown map '" + m.requires + "'");
        }
        if (!m.available) {
            return;
        }
        WaveSetDefinition set = waveSets.get(m.waveSet);
        if (set == null) {
            throw new DataException(MAPS_DIR + m.id + ".json: unknown waveSet '" + m.waveSet + "'");
        }
        for (WaveDefinition wave : set.waves) {
            for (EnemySpawnDefinition group : wave.groups) {
                if (group.path >= m.paths.size) {
                    throw new DataException(WAVES + " (" + set.id + "): wave " + wave.number + " uses path "
                            + group.path + " but map '" + m.id + "' only has " + m.paths.size + " path(s)");
                }
            }
        }
    }

    public EnemyDefinition enemy(String id) {
        EnemyDefinition d = enemies.get(id);
        if (d == null) {
            throw new DataException("Unknown enemy '" + id + "'");
        }
        return d;
    }

    public UnitDefinition unit(String id) {
        UnitDefinition d = id == null ? null : units.get(id);
        if (d == null) {
            throw new DataException("Unknown unit '" + id + "' (check units.json)");
        }
        return d;
    }

    public HeroDefinition hero(String id) {
        HeroDefinition d = heroes.get(id);
        if (d == null) {
            throw new DataException("Unknown hero '" + id + "'");
        }
        return d;
    }

    public TowerType tower(String id) {
        for (int i = 0; i < towers.size; i++) {
            if (towers.get(i).id.equals(id)) {
                return towers.get(i);
            }
        }
        throw new DataException("Unknown tower '" + id + "'");
    }

    public WaveSetDefinition waveSet(String id) {
        WaveSetDefinition s = waveSets.get(id);
        if (s == null) {
            throw new DataException("Unknown wave set '" + id + "'");
        }
        return s;
    }

    /** @return the map or null. */
    public MapDefinition map(String id) {
        // index loop: this lookup is used while other code iterates over maps (LibGDX iterators cannot nest)
        for (int i = 0; i < maps.size; i++) {
            if (maps.get(i).id.equals(id)) {
                return maps.get(i);
            }
        }
        return null;
    }

    public DifficultyDefinition difficulty(Difficulty level) {
        return difficulties.get(level);
    }

    public ArmouryUpgrade armouryUpgrade(String id) {
        for (int i = 0; i < armoury.size; i++) {
            if (armoury.get(i).id.equals(id)) {
                return armoury.get(i);
            }
        }
        return null;
    }
}
