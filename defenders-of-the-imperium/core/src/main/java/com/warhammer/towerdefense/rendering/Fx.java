package com.warhammer.towerdefense.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.audio.AudioManager;
import com.warhammer.towerdefense.combat.ProjectileType;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.data.BossDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.DropPod;
import com.warhammer.towerdefense.entities.Enemy;
import com.warhammer.towerdefense.heroes.Hero;
import com.warhammer.towerdefense.save.SettingsSave;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.UpgradeOption;
import com.warhammer.towerdefense.waves.WaveDefinition;
import com.warhammer.towerdefense.world.GameEvents;

/**
 * Presentation feedback for simulation events: pooled particles, ground decals, floating texts, screen shake,
 * banners and sounds. It observes the model through {@link GameEvents} and never changes gameplay.
 */
public final class Fx implements GameEvents {
    private static final int MAX_PARTICLES = 2500;
    private static final int MAX_DECALS = 160;
    private static final int MAX_TEXTS = 60;
    private static final Color ICHOR = new Color(0.55f, 0.2f, 0.62f, 1f);
    private static final Color BLOOD = new Color(0.6f, 0.05f, 0.05f, 1f);

    static final class Particle {
        TextureRegion region;
        float x, y, vx, vy, life, maxLife, size0, size1, rot, spin, gravity, drag;
        float r, g, b, a0, a1;
        boolean additive;
    }

    static final class Decal {
        TextureRegion region;
        float x, y, w, h, rot, life, maxLife;
        float r, g, b;
    }

    static final class FloatText {
        String text;
        float x, y, life;
        Color color = new Color();
    }

    private final Assets assets;
    private final AudioManager audio;
    private final SettingsSave settings;
    private final Array<Particle> active = new Array<>(false, 512);
    private final Array<Particle> free = new Array<>(false, 512);
    private final Array<Decal> decals = new Array<>(false, MAX_DECALS);
    private final Array<FloatText> texts = new Array<>(false, MAX_TEXTS);
    private final TextureRegion soft;
    private final TextureRegion spark;
    private final TextureRegion smoke;
    private final TextureRegion flash;
    private final TextureRegion ring;
    private final TextureRegion debris;
    private final TextureRegion plus;
    public float shake;
    public float damageFlash;
    public String banner;
    public String bannerSub;
    public float bannerTime;
    public Color bannerColor = new Color(Color.WHITE);
    public float waveCleared;

    public Fx(Assets assets, AudioManager audio, SettingsSave settings) {
        this.assets = assets;
        this.audio = audio;
        this.settings = settings;
        soft = assets.sprite("fx_soft").region;
        spark = assets.sprite("fx_spark").region;
        smoke = assets.sprite("fx_smoke").region;
        flash = assets.sprite("fx_flash").region;
        ring = assets.sprite("fx_ring").region;
        debris = assets.sprite("fx_debris").region;
        plus = assets.sprite("fx_plus").region;
    }

    // ------------------------------------------------------------------ primitives

    private float density() {
        switch (settings.particleQuality()) {
            case OFF:
                return 0.15f;
            case LOW:
                return 0.45f;
            default:
                return 1f;
        }
    }

    private Particle spawn(TextureRegion region, float x, float y, float vx, float vy, float life, float size0,
                           float size1, Color c, float a0, float a1, boolean additive) {
        if (active.size >= MAX_PARTICLES) {
            return null;
        }
        Particle p = free.size > 0 ? free.pop() : new Particle();
        p.region = region;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size0 = size0;
        p.size1 = size1;
        p.r = c.r;
        p.g = c.g;
        p.b = c.b;
        p.a0 = a0;
        p.a1 = a1;
        p.additive = additive;
        p.rot = MathUtils.random(360f);
        p.spin = MathUtils.random(-90f, 90f);
        p.gravity = 0f;
        p.drag = 1.5f;
        active.add(p);
        return p;
    }

    private int count(int n) {
        return Math.max(1, Math.round(n * density()));
    }

    public void burst(float x, float y, int n, Color c, float speed, float size, float life) {
        for (int i = 0, m = count(n); i < m; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float s = MathUtils.random(speed * 0.3f, speed);
            Particle p = spawn(spark, x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, life * MathUtils.random(0.6f, 1f),
                    size, size * 0.3f, c, 1f, 0f, true);
            if (p != null) {
                p.rot = a * MathUtils.radiansToDegrees;
                p.spin = 0;
            }
        }
    }

    public void explosion(float x, float y, float radius) {
        spawn(flash, x, y, 0, 0, 0.25f, radius * 1.2f, radius * 2.2f, Color.WHITE, 1f, 0f, true);
        spawn(ring, x, y, 0, 0, 0.45f, radius * 0.6f, radius * 2.4f, new Color(1f, 0.8f, 0.5f, 1f), 0.8f, 0f, true);
        for (int i = 0, m = count(10); i < m; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float s = MathUtils.random(20f, 80f) * radius / 40f;
            spawn(soft, x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(0.3f, 0.6f), radius * 0.7f,
                    radius * 0.2f, new Color(1f, MathUtils.random(0.4f, 0.7f), 0.15f, 1f), 1f, 0f, true);
        }
        for (int i = 0, m = count(7); i < m; i++) {
            Particle p = spawn(smoke, x + MathUtils.random(-radius, radius) * 0.4f, y + MathUtils.random(-radius, radius) * 0.3f,
                    MathUtils.random(-15f, 15f), MathUtils.random(10f, 35f), MathUtils.random(0.9f, 1.6f), radius * 0.5f,
                    radius * 1.3f, new Color(0.25f, 0.23f, 0.22f, 1f), 0.7f, 0f, false);
            if (p != null) {
                p.drag = 0.8f;
            }
        }
        for (int i = 0, m = count(8); i < m; i++) {
            float a = MathUtils.random(0.2f, MathUtils.PI - 0.2f);
            float s = MathUtils.random(60f, 160f);
            Particle p = spawn(debris, x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, 0.8f, 5f, 4f,
                    new Color(0.35f, 0.3f, 0.25f, 1f), 1f, 0.6f, false);
            if (p != null) {
                p.gravity = -380f;
                p.drag = 0.3f;
            }
        }
        decal("decal_crater", x, y, radius * 1.3f, radius * 0.7f, 0.55f, 0.5f, 0.45f, 18f);
        addShake(Math.min(1f, radius / 90f));
    }

    public void decal(String sprite, float x, float y, float w, float h, float r, float g, float b, float life) {
        if (decals.size >= MAX_DECALS) {
            decals.removeIndex(0);
        }
        Decal d = new Decal();
        d.region = assets.sprite(sprite).region;
        d.x = x;
        d.y = y;
        d.w = w;
        d.h = h;
        d.rot = MathUtils.random(-15f, 15f);
        d.life = life;
        d.maxLife = life;
        d.r = r;
        d.g = g;
        d.b = b;
        decals.add(d);
    }

    public void text(String s, float x, float y, Color c) {
        if (texts.size >= MAX_TEXTS) {
            texts.removeIndex(0);
        }
        FloatText t = new FloatText();
        t.text = s;
        t.x = x;
        t.y = y;
        t.life = 1.1f;
        t.color.set(c);
        texts.add(t);
    }

    public void addShake(float amount) {
        if (settings.screenShake) {
            shake = Math.min(1.2f, shake + amount);
        }
    }

    public void showBanner(String text, String sub, Color color, float seconds) {
        banner = text;
        bannerSub = sub;
        bannerColor.set(color);
        bannerTime = seconds;
    }

    // ------------------------------------------------------------------ update & draw

    public void update(float dt) {
        for (int i = active.size - 1; i >= 0; i--) {
            Particle p = active.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                active.removeIndex(i);
                free.add(p);
                continue;
            }
            p.vy += p.gravity * dt;
            float damp = Math.max(0f, 1f - p.drag * dt);
            p.vx *= damp;
            p.vy *= damp;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.spin * dt;
        }
        for (int i = decals.size - 1; i >= 0; i--) {
            Decal d = decals.get(i);
            d.life -= dt;
            if (d.life <= 0f) {
                decals.removeIndex(i);
            }
        }
        for (int i = texts.size - 1; i >= 0; i--) {
            FloatText t = texts.get(i);
            t.life -= dt;
            t.y += 26f * dt;
            if (t.life <= 0f) {
                texts.removeIndex(i);
            }
        }
        shake = Math.max(0f, shake - dt * 2.2f);
        damageFlash = Math.max(0f, damageFlash - dt * 1.6f);
        bannerTime = Math.max(0f, bannerTime - dt);
        waveCleared = Math.max(0f, waveCleared - dt);
    }

    public void drawDecals(SpriteBatch batch) {
        for (Decal d : decals) {
            float a = Math.min(1f, d.life / (d.maxLife * 0.3f)) * 0.8f;
            batch.setColor(d.r, d.g, d.b, a);
            batch.draw(d.region, d.x - d.w / 2, d.y - d.h / 2, d.w / 2, d.h / 2, d.w, d.h, 1, 1, d.rot);
        }
        batch.setColor(Color.WHITE);
    }

    public void drawParticles(SpriteBatch batch) {
        drawPass(batch, false);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawPass(batch, true);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawPass(SpriteBatch batch, boolean additive) {
        for (int i = 0; i < active.size; i++) {
            Particle p = active.get(i);
            if (p.additive != additive) {
                continue;
            }
            float t = 1f - p.life / p.maxLife;
            float size = MathUtils.lerp(p.size0, p.size1, t);
            float aspect = p.region.getRegionHeight() / (float) p.region.getRegionWidth();
            batch.setColor(p.r, p.g, p.b, MathUtils.lerp(p.a0, p.a1, t));
            batch.draw(p.region, p.x - size / 2, p.y - size * aspect / 2, size / 2, size * aspect / 2, size,
                    size * aspect, 1, 1, p.rot);
        }
    }

    public void drawTexts(SpriteBatch batch) {
        for (FloatText t : texts) {
            assets.small.setColor(t.color.r, t.color.g, t.color.b, Math.min(1f, t.life * 2f));
            assets.small.draw(batch, t.text, t.x - 10, t.y);
        }
        assets.small.setColor(Color.WHITE);
    }

    public int particleCount() {
        return active.size;
    }

    // ------------------------------------------------------------------ events

    @Override
    public void towerBuilt(Tower tower) {
        for (int i = 0, m = count(14); i < m; i++) {
            spawn(smoke, tower.x + MathUtils.random(-40f, 40f), tower.y + MathUtils.random(-8f, 16f), MathUtils.random(-20f, 20f),
                    MathUtils.random(10f, 40f), 0.9f, 14f, 40f, new Color(0.6f, 0.55f, 0.45f, 1f), 0.6f, 0f, false);
        }
        audio.play("build");
    }

    @Override
    public void towerUpgraded(Tower tower, UpgradeOption option) {
        burst(tower.x, tower.y + 40, 24, new Color(1f, 0.85f, 0.4f, 1f), 140f, 12f, 0.7f);
        spawn(ring, tower.x, tower.y + 20, 0, 0, 0.5f, 30f, 150f, new Color(1f, 0.85f, 0.4f, 1f), 0.9f, 0f, true);
        audio.play("upgrade");
    }

    @Override
    public void towerSold(Tower tower, int refund) {
        text("+" + refund, tower.x, tower.y + 50, Ui_GOLD);
        audio.play("sell");
    }

    private static final Color Ui_GOLD = new Color(0.95f, 0.78f, 0.35f, 1f);

    @Override
    public void towerFired(Tower tower, ProjectileType type, float fromX, float fromY, float toX, float toY) {
        muzzle(type, fromX, fromY, toX, toY);
    }

    @Override
    public void shotFired(ProjectileType type, float fromX, float fromY, float toX, float toY) {
        muzzle(type, fromX, fromY, toX, toY);
    }

    private void muzzle(ProjectileType type, float fromX, float fromY, float toX, float toY) {
        Color c;
        switch (type) {
            case PLASMA:
            case SKULL_ZAP:
                c = new Color(0.5f, 0.85f, 1f, 1f);
                break;
            case LASER:
                c = new Color(1f, 0.35f, 0.3f, 1f);
                break;
            case VENOM:
            case BIO_PLASMA:
                c = new Color(0.6f, 1f, 0.3f, 1f);
                break;
            default:
                c = new Color(1f, 0.75f, 0.3f, 1f);
        }
        float a = MathUtils.atan2(toY - fromY, toX - fromX);
        spawn(soft, fromX + MathUtils.cos(a) * 8, fromY + MathUtils.sin(a) * 8, 0, 0, 0.08f, 16f, 8f, c, 1f, 0f, true);
        switch (type) {
            case BOLT:
            case STORM_BOLT:
            case PISTOL_BOLT:
                audio.play("bolter");
                break;
            case HEAVY_BOLT:
                audio.play("heavy_bolter");
                break;
            case PLASMA:
                audio.play("plasma");
                break;
            case LASER:
                audio.play("lascannon");
                break;
            case MISSILE:
            case ROCKET:
                audio.play("missile");
                break;
            default:
                break;
        }
    }

    @Override
    public void projectileImpact(ProjectileType type, float x, float y, float splashRadius, boolean hostile) {
        switch (type) {
            case MISSILE:
            case ROCKET:
            case BIO_PLASMA:
                explosion(x, y, Math.max(26f, splashRadius));
                audio.play("explosion_small");
                break;
            case PLASMA:
                burst(x, y, 10, new Color(0.5f, 0.85f, 1f, 1f), 120f, 10f, 0.4f);
                spawn(flash, x, y, 0, 0, 0.18f, 20f, 50f, new Color(0.6f, 0.9f, 1f, 1f), 1f, 0f, true);
                break;
            case LASER:
                burst(x, y, 8, new Color(1f, 0.4f, 0.3f, 1f), 100f, 9f, 0.3f);
                break;
            case HEAVY_BOLT:
                burst(x, y, 5, new Color(1f, 0.7f, 0.3f, 1f), 90f, 8f, 0.25f);
                if (splashRadius > 0) {
                    spawn(flash, x, y, 0, 0, 0.14f, 10f, splashRadius * 1.4f, new Color(1f, 0.7f, 0.3f, 1f), 0.9f, 0f, true);
                }
                break;
            default:
                burst(x, y, 3, new Color(1f, 0.8f, 0.4f, 1f), 70f, 6f, 0.2f);
        }
    }

    @Override
    public void enemyHit(Enemy enemy, float damage, com.warhammer.towerdefense.combat.DamageType type) {
        if (settings.showDamageNumbers && damage >= 40f) {
            text(String.valueOf(Math.round(damage)), enemy.x + MathUtils.random(-6f, 6f), enemy.y + enemy.def.healthBarOffset,
                    new Color(1f, 0.95f, 0.6f, 1f));
        }
    }

    @Override
    public void enemyKilled(Enemy enemy, int bounty) {
        float s = enemy.def.radius * 2.4f;
        decal("decal_splat_" + MathUtils.random(2), enemy.x, enemy.y, s * 1.6f, s, ICHOR.r, ICHOR.g, ICHOR.b, 14f);
        for (int i = 0, m = count(8 + (int) enemy.def.radius); i < m; i++) {
            float a = MathUtils.random(0.1f, MathUtils.PI - 0.1f);
            float sp = MathUtils.random(50f, 130f);
            Particle p = spawn(soft, enemy.x, enemy.y + enemy.def.radius, MathUtils.cos(a) * sp, MathUtils.sin(a) * sp,
                    0.6f, 6f, 3f, ICHOR, 1f, 0.2f, false);
            if (p != null) {
                p.gravity = -300f;
            }
        }
        if (bounty > 0) {
            text("+" + bounty, enemy.x, enemy.y + enemy.def.healthBarOffset + 6, Ui_GOLD);
        }
        audio.play(enemy.def.boss ? "boss_death" : "tyranid_death");
    }

    @Override
    public void enemyLeaked(Enemy enemy, int damage) {
        damageFlash = 1f;
        addShake(0.3f + damage / 40f);
        audio.play("warning");
    }

    @Override
    public void unitMelee(AlliedUnit attacker, Enemy target, float damage) {
        burst(target.x, target.y + target.def.radius, 3, new Color(0.7f, 0.3f, 0.75f, 1f), 70f, 6f, 0.25f);
    }

    @Override
    public void unitDied(AlliedUnit unit) {
        decal("decal_splat_" + MathUtils.random(2), unit.x, unit.y, 30, 18, BLOOD.r, BLOOD.g, BLOOD.b, 10f);
        burst(unit.x, unit.y + 15, 10, new Color(0.3f, 0.45f, 0.9f, 1f), 90f, 7f, 0.5f);
    }

    @Override
    public void unitRetired(AlliedUnit unit) {
        spawn(soft, unit.x, unit.y + 15, 0, 20, 0.5f, 20f, 40f, new Color(0.5f, 0.7f, 1f, 1f), 0.8f, 0f, true);
    }

    @Override
    public void heroLevelUp(Hero hero) {
        burst(hero.x, hero.y + 20, 30, new Color(1f, 0.85f, 0.35f, 1f), 160f, 10f, 0.9f);
        showBanner("LEVEL " + hero.level, hero.name, new Color(1f, 0.85f, 0.35f, 1f), 2.2f);
        audio.play("level_up");
    }

    @Override
    public void heroAbility(Hero hero, AbilityDefinition ability, float x, float y) {
        spawn(ring, hero.x, hero.y + 16, 0, 0, 0.5f, 20f, 160f, new Color(1f, 0.85f, 0.4f, 1f), 0.8f, 0f, true);
    }

    @Override
    public void heroDied(Hero hero) {
        showBanner("THE CAPTAIN HAS FALLEN", "He will return", Ui.RED_COLOR, 2.5f);
    }

    @Override
    public void globalAbilityCast(AbilityDefinition ability, float x, float y) {
        audio.play("ability");
    }

    @Override
    public void dropPodLanded(DropPod pod) {
        explosion(pod.x, pod.y, pod.impactRadius);
        audio.play("explosion_large");
    }

    @Override
    public void orbitalStrike(float x, float y, float radius) {
        explosion(x, y, radius);
        spawn(flash, x, y, 0, 0, 0.5f, radius * 2f, radius * 4f, new Color(0.7f, 0.9f, 1f, 1f), 1f, 0f, true);
        addShake(1f);
        audio.play("explosion_large");
    }

    @Override
    public void gunshipBomb(float x, float y, float radius) {
        explosion(x, y, radius);
        audio.play("explosion_small");
    }

    @Override
    public void waveStarted(int wave, int total, WaveDefinition definition) {
        showBanner(String.format("WAVE %02d", wave), definition.title, Ui.IVORY_COLOR, 2.8f);
        audio.play(definition.boss ? "boss_warning" : "wave_start");
    }

    @Override
    public void waveCompleted(int wave, int reward, boolean flawless) {
        waveCleared = 2.5f;
        if (reward > 0) {
            text("WAVE " + wave + " CLEARED  +" + reward + (flawless ? "  FLAWLESS" : ""), 520, 560, Ui_GOLD);
        }
    }

    @Override
    public void earlyCall(int bonus) {
        text("EARLY DEPLOYMENT +" + bonus, 560, 620, Ui_GOLD);
    }

    @Override
    public void bossSpawned(Enemy boss) {
        showBanner("WARNING: " + boss.def.name.toUpperCase(), "The Hive Mind's champion approaches", Ui.RED_COLOR, 3.5f);
        addShake(0.8f);
        audio.play("boss_roar");
    }

    @Override
    public void bossPhase(Enemy boss, int phase, String announcement) {
        if (phase > 0 && announcement != null && !announcement.isEmpty()) {
            showBanner(announcement, "", Ui.RED_COLOR, 2.5f);
            addShake(0.6f);
            audio.play("boss_roar");
        }
    }

    @Override
    public void bossAbility(Enemy boss, BossDefinition.AbilityType type, float x, float y, float radius) {
        Color c = type == BossDefinition.AbilityType.SPAWN ? new Color(0.6f, 0.3f, 0.7f, 1f) : new Color(0.75f, 0.35f, 1f, 1f);
        spawn(ring, x, y + 10, 0, 0, 0.6f, 20f, radius * 2.4f, c, 0.9f, 0f, true);
        if (type == BossDefinition.AbilityType.LEAP) {
            explosion(x, y, radius * 0.6f);
        }
    }

    @Override
    public void synapseCollapse(float x, float y, float radius) {
        spawn(ring, x, y + 10, 0, 0, 0.7f, 20f, radius * 2f, new Color(0.7f, 0.35f, 1f, 1f), 0.8f, 0f, true);
    }

    @Override
    public void baseDamaged(int damage, int hpLeft) {
        text("-" + damage, 90, 470, new Color(1f, 0.3f, 0.25f, 1f));
    }

    @Override
    public void towerOffline(Tower tower) {
        burst(tower.x, tower.y + 40, 12, new Color(0.7f, 1f, 0.3f, 1f), 100f, 8f, 0.5f);
        text("SUPPRESSED", tower.x - 30, tower.y + 70, new Color(0.8f, 1f, 0.3f, 1f));
    }

    public void healPlus(float x, float y) {
        spawn(plus, x, y, 0, 25, 0.8f, 8f, 6f, new Color(0.4f, 1f, 0.4f, 1f), 1f, 0f, true);
    }

    /** Colours shared with the HUD without a dependency cycle. */
    static final class Ui {
        static final Color RED_COLOR = new Color(1f, 0.3f, 0.25f, 1f);
        static final Color IVORY_COLOR = new Color(0.95f, 0.93f, 0.86f, 1f);

        private Ui() {
        }
    }
}
