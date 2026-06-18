package com.github.roleplaycauldron.brotkrumen;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests bundled default resources.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResourceDefaultsTest {

    @Test
    void defaultConfigDocumentsGuidedPathAndGoalCompletionSettings() {
        final YamlConfiguration config = resourceYaml("config.yml");

        assertEquals(4, config.getInt("visualizer.guidedPath.windowSize"),
                "Default guided path window size should be documented");
        assertEquals(1, config.getInt("visualizer.guidedPath.lookBehind"),
                "Default guided path look-behind should be documented");
        assertFalse(config.getBoolean("visualizer.guidedPath.keepLookBehindOnCompletion"),
                "Default guided path completion look-behind should be disabled");
        assertTrue(config.getBoolean("commands.resolve.goal.message.enabled"),
                "Default goal message should be enabled");
        assertTrue(config.getBoolean("commands.resolve.goal.sound.enabled"),
                "Default goal sound should be enabled");
        assertEquals("entity.player.levelup", config.getString("commands.resolve.goal.sound.name"),
                "Default goal sound should be documented");
        assertFalse(config.getBoolean("commands.resolve.goal.title.enabled"),
                "Default goal title should be disabled");
        assertEquals("preview", config.getString("editor.defaultEditPlacementMode"),
                "Existing graph edit sessions should default to preview mode");
        assertFalse(config.getBoolean("editor.placeNodesOnGround"),
                "Ground-snapped editor placement should default to disabled");
        assertEquals("spellbookEffect", config.getString("visualizer.defaultRenderer"),
                "Default renderer should be configured once under visualizer settings");
        assertEquals("ember", config.getString("visualizer.defaultSpellbookEffectPreset"),
                "Spellbook effect default preset should be documented");
        assertEquals("ember", config.getString("visualizer.defaultBlockDisplayPreset"),
                "Block-display default preset should be documented");
        assertFalse(config.contains("commands.resolve.visualizerBackend"),
                "Resolve should use visualizer.defaultRenderer instead of its legacy backend setting");
        assertEquals("ember", config.getString("editor.defaultPreset"),
                "Editor temporary preset default should reference a bundled preset");
    }

    @Test
    void bundledLanguagesContainResolveCompletionTitleKeys() {
        final YamlConfiguration english = resourceYaml("language/en-us.yml");
        final YamlConfiguration german = resourceYaml("language/de-de.yml");

        assertTrue(english.contains("messages.commands.bk.resolve.status.guidanceCompleteTitle"),
                "English title key should exist");
        assertTrue(english.contains("messages.commands.bk.resolve.status.guidanceCompleteSubtitle"),
                "English subtitle key should exist");
        assertTrue(english.contains("messages.commands.bkeditor.status.placeNodesOnGroundSet"),
                "English editor ground placement setting key should exist");
        assertTrue(english.contains("messages.commands.bkeditor.status.graphPresetSet"),
                "English graph preset setting key should exist");
        assertTrue(german.contains("messages.commands.bk.resolve.status.guidanceCompleteTitle"),
                "German title key should exist");
        assertTrue(german.contains("messages.commands.bk.resolve.status.guidanceCompleteSubtitle"),
                "German subtitle key should exist");
        assertTrue(german.contains("messages.commands.bkeditor.status.placeNodesOnGroundSet"),
                "German editor ground placement setting key should exist");
        assertTrue(german.contains("messages.commands.bkeditor.status.graphPresetSet"),
                "German graph preset setting key should exist");
    }

    private YamlConfiguration resourceYaml(final String path) {
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Resource should exist: " + path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
