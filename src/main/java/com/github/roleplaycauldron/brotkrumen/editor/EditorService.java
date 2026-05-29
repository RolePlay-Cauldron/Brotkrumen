package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State manager for editor mode.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals",
        "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class EditorService {

    private static final double EDIT_NODE_SELECTION_RADIUS = 1.5D;

    private static final double MIN_WARP_COST = 0.0D;

    private static final String WAITING_FOR_ANCHOR_ACTION_BAR =
            "Placement continues after walking through a node. /bkeditor continue also resumes it.";

    private static final Set<String> SUPPORTED_PRESETS = Set.of("default", "ember", "prism");

    private final Map<UUID, EditorSession> playerEditors = new ConcurrentHashMap<>();

    private final VisualizerRegistry visualizerRegistry;

    private final Brotkrumen plugin;

    private final LoggerFactory loggerFactory;

    private final EffectExecutor effectExecutor;

    private final GraphService graphService;

    private final WarpService warpService;

    private final WrappedLogger log;

    public EditorService(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin,
                         final LoggerFactory loggerFactory, final EffectExecutor effectExecutor,
                         final GraphService graphService, final WarpService warpService) {
        this.visualizerRegistry = visualizerRegistry;
        this.plugin = plugin;
        this.loggerFactory = loggerFactory;
        this.effectExecutor = effectExecutor;
        this.graphService = graphService;
        this.warpService = warpService;

        this.log = loggerFactory.create(EditorService.class);
    }

    public static boolean isSupportedPreset(final String preset) {
        return preset != null && SUPPORTED_PRESETS.contains(preset.toLowerCase(Locale.ROOT));
    }

    public static Set<String> supportedPresets() {
        return SUPPORTED_PRESETS;
    }

    /* default */
    static String waitingAnchorActionBarMessage() {
        return WAITING_FOR_ANCHOR_ACTION_BAR;
    }

    public EditorResult startGraphCreation(final UUID playerId, final String graphName, final EditorSettings settings) {
        final EditorResult validation = validateStart(playerId, graphName, settings);
        if (!validation.success()) {
            return validation;
        }
        if (graphService.getGraphByName(graphName).isPresent()) {
            return EditorResult.failure("A graph with that name already exists.");
        }

        final EditorSession session = EditorSession.create(new Graph(graphName), settings.normalized());
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session.graph);
        return EditorResult.success("We will trace your steps now and create graph " + graphName + "!");
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

    public EditorResult startGraphEdit(final UUID playerId, final String graphName, final EditorSettings settings) {
        final EditorResult validation = validateStart(playerId, graphName, settings);
        if (!validation.success()) {
            return validation;
        }

        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("No graph with that name exists.");
        }

        final EditorSession session = EditorSession.edit(graph.get(), settings.normalized());
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session.graph);
        return EditorResult.success("Editing graph " + graphName + ". Walk through an existing node to append nodes.");
    }

    private EditorResult validateStart(final UUID playerId, final String graphName, final EditorSettings settings) {
        if (playerEditors.containsKey(playerId)) {
            return EditorResult.failure("You are already editing a graph.");
        }
        if (graphName == null || graphName.isBlank()) {
            return EditorResult.failure("Please specify a graph name.");
        }
        if (settings == null) {
            return EditorResult.failure("Editor settings are required.");
        }
        if (settings.nodeDistance <= 0) {
            return EditorResult.failure("Node distance must be greater than zero.");
        }
        if (!isSupportedPreset(settings.preset)) {
            return EditorResult.failure("Unknown editor preset.");
        }
        return EditorResult.success("");
    }

    private Optional<Node> selectExistingNode(final EditorSession session, final Location loc) {
        return session.graph.getNodes().stream()
                .filter(node -> sameWorld(node, loc))
                .filter(node -> distance(node, loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(node -> distance(node, loc)));
    }

    /**
     * Selects the closest nearby node without changing placement state.
     *
     * @param playerId editor player id
     * @param loc      selection origin
     * @return selection result
     */
    public EditorResult selectNearbyNode(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        return selectExistingNode(session, loc)
                .map(node -> {
                    session.selectedNode = node;
                    session.selectedEdge = null;
                    rememberEdgeEndpoint(session, node);
                    return EditorResult.success(selectedNodeMessage(session, node));
                })
                .orElseGet(() -> EditorResult.failure("No nearby graph node found."));
    }

    /**
     * Selects the closest nearby edge without changing placement state.
     *
     * @param playerId editor player id
     * @param loc      selection origin
     * @return selection result
     */
    public EditorResult selectNearbyEdge(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        return session.graph.getEdges().stream()
                .filter(edge -> edgeDistance(session, edge, loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(edge -> edgeDistance(session, edge, loc)))
                .map(edge -> {
                    session.selectedEdge = edge;
                    session.selectedNode = null;
                    clearEdgeEndpoints(session);
                    return EditorResult.success("Selected edge " + edge.edgeId() + ".");
                })
                .orElseGet(() -> EditorResult.failure("No nearby graph edge found."));
    }

    /**
     * Reports the selected node or edge.
     *
     * @param playerId editor player id
     * @return inspection result
     */
    public EditorResult showSelection(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (session.selectedNode != null) {
            return EditorResult.success("Selected node " + session.selectedNode.graphId() + ".");
        }
        if (session.selectedEdge != null) {
            return EditorResult.success("Selected edge " + session.selectedEdge.edgeId() + " from "
                    + session.selectedEdge.source() + " to " + session.selectedEdge.target() + ".");
        }
        return EditorResult.failure("No graph element is selected.");
    }

    /**
     * Clears the selected graph node or edge without changing placement state.
     *
     * @param playerId editor player id
     * @return clear result
     */
    public EditorResult clearSelection(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        session.selectedNode = null;
        session.selectedEdge = null;
        clearEdgeEndpoints(session);
        return EditorResult.success("Cleared graph selection.");
    }

    /**
     * Resolves a teleport destination for the selected graph element.
     *
     * @param playerId       editor player id
     * @param playerLocation current player location, used to keep the selected world instance
     * @return teleport destination and user-facing result
     */
    public SelectionTeleportResult teleportToSelection(final UUID playerId, final Location playerLocation) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return SelectionTeleportResult.failure("You are not editing a graph.");
        }
        if (session.selectedNode != null) {
            return SelectionTeleportResult.success("Teleported to selected node.",
                    location(playerLocation, session.selectedNode.x(), session.selectedNode.y(), session.selectedNode.z()));
        }
        if (session.selectedEdge != null) {
            final Optional<Location> midpoint = edgeMidpoint(session, session.selectedEdge, playerLocation);
            return midpoint.map(location -> SelectionTeleportResult.success("Teleported to selected edge.", location))
                    .orElseGet(() -> SelectionTeleportResult.failure("The selected edge is incomplete."));
        }
        return SelectionTeleportResult.failure("No graph element is selected.");
    }

    public EditorResult handleMovement(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        if (session.placementMode == PlacementMode.PREVIEW) {
            return EditorResult.success("");
        }

        if (session.placementMode == PlacementMode.WAITING_FOR_ANCHOR
                || session.mode == EditorMode.EDIT && session.selectedAppendNode == null) {
            return selectAppendAnchor(session, loc);
        }

        if (session.lastPlacedNode != null && distance(session.lastPlacedNode, loc) <= session.nodeDistance) {
            return EditorResult.success("");
        }

        addNodeToPath(session, loc);
        refreshVisualizer(playerId);
        return EditorResult.success("Added node to graph.");
    }

    private EditorResult selectAppendAnchor(final EditorSession session, final Location loc) {
        return selectExistingNode(session, loc)
                .map(node -> {
                    session.selectedAppendNode = node;
                    session.lastPlacedNode = node;
                    session.placementMode = PlacementMode.AUTO;
                    return EditorResult.success("Selected existing node as append anchor.");
                })
                .orElseGet(() -> EditorResult.success(""));
    }

    private void addNodeToPath(final EditorSession session, final Location loc) {
        final Node created = session.graph.addNode(new Node(null, loc));

        if (session.lastPlacedNode != null) {
            session.graph.addEdge(session.lastPlacedNode.graphId(), created.graphId(),
                    distance(session.lastPlacedNode, loc), Set.of(EdgeFlag.UNDIRECTED));
        }

        session.lastPlacedNode = created;
        session.createdNodes.addLast(created);
    }

    public EditorResult preview(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        session.placementMode = PlacementMode.PREVIEW;
        return EditorResult.success("Preview mode enabled.");
    }

    public EditorResult placeNode(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (session.mode == EditorMode.EDIT && session.lastPlacedNode == null) {
            return EditorResult.failure("Walk through an existing node before placing new edit nodes.");
        }

        addNodeToPath(session, loc);
        refreshVisualizer(playerId);
        return EditorResult.success("Placed node.");
    }

    /**
     * Creates or replaces an edge relationship between the selected node pair.
     *
     * @param playerId editor player id
     * @param edgeType requested edge type
     * @return mutation result
     */
    public EditorResult createSelectedNodeEdge(final UUID playerId, final EdgeType edgeType) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (edgeType == null) {
            return EditorResult.failure("Edge type is required.");
        }
        if (session.edgeEndpointOne == null || session.edgeEndpointTwo == null) {
            return EditorResult.failure("Select two graph nodes before creating an edge.");
        }

        final List<Edge> created = replaceRelationship(session, session.edgeEndpointOne.graphId(),
                session.edgeEndpointTwo.graphId(), distance(session.edgeEndpointOne, session.edgeEndpointTwo), edgeType,
                Set.of());
        session.selectedEdge = created.getFirst();
        session.selectedNode = null;
        refreshVisualizer(playerId);
        return EditorResult.success("Set " + edgeType.configValue() + " edge between selected nodes.");
    }

    /**
     * Changes the selected edge relationship type while preserving state flags.
     *
     * @param playerId editor player id
     * @param edgeType requested edge type
     * @return mutation result
     */
    public EditorResult updateSelectedEdgeType(final UUID playerId, final EdgeType edgeType) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (edgeType == null) {
            return EditorResult.failure("Edge type is required.");
        }
        if (session.selectedEdge == null) {
            return EditorResult.failure("Select a graph edge before changing its type.");
        }

        final Edge selected = session.selectedEdge;
        final List<Edge> created = replaceRelationship(session, selected.source(), selected.target(), selected.cost(),
                edgeType, selected.flags());
        session.selectedEdge = created.getFirst();
        refreshVisualizer(playerId);
        return EditorResult.success("Set selected edge type to " + edgeType.configValue() + ".");
    }

    /**
     * Changes the selected edge relationship open/blocked state while preserving edge type.
     *
     * @param playerId  editor player id
     * @param edgeState requested edge state
     * @return mutation result
     */
    public EditorResult updateSelectedEdgeState(final UUID playerId, final EdgeState edgeState) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (edgeState == null) {
            return EditorResult.failure("Edge state is required.");
        }
        if (session.selectedEdge == null) {
            return EditorResult.failure("Select a graph edge before changing its state.");
        }

        final Edge selected = session.selectedEdge;
        final List<Edge> updated = session.graph.updateRelationshipBlocked(selected.source(), selected.target(),
                edgeState == EdgeState.BLOCKED);
        session.selectedEdge = updated.stream()
                .filter(edge -> edge.edgeId().equals(selected.edgeId()))
                .findFirst()
                .orElseGet(() -> updated.isEmpty() ? null : updated.getFirst());
        refreshVisualizer(playerId);
        return EditorResult.success("Set selected edge state to " + edgeState.configValue() + ".");
    }

    /**
     * Removes the selected edge relationship from the working graph.
     *
     * @param playerId editor player id
     * @return removal result
     */
    public EditorResult removeSelectedEdge(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (session.selectedEdge == null) {
            return EditorResult.failure("Select a graph edge before removing it.");
        }

        final Edge selected = session.selectedEdge;
        final int removed = session.graph.removeEdgesBetween(selected.source(), selected.target()).size();
        session.selectedEdge = null;
        session.selectedNode = null;
        clearEdgeEndpoints(session);
        refreshVisualizer(playerId);
        return EditorResult.success("Removed " + removed + " selected edge record" + (removed == 1 ? "." : "s."));
    }

    private boolean sameWorld(final Node node, final Location loc) {
        return loc.getWorld() != null && Objects.equals(node.worldId(), loc.getWorld().getUID());
    }

    private void rememberEdgeEndpoint(final EditorSession session, final Node node) {
        if (session.edgeEndpointOne == null) {
            session.edgeEndpointOne = node;
            return;
        }
        if (sameNode(session.edgeEndpointOne, node)) {
            if (session.edgeEndpointTwo != null) {
                session.edgeEndpointOne = session.edgeEndpointTwo;
                session.edgeEndpointTwo = node;
            }
            return;
        }
        if (session.edgeEndpointTwo == null) {
            session.edgeEndpointTwo = node;
            return;
        }
        if (!sameNode(session.edgeEndpointTwo, node)) {
            session.edgeEndpointOne = session.edgeEndpointTwo;
            session.edgeEndpointTwo = node;
        }
    }

    private boolean sameNode(final Node first, final Node second) {
        return first != null && second != null && first.graphId().equals(second.graphId());
    }

    private void clearEdgeEndpoints(final EditorSession session) {
        session.edgeEndpointOne = null;
        session.edgeEndpointTwo = null;
    }

    private String selectedNodeMessage(final EditorSession session, final Node node) {
        if (session.edgeEndpointOne != null && session.edgeEndpointTwo != null) {
            return "Selected node " + node.graphId() + ". Edge endpoints: "
                    + session.edgeEndpointOne.graphId() + " -> " + session.edgeEndpointTwo.graphId() + ".";
        }
        return "Selected node " + node.graphId() + ".";
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

    private double distance(final Node first, final Node second) {
        final double deltaX = first.x() - second.x();
        final double deltaY = first.y() - second.y();
        final double deltaZ = first.z() - second.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private List<Edge> replaceRelationship(final EditorSession session, final UUID source, final UUID target,
                                           final double cost, final EdgeType edgeType, final Set<EdgeFlag> flags) {
        return edgeType == EdgeType.DIRECTED
                ? List.of(session.graph.replaceDirectedRelationship(source, target, cost, flags))
                : session.graph.replaceUndirectedRelationship(source, target, cost, flags);
    }

    private double edgeDistance(final EditorSession session, final Edge edge, final Location loc) {
        final Node source = session.graph.getNodeById(edge.source());
        final Node target = session.graph.getNodeById(edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Double.MAX_VALUE;
        }
        final double directionX = target.x() - source.x();
        final double directionY = target.y() - source.y();
        final double directionZ = target.z() - source.z();
        final double lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        final double offset = lengthSquared == 0.0D ? 0.0D
                : ((loc.getX() - source.x()) * directionX + (loc.getY() - source.y()) * directionY
                   + (loc.getZ() - source.z()) * directionZ) / lengthSquared;
        final double pathOffset = Math.max(0.0D, Math.min(1.0D, offset));
        final double nearestX = source.x() + directionX * pathOffset;
        final double nearestY = source.y() + directionY * pathOffset;
        final double nearestZ = source.z() + directionZ * pathOffset;
        final double deltaX = loc.getX() - nearestX;
        final double deltaY = loc.getY() - nearestY;
        final double deltaZ = loc.getZ() - nearestZ;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private Optional<Location> edgeMidpoint(final EditorSession session, final Edge edge, final Location loc) {
        final Node source = session.graph.getNodeById(edge.source());
        final Node target = session.graph.getNodeById(edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Optional.empty();
        }
        return Optional.of(location(loc, (source.x() + target.x()) / 2.0D, (source.y() + target.y()) / 2.0D,
                (source.z() + target.z()) / 2.0D));
    }

    private Location location(final Location source, final double targetX, final double targetY, final double targetZ) {
        return new Location(source.getWorld(), targetX, targetY, targetZ, source.getYaw(), source.getPitch());
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public EditorResult finishRouteCreation(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        unregisterVisualizer(playerId);
        if (plugin == null) {
            try {
                graphService.saveGraph(session.graph);
                saveWarps(session);
            } catch (final Exception e) {
                log.errorF("The graph could not be saved: {}", e.getMessage());
                return EditorResult.failure("Failed to save the graph, aborting editing");
            } finally {
                playerEditors.remove(playerId);
            }
            return EditorResult.success("Route creation finished.");
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            graphService.saveGraph(session.graph);
            saveWarps(session);
            playerEditors.remove(playerId);
        });
        return EditorResult.success("Route creation finished.");
    }

    private void saveWarps(final EditorSession session) {
        if (warpService == null) {
            return;
        }
        for (final String key : session.pendingDeletions) {
            warpService.removeWarp(key);
        }
        for (final Warp warp : session.pendingWarps.values()) {
            warpService.saveWarp(warp);
        }
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

    /**
     * Returns whether an active editor session is waiting for a node append anchor.
     *
     * @param playerId editor player id
     * @return true if the editor session should receive waiting guidance
     */
    public boolean isWaitingForAppendAnchor(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session != null && session.placementMode == PlacementMode.WAITING_FOR_ANCHOR;
    }

    /* default */ UUID[] editorPlayerIds() {
        return playerEditors.keySet().toArray(UUID[]::new);
    }

    public EditorResult continuePlacement(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        if (session.continueRequiresNode) {
            session.placementMode = PlacementMode.WAITING_FOR_ANCHOR;
            return EditorResult.success("Walk through a node to continue placement.", WAITING_FOR_ANCHOR_ACTION_BAR);
        }

        session.placementMode = PlacementMode.AUTO;
        return EditorResult.success("Automatic placement resumed.");
    }

    public EditorResult undo(final UUID playerId, final int amount) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (amount <= 0) {
            return EditorResult.failure("Undo amount must be greater than zero.");
        }
        if (session.createdNodes.isEmpty()) {
            return EditorResult.failure("There are no session-created nodes to undo.");
        }

        int removed = 0;
        while (removed < amount && !session.createdNodes.isEmpty()) {
            session.graph.removeNode(session.createdNodes.removeLast());
            removed++;
        }
        session.lastPlacedNode = session.createdNodes.peekLast();
        if (session.lastPlacedNode == null) {
            session.lastPlacedNode = session.selectedAppendNode;
        }
        session.placementMode = session.continueRequiresNode ? PlacementMode.WAITING_FOR_ANCHOR : PlacementMode.PREVIEW;
        refreshVisualizer(playerId);

        final String message = "Undid " + removed + " node" + (removed == 1 ? "." : "s.");
        return session.continueRequiresNode
                ? EditorResult.success(message, WAITING_FOR_ANCHOR_ACTION_BAR)
                : EditorResult.success(message);
    }

    public EditorResult settingsSummary(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }

        return EditorResult.success("Settings: node-distance=" + session.nodeDistance
                + ", placement=" + session.placementMode.configValue()
                + ", continue-requires-node=" + session.continueRequiresNode
                + ", preset=" + session.preset + ".");
    }

    public EditorResult updateNodeDistance(final UUID playerId, final int nodeDistance) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (nodeDistance <= 0) {
            return EditorResult.failure("Node distance must be greater than zero.");
        }
        session.nodeDistance = nodeDistance;
        return EditorResult.success("Node distance set to " + nodeDistance + ".");
    }

    public EditorResult updatePlacementMode(final UUID playerId, final PlacementMode placementMode) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (placementMode == null) {
            return EditorResult.failure("Placement mode is required.");
        }
        session.placementMode = placementMode;
        refreshVisualizer(playerId);
        return EditorResult.success("Placement mode set to " + placementMode.configValue() + ".");
    }

    /**
     * Creates a default warp targeting the selected node.
     *
     * @param playerId editor player id
     * @param key      warp key
     * @return creation result
     */
    public EditorResult createSelectedWarp(final UUID playerId, final String key) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (session.selectedNode == null) {
            return EditorResult.failure("Select a graph node before creating a selected warp.");
        }
        return createWarp(playerId, session, key, session.selectedNode);
    }

    /**
     * Creates a default warp targeting a new node at the current player location.
     *
     * @param playerId editor player id
     * @param key      warp key
     * @param loc      node location
     * @return creation result
     */
    public EditorResult createWarpHere(final UUID playerId, final String key, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        final Node node = session.graph.addNode(new Node(null, loc));
        session.selectedNode = node;
        session.selectedEdge = null;
        refreshVisualizer(playerId);
        return createWarp(playerId, session, key, node);
    }

    private EditorResult createWarp(final UUID playerId, final EditorSession session, final String key,
                                    final Node target) {
        final EditorResult check = validateWarp(key);
        if (!check.success()) {
            return check;
        }
        session.pendingDeletions.remove(key);
        session.pendingWarps.put(key, new Warp(key, target.graphId(), 1.0D, true, true));
        session.selectedNode = ensureWarpFlag(session, target);
        refreshVisualizer(playerId);
        return EditorResult.success("Created warp " + key + " for node " + target.graphId() + ".");
    }

    /**
     * Changes the route cost of a persisted warp.
     *
     * @param playerId editor player id
     * @param key      warp key
     * @param cost     new cost
     * @return update result
     */
    public EditorResult updateWarpCost(final UUID playerId, final String key, final double cost) {
        if (cost < MIN_WARP_COST) {
            return EditorResult.failure("Warp cost must not be negative.");
        }
        return updateWarp(playerId, key, warp -> new Warp(warp.key(), warp.targetNodeId(), cost, warp.enabled(),
                warp.needPermission()), "cost");
    }

    /**
     * Changes the enabled state of a persisted warp.
     *
     * @param playerId editor player id
     * @param key      warp key
     * @param enabled  new enabled state
     * @return update result
     */
    public EditorResult updateWarpEnabled(final UUID playerId, final String key, final boolean enabled) {
        return updateWarp(playerId, key, warp -> new Warp(warp.key(), warp.targetNodeId(), warp.cost(), enabled,
                warp.needPermission()), "enabled state");
    }

    /**
     * Changes the permission requirement of a persisted warp.
     *
     * @param playerId       editor player id
     * @param key            warp key
     * @param needPermission new permission state
     * @return update result
     */
    public EditorResult updateWarpPermission(final UUID playerId, final String key, final boolean needPermission) {
        return updateWarp(playerId, key, warp -> new Warp(warp.key(), warp.targetNodeId(), warp.cost(),
                warp.enabled(), needPermission), "permission requirement");
    }

    private EditorResult updateWarp(final UUID playerId, final String key,
                                    final java.util.function.UnaryOperator<Warp> change, final String property) {
        final EditorResult check = validateWarpOperation(playerId, key);
        if (!check.success()) {
            return check;
        }
        final EditorSession session = playerEditors.get(playerId);
        Warp warp = session.pendingWarps.get(key);
        if (warp == null && !session.pendingDeletions.contains(key)) {
            warp = warpService.getWarp(key).orElse(null);
        }

        if (warp == null) {
            return EditorResult.failure("No warp with that key exists.");
        }
        session.pendingWarps.put(key, change.apply(warp));
        return EditorResult.success("Updated warp " + key + " " + property + ".");
    }

    /**
     * Removes a warp and clears unused target capability metadata.
     *
     * @param playerId editor player id
     * @param key      warp key
     * @return removal result
     */
    public EditorResult removeWarp(final UUID playerId, final String key) {
        final EditorResult check = validateWarpOperation(playerId, key);
        if (!check.success()) {
            return check;
        }
        final EditorSession session = playerEditors.get(playerId);
        Warp warp = session.pendingWarps.remove(key);
        if (warp == null && !session.pendingDeletions.contains(key)) {
            warp = warpService.getWarp(key).orElse(null);
        }

        if (warp == null) {
            return EditorResult.failure("No warp with that key exists.");
        }

        session.pendingDeletions.add(key);
        if (warpsTargeting(session, warp.targetNodeId()).isEmpty()) {
            clearWarpFlag(session, warp.targetNodeId());
            refreshVisualizer(playerId);
        }
        return EditorResult.success("Removed warp " + key + ".");
    }

    private Collection<Warp> warpsTargeting(final EditorSession session, final UUID targetNodeId) {
        final Set<Warp> warps = new LinkedHashSet<>(warpService.getWarpsTargeting(targetNodeId));
        warps.removeIf(warp -> session.pendingDeletions.contains(warp.key()));
        warps.addAll(session.pendingWarps.values().stream()
                .filter(warp -> warp.targetNodeId().equals(targetNodeId))
                .toList());
        return warps;
    }

    /**
     * Lists warps for the active graph or every managed warp.
     *
     * @param playerId editor player id
     * @param all      whether to include all persisted warps
     * @return listing result
     */
    public EditorResult listWarps(final UUID playerId, final boolean all) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (warpService == null) {
            return EditorResult.failure("Warp storage is unavailable.");
        }
        final Collection<UUID> graphNodeIds = session.graph.getNodes().stream().map(Node::graphId).toList();
        final Set<Warp> warps = new LinkedHashSet<>(all ? warpService.getManagedWarps() : warpService.getWarpsTargeting(graphNodeIds));

        warps.removeIf(warp -> session.pendingDeletions.contains(warp.key()));
        for (final Warp pending : session.pendingWarps.values()) {
            if (all || graphNodeIds.contains(pending.targetNodeId())) {
                warps.removeIf(w -> w.key().equals(pending.key()));
                warps.add(pending);
            }
        }

        if (warps.isEmpty()) {
            return EditorResult.success(all ? "No persisted warps." : "No warps target the active graph.");
        }
        final String scope = all ? "All warps: " : "Active graph warps: ";
        return EditorResult.success(scope + warps.stream().map(Warp::key).sorted().collect(
                java.util.stream.Collectors.joining(", ")) + ".");
    }

    private EditorResult validateWarpOperation(final UUID playerId, final String key) {
        if (!playerEditors.containsKey(playerId)) {
            return EditorResult.failure("You are not editing a graph.");
        }
        return validateWarp(key);
    }

    private EditorResult validateWarp(final String key) {
        if (warpService == null) {
            return EditorResult.failure("Warp storage is unavailable.");
        }
        return key == null || key.isBlank() ? EditorResult.failure("Please specify a warp key.")
                : EditorResult.success("");
    }

    private Node ensureWarpFlag(final EditorSession session, final Node node) {
        if (node.flags().contains(NodeFlag.WARP)) {
            return node;
        }
        final Set<NodeFlag> flags = node.flags().isEmpty() ? EnumSet.noneOf(NodeFlag.class) : EnumSet.copyOf(node.flags());
        flags.add(NodeFlag.WARP);
        return session.graph.updateNode(new Node(node.dbId(), node.graphId(), node.x(), node.y(), node.z(),
                node.worldId(), flags));
    }

    private void clearWarpFlag(final EditorSession session, final UUID targetNodeId) {
        final Node target = session.graph.getNodeById(targetNodeId);
        if (target == null || !target.flags().contains(NodeFlag.WARP)) {
            return;
        }
        final Set<NodeFlag> flags = new LinkedHashSet<>(target.flags());
        flags.remove(NodeFlag.WARP);
        final Node updated = session.graph.updateNode(new Node(target.dbId(), target.graphId(), target.x(), target.y(),
                target.z(), target.worldId(), flags));
        if (session.selectedNode != null && session.selectedNode.graphId().equals(targetNodeId)) {
            session.selectedNode = updated;
        }
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

    public EditorResult updateContinueRequiresNode(final UUID playerId, final boolean continueRequiresNode) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        session.continueRequiresNode = continueRequiresNode;
        return EditorResult.success("Continue requires node set to " + continueRequiresNode + ".");
    }

    public Graph getWorkingGraph(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session == null ? null : session.graph;
    }

    public EditorResult updatePreset(final UUID playerId, final String preset) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("You are not editing a graph.");
        }
        if (!isSupportedPreset(preset)) {
            return EditorResult.failure("Unknown editor preset.");
        }
        session.preset = preset.toLowerCase(Locale.ROOT);
        refreshVisualizer(playerId);
        return EditorResult.success("Editor preset set to " + session.preset + ".");
    }

    private void refreshVisualizer(final UUID playerId) {
        if (visualizerRegistry != null) {
            visualizerRegistry.refresh(playerId);
        }
    }

    public EditorSettings getSettings(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session == null ? null : session.settings();
    }

    public enum PlacementMode {
        AUTO("auto"),
        PREVIEW("preview"),
        WAITING_FOR_ANCHOR("waiting-for-anchor");

        private final String serializedValue;

        PlacementMode(final String configValue) {
            this.serializedValue = configValue;
        }

        public static Optional<PlacementMode> parse(final String input) {
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }
            final String normalized = input.toLowerCase(Locale.ROOT);
            for (final PlacementMode mode : values()) {
                if (mode.serializedValue.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                    return Optional.of(mode);
                }
            }
            return Optional.empty();
        }

        public String configValue() {
            return serializedValue;
        }
    }

    public enum EdgeType {
        DIRECTED("directed"),
        UNDIRECTED("undirected");

        private final String serializedValue;

        EdgeType(final String configValue) {
            this.serializedValue = configValue;
        }

        public static Optional<EdgeType> parse(final String input) {
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }
            final String normalized = input.toLowerCase(Locale.ROOT);
            for (final EdgeType type : values()) {
                if (type.serializedValue.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        public String configValue() {
            return serializedValue;
        }
    }

    public enum EdgeState {
        OPEN("open"),
        BLOCKED("blocked");

        private final String serializedValue;

        EdgeState(final String configValue) {
            this.serializedValue = configValue;
        }

        public static Optional<EdgeState> parse(final String input) {
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }
            final String normalized = input.toLowerCase(Locale.ROOT);
            for (final EdgeState state : values()) {
                if (state.serializedValue.equals(normalized) || state.name().equalsIgnoreCase(normalized)) {
                    return Optional.of(state);
                }
            }
            return Optional.empty();
        }

        public String configValue() {
            return serializedValue;
        }
    }

    private enum EditorMode {
        CREATE,
        EDIT
    }

    public record EditorSettings(int nodeDistance, PlacementMode placementMode, boolean continueRequiresNode,
                                 String preset) {

        public EditorSettings normalized() {
            return new EditorSettings(nodeDistance, Objects.requireNonNullElse(placementMode, PlacementMode.AUTO),
                    continueRequiresNode, preset == null || preset.isBlank() ? "default" : preset.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Editor operation result.
     *
     * @param success whether the operation succeeded
     * @param message user-facing message
     */
    public record EditorResult(boolean success, String message, String actionBarMessage) {

        public EditorResult(final boolean success, final String message) {
            this(success, message, "");
        }

        public static EditorResult success(final String message) {
            return new EditorResult(true, message);
        }

        public static EditorResult success(final String message, final String actionBarMessage) {
            return new EditorResult(true, message, actionBarMessage);
        }

        public static EditorResult failure(final String message) {
            return new EditorResult(false, message);
        }
    }

    /**
     * Selected element teleport result.
     *
     * @param result      user-facing result
     * @param destination teleport destination, or null when rejected
     */
    public record SelectionTeleportResult(EditorResult result, Location destination) {

        public static SelectionTeleportResult success(final String message, final Location destination) {
            return new SelectionTeleportResult(EditorResult.success(message), destination);
        }

        public static SelectionTeleportResult failure(final String message) {
            return new SelectionTeleportResult(EditorResult.failure(message), null);
        }
    }

    private static final class EditorSession {

        private final EditorMode mode;

        private final Deque<Node> createdNodes = new LinkedList<>();

        private final Map<String, Warp> pendingWarps = new ConcurrentHashMap<>();

        private final Set<String> pendingDeletions = ConcurrentHashMap.newKeySet();

        private final Graph graph;

        private int nodeDistance;

        private PlacementMode placementMode;

        private boolean continueRequiresNode;

        private Node selectedAppendNode;

        private Node selectedNode;

        private Edge selectedEdge;

        private Node edgeEndpointOne;

        private Node edgeEndpointTwo;

        private Node lastPlacedNode;

        private String preset;

        private EditorSession(final EditorMode mode, final Graph graph, final EditorSettings settings) {
            this.mode = mode;
            this.graph = graph;
            this.nodeDistance = settings.nodeDistance;
            this.placementMode = mode == EditorMode.EDIT ? PlacementMode.WAITING_FOR_ANCHOR : settings.placementMode;
            this.continueRequiresNode = settings.continueRequiresNode;
            this.preset = settings.preset;
        }

        private static EditorSession create(final Graph graph, final EditorSettings settings) {
            return new EditorSession(EditorMode.CREATE, graph, settings);
        }

        private static EditorSession edit(final Graph graph, final EditorSettings settings) {
            return new EditorSession(EditorMode.EDIT, graph, settings);
        }

        private EditorSettings settings() {
            return new EditorSettings(nodeDistance, placementMode, continueRequiresNode, preset);
        }
    }
}
