package com.warhammer.towerdefense.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.abilities.AbilitySystem;
import com.warhammer.towerdefense.combat.ProjectileMotion;
import com.warhammer.towerdefense.combat.StatusType;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.Barrier;
import com.warhammer.towerdefense.entities.DropPod;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.entities.EnemyState;
import com.warhammer.towerdefense.entities.Entity;
import com.warhammer.towerdefense.entities.OrbitalStrike;
import com.warhammer.towerdefense.entities.Projectile;
import com.warhammer.towerdefense.entities.ServoSkull;
import com.warhammer.towerdefense.entities.Thunderhawk;
import com.warhammer.towerdefense.entities.UnitState;
import com.warhammer.towerdefense.heroes.Hero;
import com.warhammer.towerdefense.input.PlayerController;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.save.SettingsSave;
import com.warhammer.towerdefense.towers.Stat;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerBehavior;
import com.warhammer.towerdefense.towers.TowerStats;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.world.GameWorld;

import java.util.Comparator;

/**
 * Draws the battlefield (view only): background, decals, build plots, y-sorted towers/units/enemies, flyers,
 * projectiles, particles and world-space overlays (range circles, health bars, status icons, targeting).
 */
public final class WorldRenderer {
    private static final float[][] CREW_2 = {{-15f, 24f}, {15f, 24f}};
    private static final float[][] CREW_3 = {{-22f, 22f}, {0f, 30f}, {22f, 22f}};
    private static final float[][] CREW_1 = {{0f, 26f}};
    private static final Comparator<Entity> DEPTH = (a, b) -> Float.compare(b.y, a.y);
    private static final float DROP_HEIGHT = 420f;

    private final GameWorld world;
    private final Assets assets;
    private final Fx fx;
    private final PlayerController control;
    private final SettingsSave settings;
    private final Texture background;
    private final Array<Entity> sorted = new Array<>(false, 512);
    private final TowerStats preview = new TowerStats();
    private final Color tmp = new Color();

    public WorldRenderer(GameWorld world, Assets assets, Fx fx, PlayerController control, SettingsSave settings) {
        this.world = world;
        this.assets = assets;
        this.fx = fx;
        this.control = control;
        this.settings = settings;
        this.background = assets.texture(world.map.background);
    }

    public void render(SpriteBatch batch) {
        if (background != null) {
            batch.draw(background, 0, 0, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT);
        } else {
            batch.setColor(0.33f, 0.44f, 0.24f, 1f);
            batch.draw(assets.white, 0, 0, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        fx.drawDecals(batch);
        drawSprite(batch, assets.sprite(world.map.base.sprite), world.map.base.x, world.map.base.y - 60f, false, 1f);
        drawSlots(batch);
        drawRanges(batch);

        sorted.clear();
        sorted.addAll(world.towers);
        for (AlliedUnit u : world.units) {
            sorted.add(u);
        }
        if (world.hero != null && world.hero.alive) {
            sorted.add(world.hero);
        }
        for (Enemy e : world.enemies) {
            if (!e.flying) {
                sorted.add(e);
            }
        }
        for (DropPod pod : world.abilities.dropPods) {
            if (pod.landed) {
                sorted.add(pod);
            }
        }
        sorted.sort(DEPTH);
        drawShadows(batch);
        for (Entity e : sorted) {
            if (e instanceof Tower) {
                drawTower(batch, (Tower) e);
            } else if (e instanceof Enemy) {
                drawEnemy(batch, (Enemy) e);
            } else if (e instanceof AlliedUnit) {
                drawUnit(batch, (AlliedUnit) e);
            } else if (e instanceof DropPod) {
                drawSprite(batch, assets.sprite("droppod_open"), e.x, e.y, false, 1f);
            }
        }
        sorted.clear();
        for (Enemy e : world.enemies) {
            if (e.flying) {
                sorted.add(e);
            }
        }
        sorted.sort(DEPTH);
        for (Entity e : sorted) {
            drawEnemy(batch, (Enemy) e);
        }
        drawAbilityEntities(batch);
        drawProjectiles(batch);
        fx.drawParticles(batch);
        drawOverlays(batch);
        fx.drawTexts(batch);
    }

    // ------------------------------------------------------------------ helpers

    private void drawSprite(SpriteBatch batch, Assets.Sprite s, float x, float y, boolean flip, float extraScale) {
        drawRegion(batch, s.region, s.originX, s.originY, s.scale * extraScale, x, y, flip, 0f);
    }

    private static void drawRegion(SpriteBatch batch, TextureRegion r, float ox, float oy, float scale, float x, float y,
                                   boolean flip, float rotation) {
        float w = r.getRegionWidth() * scale;
        float h = r.getRegionHeight() * scale;
        batch.draw(r, x - ox * scale, y - oy * scale, ox * scale, oy * scale, w, h, flip ? -1f : 1f, 1f, rotation);
    }

    private void drawAnim(SpriteBatch batch, Assets.AnimSet set, String anim, float time, float x, float y, boolean flip,
                          float scale) {
        Animation<TextureRegion> a = set.get(anim);
        drawRegion(batch, a.getKeyFrame(time), set.originX, set.originY, set.scale * scale, x, y, flip, 0f);
    }

    private void circle(SpriteBatch batch, float x, float y, float radius, Color c, boolean fill) {
        Assets.Sprite ring = assets.sprite(fill ? "range_fill" : "range_ring");
        float size = radius * 2f * 256f / 252f;
        batch.setColor(c);
        batch.draw(ring.region, x - size / 2, y - size / 2, size, size);
        batch.setColor(Color.WHITE);
    }

    private void bar(SpriteBatch batch, float x, float y, float w, float fraction, Color fill) {
        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(assets.white, x - w / 2 - 1, y - 1, w + 2, 5);
        batch.setColor(0.25f, 0.05f, 0.05f, 1f);
        batch.draw(assets.white, x - w / 2, y, w, 3);
        batch.setColor(fill);
        batch.draw(assets.white, x - w / 2, y, w * Math.max(0f, fraction), 3);
        batch.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------ layers

    private void drawSlots(SpriteBatch batch) {
        Assets.Sprite slot = assets.sprite("tower_slot");
        BuildPoint hover = world.slotAt(control.mouseX, control.mouseY);
        for (BuildPoint p : world.map.buildPoints) {
            if (world.towerAt(p) != null) {
                continue;
            }
            boolean highlighted = p == control.selectedSlot || p == hover || control.placing != null;
            if (highlighted) {
                float pulse = 0.75f + 0.25f * MathUtils.sin(world.time * 6f);
                batch.setColor(1f, 0.9f * pulse + 0.1f, 0.5f, 1f);
            }
            drawSprite(batch, slot, p.x, p.y, false, 1f);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawRanges(SpriteBatch batch) {
        Tower t = control.selectedTower;
        if (t != null) {
            float range = t.type.behavior == TowerBehavior.BARRACKS ? t.stats.get(Stat.RALLY_RANGE) : t.effectiveRange();
            circle(batch, t.x, t.y, range, tmp.set(0.4f, 0.7f, 1f, 0.7f), true);
            circle(batch, t.x, t.y, range, tmp.set(0.6f, 0.85f, 1f, 0.9f), false);
            float aura = t.stats.get(Stat.AURA_RADIUS);
            if (aura > 0f) {
                circle(batch, t.x, t.y, aura, tmp.set(0.3f, 1f, 0.5f, 0.6f), false);
            }
            if (t.type.behavior == TowerBehavior.BARRACKS) {
                drawSprite(batch, assets.sprite("rally_flag"), t.rallyX, t.rallyY, false, 1f);
            }
        }
        TowerType type = control.placing;
        BuildPoint slot = control.selectedSlot;
        if (type != null) {
            slot = world.slotAt(control.mouseX, control.mouseY);
        }
        if (type != null && slot != null && world.towerAt(slot) == null) {
            preview.reset();
            type.tiers.first().applyTo(preview);
            float range = type.behavior == TowerBehavior.BARRACKS ? preview.get(Stat.RALLY_RANGE) : preview.get(Stat.RANGE);
            circle(batch, slot.x, slot.y, range, tmp.set(1f, 0.85f, 0.4f, 0.6f), true);
            circle(batch, slot.x, slot.y, range, tmp.set(1f, 0.85f, 0.4f, 0.9f), false);
        }
        if (control.rallyMode && t != null) {
            boolean ok = world.isValidRallyPoint(t, control.mouseX, control.mouseY);
            batch.setColor(ok ? Color.WHITE : Color.RED);
            drawSprite(batch, assets.sprite("rally_flag"), control.mouseX, control.mouseY, false, 1f);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawShadows(SpriteBatch batch) {
        Assets.Sprite shadow = assets.sprite("shadow");
        batch.setColor(1f, 1f, 1f, 0.8f);
        for (Entity e : sorted) {
            float size;
            if (e instanceof Enemy) {
                Enemy en = (Enemy) e;
                if (en.state == EnemyState.DYING) {
                    continue;
                }
                size = en.def.radius / 9f;
            } else if (e instanceof AlliedUnit && !(e instanceof Barrier)) {
                size = ((AlliedUnit) e).radius / 9f;
            } else {
                continue;
            }
            drawSprite(batch, shadow, e.x, e.y, false, size);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawTower(SpriteBatch batch, Tower t) {
        float build = t.isBuilding() ? 1f - t.buildTimer / Constants.TOWER_BUILD_TIME : 1f;
        float scale = 0.7f + 0.3f * build;
        if (t.isOffline()) {
            batch.setColor(0.55f, 0.65f, 0.5f, 1f);
        } else if (t.upgradeFlash > 0f) {
            float f = t.upgradeFlash;
            batch.setColor(1f, 1f, 1f - 0.3f * f, 1f);
        } else {
            batch.setColor(1f, 1f, 1f, 0.4f + 0.6f * build);
        }
        drawSprite(batch, assets.sprite(t.sprite()), t.x, t.y, false, scale);
        batch.setColor(Color.WHITE);
        if (t == control.selectedTower) {
            circle(batch, t.x, t.y, 44f, tmp.set(1f, 0.85f, 0.4f, 0.35f), false);
        }
        String crew = t.stats.crew;
        if (crew == null || build < 1f) {
            return;
        }
        int count = Math.max(1, t.stats.getInt(Stat.CREW_COUNT));
        float[][] spots = count >= 3 ? CREW_3 : count == 2 ? CREW_2 : CREW_1;
        Assets.AnimSet set = assets.anim(crew);
        boolean flip = t.aimX < t.x;
        for (int i = 0; i < spots.length; i++) {
            boolean firing = t.fireAnim > 0f && (spots.length == 1 || t.shotIndex % spots.length == i);
            String anim = firing ? "attack" : "idle";
            float time = firing ? (0.25f - t.fireAnim) : world.time + i * 0.37f;
            drawAnim(batch, set, anim, time, t.x + spots[i][0], t.y + spots[i][1], flip, 0.85f);
        }
    }

    private void drawUnit(SpriteBatch batch, AlliedUnit u) {
        if (u instanceof Barrier) {
            Barrier b = (Barrier) u;
            batch.setColor(1f, 1f, 1f, 0.5f + 0.5f * b.lifeFraction());
            drawSprite(batch, assets.sprite("barrier"), u.x, u.y, false, 1f);
            batch.setColor(Color.WHITE);
            return;
        }
        Assets.AnimSet set = assets.anim(u.sprite);
        String anim;
        float time = u.stateTime;
        if (u.attackAnim > 0f) {
            anim = "attack";
            time = 0.3f - u.attackAnim;
        } else if (u.state == UnitState.MOVING) {
            anim = "walk";
        } else {
            anim = "idle";
        }
        if (u instanceof Hero && ((Hero) u).isEmpowered()) {
            batch.setColor(1f, 0.95f, 0.7f, 1f);
            Assets.Sprite glow = assets.sprite("fx_soft");
            batch.setColor(1f, 0.85f, 0.3f, 0.6f);
            drawSprite(batch, glow, u.x, u.y + 22, false, 2.6f);
        }
        float flash = u.hitFlash;
        batch.setColor(1f, 1f - flash * 0.5f, 1f - flash * 0.5f, 1f);
        drawAnim(batch, set, anim, time, u.x, u.y, u.facing < 0f, u instanceof Hero ? 1.1f : 1f);
        batch.setColor(Color.WHITE);
        if (u instanceof Hero && control.heroSelected) {
            circle(batch, u.x, u.y + 4, 22f, tmp.set(1f, 0.85f, 0.35f, 0.9f), false);
        }
    }

    private void drawEnemy(SpriteBatch batch, Enemy e) {
        Assets.AnimSet set = assets.anim(e.def.sprite);
        String anim;
        float time = e.animTime;
        float alpha = 1f;
        float altitude = e.flying ? e.def.flyHeight + MathUtils.sin(e.animTime * 3f + e.serial) * 3f : 0f;
        if (e.state == EnemyState.DYING) {
            anim = "death";
            time = e.deathTimer;
            float fade = e.deathTimer / Constants.ENEMY_DEATH_TIME;
            alpha = fade > 0.6f ? 1f - (fade - 0.6f) / 0.4f : 1f;
            altitude *= Math.max(0f, 1f - fade * 2.5f);
        } else if (e.state == EnemyState.FIGHTING || e.state == EnemyState.CASTING) {
            anim = "attack";
        } else if (e.has(StatusType.STUN)) {
            anim = "idle";
        } else {
            anim = "walk";
        }
        if (e.flying) {
            batch.setColor(1f, 1f, 1f, 0.5f * alpha);
            drawSprite(batch, assets.sprite("shadow"), e.x, e.y, false, e.def.radius / 9f);
        }
        float flash = e.hitFlash;
        float r = 1f;
        float g = 1f - flash * 0.45f;
        float b = 1f - flash * 0.45f;
        if (e.has(StatusType.BURN)) {
            g *= 0.8f;
            b *= 0.6f;
        } else if (e.has(StatusType.SLOW)) {
            r *= 0.7f;
            g *= 0.85f;
        }
        batch.setColor(r, g, b, alpha);
        drawAnim(batch, set, anim, time, e.x, e.y + altitude, e.facing < 0f, e.def.scale);
        if (e.has(StatusType.SHIELD)) {
            batch.setColor(1f, 1f, 1f, 0.8f);
            drawSprite(batch, assets.sprite("fx_shield"), e.x, e.y + e.def.radius * 1.6f + altitude, false,
                    e.def.radius / 18f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawAbilityEntities(SpriteBatch batch) {
        for (DropPod pod : world.abilities.dropPods) {
            if (!pod.landed) {
                float h = (1f - pod.fallFraction()) * DROP_HEIGHT;
                circle(batch, pod.x, pod.y, pod.impactRadius, tmp.set(1f, 0.5f, 0.2f, 0.5f), false);
                drawSprite(batch, assets.sprite("droppod"), pod.x, pod.y + h, false, 1f);
            }
        }
        for (OrbitalStrike s : world.abilities.strikes) {
            if (!s.fired) {
                float pulse = 0.5f + 0.5f * MathUtils.sin(world.time * 14f);
                circle(batch, s.x, s.y, s.radius, tmp.set(1f, 0.2f, 0.2f, 0.25f + 0.25f * pulse), true);
                drawSprite(batch, assets.sprite("aoe_target"), s.x, s.y, false, s.radius / 48f);
                if (s.chargeFraction() > 0.7f) {
                    batch.setColor(1f, 1f, 1f, (s.chargeFraction() - 0.7f) / 0.3f);
                    drawSprite(batch, assets.sprite("lance"), s.x, s.y, false, 1f);
                }
            } else {
                batch.setColor(1f, 1f, 1f, Math.min(1f, s.afterglow));
                drawSprite(batch, assets.sprite("lance"), s.x, s.y, false, 1.4f);
            }
            batch.setColor(Color.WHITE);
        }
        for (ServoSkull skull : world.skulls) {
            drawSprite(batch, assets.sprite("servo_skull"), skull.x, skull.y + MathUtils.sin(world.time * 4f + skull.orbitAngle) * 2f,
                    false, 1f);
        }
        for (Thunderhawk t : world.abilities.gunships) {
            batch.setColor(0f, 0f, 0f, 0.35f);
            drawSprite(batch, assets.sprite("thunderhawk"), t.x, t.lineY, false, 0.9f);
            batch.setColor(Color.WHITE);
            drawSprite(batch, assets.sprite("thunderhawk"), t.x, t.lineY + AbilitySystem.GUNSHIP_ALTITUDE, false, 1f);
        }
    }

    private void drawProjectiles(SpriteBatch batch) {
        for (Projectile p : world.projectiles.active) {
            Assets.Sprite s = assets.sprite(p.type.sprite);
            if (p.type.motion == ProjectileMotion.BEAM) {
                float dx = p.targetX - p.x;
                float dy = p.targetY - p.y;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float alpha = 1f - p.age / Math.max(0.01f, p.duration);
                float w = s.region.getRegionWidth();
                float h = s.region.getRegionHeight() * s.scale;
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(s.region, p.x, p.y - h / 2, 0, h / 2, w, h, len / w, 1f, angle);
                batch.setColor(Color.WHITE);
                continue;
            }
            float y = p.y + p.height;
            if (p.type.motion == ProjectileMotion.BALLISTIC) {
                batch.setColor(0f, 0f, 0f, 0.3f);
                drawSprite(batch, assets.sprite("shadow"), p.x, p.y, false, 0.35f);
                batch.setColor(Color.WHITE);
            }
            drawRegion(batch, s.region, s.originX, s.originY, s.scale, p.x, y, false, p.angle);
        }
    }

    private void drawOverlays(SpriteBatch batch) {
        if (settings.showHealthBars) {
            for (Enemy e : world.enemies) {
                if (!e.isTargetable() || e.def.boss || (e.hp >= e.maxHp && !e.def.elite)) {
                    continue;
                }
                float y = e.y + e.def.healthBarOffset + (e.flying ? e.def.flyHeight : 0f);
                bar(batch, e.x, y, Math.max(16f, e.def.healthBarWidth), e.hpFraction(), Color.RED);
                int shown = 0;
                for (int i = 0; i < StatusType.COUNT && shown < 3; i++) {
                    if (e.statusTime[i] > 0f) {
                        StatusType st = StatusType.byIndex(i);
                        drawSprite(batch, assets.sprite(st.icon), e.x - 12 + shown * 12, y + 11, false, 0.9f);
                        shown++;
                    }
                }
                if (e.synapseBuffed && shown < 3) {
                    drawSprite(batch, assets.sprite("status_synapse"), e.x - 12 + shown * 12, y + 11, false, 0.9f);
                }
            }
            for (AlliedUnit u : world.units) {
                if (u.alive && u.hp < u.maxHp) {
                    bar(batch, u.x, u.y + 42, 22f, u.hpFraction(), Color.GREEN);
                }
            }
            if (world.hero != null && world.hero.alive) {
                bar(batch, world.hero.x, world.hero.y + 52, 30f, world.hero.hpFraction(), Color.GOLD);
            }
            for (Tower t : world.towers) {
                if (t.integrity < t.maxIntegrity - 1f || t.isOffline()) {
                    bar(batch, t.x, t.y + 78, 40f, t.isOffline() ? 0f : t.integrity / t.maxIntegrity, Color.CYAN);
                }
            }
        }
        if (control.abilityTargeting >= 0) {
            AbilitySystem.Slot slot = world.abilities.slots.get(control.abilityTargeting);
            float r = Math.max(30f, slot.definition.targetRadius);
            circle(batch, control.mouseX, control.mouseY, r, tmp.set(1f, 0.4f, 0.3f, 0.35f), true);
            drawSprite(batch, assets.sprite("aoe_target"), control.mouseX, control.mouseY, false, r / 48f);
        }
    }
}
