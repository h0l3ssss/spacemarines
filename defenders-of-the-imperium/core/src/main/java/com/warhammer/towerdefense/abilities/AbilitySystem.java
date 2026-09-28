package com.warhammer.towerdefense.abilities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.combat.DamageInfo;
import com.warhammer.towerdefense.combat.DamageType;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.data.UnitDefinition;
import com.warhammer.towerdefense.entities.DropPod;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.OrbitalStrike;
import com.warhammer.towerdefense.entities.SpaceMarine;
import com.warhammer.towerdefense.entities.Thunderhawk;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Global abilities (reinforcements and fire support): requisition cost, cooldowns, and the lifecycle of the drop
 * pods, lance strikes and gunships they create.
 */
public final class AbilitySystem {
    /** Unit archetype (units.json) deployed by drop pods. */
    public static final String DROP_UNIT = "tactical_marine";
    /** Visual flight altitude of the gunship above its ground line. */
    public static final float GUNSHIP_ALTITUDE = 90f;
    private static final float MAP_MARGIN = 24f;
    private static final float STRIKE_AFTERGLOW = 0.9f;
    private static final float BOMB_INTERVAL = 0.07f;
    private static final float[][] DROP_FORMATION = {{-20f, -8f}, {20f, -8f}, {0f, -26f}, {0f, 12f}};

    public static final class Slot {
        public final AbilityDefinition definition;
        final GlobalAbility ability;
        public float cooldown;

        Slot(AbilityDefinition definition, GlobalAbility ability) {
            this.definition = definition;
            this.ability = ability;
        }

        public float cooldownFraction() {
            return definition.cooldown <= 0f ? 0f : cooldown / definition.cooldown;
        }
    }

    public final Array<Slot> slots = new Array<>();
    public final Array<DropPod> dropPods = new Array<>();
    public final Array<OrbitalStrike> strikes = new Array<>();
    public final Array<Thunderhawk> gunships = new Array<>();

    private final GameWorld world;
    private final DamageInfo payload = new DamageInfo();
    private final Array<Enemy> candidates = new Array<>(false, 32);

    public AbilitySystem(GameWorld world, Array<AbilityDefinition> definitions) {
        this.world = world;
        for (AbilityDefinition def : definitions) {
            slots.add(new Slot(def, GlobalAbilities.create(def.id)));
        }
    }

    public boolean canCast(int index) {
        if (index < 0 || index >= slots.size) {
            return false;
        }
        Slot s = slots.get(index);
        return s.cooldown <= 0f && world.state.canAfford(s.definition.cost);
    }

    public boolean cast(int index, float x, float y) {
        if (!canCast(index)) {
            return false;
        }
        Slot s = slots.get(index);
        world.state.spend(s.definition.cost);
        s.cooldown = s.definition.cooldown * world.bonuses.abilityCooldownMult;
        float cx = MathUtils.clamp(x, MAP_MARGIN, Constants.WORLD_WIDTH - MAP_MARGIN);
        float cy = MathUtils.clamp(y, MAP_MARGIN, Constants.WORLD_HEIGHT - MAP_MARGIN);
        s.ability.cast(world, this, s.definition, cx, cy);
        world.events.globalAbilityCast(s.definition, cx, cy);
        return true;
    }

    public void update(float dt) {
        for (Slot s : slots) {
            if (s.cooldown > 0f) {
                s.cooldown = Math.max(0f, s.cooldown - dt);
            }
        }
        updateDropPods(dt);
        updateStrikes(dt);
        updateGunships(dt);
    }

    private void updateDropPods(float dt) {
        for (int i = dropPods.size - 1; i >= 0; i--) {
            DropPod pod = dropPods.get(i);
            pod.timer += dt;
            if (!pod.landed) {
                if (pod.timer >= pod.fallTime) {
                    land(pod);
                }
                continue;
            }
            pod.remaining -= dt;
            if (pod.remaining <= 0f) {
                pod.alive = false;
                dropPods.removeIndex(i);
            }
        }
    }

    private void land(DropPod pod) {
        pod.landed = true;
        pod.remaining = pod.marineDuration;
        payload.reset();
        payload.amount = pod.impactDamage;
        payload.type = DamageType.EXPLOSIVE;
        payload.splashRadius = pod.impactRadius;
        payload.hitsAir = false;
        payload.source = DamageInfo.Source.ABILITY;
        payload.withStatus(StatusType.STUN, 1f, pod.stun, 1f);
        world.damage.splash(pod.x, pod.y, payload, null);

        UnitDefinition def = world.data.unit(DROP_UNIT);
        int count = Math.min(pod.marines, DROP_FORMATION.length);
        for (int i = 0; i < count; i++) {
            SpaceMarine m = new SpaceMarine();
            m.configure(def);
            m.maxHp = pod.marineHp * world.bonuses.unitHpMult;
            m.armor = 0.35f;
            m.damageMin = pod.marineDamage * 0.8f;
            m.damageMax = pod.marineDamage * 1.2f;
            m.attackCooldown = 0.9f;
            m.rangedDamage = pod.marineDamage * 0.7f;
            m.rangedRange = 130f;
            m.rangedCooldown = 0.8f;
            m.regen = 6f;
            m.engageRadius = 90f;
            m.lifetime = pod.marineDuration;
            m.squadIndex = i;
            m.spawn(pod.x + DROP_FORMATION[i][0] * 0.3f, pod.y - 6f);
            m.guardX = pod.x + DROP_FORMATION[i][0];
            m.guardY = pod.y + DROP_FORMATION[i][1];
            world.addUnit(m);
        }
        world.events.dropPodLanded(pod);
    }

    private void updateStrikes(float dt) {
        for (int i = strikes.size - 1; i >= 0; i--) {
            OrbitalStrike s = strikes.get(i);
            s.timer += dt;
            if (!s.fired && s.timer >= s.delay) {
                payload.reset();
                payload.amount = s.damage;
                payload.type = DamageType.ENERGY;
                payload.armorPierce = 0.5f;
                payload.bossMult = 0.5f;
                payload.splashRadius = s.radius;
                payload.splashFalloff = 0.35f;
                payload.source = DamageInfo.Source.ABILITY;
                world.damage.splash(s.x, s.y, payload, null);
                s.fired = true;
                s.afterglow = STRIKE_AFTERGLOW;
                world.events.orbitalStrike(s.x, s.y, s.radius);
            }
            if (s.fired) {
                s.afterglow -= dt;
                if (s.afterglow <= 0f) {
                    s.alive = false;
                    strikes.removeIndex(i);
                }
            }
        }
    }

    private void updateGunships(float dt) {
        for (int i = gunships.size - 1; i >= 0; i--) {
            Thunderhawk t = gunships.get(i);
            t.x += t.vx * dt;
            if (t.x > -40f && t.x < Constants.WORLD_WIDTH + 40f) {
                t.fireTimer -= dt;
                if (t.fireTimer <= 0f) {
                    t.fireTimer = t.fireInterval;
                    strafe(t);
                }
            }
            if (!t.bombing && t.bombs > 0 && t.x >= t.bombX) {
                t.bombing = true;
                t.bombTimer = 0f;
            }
            if (t.bombing && t.bombs > 0) {
                t.bombTimer -= dt;
                if (t.bombTimer <= 0f) {
                    dropBomb(t);
                    t.bombs--;
                    t.bombTimer = BOMB_INTERVAL;
                }
            }
            if (t.x > t.exitX) {
                t.alive = false;
                gunships.removeIndex(i);
            }
        }
    }

    private void strafe(Thunderhawk t) {
        candidates.clear();
        world.targeting.collect(t.x + 90f, t.lineY, t.strafeWidth + 60f, true, true, candidates);
        if (candidates.isEmpty()) {
            return;
        }
        Enemy target = candidates.get(world.random.nextInt(candidates.size));
        payload.reset();
        payload.amount = t.strafeDamage;
        payload.type = DamageType.KINETIC;
        payload.armorPierce = 0.2f;
        payload.source = DamageInfo.Source.ABILITY;
        world.projectiles.fire(ProjectileType.HEAVY_BOLT, t.x + 40f, t.lineY + GUNSHIP_ALTITUDE, target, payload);
    }

    private void dropBomb(Thunderhawk t) {
        float bx = t.x;
        float by = t.lineY + (world.random.nextFloat() - 0.5f) * 50f;
        payload.reset();
        payload.amount = t.bombDamage;
        payload.type = DamageType.EXPLOSIVE;
        payload.splashRadius = t.bombRadius;
        payload.hitsAir = false;
        payload.source = DamageInfo.Source.ABILITY;
        payload.withStatus(StatusType.BURN, 1f, 3f, 12f);
        world.damage.splash(bx, by, payload, null);
        world.events.gunshipBomb(bx, by, t.bombRadius);
    }
}
