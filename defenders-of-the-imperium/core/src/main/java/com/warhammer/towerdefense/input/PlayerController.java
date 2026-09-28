package com.warhammer.towerdefense.input;

import com.warhammer.towerdefense.Constants;
import com.warhammer.towerdefense.map.BuildPoint;
import com.warhammer.towerdefense.towers.Tower;
import com.warhammer.towerdefense.towers.TowerType;
import com.warhammer.towerdefense.world.GameWorld;

/**
 * Selection state machine for world interaction: build plots, towers, the hero, tower placement, rally points and
 * targeted abilities. Both the HUD and the world renderer read this state.
 */
public final class PlayerController {
    private static final float HERO_PICK_RADIUS = 26f;

    private final GameWorld world;
    public BuildPoint selectedSlot;
    public Tower selectedTower;
    public boolean heroSelected;
    public TowerType placing;
    public int abilityTargeting = -1;
    public boolean rallyMode;
    public float mouseX;
    public float mouseY;
    /** Feedback for the HUD when an action failed (e.g. not enough requisition). */
    public String message;
    public float messageTime;

    public PlayerController(GameWorld world) {
        this.world = world;
    }

    public void clear() {
        selectedSlot = null;
        selectedTower = null;
        heroSelected = false;
        placing = null;
        abilityTargeting = -1;
        rallyMode = false;
    }

    public void update(float dt) {
        messageTime = Math.max(0f, messageTime - dt);
        if (selectedTower != null && !selectedTower.alive) {
            selectedTower = null;
        }
    }

    public void say(String text) {
        message = text;
        messageTime = 2.2f;
    }

    /** Left click in world coordinates. */
    public void click(float x, float y) {
        if (abilityTargeting >= 0) {
            if (!world.castGlobalAbility(abilityTargeting, x, y)) {
                say("NOT ENOUGH REQUISITION");
            }
            abilityTargeting = -1;
            return;
        }
        if (rallyMode && selectedTower != null) {
            if (!world.setRallyPoint(selectedTower, x, y)) {
                say("RALLY POINT MUST BE ON THE ROAD, IN RANGE");
            }
            rallyMode = false;
            return;
        }
        BuildPoint slot = world.slotAt(x, y);
        if (placing != null) {
            if (slot != null && world.towerAt(slot) == null) {
                build(slot, placing);
                placing = null;
                return;
            }
            placing = null;
        }
        if (slot != null) {
            Tower t = world.towerAt(slot);
            clear();
            if (t != null) {
                selectedTower = t;
            } else {
                selectedSlot = slot;
            }
            return;
        }
        if (world.hero != null && world.hero.alive && world.hero.dst2(x, y - 16) <= HERO_PICK_RADIUS * HERO_PICK_RADIUS) {
            clear();
            heroSelected = true;
            return;
        }
        if (heroSelected) {
            world.orderHero(x, y);
            return;
        }
        clear();
    }

    public void build(BuildPoint slot, TowerType type) {
        if (world.towerAt(slot) != null) {
            return;
        }
        if (!world.state.canAfford(world.buildCost(type))) {
            say("NOT ENOUGH REQUISITION");
            return;
        }
        Tower t = world.build(slot, type);
        clear();
        selectedTower = t;
    }

    /** Hotkeys 1-4: build on the selected plot, or enter placement mode. */
    public void chooseTower(int index) {
        if (index < 0 || index >= world.data.towers.size) {
            return;
        }
        TowerType type = world.data.towers.get(index);
        if (selectedSlot != null) {
            build(selectedSlot, type);
        } else {
            clear();
            placing = type;
        }
    }

    public void startAbilityTargeting(int index) {
        if (!world.abilities.canCast(index)) {
            say(world.abilities.slots.get(index).cooldown > 0f ? "ABILITY RECHARGING" : "NOT ENOUGH REQUISITION");
            return;
        }
        boolean hero = heroSelected;
        clear();
        heroSelected = hero;
        abilityTargeting = index;
    }

    public boolean hasSelection() {
        return selectedSlot != null || selectedTower != null || heroSelected || placing != null || abilityTargeting >= 0
                || rallyMode;
    }

    public boolean isOverWorldUi(float uiY) {
        return uiY > Constants.UI_HEIGHT - 46;
    }
}
