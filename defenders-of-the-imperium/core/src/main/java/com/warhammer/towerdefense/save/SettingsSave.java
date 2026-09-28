package com.warhammer.towerdefense.save;

import com.warhammer.towerdefense.data.Difficulty;

/** Player settings saved as JSON. Values are normalised after loading so a hand-edited file cannot break the game. */
public class SettingsSave {
    public enum ParticleQuality {
        OFF, LOW, HIGH
    }

    public static final int[][] RESOLUTIONS = {
            {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}
    };
    public static final int[] FPS_OPTIONS = {30, 60, 120, 144, 0};

    public int version = 1;
    public float musicVolume = 0.6f;
    public float sfxVolume = 0.8f;
    public float voiceVolume = 0.8f;
    public float ambientVolume = 0.5f;
    public boolean fullscreen;
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public boolean vsync = true;
    /** Foreground frame cap, 0 = unlimited. */
    public int fpsLimit = 60;
    public boolean showFps;
    public boolean showDamageNumbers = true;
    public boolean showHealthBars = true;
    public boolean screenShake = true;
    public String particles = ParticleQuality.HIGH.name();
    public String difficulty = Difficulty.VETERAN.name();

    public ParticleQuality particleQuality() {
        try {
            return ParticleQuality.valueOf(particles);
        } catch (RuntimeException e) {
            return ParticleQuality.HIGH;
        }
    }

    public Difficulty difficulty() {
        try {
            return Difficulty.valueOf(difficulty);
        } catch (RuntimeException e) {
            return Difficulty.VETERAN;
        }
    }

    public int resolutionIndex() {
        for (int i = 0; i < RESOLUTIONS.length; i++) {
            if (RESOLUTIONS[i][0] == windowWidth && RESOLUTIONS[i][1] == windowHeight) {
                return i;
            }
        }
        return 0;
    }

    public SettingsSave normalise() {
        musicVolume = clamp01(musicVolume);
        sfxVolume = clamp01(sfxVolume);
        voiceVolume = clamp01(voiceVolume);
        ambientVolume = clamp01(ambientVolume);
        int[] res = RESOLUTIONS[resolutionIndex()];
        windowWidth = res[0];
        windowHeight = res[1];
        boolean validFps = false;
        for (int f : FPS_OPTIONS) {
            validFps |= f == fpsLimit;
        }
        if (!validFps) {
            fpsLimit = 60;
        }
        particles = particleQuality().name();
        difficulty = difficulty().name();
        return this;
    }

    private static float clamp01(float v) {
        return Float.isNaN(v) ? 0.5f : Math.max(0f, Math.min(1f, v));
    }
}
