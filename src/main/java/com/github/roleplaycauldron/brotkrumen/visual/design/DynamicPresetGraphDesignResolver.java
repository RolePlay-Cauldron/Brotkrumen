package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Graph design resolver backed by the current graph preset cache and graph metadata.
 */
public final class DynamicPresetGraphDesignResolver implements GraphDesignResolver {

    private final Supplier<Graph> activeGraph;

    private final Supplier<Collection<Graph>> referenceGraphs;

    private final Supplier<VisualPresetRegistry> presetRegistry;

    private final Supplier<VisualizerRenderSettings> renderSettings;

    private final Supplier<Map<Integer, String>> spellbookEffectOverrides;

    private final Supplier<Map<Integer, String>> blockDisplayOverrides;

    private final Map<VisualEdgeRole, EdgeRenderStrategy> renderStrategies;

    /**
     * Creates a dynamic resolver.
     *
     * @param activeGraph              active graph supplier
     * @param referenceGraphs          visible reference graph supplier
     * @param presetRegistry           preset registry supplier
     * @param renderSettings           render settings supplier
     * @param spellbookEffectOverrides temporary Spellbook effect overrides
     * @param blockDisplayOverrides    temporary block-display overrides
     */
    public DynamicPresetGraphDesignResolver(final Supplier<Graph> activeGraph,
                                            final Supplier<Collection<Graph>> referenceGraphs,
                                            final Supplier<VisualPresetRegistry> presetRegistry,
                                            final Supplier<VisualizerRenderSettings> renderSettings,
                                            final Supplier<Map<Integer, String>> spellbookEffectOverrides,
                                            final Supplier<Map<Integer, String>> blockDisplayOverrides) {
        this.activeGraph = activeGraph;
        this.referenceGraphs = referenceGraphs;
        this.presetRegistry = presetRegistry;
        this.renderSettings = renderSettings;
        this.spellbookEffectOverrides = spellbookEffectOverrides;
        this.blockDisplayOverrides = blockDisplayOverrides;
        this.renderStrategies = defaultEdgeRenderStrategies();
    }

    private static Map<VisualEdgeRole, EdgeRenderStrategy> defaultEdgeRenderStrategies() {
        final Map<VisualEdgeRole, EdgeRenderStrategy> result = new EnumMap<>(VisualEdgeRole.class);
        result.put(VisualEdgeRole.DEFAULT_LOCAL, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.DIRECTED_LOCAL, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.UNDIRECTED_LOCAL, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.BLOCKED, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.INTER_GRAPH, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.DIRECTED_INTER_GRAPH, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, EdgeRenderStrategy.FULL_EDGE);
        result.put(VisualEdgeRole.TELEPORT, EdgeRenderStrategy.ENDPOINTS_ONLY);
        return result;
    }

    @Override
    public ParticleNodeDesign resolveParticleNode(final VisualNode node) {
        return spellbookEffectDesign(graphFor(node.ref().graphDbId())).nodeDesign(node.role());
    }

    @Override
    public ParticleEdgeDesign resolveParticleEdge(final VisualEdge edge) {
        return spellbookEffectDesign(graphFor(edge.source().graphDbId())).edgeDesign(edge.role());
    }

    @Override
    public BlockNodeDesign resolveBlockNode(final VisualNode node) {
        return blockDisplayDesign(graphFor(node.ref().graphDbId())).nodeDesign(node.role());
    }

    @Override
    public BlockEdgeDesign resolveBlockEdge(final VisualEdge edge) {
        return blockDisplayDesign(graphFor(edge.source().graphDbId())).edgeDesign(edge.role());
    }

    @Override
    public EdgeRenderStrategy resolveEdgeRenderStrategy(final VisualEdge edge) {
        return renderStrategies.getOrDefault(edge.role(), EdgeRenderStrategy.FULL_EDGE);
    }

    private ParticleDesignSet spellbookEffectDesign(final Graph graph) {
        final VisualizerRenderSettings settings = renderSettings.get();
        final String graphPreset = graph == null ? null : graph.getSpellbookEffectPreset();
        final String preset = graph == null ? graphPreset
                : spellbookEffectOverrides.get().getOrDefault(graph.getGraphId(), graphPreset);
        return presetRegistry.get().resolve(preset, settings.defaultSpellbookEffectPreset(),
                        VisualRenderer.SPELLBOOK_EFFECT)
                .flatMap(VisualPreset::spellbookEffectDesign)
                .orElseThrow(() -> new IllegalStateException("No spellbookEffect-compatible visual preset is available."));
    }

    private BlockDisplayDesignSet blockDisplayDesign(final Graph graph) {
        final VisualizerRenderSettings settings = renderSettings.get();
        final String graphPreset = graph == null ? null : graph.getBlockDisplayPreset();
        final String preset = graph == null ? graphPreset
                : blockDisplayOverrides.get().getOrDefault(graph.getGraphId(), graphPreset);
        return presetRegistry.get().resolve(preset, settings.defaultBlockDisplayPreset(), VisualRenderer.BLOCK_DISPLAY)
                .flatMap(VisualPreset::blockDisplayDesign)
                .orElseThrow(() -> new IllegalStateException("No blockDisplay-compatible visual preset is available."));
    }

    private Graph graphFor(final int graphId) {
        final Graph active = activeGraph.get();
        if (active != null && active.getGraphId() == graphId) {
            return active;
        }
        final Collection<Graph> references = referenceGraphs.get();
        return Stream.concat(active == null ? Stream.empty() : Stream.of(active),
                        references == null ? Stream.empty() : references.stream())
                .filter(graph -> graph.getGraphId() == graphId)
                .findFirst()
                .orElse(null);
    }
}
