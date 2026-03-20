package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.bukkit.Location;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State Manager for Editor Mode.
 */
public class EditorService {

    private final Map<UUID, GraphCreationState> playerEditors = new ConcurrentHashMap<>();

    private final VisualizerRegistry visualizerRegistry;

    private final Brotkrumen plugin;

    private final LoggerFactory loggerFactory;

    public EditorService(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin, final LoggerFactory loggerFactory) {
        this.visualizerRegistry = visualizerRegistry;
        this.plugin = plugin;
        this.loggerFactory = loggerFactory;
    }

    public void startGraphCreation(final UUID playerId, final String graphName, final int nodeDistance) {
        if (playerEditors.containsKey(playerId)) {
            System.out.println("Player already editing");
            return;
        }
        final GraphCreationState state = new GraphCreationState(graphName, nodeDistance);
        this.playerEditors.put(playerId, state);
        visualizerRegistry.register(playerId, GraphVisualizerFactory.blockDisplayGraph(plugin, loggerFactory, state.graph, playerId));
        System.out.println("Started editing for " + playerId);
    }

    public void addNodeToPath(final UUID playerId, final Location loc) {
        System.out.println("Adding node to path: " + loc);
        final GraphCreationState state = this.playerEditors.get(playerId);

        final var created = state.graph.addNode(new Node(null, loc));

        if (state.lastPlacedNode == null) {
            state.lastPlacedNode = created;
            return;
        }
        state.graph.addEdge(state.lastPlacedNode.graphId(), created.graphId(),
                loc.distance(state.lastPlacedNode.toCenterLocation()), Set.of(EdgeFlag.UNDIRECTED));

        state.lastPlacedNode = created;
    }

    public void finishRouteCreation(final UUID playerId) {
        visualizerRegistry.unregister(playerId);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getGraphService().saveGraph(playerEditors.get(playerId).graph);
            System.out.printf("Finished editing Graph '%s' for '%s'%n", playerEditors.get(playerId).graph.getName(), playerId);
            playerEditors.remove(playerId);
        });
    }

    /**
     * Checks if a player is in editing mode.
     *
     * @param playerId the player to check
     * @return true if in editing mode, false otherwise
     */
    public boolean isEditing(final UUID playerId) {
        return this.playerEditors.containsKey(playerId);
    }

    public int getNodeDistance(final UUID playerId) {
        return this.playerEditors.get(playerId).nodeDistance;
    }

    /**
     * State for Creation of a Graph.
     */
    public static final class GraphCreationState {

        private final int nodeDistance;

        public Graph graph;

        public Node lastPlacedNode;

        /**
         * Creates an internal Graph.
         *
         * @param graphName    graph name
         * @param nodeDistance
         */
        public GraphCreationState(final String graphName, final int nodeDistance) {
            this.nodeDistance = nodeDistance;
            Objects.requireNonNull(graphName, "Graph name cannot be null");
            this.graph = new Graph(graphName);
        }
    }
}
