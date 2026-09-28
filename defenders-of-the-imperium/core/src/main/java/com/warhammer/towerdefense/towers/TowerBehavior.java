package com.warhammer.towerdefense.towers;

/**
 * Reusable tower behaviours. Each of the four Space Marine towers is data (towers.json) plugged into one of these:
 * Tactical Bastion and Devastator Emplacement are RANGED, Assault Bastion is BARRACKS, Techmarine Fortification is
 * SUPPORT. New towers can be added without code as long as they reuse a behaviour.
 */
public enum TowerBehavior {
    RANGED, BARRACKS, SUPPORT
}
