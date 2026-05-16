package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Design profile for graph and graph-network visualization.
 */
@SuppressWarnings({"PMD.DataClass", "PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal", "PMD.AvoidFieldNameMatchingMethodName"})
public class GraphNetworkDesignProfile {

    private final DesignSet defaultDesign;

    private final DesignSet networkDesign;

    private final Map<Integer, DesignSet> graphDesigns;

    private final Map<VisualEdgeKind, EdgeDesign> edgeKindDesigns;

    private final Map<NodeRef, NodeDesign> nodeOverrides;

    private final Map<VisualEdgeId, EdgeDesign> edgeOverrides;

    private final InterGraphEdgeDesignStrategy interGraphStrategy;

    /**
     * Creates a profile.
     *
     * @param builder builder
     */
    private GraphNetworkDesignProfile(final Builder builder) {
        this.defaultDesign = builder.defaultDesign;
        this.networkDesign = builder.networkDesign;
        this.graphDesigns = Map.copyOf(builder.graphDesigns);
        this.edgeKindDesigns = Map.copyOf(builder.edgeKindDesigns);
        this.nodeOverrides = Map.copyOf(builder.nodeOverrides);
        this.edgeOverrides = Map.copyOf(builder.edgeOverrides);
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

    public DesignSet defaultDesign() {
        return defaultDesign;
    }

    public DesignSet networkDesign() {
        return networkDesign;
    }

    public Map<Integer, DesignSet> graphDesigns() {
        return graphDesigns;
    }

    public Map<VisualEdgeKind, EdgeDesign> edgeKindDesigns() {
        return edgeKindDesigns;
    }

    public Map<NodeRef, NodeDesign> nodeOverrides() {
        return nodeOverrides;
    }

    public Map<VisualEdgeId, EdgeDesign> edgeOverrides() {
        return edgeOverrides;
    }

    public InterGraphEdgeDesignStrategy interGraphStrategy() {
        return interGraphStrategy;
    }

    /**
     * Builder for design profiles.
     */
    @SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidFieldNameMatchingMethodName"})
    public static class Builder {

        private final Map<Integer, DesignSet> graphDesigns = new HashMap<>();

        private final Map<VisualEdgeKind, EdgeDesign> edgeKindDesigns = new EnumMap<>(VisualEdgeKind.class);

        private final Map<NodeRef, NodeDesign> nodeOverrides = new HashMap<>();

        private final Map<VisualEdgeId, EdgeDesign> edgeOverrides = new HashMap<>();

        private DesignSet defaultDesign = DesignSet.defaults();

        private DesignSet networkDesign;

        private InterGraphEdgeDesignStrategy interGraphStrategy = InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH;

        public Builder defaultDesign(final DesignSet design) {
            this.defaultDesign = design;
            return this;
        }

        public Builder networkDesign(final DesignSet design) {
            this.networkDesign = design;
            return this;
        }

        public Builder graphDesign(final int graphId, final DesignSet design) {
            this.graphDesigns.put(graphId, design);
            return this;
        }

        public Builder edgeKindDesign(final VisualEdgeKind kind, final EdgeDesign design) {
            this.edgeKindDesigns.put(kind, design);
            return this;
        }

        public Builder nodeOverride(final NodeRef nodeRef, final NodeDesign design) {
            this.nodeOverrides.put(nodeRef, design);
            return this;
        }

        public Builder edgeOverride(final VisualEdgeId edgeId, final EdgeDesign design) {
            this.edgeOverrides.put(edgeId, design);
            return this;
        }

        public Builder interGraphStrategy(final InterGraphEdgeDesignStrategy strategy) {
            this.interGraphStrategy = strategy;
            return this;
        }

        public GraphNetworkDesignProfile build() {
            return new GraphNetworkDesignProfile(this);
        }
    }
}
