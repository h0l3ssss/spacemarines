package com.warhammer.towerdefense.data;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectFloatMap;

import java.util.Arrays;
import java.util.Locale;

/** Small helpers for strict, readable JSON parsing with helpful error messages. */
public final class JsonUtil {
    private static final JsonReader READER = new JsonReader();

    private JsonUtil() {
    }

    public static JsonValue parse(DataSource source, String path) {
        String text = source.read(path);
        try {
            return READER.parse(text);
        } catch (RuntimeException e) {
            throw new DataException("Malformed JSON in '" + path + "': " + e.getMessage(), e);
        }
    }

    public static String requireString(JsonValue v, String key, String context) {
        String s = v.getString(key, null);
        if (s == null || s.isEmpty()) {
            throw new DataException(context + ": missing required field '" + key + "'");
        }
        return s;
    }

    public static float requireFloat(JsonValue v, String key, String context) {
        JsonValue child = v.get(key);
        if (child == null || !child.isNumber()) {
            throw new DataException(context + ": missing numeric field '" + key + "'");
        }
        return child.asFloat();
    }

    public static JsonValue requireChild(JsonValue v, String key, String context) {
        JsonValue child = v.get(key);
        if (child == null) {
            throw new DataException(context + ": missing section '" + key + "'");
        }
        return child;
    }

    public static <E extends Enum<E>> E enumValue(JsonValue v, String key, Class<E> type, E fallback, String context) {
        String s = v.getString(key, null);
        if (s == null) {
            return fallback;
        }
        return parseEnum(s, type, context + "." + key);
    }

    public static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String context) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DataException(context + ": invalid value '" + raw + "', expected one of "
                    + Arrays.toString(type.getEnumConstants()));
        }
    }

    public static Array<String> stringArray(JsonValue v, String key) {
        Array<String> out = new Array<>();
        JsonValue arr = v.get(key);
        if (arr != null) {
            for (JsonValue c = arr.child; c != null; c = c.next) {
                out.add(c.asString());
            }
        }
        return out;
    }

    /** Parses {@code [x, y]} or {@code {"x":..,"y":..}}. */
    public static Vector2 point(JsonValue v, String context) {
        if (v.isArray() && v.size >= 2) {
            return new Vector2(v.get(0).asFloat(), v.get(1).asFloat());
        }
        if (v.isObject() && v.has("x") && v.has("y")) {
            return new Vector2(v.getFloat("x"), v.getFloat("y"));
        }
        throw new DataException(context + ": expected a point [x, y]");
    }

    /** Reads every numeric child of an object into a float map (used for flexible ability parameters). */
    public static ObjectFloatMap<String> floatParams(JsonValue v) {
        ObjectFloatMap<String> out = new ObjectFloatMap<>();
        if (v == null) {
            return out;
        }
        for (JsonValue c = v.child; c != null; c = c.next) {
            if (c.isNumber()) {
                out.put(c.name, c.asFloat());
            }
        }
        return out;
    }
}
