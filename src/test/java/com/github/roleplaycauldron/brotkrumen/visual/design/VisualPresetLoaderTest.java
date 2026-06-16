package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class VisualPresetLoaderTest {

    @TempDir
    private Path tempDir;

    @Test
    void bundledPresetsLoadEmberAndPrismWithoutDefault() {
        final VisualPresetRegistry registry = loader(null).parse(resourceYaml("presets.yml"));

        assertEquals(java.util.Set.of("ember", "prism"), registry.presetNames(),
                "Bundled presets should expose only ember and prism");
        assertTrue(registry.supports("ember", VisualRenderer.SPELLBOOK_EFFECT),
                "Ember should support Spellbook effect rendering");
        assertTrue(registry.supports("prism", VisualRenderer.BLOCK_DISPLAY),
                "Prism should support block-display rendering");
        assertFalse(registry.find("default").isPresent(), "No default preset should be bundled");
    }

    @Test
    void bundledPresetsPreserveCurrentStyleValues() {
        final VisualPresetRegistry registry = loader(null).parse(resourceYaml("presets.yml"));
        final VisualPreset ember = registry.find("ember").orElseThrow();
        final VisualPreset prism = registry.find("prism").orElseThrow();

        assertEquals(Particle.FLAME,
                ember.spellbookEffectDesign().orElseThrow().nodeDesign(VisualNodeRole.DEFAULT).particle(),
                "Ember particle default node should use flame");
        assertEquals(Particle.LAVA,
                ember.spellbookEffectDesign().orElseThrow().edgeDesign(VisualEdgeRole.INTER_GRAPH).particle(),
                "Ember particle inter-graph edge should use lava");
        assertEquals(Material.COAL_BLOCK,
                ember.blockDisplayDesign().orElseThrow().nodeDesign(VisualNodeRole.DEFAULT).blockMaterial(),
                "Ember block default node should use coal block");
        assertEquals(Material.CYAN_STAINED_GLASS,
                prism.blockDisplayDesign().orElseThrow().edgeDesign(VisualEdgeRole.DIRECTED_LOCAL).blockMaterial(),
                "Prism directed block edge should stay distinct");
        assertEquals(Particle.WITCH,
                prism.spellbookEffectDesign().orElseThrow()
                        .edgeDesign(VisualEdgeRole.DIRECTED_INTER_GRAPH).particle(),
                "Prism directed inter-graph particle edge should use witch particles");
    }

    @Test
    void particleOnlyBlockOnlyAndCombinedPresetsAreValid() {
        final VisualPresetRegistry registry = loader(null).parse(yaml("""
                particle-only:
                  particle:
                    nodes:
                      DEFAULT:
                        shape: { type: cube, size: 0.4, points-per-edge: 4 }
                        particle: { type: FLAME }
                    edges:
                      DEFAULT_LOCAL:
                        shape: { type: line, points: 4 }
                        particle: { type: FLAME }
                block-only:
                  block-display:
                    nodes:
                      DEFAULT: { material: STONE, scale: 0.4 }
                    edges:
                      DEFAULT_LOCAL: { material: STONE, thickness: 0.2, node-clearance: 0.5 }
                combined:
                  particle:
                    nodes:
                      DEFAULT:
                        shape: { type: sphere, radius: 0.4, points: 8 }
                        particle: { type: END_ROD }
                    edges:
                      DEFAULT_LOCAL:
                        shape: { type: moving-point, speed: 1.0, spacing: 0.2, amount-points: 4 }
                        particle: { type: END_ROD }
                  block-display:
                    nodes:
                      DEFAULT: { material: GOLD_BLOCK, scale: 0.4 }
                    edges:
                      DEFAULT_LOCAL: { material: GOLD_BLOCK, thickness: 0.2, node-clearance: 0.5 }
                """));

        assertTrue(registry.supports("particle-only", VisualRenderer.SPELLBOOK_EFFECT),
                "Particle-only preset should support Spellbook effect rendering");
        assertFalse(registry.supports("particle-only", VisualRenderer.BLOCK_DISPLAY),
                "Particle-only preset should not support block-display rendering");
        assertTrue(registry.supports("block-only", VisualRenderer.BLOCK_DISPLAY),
                "Block-only preset should support block-display rendering");
        assertFalse(registry.supports("block-only", VisualRenderer.SPELLBOOK_EFFECT),
                "Block-only preset should not support Spellbook effect rendering");
        assertTrue(registry.supports("combined", VisualRenderer.SPELLBOOK_EFFECT),
                "Combined preset should support Spellbook effect rendering");
        assertTrue(registry.supports("combined", VisualRenderer.BLOCK_DISPLAY),
                "Combined preset should support block-display rendering");
    }

    @Test
    void invalidPresetsAreSkippedWhileValidPresetRemainsAvailable() {
        final WrappedLogger logger = mock(WrappedLogger.class);
        final VisualPresetRegistry registry = loader(logger).parse(yaml("""
                valid:
                  block-display:
                    nodes:
                      DEFAULT: { material: STONE, scale: 0.4 }
                    edges:
                      DEFAULT_LOCAL: { material: STONE, thickness: 0.2, node-clearance: 0.5 }
                Valid:
                  block-display:
                    nodes:
                      DEFAULT: { material: DIRT, scale: 0.4 }
                    edges:
                      DEFAULT_LOCAL: { material: DIRT, thickness: 0.2, node-clearance: 0.5 }
                badRole:
                  block-display:
                    nodes:
                      MISSING_ROLE: { material: STONE, scale: 0.4 }
                    edges:
                      DEFAULT_LOCAL: { material: STONE, thickness: 0.2, node-clearance: 0.5 }
                badSection:
                  unknown: true
                badEffect:
                  particle:
                    nodes:
                      DEFAULT:
                        shape: { type: does-not-exist }
                        particle: { type: FLAME }
                    edges:
                      DEFAULT_LOCAL:
                        shape: { type: line, points: 4 }
                        particle: { type: FLAME }
                """));

        assertEquals(java.util.Set.of("valid"), registry.presetNames(),
                "Only the valid preset should survive validation");
        verify(logger, atLeast(4)).error(contains("Skipping visual preset"));
    }

    @Test
    void startupFailsWhenNoValidPresetExists() throws IOException {
        final JavaPlugin plugin = pluginWithPresetFile("");

        assertThrows(VisualPresetLoadException.class,
                () -> new VisualPresetLoader(plugin, null).loadForStartup(),
                "Startup loading should fail when no preset is usable");
    }

    @Test
    void reloadKeepsCurrentRegistryWhenNoValidPresetExists() throws IOException {
        final VisualPresetRegistry current = loader(null).parse(resourceYaml("presets.yml"));
        final WrappedLogger logger = mock(WrappedLogger.class);
        final JavaPlugin plugin = pluginWithPresetFile("");

        final VisualPresetRegistry reloaded = new VisualPresetLoader(plugin, logger).reload(current);

        assertSame(current, reloaded, "Reload should retain previous cache when the new file has no valid presets");
        verify(logger).error(contains("Keeping the previous preset cache"));
    }

    private VisualPresetLoader loader(final WrappedLogger logger) {
        return new VisualPresetLoader(null, logger);
    }

    private JavaPlugin pluginWithPresetFile(final String content) throws IOException {
        Files.writeString(tempDir.resolve("presets.yml"), content, StandardCharsets.UTF_8);
        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        return plugin;
    }

    private YamlConfiguration resourceYaml(final String path) {
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Resource should exist: " + path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private YamlConfiguration yaml(final String content) {
        final YamlConfiguration config = new YamlConfiguration();
        assertDoesNotThrow(() -> config.loadFromString(content));
        return config;
    }
}
