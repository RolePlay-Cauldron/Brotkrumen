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
@SuppressWarnings({"PMD.DataClass", "PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal",
        "PMD.AvoidFieldNameMatchingMethodName", "PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
public class GraphNetworkDesignProfile {

    private final ParticleDesignSet defaultParticleDesign;

    private final ParticleDesignSet networkParticleDesign;

    private final BlockDisplayDesignSet defaultBlockDisplayDesign;

    private final BlockDisplayDesignSet networkBlockDisplayDesign;

    private final Map<Integer, ParticleDesignSet> particleGraphDesigns;

    private final Map<Integer, BlockDisplayDesignSet> blockDisplayGraphDesigns;

    private final Map<VisualEdgeKind, ParticleEdgeDesign> particleEdgeKindDesigns;

    private final Map<NodeRef, ParticleNodeDesign> particleNodeOverrides;

    private final Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeOverrides;

    private final Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayEdgeKindDesigns;

    private final Map<NodeRef, BlockNodeDesign> blockDisplayNodeOverrides;

    private final Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeOverrides;

    private final Map<VisualEdgeRole, EdgeRenderStrategy> edgeRenderStrategies;

    private final InterGraphEdgeDesignStrategy interGraphStrategy;

    /**
     * Creates a profile.
     *
     * @param builder builder
     */
    private GraphNetworkDesignProfile(final Builder builder) {
        this.defaultParticleDesign = builder.defaultParticleDesign;
        this.networkParticleDesign = builder.networkParticleDesign;
        this.defaultBlockDisplayDesign = builder.defaultBlockDisplayDesign;
        this.networkBlockDisplayDesign = builder.networkBlockDisplayDesign;
        this.particleGraphDesigns = Map.copyOf(builder.particleGraphDesigns);
        this.blockDisplayGraphDesigns = Map.copyOf(builder.blockDisplayGraphDesigns);
        this.particleEdgeKindDesigns = Map.copyOf(builder.particleEdgeKindDesigns);
        this.particleNodeOverrides = Map.copyOf(builder.particleNodeOverrides);
        this.particleEdgeOverrides = Map.copyOf(builder.particleEdgeOverrides);
        this.blockDisplayEdgeKindDesigns = Map.copyOf(builder.blockDisplayEdgeKindDesigns);
        this.blockDisplayNodeOverrides = Map.copyOf(builder.blockDisplayNodeOverrides);
        this.blockDisplayEdgeOverrides = Map.copyOf(builder.blockDisplayEdgeOverrides);
        this.edgeRenderStrategies = Map.copyOf(builder.edgeRenderStrategies);
        this.interGraphStrategy = builder.interGraphStrategy;
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

    public ParticleDesignSet defaultParticleDesign() {
        return defaultParticleDesign;
    }

    public ParticleDesignSet networkParticleDesign() {
        return networkParticleDesign;
    }

    public BlockDisplayDesignSet defaultBlockDisplayDesign() {
        return defaultBlockDisplayDesign;
    }

    public BlockDisplayDesignSet networkBlockDisplayDesign() {
        return networkBlockDisplayDesign;
    }

    public Map<Integer, ParticleDesignSet> particleGraphDesigns() {
        return particleGraphDesigns;
    }

    public Map<Integer, BlockDisplayDesignSet> blockDisplayGraphDesigns() {
        return blockDisplayGraphDesigns;
    }

    public Map<VisualEdgeKind, ParticleEdgeDesign> particleEdgeKindDesigns() {
        return particleEdgeKindDesigns;
    }

    public Map<NodeRef, ParticleNodeDesign> particleNodeOverrides() {
        return particleNodeOverrides;
    }

    public Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeOverrides() {
        return particleEdgeOverrides;
    }

    public Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayEdgeKindDesigns() {
        return blockDisplayEdgeKindDesigns;
    }

    public Map<NodeRef, BlockNodeDesign> blockDisplayNodeOverrides() {
        return blockDisplayNodeOverrides;
    }

    public Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeOverrides() {
        return blockDisplayEdgeOverrides;
    }

    public Map<VisualEdgeRole, EdgeRenderStrategy> edgeRenderStrategies() {
        return edgeRenderStrategies;
    }

    public InterGraphEdgeDesignStrategy interGraphStrategy() {
        return interGraphStrategy;
    }

    /**
     * Builder for renderer-specific design profiles.
     */
    @SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidFieldNameMatchingMethodName", "PMD.TooManyMethods"})
    public static class Builder {

        private final Map<Integer, ParticleDesignSet> particleGraphDesigns = new HashMap<>();

        private final Map<Integer, BlockDisplayDesignSet> blockDisplayGraphDesigns = new HashMap<>();

        private final Map<VisualEdgeKind, ParticleEdgeDesign> particleEdgeKindDesigns =
                new EnumMap<>(VisualEdgeKind.class);

        private final Map<NodeRef, ParticleNodeDesign> particleNodeOverrides = new HashMap<>();

        private final Map<VisualEdgeId, ParticleEdgeDesign> particleEdgeOverrides = new HashMap<>();

        private final Map<VisualEdgeKind, BlockEdgeDesign> blockDisplayEdgeKindDesigns =
                new EnumMap<>(VisualEdgeKind.class);

        private final Map<NodeRef, BlockNodeDesign> blockDisplayNodeOverrides = new HashMap<>();

        private final Map<VisualEdgeId, BlockEdgeDesign> blockDisplayEdgeOverrides = new HashMap<>();

        private final Map<VisualEdgeRole, EdgeRenderStrategy> edgeRenderStrategies = defaultEdgeRenderStrategies();

        private ParticleDesignSet defaultParticleDesign = ParticleDesignSet.defaults();

        private ParticleDesignSet networkParticleDesign;

        private BlockDisplayDesignSet defaultBlockDisplayDesign = BlockDisplayDesignSet.defaults();

        private BlockDisplayDesignSet networkBlockDisplayDesign;

        private InterGraphEdgeDesignStrategy interGraphStrategy = InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH;

        private static Map<VisualEdgeRole, EdgeRenderStrategy> defaultEdgeRenderStrategies() {
            final Map<VisualEdgeRole, EdgeRenderStrategy> result = new EnumMap<>(VisualEdgeRole.class);
            result.put(VisualEdgeRole.DEFAULT_LOCAL, EdgeRenderStrategy.FULL_EDGE);
            result.put(VisualEdgeRole.INTER_GRAPH, EdgeRenderStrategy.FULL_EDGE);
            result.put(VisualEdgeRole.TELEPORT, EdgeRenderStrategy.ENDPOINTS_ONLY);
            result.put(VisualEdgeRole.GLOBAL_TELEPORT, EdgeRenderStrategy.ENDPOINTS_ONLY);
            return result;
        }

        public Builder particleDefaultDesign(final ParticleDesignSet design) {
            this.defaultParticleDesign = design;
            return this;
        }

        public Builder particleNetworkDesign(final ParticleDesignSet design) {
            this.networkParticleDesign = design;
            return this;
        }

        public Builder particleGraphDesign(final int graphId, final ParticleDesignSet design) {
            this.particleGraphDesigns.put(graphId, design);
            return this;
        }

        public Builder blockDisplayDefaultDesign(final BlockDisplayDesignSet design) {
            this.defaultBlockDisplayDesign = design;
            return this;
        }

        public Builder blockDisplayNetworkDesign(final BlockDisplayDesignSet design) {
            this.networkBlockDisplayDesign = design;
            return this;
        }

        public Builder blockDisplayGraphDesign(final int graphId, final BlockDisplayDesignSet design) {
            this.blockDisplayGraphDesigns.put(graphId, design);
            return this;
        }

        public Builder particleEdgeKindDesign(final VisualEdgeKind kind, final ParticleEdgeDesign design) {
            this.particleEdgeKindDesigns.put(kind, design);
            return this;
        }

        public Builder particleNodeOverride(final NodeRef nodeRef, final ParticleNodeDesign design) {
            this.particleNodeOverrides.put(nodeRef, design);
            return this;
        }

        public Builder particleEdgeOverride(final VisualEdgeId edgeId, final ParticleEdgeDesign design) {
            this.particleEdgeOverrides.put(edgeId, design);
            return this;
        }

        public Builder blockDisplayEdgeKindDesign(final VisualEdgeKind kind, final BlockEdgeDesign design) {
            this.blockDisplayEdgeKindDesigns.put(kind, design);
            return this;
        }

        public Builder blockDisplayNodeOverride(final NodeRef nodeRef, final BlockNodeDesign design) {
            this.blockDisplayNodeOverrides.put(nodeRef, design);
            return this;
        }

        public Builder blockDisplayEdgeOverride(final VisualEdgeId edgeId, final BlockEdgeDesign design) {
            this.blockDisplayEdgeOverrides.put(edgeId, design);
            return this;
        }

        public Builder interGraphStrategy(final InterGraphEdgeDesignStrategy strategy) {
            this.interGraphStrategy = strategy;
            return this;
        }

        public GraphNetworkDesignProfile build() {
            return new GraphNetworkDesignProfile(this);
        }

        public Builder edgeRenderStrategy(final VisualEdgeRole role, final EdgeRenderStrategy strategy) {
            this.edgeRenderStrategies.put(role, strategy);
            return this;
        }
    }
}
