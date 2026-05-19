package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.render.BlockDisplayGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;
import com.github.roleplaycauldron.brotkrumen.visual.render.ParticleGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GraphNetworkVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.PathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.SingleGraphVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.ViewerLocationSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Factory methods for source-based graph visualizers.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public final class GraphVisualizerFactory {

    private static final String GUIDED_PATH_CONFIG = "visualizer.guidedPath";

    private GraphVisualizerFactory() {
    }

    /**
     * Creates a block-display visualizer for one graph.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param graph         graph
     * @param viewerId      viewer id
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                    final Graph graph, final UUID viewerId) {
        return blockDisplayGraph(plugin, loggerFactory, graph, viewerId, ProfileGraphDesignResolver.defaults());
    }

    /**
     * Creates a block-display visualizer for one graph.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param graph         graph
     * @param viewerId      viewer id
     * @param designs       design resolver
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                    final Graph graph, final UUID viewerId,
                                                    final GraphDesignResolver designs) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new SingleGraphVisualSource(graph),
                new BlockDisplayGraphRenderer(plugin, viewerId),
                designs
        );
    }

    /**
     * Creates a block-display visualizer for a network.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param viewerId      viewer id
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayNetwork(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                      final GraphNetwork network, final UUID viewerId,
                                                      final GraphNetworkDesignProfile profile) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new GraphNetworkVisualSource(network),
                new BlockDisplayGraphRenderer(plugin, viewerId),
                new ProfileGraphDesignResolver(profile)
        );
    }

    /**
     * Creates a block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                          final GraphNetwork network, final List<NodeRef> path,
                                                          final UUID viewerId,
                                                          final GraphNetworkDesignProfile profile) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new PathVisualGraphSource(new GraphNetworkVisualSource(network), path),
                new BlockDisplayGraphRenderer(plugin, viewerId),
                new ProfileGraphDesignResolver(profile)
        );
    }

    /**
     * Creates a guided block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                                final GraphNetwork network, final List<NodeRef> path,
                                                                final UUID viewerId,
                                                                final GraphNetworkDesignProfile profile) {
        return blockDisplayGuidedNetworkPath(plugin, loggerFactory, network, path, viewerId, profile,
                guidedPathOptions(plugin));
    }

    /**
     * Creates a guided block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param profile       design profile
     * @param options       guided path options
     * @return visualizer
     */
    public static GraphVisualizer blockDisplayGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                                final GraphNetwork network, final List<NodeRef> path,
                                                                final UUID viewerId,
                                                                final GraphNetworkDesignProfile profile,
                                                                final GuidedPathOptions options) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new GuidedPathVisualGraphSource(new GraphNetworkVisualSource(network), path,
                        viewerLocationSource(plugin, viewerId), options),
                new BlockDisplayGraphRenderer(plugin, viewerId),
                new ProfileGraphDesignResolver(profile)
        );
    }

    /**
     * Creates a Spellbook particle visualizer for one graph.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param graph         graph
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @return visualizer
     */
    public static GraphVisualizer particleGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                final Graph graph, final UUID viewerId,
                                                final EffectExecutor executor) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new SingleGraphVisualSource(graph),
                new ParticleGraphRenderer(plugin, viewerId, executor),
                ProfileGraphDesignResolver.defaults()
        );
    }

    /**
     * Creates a Spellbook particle visualizer for a network.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer particleNetwork(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                  final GraphNetwork network, final UUID viewerId,
                                                  final EffectExecutor executor,
                                                  final GraphNetworkDesignProfile profile) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new GraphNetworkVisualSource(network),
                new ParticleGraphRenderer(plugin, viewerId, executor),
                new ProfileGraphDesignResolver(profile)
        );
    }

    /**
     * Creates a Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer particleNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                      final GraphNetwork network, final List<NodeRef> path,
                                                      final UUID viewerId, final EffectExecutor executor,
                                                      final GraphNetworkDesignProfile profile) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new PathVisualGraphSource(new GraphNetworkVisualSource(network), path),
                new ParticleGraphRenderer(plugin, viewerId, executor),
                new ProfileGraphDesignResolver(profile)
        );
    }

    /**
     * Creates a guided Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @return visualizer
     */
    public static GraphVisualizer particleGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                            final GraphNetwork network, final List<NodeRef> path,
                                                            final UUID viewerId, final EffectExecutor executor,
                                                            final GraphNetworkDesignProfile profile) {
        return particleGuidedNetworkPath(plugin, loggerFactory, network, path, viewerId, executor, profile,
                guidedPathOptions(plugin));
    }

    /**
     * Creates a guided Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param path          node-reference path
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @param options       guided path options
     * @return visualizer
     */
    public static GraphVisualizer particleGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                            final GraphNetwork network, final List<NodeRef> path,
                                                            final UUID viewerId, final EffectExecutor executor,
                                                            final GraphNetworkDesignProfile profile,
                                                            final GuidedPathOptions options) {
        return new PlayerGraphVisualizer(
                loggerFactory,
                new GuidedPathVisualGraphSource(new GraphNetworkVisualSource(network), path,
                        viewerLocationSource(plugin, viewerId), options),
                new ParticleGraphRenderer(plugin, viewerId, executor),
                guidedParticleResolver(profile)
        );
    }

    private static GraphDesignResolver guidedParticleResolver(final GraphNetworkDesignProfile profile) {
        final ProfileGraphDesignResolver delegate = new ProfileGraphDesignResolver(profile);
        return new GraphDesignResolver() {
            @Override
            public com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeDesign resolveParticleNode(
                    final VisualNode node) {
                return delegate.resolveParticleNode(node);
            }

            @Override
            public ParticleEdgeDesign resolveParticleEdge(final VisualEdge edge) {
                final ParticleEdgeDesign explicit = profile.particleEdgeOverrides().get(edge.id());
                if (explicit != null) {
                    return explicit;
                }
                return ParticleEdgeDesign.movingPoint(delegate.resolveParticleEdge(edge).particle(), 0.2f);
            }

            @Override
            public com.github.roleplaycauldron.brotkrumen.visual.design.BlockNodeDesign resolveBlockNode(
                    final VisualNode node) {
                return delegate.resolveBlockNode(node);
            }

            @Override
            public com.github.roleplaycauldron.brotkrumen.visual.design.BlockEdgeDesign resolveBlockEdge(
                    final VisualEdge edge) {
                return delegate.resolveBlockEdge(edge);
            }

            @Override
            public EdgeRenderStrategy resolveEdgeRenderStrategy(final VisualEdge edge) {
                return delegate.resolveEdgeRenderStrategy(edge);
            }
        };
    }

    private static GuidedPathOptions guidedPathOptions(final Brotkrumen plugin) {
        if (plugin == null) {
            return GuidedPathOptions.defaults();
        }
        return GuidedPathOptions.fromConfig(plugin.getConfig().getConfigurationSection(GUIDED_PATH_CONFIG));
    }

    private static ViewerLocationSource viewerLocationSource(final Brotkrumen plugin, final UUID viewerId) {
        if (plugin == null) {
            return () -> null;
        }
        return () -> {
            final Player player = plugin.getServer().getPlayer(viewerId);
            return player == null ? null : player.getLocation();
        };
    }
}
