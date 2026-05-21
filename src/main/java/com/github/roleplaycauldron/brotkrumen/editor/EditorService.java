package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bukkit.Location;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State manager for editor mode.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals"})
public class EditorService {

    private static final double EDIT_NODE_SELECTION_RADIUS = 1.5D;

    private final Map<UUID, EditorSession> playerEditors = new ConcurrentHashMap<>();

    private final VisualizerRegistry visualizerRegistry;

    private final Brotkrumen plugin;

    private final LoggerFactory loggerFactory;

    private final EffectExecutor effectExecutor;

    private final GraphService graphService;

    public EditorService(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin,
                         final LoggerFactory loggerFactory, final EffectExecutor effectExecutor,
                         final GraphService graphService) {
        this.visualizerRegistry = visualizerRegistry;
        this.plugin = plugin;
        this.loggerFactory = loggerFactory;
        this.effectExecutor = effectExecutor;
        this.graphService = graphService;
    }

    /* default */ EditorService(final GraphService graphService) {
        this(null, null, null, null, graphService);
    }

    public EditorResult startGraphCreation(final UUID playerId, final String graphName, final int nodeDistance) {
        final EditorResult validation = validateStart(playerId, graphName, nodeDistance);
        if (!validation.success()) {
            return validation;
        }
        if (graphService.getGraphByName(graphName).isPresent()) {
            return EditorResult.failure("A graph with that name already exists.");
        }

        final EditorSession session = EditorSession.create(new Graph(graphName), nodeDistance);
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session.graph);
        return EditorResult.success("We will trace your steps now and create graph " + graphName + "!");
    }

    public EditorResult startGraphEdit(final UUID playerId, final String graphName, final int nodeDistance) {
        final EditorResult validation = validateStart(playerId, graphName, nodeDistance);
        if (!validation.success()) {
            return validation;
        }

        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("No graph with that name exists.");
        }

        final EditorSession session = EditorSession.edit(graph.get(), nodeDistance);
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session.graph);
        return EditorResult.success("Editing graph " + graphName + ". Walk through an existing node to append nodes.");
    }

    private EditorResult validateStart(final UUID playerId, final String graphName, final int nodeDistance) {
        if (playerEditors.containsKey(playerId)) {
            return EditorResult.failure("You are already editing a graph.");
        }
        if (graphName == null || graphName.isBlank()) {
            return EditorResult.failure("Please specify a graph name.");
        }
        if (nodeDistance <= 0) {
            return EditorResult.failure("Node distance must be greater than zero.");
        }
        return EditorResult.success("");
    }

    public EditorResult renameActiveGraph(final UUID playerId, final String newName) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (newName == null || newName.isBlank()) {
            return EditorResult.failure("Please specify a graph name.");
        }

        final Optional<Graph> existing = graphService.getGraphByName(newName);
        if (existing.isPresent() && existing.get().getGraphId() != session.graph.getGraphId()) {
            return EditorResult.failure("A graph with that name already exists.");
        }

        session.graph.setName(newName);
        return EditorResult.success("Renamed active graph to " + newName + ".");
    }

    public EditorResult handleMovement(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        if (session.mode == EditorMode.EDIT && session.selectedAppendNode == null) {
            return selectExistingNode(session, loc)
                    .map(node -> {
                        session.selectedAppendNode = node;
                        session.lastPlacedNode = node;
                        return EditorResult.success("Selected existing node as append anchor.");
                    })
                    .orElseGet(() -> EditorResult.success(""));
        }

        if (session.lastPlacedNode != null && distance(session.lastPlacedNode, loc) <= session.nodeDistance) {
            return EditorResult.success("");
        }

        addNodeToPath(session, loc);
        return EditorResult.success("Added node to graph.");
    }

    private Optional<Node> selectExistingNode(final EditorSession session, final Location loc) {
        return session.graph.getNodes().stream()
                .filter(node -> sameWorld(node, loc))
                .filter(node -> distance(node, loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(node -> distance(node, loc)));
    }

    private void addNodeToPath(final EditorSession session, final Location loc) {
        final Node created = session.graph.addNode(new Node(null, loc));

        if (session.lastPlacedNode != null) {
            session.graph.addEdge(session.lastPlacedNode.graphId(), created.graphId(),
                    distance(session.lastPlacedNode, loc), Set.of(EdgeFlag.UNDIRECTED));
        }

        session.lastPlacedNode = created;
    }

    private boolean sameWorld(final Node node, final Location loc) {
        return loc.getWorld() != null && Objects.equals(node.worldId(), loc.getWorld().getUID());
    }

    private double distance(final Node node, final Location loc) {
        if (!sameWorld(node, loc)) {
            return Double.MAX_VALUE;
        }
        final double deltaX = loc.getX() - node.x();
        final double deltaY = loc.getY() - node.y();
        final double deltaZ = loc.getZ() - node.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public EditorResult finishRouteCreation(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        unregisterVisualizer(playerId);
        if (plugin == null) {
            graphService.saveGraph(session.graph);
            playerEditors.remove(playerId);
            return EditorResult.success("Route creation finished.");
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            graphService.saveGraph(session.graph);
            playerEditors.remove(playerId);
        });
        return EditorResult.success("Route creation finished.");
    }

    public EditorResult cancel(final UUID playerId) {
        if (playerEditors.remove(playerId) == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        unregisterVisualizer(playerId);
        return EditorResult.success("Editor session cancelled.");
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

    private void registerVisualizer(final UUID playerId, final Graph graph) {
        if (visualizerRegistry == null) {
            return;
        }
        visualizerRegistry.register(playerId,
                GraphVisualizerFactory.particleGraph(plugin, loggerFactory, graph, playerId, effectExecutor));
    }

    private void unregisterVisualizer(final UUID playerId) {
        if (visualizerRegistry != null) {
            visualizerRegistry.unregister(playerId);
        }
    }

    public Graph getWorkingGraph(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session == null ? null : session.graph;
    }

    private enum EditorMode {
        CREATE,
        EDIT
    }

    /**
     * Editor operation result.
     *
     * @param success whether the operation succeeded
     * @param message user-facing message
     */
    public record EditorResult(boolean success, String message) {

        public static EditorResult success(final String message) {
            return new EditorResult(true, message);
        }

        public static EditorResult failure(final String message) {
            return new EditorResult(false, message);
        }
    }

    private static final class EditorSession {

        private final EditorMode mode;

        private final int nodeDistance;

        private final Graph graph;

        private Node selectedAppendNode;

        private Node lastPlacedNode;

        private EditorSession(final EditorMode mode, final Graph graph, final int nodeDistance) {
            this.mode = mode;
            this.graph = graph;
            this.nodeDistance = nodeDistance;
        }

        private static EditorSession create(final Graph graph, final int nodeDistance) {
            return new EditorSession(EditorMode.CREATE, graph, nodeDistance);
        }

        private static EditorSession edit(final Graph graph, final int nodeDistance) {
            return new EditorSession(EditorMode.EDIT, graph, nodeDistance);
        }
    }
}
