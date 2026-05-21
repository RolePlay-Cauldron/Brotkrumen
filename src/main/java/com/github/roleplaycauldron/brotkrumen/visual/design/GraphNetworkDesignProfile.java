package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer-specific design profile for graph and graph-network visualization.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
public final class GraphNetworkDesignProfile {

    private final ParticleDesignSet defaultParticleDesignSet;

    private final ParticleDesignSet networkParticleDesignSet;

    private final BlockDisplayDesignSet defaultBlockDisplayDesignSet;

    private final BlockDisplayDesignSet networkBlockDisplayDesignSet;

    private final Map<Integer, ParticleDesignSet> particleDesignSetsByGraph;

    private final Map<Integer, BlockDisplayDesignSet> blockDisplayDesignSetsByGraph;

    private final Map<VisualEdgeKind, ParticleEdgeDesign> particleDesignsByEdgeKind;

    private final Map<NodeRef, ParticleNodeDesign> particleNodeDesignOverrides;

    private final Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeDesignOverrides;

    private final Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayDesignsByEdgeKind;

    private final Map<NodeRef, BlockNodeDesign> blockDisplayNodeDesignOverrides;

    private final Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeDesignOverrides;

    private final Map<VisualEdgeRole, EdgeRenderStrategy> renderStrategiesByRole;

    private final InterGraphEdgeDesignStrategy interGraphDesignStrategy;

    /**
     * Creates a profile.
     *
     * @param builder builder
     */
    private GraphNetworkDesignProfile(final Builder builder) {
        this.defaultParticleDesignSet = builder.defaultParticleDesignSet;
        this.networkParticleDesignSet = builder.networkParticleDesignSet;
        this.defaultBlockDisplayDesignSet = builder.defaultBlockDisplayDesignSet;
        this.networkBlockDisplayDesignSet = builder.networkBlockDisplayDesignSet;
        this.particleDesignSetsByGraph = Map.copyOf(builder.particleDesignSetsByGraph);
        this.blockDisplayDesignSetsByGraph = Map.copyOf(builder.blockDisplayDesignSetsByGraph);
        this.particleDesignsByEdgeKind = Map.copyOf(builder.particleDesignsByEdgeKind);
        this.particleNodeDesignOverrides = Map.copyOf(builder.particleNodeDesignOverrides);
        this.particleEdgeDesignOverrides = Map.copyOf(builder.particleEdgeDesignOverrides);
        this.blockDisplayDesignsByEdgeKind = Map.copyOf(builder.blockDisplayDesignsByEdgeKind);
        this.blockDisplayNodeDesignOverrides = Map.copyOf(builder.blockDisplayNodeDesignOverrides);
        this.blockDisplayEdgeDesignOverrides = Map.copyOf(builder.blockDisplayEdgeDesignOverrides);
        this.renderStrategiesByRole = Map.copyOf(builder.renderStrategiesByRole);
        this.interGraphDesignStrategy = builder.interGraphDesignStrategy;
    }

    /**
     * Creates a builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default profile.
     *
     * @return default profile
     */
    public static GraphNetworkDesignProfile defaults() {
        return builder().build();
    }

    /**
     * Returns the default particle design.
     *
     * @return default particle design
     */
    public ParticleDesignSet defaultParticleDesign() {
        return defaultParticleDesignSet;
    }

    /**
     * Returns the optional network-level particle design.
     *
     * @return network particle design or {@code null}
     */
    public ParticleDesignSet networkParticleDesign() {
        return networkParticleDesignSet;
    }

    /**
     * Returns the default block-display design.
     *
     * @return default block-display design
     */
    public BlockDisplayDesignSet defaultBlockDisplayDesign() {
        return defaultBlockDisplayDesignSet;
    }

    /**
     * Returns the optional network-level block-display design.
     *
     * @return network block-display design or {@code null}
     */
    public BlockDisplayDesignSet networkBlockDisplayDesign() {
        return networkBlockDisplayDesignSet;
    }

    /**
     * Returns graph-specific particle designs.
     *
     * @return particle designs by graph id
     */
    public Map<Integer, ParticleDesignSet> particleGraphDesigns() {
        return particleDesignSetsByGraph;
    }

    /**
     * Returns graph-specific block-display designs.
     *
     * @return block-display designs by graph id
     */
    public Map<Integer, BlockDisplayDesignSet> blockDisplayGraphDesigns() {
        return blockDisplayDesignSetsByGraph;
    }

    /**
     * Returns particle edge-kind overrides.
     *
     * @return particle edge-kind design map
     */
    public Map<VisualEdgeKind, ParticleEdgeDesign> particleEdgeKindDesigns() {
        return particleDesignsByEdgeKind;
    }

    /**
     * Returns particle node overrides.
     *
     * @return particle node override map
     */
    public Map<NodeRef, ParticleNodeDesign> particleNodeOverrides() {
        return particleNodeDesignOverrides;
    }

    /**
     * Returns particle edge overrides.
     *
     * @return particle edge override map
     */
    public Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeOverrides() {
        return particleEdgeDesignOverrides;
    }

    /**
     * Returns block-display edge-kind overrides.
     *
     * @return block-display edge-kind design map
     */
    public Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayEdgeKindDesigns() {
        return blockDisplayDesignsByEdgeKind;
    }

    /**
     * Returns block-display node overrides.
     *
     * @return block-display node override map
     */
    public Map<NodeRef, BlockNodeDesign> blockDisplayNodeOverrides() {
        return blockDisplayNodeDesignOverrides;
    }

    /**
     * Returns block-display edge overrides.
     *
     * @return block-display edge override map
     */
    public Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeOverrides() {
        return blockDisplayEdgeDesignOverrides;
    }

    /**
     * Returns edge render strategies by visual role.
     *
     * @return render strategies by role
     */
    public Map<VisualEdgeRole, EdgeRenderStrategy> edgeRenderStrategies() {
        return renderStrategiesByRole;
    }

    /**
     * Returns the intergraph edge design strategy.
     *
     * @return intergraph edge design strategy
     */
    public InterGraphEdgeDesignStrategy interGraphStrategy() {
        return interGraphDesignStrategy;
    }

    /**
     * Builder for renderer-specific design profiles.
     */
    @SuppressWarnings("PMD.TooManyMethods")
    public static final class Builder {

        private final Map<Integer, ParticleDesignSet> particleDesignSetsByGraph = new HashMap<>();

        private final Map<Integer, BlockDisplayDesignSet> blockDisplayDesignSetsByGraph = new HashMap<>();

        private final Map<VisualEdgeKind, ParticleEdgeDesign> particleDesignsByEdgeKind =
                new EnumMap<>(VisualEdgeKind.class);

        private final Map<NodeRef, ParticleNodeDesign> particleNodeDesignOverrides = new HashMap<>();

        private final Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeDesignOverrides = new HashMap<>();

        private final Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayDesignsByEdgeKind =
                new EnumMap<>(VisualEdgeKind.class);

        private final Map<NodeRef, BlockNodeDesign> blockDisplayNodeDesignOverrides = new HashMap<>();

        private final Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeDesignOverrides = new HashMap<>();

        private final Map<VisualEdgeRole, EdgeRenderStrategy> renderStrategiesByRole = defaultEdgeRenderStrategies();

        private ParticleDesignSet defaultParticleDesignSet = ParticleDesignSet.defaults();

        private ParticleDesignSet networkParticleDesignSet;

        private BlockDisplayDesignSet defaultBlockDisplayDesignSet = BlockDisplayDesignSet.defaults();

        private BlockDisplayDesignSet networkBlockDisplayDesignSet;

        private InterGraphEdgeDesignStrategy interGraphDesignStrategy = InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH;

        /**
         * Creates a builder.
         */
        private Builder() {
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

        /**
         * Sets the default particle design.
         *
         * @param design particle design
         * @return this builder
         */
        public Builder particleDefaultDesign(final ParticleDesignSet design) {
            this.defaultParticleDesignSet = design;
            return this;
        }

        /**
         * Sets the network particle design.
         *
         * @param design particle design
         * @return this builder
         */
        public Builder particleNetworkDesign(final ParticleDesignSet design) {
            this.networkParticleDesignSet = design;
            return this;
        }

        /**
         * Sets a graph-specific particle design.
         *
         * @param graphId graph id
         * @param design  particle design
         * @return this builder
         */
        public Builder particleGraphDesign(final int graphId, final ParticleDesignSet design) {
            this.particleDesignSetsByGraph.put(graphId, design);
            return this;
        }

        /**
         * Sets the default block-display design.
         *
         * @param design block-display design
         * @return this builder
         */
        public Builder blockDisplayDefaultDesign(final BlockDisplayDesignSet design) {
            this.defaultBlockDisplayDesignSet = design;
            return this;
        }

        /**
         * Sets the network block-display design.
         *
         * @param design block-display design
         * @return this builder
         */
        public Builder blockDisplayNetworkDesign(final BlockDisplayDesignSet design) {
            this.networkBlockDisplayDesignSet = design;
            return this;
        }

        /**
         * Sets a graph-specific block-display design.
         *
         * @param graphId graph id
         * @param design  block-display design
         * @return this builder
         */
        public Builder blockDisplayGraphDesign(final int graphId, final BlockDisplayDesignSet design) {
            this.blockDisplayDesignSetsByGraph.put(graphId, design);
            return this;
        }

        /**
         * Sets a particle edge-kind design.
         *
         * @param kind   edge kind
         * @param design particle edge design
         * @return this builder
         */
        public Builder particleEdgeKindDesign(final VisualEdgeKind kind, final ParticleEdgeDesign design) {
            this.particleDesignsByEdgeKind.put(kind, design);
            return this;
        }

        /**
         * Sets a particle node override.
         *
         * @param nodeRef node reference
         * @param design  particle node design
         * @return this builder
         */
        public Builder particleNodeOverride(final NodeRef nodeRef, final ParticleNodeDesign design) {
            this.particleNodeDesignOverrides.put(nodeRef, design);
            return this;
        }

        /**
         * Sets a particle edge override.
         *
         * @param edgeId edge id
         * @param design particle edge design
         * @return this builder
         */
        public Builder particleEdgeOverride(final VisualEdgeId edgeId, final ParticleEdgeDesign design) {
            this.particleEdgeDesignOverrides.put(edgeId, design);
            return this;
        }

        /**
         * Sets a block-display edge-kind design.
         *
         * @param kind   edge kind
         * @param design block-display edge design
         * @return this builder
         */
        public Builder blockDisplayEdgeKindDesign(final VisualEdgeKind kind, final BlockEdgeDesign design) {
            this.blockDisplayDesignsByEdgeKind.put(kind, design);
            return this;
        }

        /**
         * Sets a block-display node override.
         *
         * @param nodeRef node reference
         * @param design  block-display node design
         * @return this builder
         */
        public Builder blockDisplayNodeOverride(final NodeRef nodeRef, final BlockNodeDesign design) {
            this.blockDisplayNodeDesignOverrides.put(nodeRef, design);
            return this;
        }

        /**
         * Sets a block-display edge override.
         *
         * @param edgeId edge id
         * @param design block-display edge design
         * @return this builder
         */
        public Builder blockDisplayEdgeOverride(final VisualEdgeId edgeId, final BlockEdgeDesign design) {
            this.blockDisplayEdgeDesignOverrides.put(edgeId, design);
            return this;
        }

        /**
         * Sets the intergraph edge design strategy.
         *
         * @param strategy intergraph edge design strategy
         * @return this builder
         */
        public Builder interGraphStrategy(final InterGraphEdgeDesignStrategy strategy) {
            this.interGraphDesignStrategy = strategy;
            return this;
        }

        /**
         * Builds an immutable design profile.
         *
         * @return design profile
         */
        public GraphNetworkDesignProfile build() {
            return new GraphNetworkDesignProfile(this);
        }

        /**
         * Sets the render strategy for a visual edge role.
         *
         * @param role     visual edge role
         * @param strategy render strategy
         * @return this builder
         */
        public Builder edgeRenderStrategy(final VisualEdgeRole role, final EdgeRenderStrategy strategy) {
            this.renderStrategiesByRole.put(role, strategy);
            return this;
        }
    }
}
