package com.warhammer.towerdefense;

import com.warhammer.towerdefense.data.Difficulty;
import com.warhammer.towerdefense.save.GameSave;
import com.warhammer.towerdefense.save.SaveManager;
import com.warhammer.towerdefense.save.SettingsSave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveLoadTest {
    @TempDir
    Path dir;

    @Test
    void campaignProgressRoundTrips() {
        SaveManager manager = new SaveManager(dir);
        GameSave save = new GameSave();
        save.recordVictory(TestSupport.MAP, 3, Difficulty.VETERAN);
        save.hero("ultramarines_captain").level = 4;
        save.hero("ultramarines_captain").xp = 600;
        save.armoury.put("logistics", 2);
        save.markEnemySeen("termagant");
        save.unlockedHeroes.add("ultramarines_captain");
        manager.saveGame(save);

        GameSave loaded = manager.loadGame();
        assertTrue(loaded.isCompleted(TestSupport.MAP));
        assertEquals(3, loaded.stars(TestSupport.MAP));
        assertEquals("VETERAN", loaded.progress(TestSupport.MAP).bestDifficulty);
        assertEquals(4, loaded.hero("ultramarines_captain").level);
        assertEquals(600, loaded.hero("ultramarines_captain").xp);
        assertEquals(2, loaded.armouryRank("logistics"));
        assertTrue(loaded.seenEnemies.contains("termagant", false));
        assertTrue(loaded.unlockedHeroes.contains("ultramarines_captain", false));
        assertTrue(loaded.isUnlocked(TestSupport.data().map("necron_tomb_world")), "next map unlocked by victory");
    }

    @Test
    void settingsRoundTripAndAreNormalised() {
        SaveManager manager = new SaveManager(dir);
        SettingsSave s = new SettingsSave();
        s.musicVolume = 0.25f;
        s.fullscreen = true;
        s.windowWidth = 1920;
        s.windowHeight = 1080;
        s.showDamageNumbers = false;
        s.particles = "LOW";
        manager.saveSettings(s);

        SettingsSave loaded = manager.loadSettings();
        assertEquals(0.25f, loaded.musicVolume, 0.0001f);
        assertTrue(loaded.fullscreen);
        assertEquals(1920, loaded.windowWidth);
        assertFalse(loaded.showDamageNumbers);
        assertEquals(SettingsSave.ParticleQuality.LOW, loaded.particleQuality());

        SettingsSave broken = new SettingsSave();
        broken.musicVolume = 7f;
        broken.windowWidth = 123;
        broken.particles = "ULTRA";
        broken.normalise();
        assertEquals(1f, broken.musicVolume, 0.0001f);
        assertEquals(1280, broken.windowWidth);
        assertEquals(SettingsSave.ParticleQuality.HIGH, broken.particleQuality());
    }

    @Test
    void corruptedFilesFallBackToDefaultsAndAreBackedUp() throws IOException {
        Files.writeString(dir.resolve(SaveManager.SAVE_FILE), "{ this is : not json", StandardCharsets.UTF_8);
        SaveManager manager = new SaveManager(dir);
        GameSave save = manager.loadGame();
        assertEquals(0, save.totalStars());
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test
    void armouryPurchasesUseStars() {
        GameSave save = new GameSave();
        var data = TestSupport.data();
        var logistics = data.armouryUpgrade("logistics");
        assertFalse(save.buyArmoury(data, logistics), "no stars yet");
        save.recordVictory(TestSupport.MAP, 3, Difficulty.CASUAL);
        assertTrue(save.buyArmoury(data, logistics));
        assertTrue(save.buyArmoury(data, logistics));
        assertEquals(1, save.starsAvailable(data));
        assertEquals(1.2f, save.bonuses(data).startingRequisitionMult, 0.0001f);
        save.resetArmoury();
        assertEquals(3, save.starsAvailable(data));
    }
}
