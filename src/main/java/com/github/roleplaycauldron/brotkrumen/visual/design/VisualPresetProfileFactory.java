package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.util.Collection;
import java.util.Map;

/**
 * Builds design profiles from graph-stored preset names and loaded presets.
 */
public final class VisualPresetProfileFactory {

    private VisualPresetProfileFactory() {
    }

    /**
     * Builds a profile for one graph.
     *
     * @param graph    graph
     * @param registry preset registry
     * @param settings renderer settings
     * @return design profile
     */
    public static GraphNetworkDesignProfile forGraph(final Graph graph, final VisualPresetRegistry registry,
                                                     final VisualizerRenderSettings settings) {
        return forGraphs(java.util.List.of(graph), registry, settings, Map.of(), Map.of());
    }

    /**
     * Builds a profile for multiple graphs.
     *
     * @param graphs                   graphs
     * @param registry                 preset registry
     * @param settings                 renderer settings
     * @param spellbookEffectOverrides temporary Spellbook effect preset overrides by graph id
     * @param blockDisplayOverrides    temporary block-display preset overrides by graph id
     * @return design profile
     */
    public static GraphNetworkDesignProfile forGraphs(final Collection<Graph> graphs,
                                                      final VisualPresetRegistry registry,
                                                      final VisualizerRenderSettings settings,
                                                      final Map<Integer, String> spellbookEffectOverrides,
                                                      final Map<Integer, String> blockDisplayOverrides) {
        final ParticleDesignSet defaultSpellbookEffect = resolveSpellbookEffect(registry,
                settings.defaultSpellbookEffectPreset(), settings);
        final BlockDisplayDesignSet defaultBlockDisplay = resolveBlockDisplay(registry,
                settings.defaultBlockDisplayPreset(), settings);
        final GraphNetworkDesignProfile.Builder builder = GraphNetworkDesignProfile.builder()
                .particleDefaultDesign(defaultSpellbookEffect)
                .particleNetworkDesign(defaultSpellbookEffect)
                .blockDisplayDefaultDesign(defaultBlockDisplay)
                .blockDisplayNetworkDesign(defaultBlockDisplay);

        for (final Graph graph : graphs) {
            final String spellbookEffectPreset = spellbookEffectOverrides.getOrDefault(graph.getGraphId(),
                    graph.getSpellbookEffectPreset());
            final String blockDisplayPreset = blockDisplayOverrides.getOrDefault(graph.getGraphId(),
                    graph.getBlockDisplayPreset());
            builder.particleGraphDesign(graph.getGraphId(), resolveSpellbookEffect(registry,
                    spellbookEffectPreset, settings));
            builder.blockDisplayGraphDesign(graph.getGraphId(), resolveBlockDisplay(registry,
                    blockDisplayPreset, settings));
        }
        return builder.build();
    }

    private static ParticleDesignSet resolveSpellbookEffect(final VisualPresetRegistry registry, final String preset,
                                                            final VisualizerRenderSettings settings) {
        return registry.resolve(preset, settings.defaultSpellbookEffectPreset(), VisualRenderer.SPELLBOOK_EFFECT)
                .flatMap(VisualPreset::spellbookEffectDesign)
                .orElseThrow(() -> new IllegalStateException("No spellbookEffect-compatible visual preset is available."));
    }

    private static BlockDisplayDesignSet resolveBlockDisplay(final VisualPresetRegistry registry, final String preset,
                                                             final VisualizerRenderSettings settings) {
        return registry.resolve(preset, settings.defaultBlockDisplayPreset(), VisualRenderer.BLOCK_DISPLAY)
                .flatMap(VisualPreset::blockDisplayDesign)
                .orElseThrow(() -> new IllegalStateException("No blockDisplay-compatible visual preset is available."));
    }
}
