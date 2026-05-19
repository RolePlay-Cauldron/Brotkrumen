package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;

/**
 * Design resolver backed by a network design profile.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
    public ParticleNodeDesign resolveParticleNode(final VisualNode node) {
        final ParticleNodeDesign override = profile.particleNodeOverrides().get(node.ref());
        if (override != null) {
            return override;
        }
        return particleDesignSetForGraph(node.ref().graphDbId()).nodeDesign(node.role());
    }

    @Override
    public ParticleEdgeDesign resolveParticleEdge(final VisualEdge edge) {
        final ParticleEdgeDesign override = profile.particleEdgeOverrides().get(edge.id());
        if (override != null) {
            return override;
        }

        if (edge.kind() == VisualEdgeKind.INTER_GRAPH) {
            return resolveInterGraphParticleEdge(edge);
        }

        final ParticleEdgeDesign roleDesign = particleDesignSetForGraph(edge.source().graphDbId())
                .edgeDesigns()
                .get(edge.role());
        if (roleDesign != null) {
            return roleDesign;
        }

        final ParticleEdgeDesign edgeKindDesign = profile.particleEdgeKindDesigns().get(edge.kind());
        if (edgeKindDesign != null) {
            return edgeKindDesign;
        }
        return particleDesignSetForGraph(edge.source().graphDbId()).edgeDesign(edge.role());
    }

    @Override
    public BlockNodeDesign resolveBlockNode(final VisualNode node) {
        final BlockNodeDesign override = profile.blockDisplayNodeOverrides().get(node.ref());
        if (override != null) {
            return override;
        }
        return blockDisplayDesignSetForGraph(node.ref().graphDbId()).nodeDesign(node.role());
    }

    @Override
    public BlockEdgeDesign resolveBlockEdge(final VisualEdge edge) {
        final BlockEdgeDesign override = profile.blockDisplayEdgeOverrides().get(edge.id());
        if (override != null) {
            return override;
        }

        if (edge.kind() == VisualEdgeKind.INTER_GRAPH) {
            return resolveInterGraphBlockEdge(edge);
        }

        final BlockEdgeDesign roleDesign = blockDisplayDesignSetForGraph(edge.source().graphDbId())
                .edgeDesigns()
                .get(edge.role());
        if (roleDesign != null) {
            return roleDesign;
        }

        final BlockEdgeDesign edgeKindDesign = profile.blockDisplayEdgeKindDesigns().get(edge.kind());
        if (edgeKindDesign != null) {
            return edgeKindDesign;
        }
        return blockDisplayDesignSetForGraph(edge.source().graphDbId()).edgeDesign(edge.role());
    }

    @Override
    public EdgeRenderStrategy resolveEdgeRenderStrategy(final VisualEdge edge) {
        return profile.edgeRenderStrategies().getOrDefault(edge.role(), EdgeRenderStrategy.FULL_EDGE);
    }

    private ParticleEdgeDesign resolveInterGraphParticleEdge(final VisualEdge edge) {
        final ParticleEdgeDesign edgeKindDesign = profile.particleEdgeKindDesigns().get(VisualEdgeKind.INTER_GRAPH);
        if (profile.interGraphStrategy() == InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH && edgeKindDesign != null) {
            return edgeKindDesign;
        }

        return switch (profile.interGraphStrategy()) {
            case SOURCE_GRAPH -> particleDesignSetForGraph(edge.source().graphDbId()).edgeDesign(edge.role());
            case TARGET_GRAPH -> particleDesignSetForGraph(edge.target().graphDbId()).edgeDesign(edge.role());
            case NETWORK_DEFAULT -> particleNetworkOrDefault().edgeDesign(edge.role());
            case EXPLICIT_INTER_GRAPH -> particleNetworkOrDefault().edgeDesign(edge.role());
        };
    }

    private BlockEdgeDesign resolveInterGraphBlockEdge(final VisualEdge edge) {
        final BlockEdgeDesign edgeKindDesign = profile.blockDisplayEdgeKindDesigns().get(VisualEdgeKind.INTER_GRAPH);
        if (profile.interGraphStrategy() == InterGraphEdgeDesignStrategy.EXPLICIT_INTER_GRAPH && edgeKindDesign != null) {
            return edgeKindDesign;
        }

        return switch (profile.interGraphStrategy()) {
            case SOURCE_GRAPH -> blockDisplayDesignSetForGraph(edge.source().graphDbId()).edgeDesign(edge.role());
            case TARGET_GRAPH -> blockDisplayDesignSetForGraph(edge.target().graphDbId()).edgeDesign(edge.role());
            case NETWORK_DEFAULT -> blockDisplayNetworkOrDefault().edgeDesign(edge.role());
            case EXPLICIT_INTER_GRAPH -> blockDisplayNetworkOrDefault().edgeDesign(edge.role());
        };
    }

    private ParticleDesignSet particleDesignSetForGraph(final int graphId) {
        final ParticleDesignSet graphDesign = profile.particleGraphDesigns().get(graphId);
        if (graphDesign != null) {
            return graphDesign;
        }
        return particleNetworkOrDefault();
    }

    private ParticleDesignSet particleNetworkOrDefault() {
        return profile.networkParticleDesign() != null ? profile.networkParticleDesign() : profile.defaultParticleDesign();
    }

    private BlockDisplayDesignSet blockDisplayDesignSetForGraph(final int graphId) {
        final BlockDisplayDesignSet graphDesign = profile.blockDisplayGraphDesigns().get(graphId);
        if (graphDesign != null) {
            return graphDesign;
        }
        return blockDisplayNetworkOrDefault();
    }

    private BlockDisplayDesignSet blockDisplayNetworkOrDefault() {
        return profile.networkBlockDisplayDesign() != null
                ? profile.networkBlockDisplayDesign()
                : profile.defaultBlockDisplayDesign();
    }
}
