package com.warhammer.towerdefense.towers;

import com.warhammer.towerdefense.data.DataException;

/** A single change to a {@link Stat}: set it, add to it or multiply it. */
public final class StatModifier {
    public enum Op {
        SET, ADD, MULT
    }

    public final Stat stat;
    public final Op op;
    public final float value;

    public StatModifier(Stat stat, Op op, float value) {
        this.stat = stat;
        this.op = op;
        this.value = value;
    }

    public void apply(TowerStats stats) {
        float current = stats.get(stat);
        switch (op) {
            case SET:
                stats.set(stat, value);
                break;
            case ADD:
                stats.set(stat, current + value);
                break;
            case MULT:
                stats.set(stat, current * value);
                break;
            default:
                throw new IllegalStateException("Unknown op " + op);
        }
    }

    /** Parses "+5", "-5", "*0.8", "=3" or a plain number (set). */
    public static StatModifier parse(Stat stat, String raw, String context) {
        String s = raw.trim();
        if (s.isEmpty()) {
            throw new DataException(context + ": empty modifier for '" + stat.key + "'");
        }
        try {
            char c = s.charAt(0);
            switch (c) {
                case '+':
                    return new StatModifier(stat, Op.ADD, Float.parseFloat(s.substring(1)));
                case '-':
                    return new StatModifier(stat, Op.ADD, -Float.parseFloat(s.substring(1)));
                case '*':
                case 'x':
                    return new StatModifier(stat, Op.MULT, Float.parseFloat(s.substring(1)));
                case '=':
                    return new StatModifier(stat, Op.SET, Float.parseFloat(s.substring(1)));
                default:
                    return new StatModifier(stat, Op.SET, Float.parseFloat(s));
            }
        } catch (NumberFormatException e) {
            throw new DataException(context + ": cannot parse modifier '" + raw + "' for '" + stat.key + "'");
        }
    }

    /** Human readable form used in tooltips: "+25% fire rate" style text is produced by the UI from this. */
    @Override
    public String toString() {
        switch (op) {
            case ADD:
                return stat.key + (value >= 0 ? " +" : " ") + value;
            case MULT:
                return stat.key + " x" + value;
            default:
                return stat.key + " = " + value;
        }
    }
}
