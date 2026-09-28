package com.warhammer.towerdefense;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.warhammer.towerdefense.audio.AudioManager;
import com.warhammer.towerdefense.save.GameSave;
import com.warhammer.towerdefense.save.SaveManager;
import com.warhammer.towerdefense.save.SettingsSave;

/**
 * Game entry point and service container (settings, save data, audio). Screens (menu, battle) are the next step;
 * until they are added this shows an empty window.
 */
public class MainGame extends Game {
    public final GameLaunchOptions options;
    public SaveManager saves;
    public SettingsSave settings;
    public GameSave progress;
    public AudioManager audio;

    public MainGame() {
        this(new GameLaunchOptions());
    }

    public MainGame(GameLaunchOptions options) {
        this.options = options;
    }

    @Override
    public void create() {
        saves = new SaveManager(SaveManager.defaultDirectory());
        settings = saves.loadSettings();
        progress = saves.loadGame();
        audio = new AudioManager(settings);
        applyDisplay();
    }

    public void saveSettings() {
        saves.saveSettings(settings);
    }

    public void saveProgress() {
        saves.saveGame(progress);
    }

    /** Applies fullscreen/windowed mode, vsync and the frame cap from the settings. */
    public void applyDisplay() {
        if (settings.fullscreen && !Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (!settings.fullscreen) {
            Gdx.graphics.setWindowedMode(settings.windowWidth, settings.windowHeight);
        }
        Gdx.graphics.setVSync(settings.vsync);
        Gdx.graphics.setForegroundFPS(settings.fpsLimit);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (audio != null) {
            audio.dispose();
        }
    }
}
