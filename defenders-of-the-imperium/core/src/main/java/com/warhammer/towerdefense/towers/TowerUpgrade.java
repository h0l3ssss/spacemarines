package com.warhammer.towerdefense.towers;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.OrderedMap;
import com.warhammer.towerdefense.data.DataException;

/**
 * One purchasable step of a tower: a tier, a specialisation unlock or a specialisation rank. It carries its cost,
 * texts and the stat modifiers / property overrides it applies.
 */
public final class TowerUpgrade {
    public String id = "";
    public String name;
    public String description = "";
    public String icon;
    public int cost;
    public final Array<StatModifier> modifiers = new Array<>();
    public final OrderedMap<String, String> properties = new OrderedMap<>();

    public void applyTo(TowerStats stats) {
        for (StatModifier m : modifiers) {
            m.apply(stats);
        }
        for (OrderedMap.Entry<String, String> e : properties.entries()) {
            stats.applyProperty(e.key, e.value);
        }
    }

    public static TowerUpgrade parse(JsonValue v, String context) {
        TowerUpgrade u = new TowerUpgrade();
        u.id = v.getString("id", "");
        u.name = v.getString("name", "Upgrade");
        u.description = v.getString("description", "");
        u.icon = v.getString("icon", null);
        u.cost = v.getInt("cost", 0);
        if (u.cost < 0) {
            throw new DataException(context + ": cost cannot be negative");
        }
        parseModifiers(v.get("stats"), u, context);
        parseModifiers(v.get("modifiers"), u, context);
        return u;
    }

    private static void parseModifiers(JsonValue block, TowerUpgrade u, String context) {
        if (block == null) {
            return;
        }
        for (JsonValue c = block.child; c != null; c = c.next) {
            String key = c.name;
            if (TowerStats.isProperty(key)) {
                u.properties.put(key, TowerStats.normaliseProperty(key, c.asString(), context + "." + key));
                continue;
            }
            Stat stat = Stat.byKey(key);
            if (stat == null) {
                throw new DataException(context + ": unknown tower stat '" + key + "'");
            }
            if (c.isNumber()) {
                u.modifiers.add(new StatModifier(stat, StatModifier.Op.SET, c.asFloat()));
            } else if (c.isString()) {
                u.modifiers.add(StatModifier.parse(stat, c.asString(), context));
            } else {
                throw new DataException(context + ": stat '" + key + "' must be a number or a modifier string");
            }
        }
    }
}
