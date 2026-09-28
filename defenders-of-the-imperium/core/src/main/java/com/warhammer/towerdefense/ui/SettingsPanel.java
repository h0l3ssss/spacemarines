package com.warhammer.towerdefense.ui;

import com.badlogic.gdx.utils.Align;
import com.warhammer.towerdefense.MainGame;
import com.warhammer.towerdefense.save.SettingsSave;

import java.util.Locale;

/** Settings list shared by the main menu and the pause menu. Every change is applied and saved immediately. */
public final class SettingsPanel {
    public static final int ACTION = 900;
    private static final String[] ROWS = {
            "MUSIC VOLUME", "SFX VOLUME", "FULLSCREEN", "RESOLUTION", "DAMAGE NUMBERS", "ENEMY HEALTH BARS",
            "SCREEN SHAKE", "PARTICLES", "SHOW FPS", "FPS LIMIT"
    };

    private final MainGame game;

    public SettingsPanel(MainGame game) {
        this.game = game;
    }

    public void draw(Ui ui, float cx, float top) {
        SettingsSave s = game.settings;
        String[] values = {
                pct(s.musicVolume), pct(s.sfxVolume), onOff(s.fullscreen), s.windowWidth + " x " + s.windowHeight,
                onOff(s.showDamageNumbers), onOff(s.showHealthBars), onOff(s.screenShake), s.particles,
                onOff(s.showFps), s.fpsLimit == 0 ? "UNLIMITED" : String.valueOf(s.fpsLimit)
        };
        for (int i = 0; i < ROWS.length; i++) {
            float y = top - i * 44;
            ui.text(ui.assets.body, ROWS[i], cx - 250, y, Ui.IVORY);
            ui.text(ui.assets.body, values[i], cx + 120, y, Ui.GOLD, Align.center, 160);
            if (i < 2) {
                ui.button(cx + 20, y - 26, 36, 32, "-", true, "ui_button", ACTION, i * 2, null);
                ui.button(cx + 184, y - 26, 36, 32, "+", true, "ui_button", ACTION, i * 2 + 1, null);
            } else {
                ui.button(cx + 184, y - 26, 70, 32, "CHANGE", true, "ui_button", ACTION, 100 + i, null);
            }
        }
    }

    public void handle(int arg) {
        SettingsSave s = game.settings;
        switch (arg) {
            case 0:
                s.musicVolume = Math.max(0f, s.musicVolume - 0.1f);
                break;
            case 1:
                s.musicVolume = Math.min(1f, s.musicVolume + 0.1f);
                break;
            case 2:
                s.sfxVolume = Math.max(0f, s.sfxVolume - 0.1f);
                break;
            case 3:
                s.sfxVolume = Math.min(1f, s.sfxVolume + 0.1f);
                break;
            case 102:
                s.fullscreen = !s.fullscreen;
                break;
            case 103: {
                int next = (s.resolutionIndex() + 1) % SettingsSave.RESOLUTIONS.length;
                s.windowWidth = SettingsSave.RESOLUTIONS[next][0];
                s.windowHeight = SettingsSave.RESOLUTIONS[next][1];
                break;
            }
            case 104:
                s.showDamageNumbers = !s.showDamageNumbers;
                break;
            case 105:
                s.showHealthBars = !s.showHealthBars;
                break;
            case 106:
                s.screenShake = !s.screenShake;
                break;
            case 107: {
                SettingsSave.ParticleQuality[] q = SettingsSave.ParticleQuality.values();
                s.particles = q[(s.particleQuality().ordinal() + 1) % q.length].name();
                break;
            }
            case 108:
                s.showFps = !s.showFps;
                break;
            case 109: {
                int idx = 0;
                for (int i = 0; i < SettingsSave.FPS_OPTIONS.length; i++) {
                    if (SettingsSave.FPS_OPTIONS[i] == s.fpsLimit) {
                        idx = i;
                    }
                }
                s.fpsLimit = SettingsSave.FPS_OPTIONS[(idx + 1) % SettingsSave.FPS_OPTIONS.length];
                break;
            }
            default:
                return;
        }
        game.saveSettings();
        game.applyDisplay();
        game.audio.applyVolumes();
    }

    private static String pct(float v) {
        return String.format(Locale.ROOT, "%d%%", Math.round(v * 100));
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }
}
