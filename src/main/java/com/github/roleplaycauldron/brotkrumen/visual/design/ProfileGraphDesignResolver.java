package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;

/**
 * Design resolver backed by a network design profile.
 */
public class ProfileGraphDesignResolver implements GraphDesignResolver {

    private final GraphNetworkDesignProfile profile;

    /**
     * Creates a resolver.
     *
     * @param profile design profile
     */
    public ProfileGraphDesignResolver(final GraphNetworkDesignProfile profile) {
        this.profile = profile;
    }

    /**
     * Creates a resolver using the default profile.
     *
     * @return default resolver
     */
    public static ProfileGraphDesignResolver defaults() {
        return new ProfileGraphDesignResolver(GraphNetworkDesignProfile.defaults());
    }

    @Override
    public NodeDesign resolveNode(final VisualNode node) {
        final NodeDesign override = profile.nodeOverrides().get(node.ref());
        if (override != null) {
            return override;
        }
        return designSetForGraph(node.ref().graphDbId()).node();
    }

    @Override
    public EdgeDesign resolveEdge(final VisualEdge edge) {
        final EdgeDesign override = profile.edgeOverrides().get(edge.id());
        if (override != null) {
            return override;
        }

        if (edge.kind() == VisualEdgeKind.INTER_GRAPH) {
            return resolveInterGraphEdge(edge);
        }

        final EdgeDesign edgeKindDesign = profile.edgeKindDesigns().get(edge.kind());
        if (edgeKindDesign != null) {
            return edgeKindDesign;
        }
        return designSetForGraph(edge.source().graphDbId()).localEdge();
    }

    private EdgeDesign resolveInterGraphEdge(final VisualEdge edge) {
        final EdgeDesign edgeKindDesign = profile.edgeKindDesigns().get(VisualEdgeKind.INTER_GRAPH);
        if (profile.interGraphStrategy() == InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH && edgeKindDesign != null) {
            return edgeKindDesign;
        }

        return switch (profile.interGraphStrategy()) {
            case SOURCE_GRAPH -> designSetForGraph(edge.source().graphDbId()).interGraphEdge();
            case TARGET_GRAPH -> designSetForGraph(edge.target().graphDbId()).interGraphEdge();
            case NETWORK_DEFAULT -> networkOrDefault().interGraphEdge();
            case EXPLICIT_INTER_GRAPH -> networkOrDefault().interGraphEdge();
        };
    }

    private DesignSet designSetForGraph(final int graphId) {
        final DesignSet graphDesign = profile.graphDesigns().get(graphId);
        if (graphDesign != null) {
            return graphDesign;
        }
        return networkOrDefault();
    }

    private DesignSet networkOrDefault() {
        return profile.networkDesign() != null ? profile.networkDesign() : profile.defaultDesign();
    }
}
