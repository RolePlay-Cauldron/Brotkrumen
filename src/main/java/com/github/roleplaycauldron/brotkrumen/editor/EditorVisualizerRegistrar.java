package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.brotkrumen.visual.design.DynamicPresetGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualizerRenderSettings;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Registers and refreshes editor visualizers for active editor sessions.
 */
final class EditorVisualizerRegistrar {

    private final VisualizerRegistry visualizerRegistry;

    private final Brotkrumen plugin;

    private final LoggerFactory loggerFactory;

    private final EffectExecutor effectExecutor;

    EditorVisualizerRegistrar(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin,
                              final LoggerFactory loggerFactory, final EffectExecutor effectExecutor) {
        this.visualizerRegistry = visualizerRegistry;
        this.plugin = plugin;
        this.loggerFactory = loggerFactory;
        this.effectExecutor = effectExecutor;
    }

    void register(final UUID playerId, final Supplier<Graph> activeGraph,
                  final Supplier<Collection<Graph>> referenceGraphs,
                  final Supplier<Collection<InterGraphEdge>> interGraphEdges,
                  final Supplier<Long> workspaceVersion, final Supplier<String> preset) {
        if (visualizerRegistry == null || plugin == null) {
            return;
        }
        final VisualizerRenderSettings renderSettings = VisualizerRenderSettings.fromConfig(plugin.getConfig());
        final DynamicPresetGraphDesignResolver resolver = new DynamicPresetGraphDesignResolver(
                activeGraph,
                referenceGraphs,
                plugin::getVisualPresetRegistry,
                () -> VisualizerRenderSettings.fromConfig(plugin.getConfig()),
                () -> temporaryPresetOverride(activeGraph.get(), preset.get(), VisualRenderer.SPELLBOOK_EFFECT),
                () -> temporaryPresetOverride(activeGraph.get(), preset.get(), VisualRenderer.BLOCK_DISPLAY));
        if (renderSettings.defaultRenderer() == VisualRenderer.BLOCK_DISPLAY) {
            visualizerRegistry.register(playerId,
                    GraphVisualizerFactory.blockDisplayEditorWorkspace(plugin, loggerFactory, activeGraph,
                            referenceGraphs, interGraphEdges, workspaceVersion, playerId, resolver));
            return;
        }
        visualizerRegistry.register(playerId,
                GraphVisualizerFactory.particleEditorWorkspace(plugin, loggerFactory, activeGraph, referenceGraphs,
                        interGraphEdges, workspaceVersion, playerId, effectExecutor, resolver));
    }

    void unregister(final UUID playerId) {
        if (visualizerRegistry != null) {
            visualizerRegistry.unregister(playerId);
        }
    }

    void refresh(final UUID playerId) {
        if (visualizerRegistry != null) {
            visualizerRegistry.refresh(playerId);
        }
    }

    private Map<Integer, String> temporaryPresetOverride(final Graph graph, final String preset,
                                                         final VisualRenderer renderer) {
        if (graph == null || preset == null || preset.isBlank() || plugin == null) {
            return Map.of();
        }
        final VisualRenderer activeRenderer = VisualizerRenderSettings.fromConfig(plugin.getConfig()).defaultRenderer();
        return activeRenderer == renderer ? Map.of(graph.getGraphId(), preset) : Map.of();
    }
}
