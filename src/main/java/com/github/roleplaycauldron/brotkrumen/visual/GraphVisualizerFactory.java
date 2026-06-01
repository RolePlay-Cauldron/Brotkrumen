package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.BlockDisplayGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.render.ParticleGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.EditorWorkspaceVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GraphNetworkVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.PathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.SingleGraphVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.ViewerLocationSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Factory methods for source-based graph visualizers.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
    public static Visualizer blockDisplayGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
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
    public static Visualizer blockDisplayGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                               final Graph graph, final UUID viewerId,
                                               final GraphDesignResolver designs) {
        return visualizer(loggerFactory, graphSource(graph), blockDisplayRenderer(plugin, viewerId), designs);
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
    public static Visualizer blockDisplayNetwork(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                 final GraphNetwork network, final UUID viewerId,
                                                 final GraphNetworkDesignProfile profile) {
        return visualizer(loggerFactory, networkSource(network), blockDisplayRenderer(plugin, viewerId),
                resolver(profile));
    }

    /**
     * Creates a block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param profile       design profile
     * @return visualizer
     */
    public static Visualizer blockDisplayNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                     final GraphNetwork network, final PathResult pathResult,
                                                     final UUID viewerId,
                                                     final GraphNetworkDesignProfile profile) {
        return visualizer(loggerFactory, pathSource(network, pathResult), blockDisplayRenderer(plugin, viewerId),
                resolver(profile));
    }

    /**
     * Creates a guided block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param profile       design profile
     * @return visualizer
     */
    public static Visualizer blockDisplayGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                           final GraphNetwork network, final PathResult pathResult,
                                                           final UUID viewerId,
                                                           final GraphNetworkDesignProfile profile) {
        return blockDisplayGuidedNetworkPath(plugin, loggerFactory, network, pathResult, viewerId, profile,
                guidedPathOptions(plugin));
    }

    /**
     * Creates a guided block-display visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param profile       design profile
     * @param options       guided path options
     * @return visualizer
     */
    public static Visualizer blockDisplayGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                           final GraphNetwork network, final PathResult pathResult,
                                                           final UUID viewerId,
                                                           final GraphNetworkDesignProfile profile,
                                                           final GuidedPathOptions options) {
        return visualizer(loggerFactory, guidedPathSource(plugin, network, pathResult, viewerId, options),
                blockDisplayRenderer(plugin, viewerId), resolver(profile));
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
    public static Visualizer particleGraph(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                           final Graph graph, final UUID viewerId,
                                           final EffectExecutor executor) {
        return visualizer(loggerFactory, graphSource(graph), particleRenderer(plugin, viewerId, executor),
                ProfileGraphDesignResolver.defaults());
    }

    /**
     * Creates a Spellbook particle visualizer for an editor workspace.
     *
     * @param plugin           plugin
     * @param loggerFactory    logger factory
     * @param activeGraph      active graph supplier
     * @param referenceGraphs  visible reference graph supplier
     * @param interGraphEdges  visible inter-graph edges supplier
     * @param workspaceVersion workspace version supplier
     * @param viewerId         viewer id
     * @param executor         effect executor
     * @return visualizer
     */
    public static Visualizer particleEditorWorkspace(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                     final Supplier<Graph> activeGraph,
                                                     final Supplier<Collection<Graph>> referenceGraphs,
                                                     final Supplier<Collection<InterGraphEdge>> interGraphEdges,
                                                     final Supplier<Long> workspaceVersion,
                                                     final UUID viewerId, final EffectExecutor executor) {
        return visualizer(loggerFactory, new EditorWorkspaceVisualSource(activeGraph, referenceGraphs, interGraphEdges,
                        workspaceVersion), particleRenderer(plugin, viewerId, executor),
                ProfileGraphDesignResolver.defaults());
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
    public static Visualizer particleNetwork(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                             final GraphNetwork network, final UUID viewerId,
                                             final EffectExecutor executor,
                                             final GraphNetworkDesignProfile profile) {
        return visualizer(loggerFactory, networkSource(network), particleRenderer(plugin, viewerId, executor),
                resolver(profile));
    }

    /**
     * Creates a Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @return visualizer
     */
    public static Visualizer particleNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                 final GraphNetwork network, final PathResult pathResult,
                                                 final UUID viewerId, final EffectExecutor executor,
                                                 final GraphNetworkDesignProfile profile) {
        return visualizer(loggerFactory, pathSource(network, pathResult), particleRenderer(plugin, viewerId, executor),
                resolver(profile));
    }

    /**
     * Creates a guided Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @return visualizer
     */
    public static Visualizer particleGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                       final GraphNetwork network, final PathResult pathResult,
                                                       final UUID viewerId, final EffectExecutor executor,
                                                       final GraphNetworkDesignProfile profile) {
        return particleGuidedNetworkPath(plugin, loggerFactory, network, pathResult, viewerId, executor, profile,
                guidedPathOptions(plugin));
    }

    /**
     * Creates a guided Spellbook particle visualizer for a network path.
     *
     * @param plugin        plugin
     * @param loggerFactory logger factory
     * @param network       graph network
     * @param pathResult    structured path result
     * @param viewerId      viewer id
     * @param executor      effect executor
     * @param profile       design profile
     * @param options       guided path options
     * @return visualizer
     */
    public static Visualizer particleGuidedNetworkPath(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                                       final GraphNetwork network, final PathResult pathResult,
                                                       final UUID viewerId, final EffectExecutor executor,
                                                       final GraphNetworkDesignProfile profile,
                                                       final GuidedPathOptions options) {
        return visualizer(loggerFactory, guidedPathSource(plugin, network, pathResult, viewerId, options),
                particleRenderer(plugin, viewerId, executor), resolver(profile));
    }

    private static Visualizer visualizer(final LoggerFactory loggerFactory, final VisualGraphSource source,
                                         final GraphRenderer renderer, final GraphDesignResolver designs) {
        return new Visualizer(loggerFactory, source, renderer, designs);
    }

    private static VisualGraphSource graphSource(final Graph graph) {
        return new SingleGraphVisualSource(graph);
    }

    private static VisualGraphSource networkSource(final GraphNetwork network) {
        return new GraphNetworkVisualSource(network);
    }

    private static VisualGraphSource pathSource(final GraphNetwork network, final PathResult pathResult) {
        return new PathVisualGraphSource(networkSource(network), pathResult);
    }

    private static VisualGraphSource guidedPathSource(final Brotkrumen plugin, final GraphNetwork network,
                                                      final PathResult pathResult, final UUID viewerId,
                                                      final GuidedPathOptions options) {
        return new GuidedPathVisualGraphSource(networkSource(network), pathResult, viewerLocationSource(plugin, viewerId),
                options);
    }

    private static GraphRenderer blockDisplayRenderer(final Brotkrumen plugin, final UUID viewerId) {
        return new BlockDisplayGraphRenderer(plugin, viewerId);
    }

    private static GraphRenderer particleRenderer(final Brotkrumen plugin, final UUID viewerId,
                                                  final EffectExecutor executor) {
        return new ParticleGraphRenderer(plugin, viewerId, executor);
    }

    private static GraphDesignResolver resolver(final GraphNetworkDesignProfile profile) {
        return new ProfileGraphDesignResolver(profile);
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
