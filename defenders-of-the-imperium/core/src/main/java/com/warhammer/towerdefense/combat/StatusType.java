package com.warhammer.towerdefense.combat;

/**
 * Status effects that can be applied to enemies. Each enemy stores the remaining time and power of every status in
 * two float arrays indexed by {@link #ordinal()}, so applying and ticking statuses never allocates.
 * <ul>
 *     <li>SLOW: power = fraction of speed removed.</li>
 *     <li>BURN / BLEED: power = damage per second.</li>
 *     <li>ARMOR_BREAK: power = armour removed.</li>
 *     <li>STUN: no movement, no attacks.</li>
 *     <li>MARKED: power = extra damage taken (0.2 = +20%).</li>
 *     <li>REGENERATION: power = hp per second.</li>
 *     <li>SHIELD: power = damage absorbed before hp is touched.</li>
 * </ul>
 */
public enum StatusType {
    SLOW("status_slow", true),
    BURN("status_burn", true),
    BLEED("status_bleed", true),
    ARMOR_BREAK("status_armorbreak", true),
    STUN("status_stun", true),
    MARKED("status_marked", true),
    REGENERATION("status_regen", false),
    SHIELD("status_shield", false);

    public static final int COUNT = values().length;
    private static final StatusType[] VALUES = values();

    public final String icon;
    public final boolean debuff;

    StatusType(String icon, boolean debuff) {
        this.icon = icon;
        this.debuff = debuff;
    }

    public static StatusType byIndex(int index) {
        return VALUES[index];
    }
}
