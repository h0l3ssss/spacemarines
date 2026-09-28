package com.warhammer.towerdefense.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.GameState;
import com.warhammer.towerdefense.abilities.AbilitySystem;
import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.DamageSystem;
import com.warhammer.towerdefense.combat.ProjectileSystem;
import com.warhammer.towerdefense.combat.SpatialGrid;
import com.warhammer.towerdefense.combat.TargetingSystem;
import com.warhammer.towerdefense.data.DifficultyDefinition;
import com.warhammer.towerdefense.data.EnemyDefinition;
import com.warhammer.towerdefense.data.GameData;
import com.warhammer.towerdefense.enemies.BossController;
import com.warhammer.towerdefense.enemies.EnemySystem;
import com.warhammer.towerdefense.enemies.FactionMechanic;
import com.warhammer.towerdefense.enemies.TyranidSynapse;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.entities.ServoSkull;
import com.warhammer.towerdefense.entities.SpaceMarine;
import com.warhammer.towerdefense.heroes.Hero;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.map.GamePath;
import com.warhammer.towerdefense.map.MapDefinition;
import com.warhammer.towerdefense.map.PathDefinition;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerBehavior;
import com.warhammer.towerdefense.towers.TowerFactory;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.waves.SpawnManager;
import com.warhammer.towerdefense.waves.WaveDefinition;
import com.warhammer.towerdefense.waves.WaveManager;

/**
 * The battle simulation (model). It owns every entity and system and advances them in a fixed order each step.
 * It has no knowledge of rendering, input or audio: the presentation layer observes it through {@link GameEvents}
 * and drives it through the public player-action methods (build, upgrade, call wave...).
 */
public final class GameWorld implements SpawnManager.SpawnTarget, WaveManager.WaveListener {
    private static final float RALLY_PATH_TOLERANCE = 75f;

    public final GameData data;
    public final MapDefinition map;
    public final DifficultyDefinition difficulty;
    public final Bonuses bonuses;
    public final GameState state;
    public final RandomXS128 random;
    public final Array<GamePath> paths = new Array<>();

    public final Array<Tower> towers = new Array<>(false, 24);
    public final Array<Enemy> enemies = new Array<>(false, 512);
    public final Array<AlliedUnit> units = new Array<>(false, 64);
    public final Array<ServoSkull> skulls = new Array<>(false, 16);
    public final Array<FactionMechanic> mechanics = new Array<>();
    public final Hero hero;

    public final SpatialGrid grid;
    public final TargetingSystem targeting;
    public final DamageSystem damage;
    public final ProjectileSystem projectiles;
    public final EnemySystem enemySystem;
    public final SpawnManager spawner;
    public final WaveManager waves;
    public final AbilitySystem abilities;

    public GameEvents events = GameEvents.NONE;
    public float time;

    private final Pool<Enemy> enemyPool = new Pool<Enemy>(128, 4096) {
        @Override
        protected Enemy newObject() {
            return new Enemy();
        }
    };
    private final Vector2 tmp = new Vector2();

    public GameWorld(GameData data, MatchSetup setup) {
        this.data = data;
        this.map = setup.map;
        this.difficulty = data.difficulty(setup.difficulty);
        this.bonuses = setup.bonuses;
        this.random = new RandomXS128(setup.seed);
        int requisition = Math.round((map.startingRequisition + difficulty.startingRequisitionBonus)
                * bonuses.startingRequisitionMult);
        this.state = new GameState(requisition, map.baseHp + bonuses.baseHpBonus);
        for (PathDefinition p : map.paths) {
            paths.add(new GamePath(p));
        }
        grid = new SpatialGrid(-Constants.GRID_MARGIN, -Constants.GRID_MARGIN,
                Constants.WORLD_WIDTH + Constants.GRID_MARGIN * 2f, Constants.WORLD_HEIGHT + Constants.GRID_MARGIN * 2f,
                Constants.GRID_CELL);
        targeting = new TargetingSystem(grid);
        damage = new DamageSystem(this);
        projectiles = new ProjectileSystem(this);
        enemySystem = new EnemySystem(this);
        spawner = new SpawnManager(this);
        waves = new WaveManager(data.waveSet(map.waveSet), spawner, this);
        abilities = new AbilitySystem(this, data.globalAbilities);
        mechanics.add(new TyranidSynapse());
        if (setup.heroId != null) {
            hero = new Hero(data.hero(setup.heroId), setup.heroLevel, setup.heroXp);
            hero.spawn(map.heroStartX, map.heroStartY);
            hero.guardX = hero.x;
            hero.guardY = hero.y;
        } else {
            hero = null;
        }
    }

    // ------------------------------------------------------------------ simulation

    public void update(float dt) {
        if (state.isOver()) {
            enemySystem.updateDeaths(dt);
            projectiles.update(dt);
            return;
        }
        time += dt;
        state.elapsed += dt;
        waves.update(dt);
        enemySystem.update(dt);
        rebuildGrid();
        for (int i = 0; i < mechanics.size; i++) {
            mechanics.get(i).update(this, dt);
        }
        applyAuras();
        updateTowers(dt);
        updateUnits(dt);
        if (hero != null) {
            hero.update(this, dt);
        }
        abilities.update(dt);
        projectiles.update(dt);
        checkEndConditions();
    }

    /** Re-indexes living enemies in the spatial grid (done every step; public for tests and tools). */
    public void rebuildGrid() {
        grid.clear();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isTargetable()) {
                grid.insert(e);
            }
        }
    }

    private void applyAuras() {
        for (int i = 0; i < towers.size; i++) {
            towers.get(i).resetBuffs();
        }
        for (int i = 0; i < units.size; i++) {
            units.get(i).resetAuras();
        }
        if (hero != null) {
            hero.resetAuras();
        }
        for (int i = 0; i < towers.size; i++) {
            Tower t = towers.get(i);
            t.strategy.applyAura(t, this);
        }
        for (int i = 0; i < units.size; i++) {
            AlliedUnit u = units.get(i);
            if (u instanceof SpaceMarine) {
                ((SpaceMarine) u).applyAura(this);
            }
        }
    }

    private void updateTowers(float dt) {
        for (int i = 0; i < towers.size; i++) {
            Tower t = towers.get(i);
            if (t.buildTimer > 0f) {
                t.buildTimer = Math.max(0f, t.buildTimer - dt);
            }
            t.upgradeFlash = Math.max(0f, t.upgradeFlash - dt * 1.5f);
            if (t.rallyTimer > 0f) {
                t.rallyTimer -= dt;
            }
            if (t.offlineTimer > 0f) {
                t.offlineTimer -= dt;
                if (t.offlineTimer <= 0f) {
                    t.offlineTimer = 0f;
                    t.integrity = Math.max(t.integrity, t.maxIntegrity * Constants.TOWER_RECOVERED_INTEGRITY);
                    events.towerOnline(t);
                }
            }
            t.strategy.update(t, this, dt);
        }
    }

    private void updateUnits(float dt) {
        for (int i = units.size - 1; i >= 0; i--) {
            if (i >= units.size) {
                continue;
            }
            AlliedUnit u = units.get(i);
            u.update(this, dt);
            if (!u.alive) {
                units.removeIndex(i);
            }
        }
    }

    private void checkEndConditions() {
        if (state.baseHp <= 0 && !state.defeat) {
            state.baseHp = 0;
            state.defeat = true;
            events.defeat();
        } else if (!state.victory && !state.defeat && waves.allWavesCompleted()) {
            state.victory = true;
            events.victory();
        }
    }

    // ------------------------------------------------------------------ spawning, leaks, rewards

    @Override
    public void spawnEnemy(String enemyId, int pathIndex, int wave, float hpMult) {
        EnemyDefinition def = data.enemy(enemyId);
        GamePath path = paths.get(MathUtils.clamp(pathIndex, 0, paths.size - 1));
        float spread = def.flying ? 0.45f : 0.32f;
        float lateral = (random.nextFloat() * 2f - 1f) * path.width * spread;
        spawnOnPath(def, path, 0f, lateral, wave, hpMult);
    }

    @Override
    public int pathCount() {
        return paths.size;
    }

    public Enemy spawnOnPath(EnemyDefinition def, GamePath path, float distance, float lateral, int wave, float hpMult) {
        Enemy e = enemyPool.obtain();
        float hp = def.hp * hpMult * difficulty.enemyHp * (def.boss ? difficulty.bossHp : 1f);
        float speed = def.speed * difficulty.enemySpeed * (0.94f + random.nextFloat() * 0.12f);
        float armor = Math.min(0.9f, def.armor + difficulty.enemyArmorBonus);
        e.init(def, path, lateral, wave, hp, speed, difficulty.enemyDamage, armor);
        if (distance > 0f) {
            e.distance = distance;
            e.placeOnPath();
        }
        if (def.boss && def.bossDefinition != null) {
            e.boss = new BossController(e, def.bossDefinition, difficulty.bossCooldown);
        }
        enemies.add(e);
        waves.onEnemySpawned(wave);
        events.enemySpawned(e);
        if (def.boss) {
            events.bossSpawned(e);
        }
        return e;
    }

    public void removeEnemyAt(int index) {
        Enemy e = enemies.removeIndex(index);
        enemyPool.free(e);
    }

    public void leak(Enemy e) {
        e.state = EnemyState.LEAKED;
        if (e.blocker != null) {
            e.blocker.engaged.removeValue(e, true);
            e.blocker = null;
        }
        int dmg = Math.max(1, Math.round(e.def.leakDamage * difficulty.leakDamage));
        state.baseHp = Math.max(0, state.baseHp - dmg);
        state.leaks++;
        waves.onEnemyRemoved(e, true);
        events.enemyLeaked(e, dmg);
        events.baseDamaged(dmg, state.baseHp);
    }

    public int bounty(Enemy e) {
        return Math.round(e.def.bounty * difficulty.income * bonuses.incomeMult);
    }

    public void grantKillXp(Enemy e, boolean heroCredit) {
        if (hero == null) {
            return;
        }
        float xp = e.def.xp * bonuses.heroXpMult;
        if (heroCredit) {
            hero.addXp(Math.round(xp), this);
        } else if (hero.alive && hero.dst2(e.x, e.y) <= Constants.HERO_XP_SHARE_RADIUS * Constants.HERO_XP_SHARE_RADIUS) {
            hero.addXp(Math.round(xp * Constants.HERO_XP_SHARE), this);
        }
    }

    @Override
    public void onWaveStarted(int wave, int total, WaveDefinition definition) {
        events.waveStarted(wave, total, definition);
    }

    @Override
    public void onWaveCompleted(int wave, int reward, boolean flawless) {
        int amount = Math.round(reward * difficulty.income * bonuses.incomeMult);
        state.earn(amount);
        if (hero != null) {
            hero.addXp(Math.round(Constants.HERO_WAVE_XP * bonuses.heroXpMult), this);
        }
        events.waveCompleted(wave, amount, flawless);
    }

    // ------------------------------------------------------------------ units

    public void addUnit(AlliedUnit unit) {
        units.add(unit);
        events.unitSpawned(unit);
    }

    /** Removes a unit without a death (squad refit, expired reinforcement, sold tower). */
    public void retireUnit(AlliedUnit unit) {
        if (!unit.alive) {
            return;
        }
        unit.alive = false;
        unit.releaseAll();
        events.unitRetired(unit);
    }

    public void onUnitDied(AlliedUnit unit) {
        events.unitDied(unit);
    }

    public void damageUnitsInRadius(float x, float y, float radius, float amount) {
        damageUnitsInRadius(x, y, radius, amount, 0f);
    }

    public void damageUnitsInRadius(float x, float y, float radius, float amount, float armorIgnore) {
        float r2 = radius * radius;
        for (int i = 0; i < units.size; i++) {
            AlliedUnit u = units.get(i);
            if (u.alive && u.dst2(x, y) <= r2) {
                damage.damageUnit(u, amount, armorIgnore);
            }
        }
        if (hero != null && hero.alive && hero.dst2(x, y) <= r2) {
            damage.damageUnit(hero, amount, armorIgnore);
        }
    }

    public Tower nearestTower(float x, float y, float range, boolean onlineOnly) {
        Tower best = null;
        float bestD2 = range * range;
        for (int i = 0; i < towers.size; i++) {
            Tower t = towers.get(i);
            if (onlineOnly && (t.isOffline() || t.isBuilding())) {
                continue;
            }
            float d2 = t.dst2(x, y);
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = t;
            }
        }
        return best;
    }

    public AlliedUnit nearestUnit(float x, float y, float range) {
        AlliedUnit best = null;
        float bestD2 = range * range;
        for (int i = 0; i < units.size; i++) {
            AlliedUnit u = units.get(i);
            if (!u.alive || !u.canAttack) {
                continue;
            }
            float d2 = u.dst2(x, y);
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = u;
            }
        }
        if (hero != null && hero.alive && hero.dst2(x, y) <= bestD2) {
            best = hero;
        }
        return best;
    }

    // ------------------------------------------------------------------ player actions

    public Tower towerAt(BuildPoint slot) {
        for (int i = 0; i < towers.size; i++) {
            if (towers.get(i).slot == slot) {
                return towers.get(i);
            }
        }
        return null;
    }

    /** Build plot under a world point, or null. */
    public BuildPoint slotAt(float x, float y) {
        BuildPoint best = null;
        float bestD2 = Constants.TOWER_SELECT_RADIUS * Constants.TOWER_SELECT_RADIUS;
        for (BuildPoint p : map.buildPoints) {
            float dx = p.x - x;
            float dy = (p.y - y) * 1.3f;
            float d2 = dx * dx + dy * dy;
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = p;
            }
        }
        return best;
    }

    public int buildCost(TowerType type) {
        return Math.round(type.buildCost() * bonuses.upgradeCostMult);
    }

    public boolean canBuild(BuildPoint slot, TowerType type) {
        return slot != null && towerAt(slot) == null && state.canAfford(buildCost(type)) && !state.isOver();
    }

    public Tower build(BuildPoint slot, TowerType type) {
        if (!canBuild(slot, type)) {
            return null;
        }
        int cost = buildCost(type);
        state.spend(cost);
        Tower t = TowerFactory.create(type, slot);
        t.totalSpent = cost;
        t.bonusDamageMult = bonuses.towerDamageMult;
        t.bonusRangeMult = bonuses.towerRangeMult;
        t.bonusUnitHpMult = bonuses.unitHpMult;
        initRallyPoint(t);
        towers.add(t);
        events.towerBuilt(t);
        return t;
    }

    private void initRallyPoint(Tower t) {
        BuildPoint slot = t.slot;
        if (slot.hasRallyPoint()) {
            t.rallyX = slot.rallyX;
            t.rallyY = slot.rallyY;
            return;
        }
        GamePath nearest = nearestPath(slot.x, slot.y);
        nearest.pointAt(nearest.project(slot.x, slot.y), tmp);
        t.rallyX = tmp.x;
        t.rallyY = tmp.y;
    }

    public GamePath nearestPath(float x, float y) {
        GamePath best = paths.first();
        float bestDist = Float.MAX_VALUE;
        for (GamePath p : paths) {
            float d = p.distanceTo(x, y);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    public float distanceToNearestPath(float x, float y) {
        float best = Float.MAX_VALUE;
        for (GamePath p : paths) {
            best = Math.min(best, p.distanceTo(x, y));
        }
        return best;
    }

    public Array<UpgradeOption> upgradeOptions(Tower t, Array<UpgradeOption> out) {
        return t.upgradeOptions(out, bonuses.upgradeCostMult);
    }

    public boolean upgrade(Tower t, UpgradeOption option) {
        if (t.isBuilding() || state.isOver() || !state.spend(option.cost)) {
            return false;
        }
        t.applyUpgrade(option);
        t.buildTimer = Constants.TOWER_BUILD_TIME * 0.5f;
        t.strategy.onStatsChanged(t, this);
        events.towerUpgraded(t, option);
        return true;
    }

    public int sell(Tower t) {
        if (!towers.contains(t, true)) {
            return 0;
        }
        int refund = t.sellValue();
        state.refund(refund);
        t.strategy.onRemoved(t, this);
        t.alive = false;
        towers.removeValue(t, true);
        events.towerSold(t, refund);
        return refund;
    }

    public boolean repair(Tower t) {
        int cost = t.repairCost();
        if (!t.needsRepair() || !state.spend(cost)) {
            return false;
        }
        t.integrity = t.maxIntegrity;
        boolean wasOffline = t.isOffline();
        t.offlineTimer = 0f;
        if (wasOffline) {
            events.towerOnline(t);
        }
        return true;
    }

    /** Moves a barracks' rally point; it must be within the tower's rally range and close to a road. */
    public boolean setRallyPoint(Tower t, float x, float y) {
        if (t.type.behavior != TowerBehavior.BARRACKS) {
            return false;
        }
        float range = t.stats.get(Stat.RALLY_RANGE);
        if (t.dst2(x, y) > range * range || distanceToNearestPath(x, y) > RALLY_PATH_TOLERANCE) {
            return false;
        }
        t.rallyX = x;
        t.rallyY = y;
        t.strategy.onRallyPointChanged(t, this);
        return true;
    }

    public boolean isValidRallyPoint(Tower t, float x, float y) {
        float range = t.stats.get(Stat.RALLY_RANGE);
        return t.dst2(x, y) <= range * range && distanceToNearestPath(x, y) <= RALLY_PATH_TOLERANCE;
    }

    /** Starts the next wave; returns the early-call bonus granted, or -1 when not possible. */
    public int callNextWave() {
        if (state.isOver()) {
            return -1;
        }
        int bonus = waves.callNextWave();
        if (bonus > 0) {
            int amount = Math.round(bonus * bonuses.incomeMult);
            state.earn(amount);
            events.earlyCall(amount);
            return amount;
        }
        return bonus;
    }

    public boolean orderHero(float x, float y) {
        if (hero == null || !hero.alive || state.isOver()) {
            return false;
        }
        hero.orderMove(MathUtils.clamp(x, 10f, Constants.WORLD_WIDTH - 10f),
                MathUtils.clamp(y, 10f, Constants.WORLD_HEIGHT - 60f));
        return true;
    }

    public boolean castHeroAbility(int index) {
        return hero != null && !state.isOver() && hero.castAbility(index, this);
    }

    public boolean castGlobalAbility(int index, float x, float y) {
        return !state.isOver() && abilities.cast(index, x, y);
    }

    // ------------------------------------------------------------------ queries for UI / debug

    public int activeEnemyCount() {
        int n = 0;
        for (int i = 0; i < enemies.size; i++) {
            if (enemies.get(i).isTargetable()) {
                n++;
            }
        }
        return n;
    }

    /** The living boss, or null. */
    public Enemy boss() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.def.boss && e.isTargetable()) {
                return e;
            }
        }
        return null;
    }

    public void debugKillAll() {
        DamageInfo info = new DamageInfo();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isTargetable()) {
                damage.kill(e, info);
            }
        }
    }

    public void debugSpawn(String enemyId, int count) {
        for (int i = 0; i < count; i++) {
            spawnEnemy(enemyId, i % paths.size, 0, 1f);
        }
    }
}
