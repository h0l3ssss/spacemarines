package com.warhammer.towerdefense.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code save.json} and {@code settings.json} in {@code ~/.defenders-of-the-imperium}.
 * Writes go to a temporary file first and are then moved into place, so a crash never leaves a half-written save.
 * A corrupted file is backed up and replaced by defaults instead of preventing the game from starting.
 */
public final class SaveManager {
    public static final String SAVE_FILE = "save.json";
    public static final String SETTINGS_FILE = "settings.json";
    private static final Logger LOG = Logger.getLogger(SaveManager.class.getName());

    private final Path directory;
    private final Json json;

    public SaveManager(Path directory) {
        this.directory = directory;
        this.json = new Json(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
        json.setElementType(GameSave.class, "maps", MapProgress.class);
        json.setElementType(GameSave.class, "heroes", HeroProgress.class);
        json.setElementType(GameSave.class, "armoury", Integer.class);
        json.setElementType(GameSave.class, "unlockedHeroes", String.class);
        json.setElementType(GameSave.class, "unlockedChapters", String.class);
        json.setElementType(GameSave.class, "seenEnemies", String.class);
    }

    public static Path defaultDirectory() {
        return Paths.get(System.getProperty("user.home", "."), ".defenders-of-the-imperium");
    }

    public Path directory() {
        return directory;
    }

    public GameSave loadGame() {
        GameSave save = read(SAVE_FILE, GameSave.class, GameSave::new);
        if (save.maps == null || save.heroes == null || save.armoury == null) {
            GameSave fresh = new GameSave();
            if (save.maps != null) {
                fresh.maps = save.maps;
            }
            save = fresh;
        }
        return save;
    }

    public void saveGame(GameSave save) {
        write(SAVE_FILE, save);
    }

    public SettingsSave loadSettings() {
        return read(SETTINGS_FILE, SettingsSave.class, SettingsSave::new).normalise();
    }

    public void saveSettings(SettingsSave settings) {
        write(SETTINGS_FILE, settings.normalise());
    }

    private <T> T read(String name, Class<T> type, Supplier<T> defaults) {
        Path file = directory.resolve(name);
        if (!Files.isRegularFile(file)) {
            return defaults.get();
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            T value = json.fromJson(type, text);
            return value != null ? value : defaults.get();
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Corrupted " + name + ", using defaults", e);
            backupCorrupted(file);
            return defaults.get();
        }
    }

    private void backupCorrupted(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not back up " + file, e);
        }
    }

    private void write(String name, Object value) {
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(name);
            Path temp = directory.resolve(name + ".tmp");
            Files.writeString(temp, json.prettyPrint(value), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "Could not write " + name, e);
        }
    }
}
