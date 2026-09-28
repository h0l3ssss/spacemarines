package com.warhammer.towerdefense.heroes;

import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.data.AbilityDefinition;
import com.warhammer.towerdefense.data.HeroDefinition;
import com.warhammer.towerdefense.entities.AlliedUnit;
import com.warhammer.towerdefense.entities.UnitState;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Player-controlled hero: walks where ordered, blocks and fights enemies, shoots with a sidearm, gains XP, levels
 * up (stronger stats, new abilities) and respawns at the fortress after dying.
 */
public class Hero extends AlliedUnit {
    public final HeroDefinition definition;
    public final Array<HeroAbilitySlot> abilities = new Array<>();
    public int level;
    public int xp;
    public float respawnTimer;

    public float empowerTimer;
    public float empowerDuration;

    public Hero(HeroDefinition definition, int level, int xp) {
        this.definition = definition;
        this.sprite = definition.sprite;
        this.name = definition.name;
        this.radius = 12f;
        this.speed = definition.speed;
        this.armor = definition.armor;
        this.regen = definition.regen;
        this.meleeRange = definition.meleeRange;
        this.attackCooldown = definition.attackCooldown;
        this.damageType = definition.damageType;
        this.maxEngaged = definition.maxEngaged;
        this.engageRadius = definition.engageRadius;
        this.rangedRange = definition.rangedRange;
        this.rangedCooldown = definition.rangedCooldown;
        this.rangedProjectile = definition.rangedProjectile;
        this.level = Math.max(1, Math.min(definition.maxLevel(), level));
        this.xp = Math.max(xp, definition.xpLevels.get(this.level - 1));
        applyLevelStats();
        hp = maxHp;
        for (AbilityDefinition a : definition.abilities) {
            abilities.add(new HeroAbilitySlot(a, HeroAbilities.create(a.id)));
        }
    }

    @Override
    public boolean isHero() {
        return true;
    }

    public void applyLevelStats() {
        int l = level - 1;
        maxHp = definition.hp + definition.hpPerLevel * l;
        damageMin = definition.damageMin + definition.damagePerLevel * l;
        damageMax = definition.damageMax + definition.damagePerLevel * l;
        rangedDamage = definition.rangedDamage + definition.rangedDamagePerLevel * l;
    }

    @Override
    public void update(GameWorld w, float dt) {
        for (HeroAbilitySlot slot : abilities) {
            slot.tick(dt);
        }
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0f) {
                respawn(w);
            }
            return;
        }
        if (empowerTimer > 0f) {
            empowerTimer -= dt;
            if (empowerTimer <= 0f) {
                endEmpower();
            }
        }
        super.update(w, dt);
    }

    /** Walk to a point, ignoring enemies on the way (engaged enemies are released). */
    public void orderMove(float tx, float ty) {
        if (!alive) {
            return;
        }
        releaseAll();
        moveOrder = true;
        moveX = tx;
        moveY = ty;
        state = UnitState.MOVING;
    }

    @Override
    protected void die(GameWorld w) {
        hp = 0f;
        alive = false;
        state = UnitState.DEAD;
        moveOrder = false;
        releaseAll();
        endEmpower();
        respawnTimer = definition.respawnTime;
        w.events.heroDied(this);
    }

    public void respawn(GameWorld w) {
        spawn(w.map.heroStartX, w.map.heroStartY);
        guardX = x;
        guardY = y;
        stunTimer = 0f;
        w.events.heroRespawned(this);
    }

    /** @return true if at least one level was gained. */
    public boolean addXp(int amount, GameWorld w) {
        if (amount <= 0 || level >= definition.maxLevel()) {
            return false;
        }
        xp += amount;
        boolean leveled = false;
        while (level < definition.maxLevel() && xp >= definition.xpLevels.get(level)) {
            level++;
            applyLevelStats();
            hp = maxHp;
            leveled = true;
            w.events.heroLevelUp(this);
        }
        return leveled;
    }

    /** Progress towards the next level in [0, 1] (1 at max level). */
    public float levelProgress() {
        if (level >= definition.maxLevel()) {
            return 1f;
        }
        int from = definition.xpLevels.get(level - 1);
        int to = definition.xpLevels.get(level);
        return Math.max(0f, Math.min(1f, (xp - from) / (float) (to - from)));
    }

    public boolean isUnlocked(HeroAbilitySlot slot) {
        return level >= slot.definition.unlockLevel;
    }

    public boolean canCast(int index) {
        if (index < 0 || index >= abilities.size || !alive) {
            return false;
        }
        HeroAbilitySlot slot = abilities.get(index);
        return isUnlocked(slot) && slot.isReady();
    }

    public boolean castAbility(int index, GameWorld w) {
        if (!canCast(index)) {
            return false;
        }
        HeroAbilitySlot slot = abilities.get(index);
        if (!slot.ability.activate(this, slot.definition, w)) {
            return false;
        }
        slot.cooldown = slot.definition.cooldown * w.bonuses.abilityCooldownMult;
        w.events.heroAbility(this, slot.definition, x, y);
        return true;
    }

    public void empower(float duration, float damageMultiplier, float speedMultiplier, float damageTaken) {
        empowerTimer = duration;
        empowerDuration = duration;
        damageMult = damageMultiplier;
        speedMult = speedMultiplier;
        damageTakenMult = damageTaken;
    }

    private void endEmpower() {
        empowerTimer = 0f;
        damageMult = 1f;
        speedMult = 1f;
        damageTakenMult = 1f;
    }

    public boolean isEmpowered() {
        return empowerTimer > 0f;
    }
}
