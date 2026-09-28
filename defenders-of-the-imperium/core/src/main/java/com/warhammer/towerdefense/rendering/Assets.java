package com.warhammer.towerdefense.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loads every art asset: sprite sheets and images listed in {@code sprites/sprites.json} (procedural),
 * {@code sprites/custom.json} (player-made art, overrides by name) and {@code ui/ui.json}; packs them into texture
 * atlases at startup; generates fonts; loads backgrounds through an {@link AssetManager}. Anything missing falls
 * back to a generated placeholder so the game always starts.
 */
public final class Assets implements Disposable {
    public static final String[] MANIFESTS = {"sprites/sprites.json", "sprites/custom.json", "ui/ui.json"};

    /** A single image with its anchor (in texture pixels) and world scale. */
    public static final class Sprite {
        public final TextureRegion region;
        public final float originX;
        public final float originY;
        public final float scale;

        Sprite(TextureRegion region, float originX, float originY, float scale) {
            this.region = region;
            this.originX = originX;
            this.originY = originY;
            this.scale = scale;
        }

        public float width() {
            return region.getRegionWidth() * scale;
        }

        public float height() {
            return region.getRegionHeight() * scale;
        }
    }

    /** Idle / walk / attack / death animations of one unit, sharing an anchor. */
    public static final class AnimSet {
        public final ObjectMap<String, Animation<TextureRegion>> anims = new ObjectMap<>();
        public float originX;
        public float originY;
        public float scale;

        public Animation<TextureRegion> get(String name) {
            Animation<TextureRegion> a = anims.get(name);
            return a != null ? a : anims.get("idle");
        }
    }

    private final AssetManager manager = new AssetManager();
    private final ObjectMap<String, Sprite> sprites = new ObjectMap<>();
    private final ObjectMap<String, AnimSet> animSets = new ObjectMap<>();
    private final ObjectMap<String, NinePatch> patches = new ObjectMap<>();
    private final Array<TextureAtlas> atlases = new Array<>();
    private Texture fallbackTexture;
    public TextureRegion white;
    public BitmapFont small;
    public BitmapFont body;
    public BitmapFont header;
    public BitmapFont title;

    public void load() {
        fallbackTexture = makeFallback();
        white = new TextureRegion(fallbackTexture, 1, 1, 2, 2);
        JsonReader reader = new JsonReader();
        ObjectMap<String, JsonValue> sheetDefs = new ObjectMap<>();
        ObjectMap<String, JsonValue> imageDefs = new ObjectMap<>();
        ObjectMap<String, JsonValue> patchDefs = new ObjectMap<>();
        for (String manifest : MANIFESTS) {
            FileHandle f = Gdx.files.internal(manifest);
            if (!f.exists()) {
                Gdx.app.error("Assets", "Missing manifest " + manifest + " (run gradlew generateAssets)");
                continue;
            }
            JsonValue root = reader.parse(f);
            collect(root.get("sheets"), sheetDefs);
            collect(root.get("images"), imageDefs);
            collect(root.get("ninePatches"), patchDefs);
        }
        PixmapPacker packer = new PixmapPacker(4096, 4096, Pixmap.Format.RGBA8888, 2, true);
        ObjectMap<String, JsonValue> all = new ObjectMap<>();
        all.putAll(sheetDefs);
        all.putAll(imageDefs);
        all.putAll(patchDefs);
        for (ObjectMap.Entry<String, JsonValue> e : all) {
            FileHandle file = Gdx.files.internal(e.value.getString("file"));
            if (!file.exists()) {
                Gdx.app.error("Assets", "Missing image " + file.path());
                continue;
            }
            Pixmap pixmap = new Pixmap(file);
            packer.pack(e.key, pixmap);
            pixmap.dispose();
        }
        TextureAtlas atlas = packer.generateTextureAtlas(Texture.TextureFilter.MipMapLinearLinear,
                Texture.TextureFilter.Linear, true);
        packer.dispose();
        atlases.add(atlas);
        for (ObjectMap.Entry<String, JsonValue> e : imageDefs) {
            TextureRegion r = atlas.findRegion(e.key);
            if (r != null) {
                sprites.put(e.key, new Sprite(r, e.value.getFloat("originX"), e.value.getFloat("originY"),
                        e.value.getFloat("scale", 1f)));
            }
        }
        for (ObjectMap.Entry<String, JsonValue> e : patchDefs) {
            TextureRegion r = atlas.findRegion(e.key);
            if (r != null) {
                JsonValue v = e.value;
                patches.put(e.key, new NinePatch(r, v.getInt("left"), v.getInt("right"), v.getInt("top"), v.getInt("bottom")));
            }
        }
        for (ObjectMap.Entry<String, JsonValue> e : sheetDefs) {
            TextureRegion r = atlas.findRegion(e.key);
            if (r != null) {
                animSets.put(e.key, buildAnims(r, e.value));
            }
        }
        white = atlas.findRegion("white") != null ? shrink(atlas.findRegion("white")) : white;
        generateFonts();
        for (String bg : new String[]{"ui/menu_background.jpg"}) {
            queueTexture(bg);
        }
        manager.finishLoading();
    }

    private static TextureRegion shrink(TextureRegion r) {
        return new TextureRegion(r, 2, 2, Math.max(1, r.getRegionWidth() - 4), Math.max(1, r.getRegionHeight() - 4));
    }

    private static void collect(JsonValue list, ObjectMap<String, JsonValue> into) {
        if (list == null) {
            return;
        }
        for (JsonValue v = list.child; v != null; v = v.next) {
            into.put(v.getString("name"), v);
        }
    }

    private static AnimSet buildAnims(TextureRegion sheet, JsonValue def) {
        AnimSet set = new AnimSet();
        int fw = def.getInt("frameWidth");
        int fh = def.getInt("frameHeight");
        set.originX = def.getFloat("originX", fw / 2f);
        set.originY = def.getFloat("originY", 0f);
        set.scale = def.getFloat("scale", 0.5f);
        for (JsonValue a = def.get("animations").child; a != null; a = a.next) {
            int row = a.getInt("row");
            int frames = a.getInt("frames");
            Array<TextureRegion> regions = new Array<>(frames);
            for (int i = 0; i < frames; i++) {
                regions.add(new TextureRegion(sheet, i * fw, row * fh, fw, fh));
            }
            Animation<TextureRegion> anim = new Animation<>(1f / a.getFloat("fps", 8f), regions,
                    a.getBoolean("loop", true) ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            set.anims.put(a.name, anim);
        }
        return set;
    }

    private void generateFonts() {
        float oversample = 1.5f;
        small = font("fonts/DroidSans-Bold.ttf", 13, 1.2f, oversample);
        body = font("fonts/DroidSans-Bold.ttf", 17, 1.5f, oversample);
        header = font("fonts/DroidSerif-Bold.ttf", 28, 2f, oversample);
        title = font("fonts/DroidSerif-Bold.ttf", 52, 3f, oversample);
    }

    private static BitmapFont font(String file, int size, float border, float oversample) {
        FileHandle f = Gdx.files.internal(file);
        if (!f.exists()) {
            BitmapFont fallback = new BitmapFont();
            fallback.getData().setScale(size / 15f);
            return fallback;
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(f);
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = Math.round(size * oversample);
        p.borderWidth = border * oversample;
        p.borderColor = new Color(0.03f, 0.03f, 0.05f, 1f);
        p.shadowOffsetY = Math.round(oversample);
        p.shadowColor = new Color(0, 0, 0, 0.6f);
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ•×";
        BitmapFont font = gen.generateFont(p);
        font.getData().setScale(1f / oversample);
        font.setUseIntegerPositions(false);
        gen.dispose();
        return font;
    }

    public void queueTexture(String path) {
        if (Gdx.files.internal(path).exists() && !manager.isLoaded(path)) {
            Texture.TextureFilter f = Texture.TextureFilter.Linear;
            com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter tp =
                    new com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter();
            tp.minFilter = f;
            tp.magFilter = f;
            manager.load(path, Texture.class, tp);
        }
    }

    /** A background texture (loaded on demand), or null when the file is missing. */
    public Texture texture(String path) {
        if (!manager.isLoaded(path)) {
            queueTexture(path);
            manager.finishLoading();
        }
        return manager.isLoaded(path) ? manager.get(path, Texture.class) : null;
    }

    public Sprite sprite(String name) {
        Sprite s = sprites.get(name);
        if (s == null) {
            s = new Sprite(new TextureRegion(fallbackTexture), 16, 0, 0.75f);
            sprites.put(name, s);
            Gdx.app.error("Assets", "Missing sprite '" + name + "', using placeholder");
        }
        return s;
    }

    public boolean hasSprite(String name) {
        return sprites.containsKey(name);
    }

    public AnimSet anim(String name) {
        AnimSet a = animSets.get(name);
        if (a == null) {
            a = new AnimSet();
            Animation<TextureRegion> still = new Animation<>(1f, new TextureRegion(fallbackTexture));
            a.anims.put("idle", still);
            a.originX = 16;
            a.originY = 0;
            a.scale = 0.75f;
            animSets.put(name, a);
            Gdx.app.error("Assets", "Missing sheet '" + name + "', using placeholder");
        }
        return a;
    }

    public NinePatch patch(String name) {
        NinePatch p = patches.get(name);
        if (p == null) {
            p = new NinePatch(white, 0, 0, 0, 0);
            patches.put(name, p);
        }
        return p;
    }

    /** A 32x32 placeholder: a hooded shape with the Imperial blue, never a plain square. */
    private static Texture makeFallback() {
        Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fillRectangle(0, 0, 4, 4);
        pm.setColor(0.15f, 0.26f, 0.62f, 1f);
        pm.fillCircle(16, 20, 11);
        pm.fillCircle(16, 8, 6);
        pm.setColor(1f, 0.2f, 0.2f, 1f);
        pm.fillCircle(19, 8, 2);
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void dispose() {
        for (TextureAtlas a : atlases) {
            a.dispose();
        }
        manager.dispose();
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
        }
        for (BitmapFont f : new BitmapFont[]{small, body, header, title}) {
            if (f != null) {
                f.dispose();
            }
        }
    }
}
