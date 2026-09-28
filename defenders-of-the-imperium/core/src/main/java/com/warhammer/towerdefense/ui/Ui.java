package com.warhammer.towerdefense.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.warhammer.towerdefense.rendering.Assets;

/**
 * Minimal immediate-mode UI for the Imperial interface: every frame widgets are drawn and registered as hit areas
 * with an action code; clicks are resolved against the areas registered in the previous frame.
 */
public final class Ui {
    public static final Color GOLD = new Color(0.95f, 0.78f, 0.35f, 1f);
    public static final Color IVORY = new Color(0.95f, 0.93f, 0.86f, 1f);
    public static final Color GREY = new Color(0.62f, 0.64f, 0.7f, 1f);
    public static final Color RED = new Color(0.92f, 0.25f, 0.2f, 1f);
    public static final Color GREEN = new Color(0.35f, 0.88f, 0.4f, 1f);
    public static final Color BLUE = new Color(0.35f, 0.6f, 1f, 1f);

    /** A clickable area registered this frame. */
    public static final class Hit {
        public final Rectangle rect = new Rectangle();
        public int action;
        public int arg;
        public String tooltip;
    }

    public final SpriteBatch batch;
    public final Assets assets;
    public float mouseX;
    public float mouseY;
    private final Array<Hit> hits = new Array<>();
    private final Array<Hit> previous = new Array<>();
    private final Array<Hit> pool = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();
    private String tooltip;

    public Ui(SpriteBatch batch, Assets assets) {
        this.batch = batch;
        this.assets = assets;
    }

    public void begin(float mx, float my) {
        mouseX = mx;
        mouseY = my;
        pool.addAll(previous);
        previous.clear();
        previous.addAll(hits);
        hits.clear();
        tooltip = null;
    }

    public void hit(float x, float y, float w, float h, int action, int arg, String tip) {
        Hit hit = pool.size > 0 ? pool.pop() : new Hit();
        hit.rect.set(x, y, w, h);
        hit.action = action;
        hit.arg = arg;
        hit.tooltip = tip;
        hits.add(hit);
        if (tip != null && hover(x, y, w, h)) {
            tooltip = tip;
        }
    }

    /** Topmost hit under the point from the last completed frame, or null. */
    public Hit pick(float x, float y) {
        for (int i = hits.size - 1; i >= 0; i--) {
            if (hits.get(i).rect.contains(x, y)) {
                return hits.get(i);
            }
        }
        return null;
    }

    public boolean hover(float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public void panel(float x, float y, float w, float h) {
        batch.setColor(Color.WHITE);
        assets.patch("ui_panel").draw(batch, x, y, w, h);
    }

    public void rect(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(assets.white, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(assets.white, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void bar(float x, float y, float w, float h, float fraction, Color fill) {
        rect(x - 1, y - 1, w + 2, h + 2, 0f, 0f, 0f, 0.8f);
        rect(x, y, w, h, 0.15f, 0.15f, 0.18f, 1f);
        rect(x, y, w * Math.max(0f, Math.min(1f, fraction)), h, fill);
    }

    /** Button with text; returns true when hovered. */
    public boolean button(float x, float y, float w, float h, String text, boolean enabled, String style, int action,
                          int arg, String tip) {
        boolean hovered = hover(x, y, w, h);
        String patch = !enabled ? "ui_button_disabled" : hovered ? "ui_button_hover" : style;
        batch.setColor(Color.WHITE);
        assets.patch(patch).draw(batch, x, y, w, h);
        text(assets.body, text, x + w / 2, y + h / 2 + 7, enabled ? IVORY : GREY, Align.center, w - 8);
        if (enabled) {
            hit(x, y, w, h, action, arg, tip);
        } else if (tip != null && hovered) {
            tooltip = tip;
        }
        return hovered;
    }

    /** Square icon button (uses the ui_slot frame). */
    public boolean iconButton(float x, float y, float size, String icon, boolean enabled, boolean selected, int action,
                              int arg, String tip) {
        boolean hovered = hover(x, y, size, size);
        batch.setColor(selected ? GOLD : hovered && enabled ? Color.WHITE : new Color(0.8f, 0.8f, 0.85f, 1f));
        assets.patch("ui_slot").draw(batch, x, y, size, size);
        batch.setColor(enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 1f));
        drawIcon(icon, x + size / 2, y + size / 2, size * 0.78f);
        batch.setColor(Color.WHITE);
        if (enabled) {
            hit(x, y, size, size, action, arg, tip);
        } else if (tip != null && hovered) {
            tooltip = tip;
        }
        return hovered;
    }

    public void drawIcon(String name, float cx, float cy, float size) {
        Assets.Sprite s = assets.sprite(name);
        float w = s.region.getRegionWidth();
        float h = s.region.getRegionHeight();
        float scale = size / Math.max(w, h);
        batch.draw(s.region, cx - w * scale / 2, cy - h * scale / 2, w * scale, h * scale);
    }

    public void text(BitmapFont font, String text, float x, float y, Color color, int align, float width) {
        font.setColor(color);
        font.draw(batch, text, x - (align == Align.center ? width / 2 : align == Align.right ? width : 0), y, width,
                align, true);
        font.setColor(Color.WHITE);
    }

    public void text(BitmapFont font, String text, float x, float y, Color color) {
        font.setColor(color);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    public float textWidth(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    public void setTooltip(String t) {
        tooltip = t;
    }

    /** Draws the pending tooltip near the mouse; call last. */
    public void drawTooltip(float screenW, float screenH) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        float w = 300;
        layout.setText(assets.small, tooltip, IVORY, w - 20, Align.left, true);
        float h = layout.height + 20;
        float x = Math.min(mouseX + 16, screenW - w - 4);
        float y = Math.min(mouseY + 16 + h, screenH - 4) - h;
        if (y < 4) {
            y = 4;
        }
        batch.setColor(Color.WHITE);
        assets.patch("ui_panel").draw(batch, x, y, w, h);
        assets.small.draw(batch, layout, x + 10, y + h - 10);
    }
}
