package com.warhammer.towerdefense.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectLongMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.warhammer.towerdefense.save.SettingsSave;

/**
 * Audio with four categories (music, sfx, voice, ambient). Sounds are looked up by name in
 * {@code assets/audio/<category>/<name>.(ogg|wav|mp3)}; missing files are silently skipped so audio can be added
 * later without code changes. Identical sounds are throttled so a volley of twenty bolters stays listenable.
 */
public final class AudioManager implements Disposable {
    public enum Category {
        MUSIC, SFX, VOICE, AMBIENT
    }

    /** Every sound effect the game triggers (file name without extension in audio/sfx). */
    public static final String[] SFX = {
            "bolter", "heavy_bolter", "plasma", "lascannon", "missile", "explosion_small", "explosion_large",
            "chainsword", "power_sword", "tyranid_death", "tyranid_screech", "boss_roar", "boss_death", "warning",
            "wave_start", "boss_warning", "build", "upgrade", "sell", "click", "error", "level_up", "ability",
            "victory", "defeat"
    };
    private static final String[] EXTENSIONS = {".ogg", ".wav", ".mp3"};
    private static final long MIN_INTERVAL_MS = 55;

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final ObjectLongMap<String> lastPlayed = new ObjectLongMap<>();
    private final SettingsSave settings;
    private Music music;
    private String musicName;

    public AudioManager(SettingsSave settings) {
        this.settings = settings;
        for (String name : SFX) {
            FileHandle f = find("audio/sfx/" + name);
            if (f != null) {
                try {
                    sounds.put(name, Gdx.audio.newSound(f));
                } catch (RuntimeException e) {
                    Gdx.app.error("Audio", "Cannot load " + f.path(), e);
                }
            }
        }
    }

    private static FileHandle find(String base) {
        for (String ext : EXTENSIONS) {
            FileHandle f = Gdx.files.internal(base + ext);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    public void play(String name) {
        Sound s = sounds.get(name);
        if (s == null || settings.sfxVolume <= 0f) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPlayed.get(name, 0L) < MIN_INTERVAL_MS) {
            return;
        }
        lastPlayed.put(name, now);
        s.play(settings.sfxVolume, MathUtils.random(0.94f, 1.06f), 0f);
    }

    /** Plays a looping track from audio/music (no-op if the file does not exist). */
    public void playMusic(String name) {
        if (name.equals(musicName)) {
            return;
        }
        stopMusic();
        musicName = name;
        FileHandle f = find("audio/music/" + name);
        if (f == null) {
            return;
        }
        try {
            music = Gdx.audio.newMusic(f);
            music.setLooping(true);
            music.setVolume(settings.musicVolume);
            music.play();
        } catch (RuntimeException e) {
            Gdx.app.error("Audio", "Cannot play " + f.path(), e);
        }
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        musicName = null;
    }

    public void applyVolumes() {
        if (music != null) {
            music.setVolume(settings.musicVolume);
        }
    }

    @Override
    public void dispose() {
        stopMusic();
        for (Sound s : sounds.values()) {
            s.dispose();
        }
    }
}
