package com.warhammer.towerdefense;

/** Command line switches (QA / debugging). Normal players never need them. */
public final class GameLaunchOptions {
    /** Starts the first map immediately, played by the AutoCommander. */
    public boolean autoplay;
    /** With autoplay: saves screenshots into ./screenshots and exits after the last one. */
    public boolean screenshots;
    /** Starts with the F3 debug overlay visible. */
    public boolean debug;

    public static GameLaunchOptions parse(String[] args) {
        GameLaunchOptions o = new GameLaunchOptions();
        for (String a : args) {
            switch (a) {
                case "--autoplay":
                    o.autoplay = true;
                    break;
                case "--screenshots":
                    o.autoplay = true;
                    o.screenshots = true;
                    break;
                case "--debug":
                    o.debug = true;
                    break;
                default:
                    break;
            }
        }
        return o;
    }
}
