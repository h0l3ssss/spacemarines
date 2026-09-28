package com.warhammer.towerdefense.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.warhammer.towerdefense.MainGame;

public final class Lwjgl3Launcher {
    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Warhammer 40,000: Defenders of the Imperium (fan-made)");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new MainGame(), config);
    }
}
