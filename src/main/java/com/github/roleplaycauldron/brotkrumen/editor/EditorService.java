package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * State manager for editor mode.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals",
        "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.ExcessivePublicCount"})
public class EditorService {

    private static final double EDIT_NODE_SELECTION_RADIUS = 1.5D;

    private static final double MIN_WARP_COST = 0.0D;

    private static final String WAITING_FOR_ANCHOR_ACTION_BAR =
            "commands.bkeditor.status.guidanceWaitingAnchorActionbar";

    private static final Set<String> SUPPORTED_PRESETS = Set.of("default", "ember", "prism");

    private final Map<UUID, EditorSession> playerEditors = new ConcurrentHashMap<>();

    private final VisualizerRegistry visualizerRegistry;

    private final Brotkrumen plugin;

    private final LoggerFactory loggerFactory;

    private final EffectExecutor effectExecutor;

    private final GraphService graphService;

    private final GraphNetworkService graphNetworkService;

    private final WarpService warpServiceInstance;

    private final WrappedLogger log;

    /**
     * Creates a new editor service.
     *
     * @param visualizerRegistry the visualizer registry
     * @param plugin             the plugin instance
     * @param loggerFactory      the logger factory
     * @param effectExecutor     the effect executor
     * @param graphService       the graph service
     * @param warpService        the warp service
     */
    public EditorService(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin,
                         final LoggerFactory loggerFactory, final EffectExecutor effectExecutor,
                         final GraphService graphService, final WarpService warpService) {
        this(visualizerRegistry, plugin, loggerFactory, effectExecutor, graphService, null, warpService);
    }

    /**
     * Creates a new editor service.
     *
     * @param visualizerRegistry  the visualizer registry
     * @param plugin              the plugin instance
     * @param loggerFactory       the logger factory
     * @param effectExecutor      the effect executor
     * @param graphService        the graph service
     * @param graphNetworkService the graph network service
     * @param warpService         the warp service
     */
    public EditorService(final VisualizerRegistry visualizerRegistry, final Brotkrumen plugin,
                         final LoggerFactory loggerFactory, final EffectExecutor effectExecutor,
                         final GraphService graphService, final GraphNetworkService graphNetworkService,
                         final WarpService warpService) {
        this.visualizerRegistry = visualizerRegistry;
        this.plugin = plugin;
        this.loggerFactory = loggerFactory;
        this.effectExecutor = effectExecutor;
        this.graphService = graphService;
        this.graphNetworkService = graphNetworkService;
        this.warpServiceInstance = warpService;

        this.log = loggerFactory.create(EditorService.class);
    }

    /**
     * Checks if the given preset is supported.
     *
     * @param preset the preset to check
     * @return true if supported
     */
    public static boolean isSupportedPreset(final String preset) {
        return preset != null && SUPPORTED_PRESETS.contains(preset.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns a set of all supported presets.
     *
     * @return supported presets
     */
    public static Set<String> supportedPresets() {
        return SUPPORTED_PRESETS;
    }

    /* default */
    static String waitingAnchorActionBarMessage() {
        return WAITING_FOR_ANCHOR_ACTION_BAR;
    }

    /**
     * Returns the warp service instance.
     *
     * @return warp service
     */
    public WarpService warpService() {
        return warpServiceInstance;
    }

    /**
     * Starts a new graph creation session.
     *
     * @param playerId  player id
     * @param graphName name of the graph to create
     * @param settings  editor settings
     * @return operation result
     */
    public EditorResult startGraphCreation(final UUID playerId, final String graphName, final EditorSettings settings) {
        final EditorResult validation = validateStart(playerId, graphName, settings);
        if (!validation.success()) {
            return validation;
        }
        if (graphService.getGraphByName(graphName).isPresent()) {
            return EditorResult.failure("commands.bkeditor.common.graphAlreadyExists");
        }

        final EditorSession session = EditorSession.create(new Graph(graphName), settings.normalized());
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session);
        return EditorResult.success("commands.bkeditor.status.sessionStartedCreate", Map.of("graph", graphName));
    }

    /**
     * Renames the active graph in the session.
     *
     * @param playerId player id
     * @param newName  new name for the graph
     * @return operation result
     */
    public EditorResult renameActiveGraph(final UUID playerId, final String newName) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (newName == null || newName.isBlank()) {
            return EditorResult.failure("commands.bkeditor.common.graphNameRequired");
        }

        final Optional<Graph> existing = graphService.getGraphByName(newName);
        if (existing.isPresent() && existing.get().getGraphId() != session.graph.getGraphId()) {
            return EditorResult.failure("commands.bkeditor.common.graphAlreadyExists");
        }

        session.graph.setName(newName);
        return EditorResult.success("commands.bkeditor.status.graphRenamed", Map.of("graph", newName));
    }

    /**
     * Starts a graph editing session for an existing graph.
     *
     * @param playerId  player id
     * @param graphName name of the graph to edit
     * @param settings  editor settings
     * @return operation result
     */
    public EditorResult startGraphEdit(final UUID playerId, final String graphName, final EditorSettings settings) {
        final EditorResult validation = validateStart(playerId, graphName, settings);
        if (!validation.success()) {
            return validation;
        }

        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("commands.bkeditor.common.graphNotFound");
        }

        final EditorSession session = EditorSession.edit(graph.get(), settings.normalized());
        loadSessionInterGraphEdges(session);
        playerEditors.put(playerId, session);
        registerVisualizer(playerId, session);
        return EditorResult.success("commands.bkeditor.status.sessionStartedEdit", Map.of("graph", graphName));
    }

    private EditorResult validateStart(final UUID playerId, final String graphName, final EditorSettings settings) {
        if (playerEditors.containsKey(playerId)) {
            return EditorResult.failure("commands.bkeditor.common.alreadyEditing");
        }
        if (graphName == null || graphName.isBlank()) {
            return EditorResult.failure("commands.bkeditor.common.graphNameRequired");
        }
        if (settings == null) {
            return EditorResult.failure("commands.bkeditor.common.settingsRequired");
        }
        if (settings.nodeDistance <= 0) {
            return EditorResult.failure("commands.bkeditor.common.nodeDistancePositive");
        }
        if (!isSupportedPreset(settings.preset)) {
            return EditorResult.failure("commands.bkeditor.common.unknownPreset");
        }
        return EditorResult.success("");
    }

    private Optional<SelectedNode> selectExistingNode(final EditorSession session, final Location loc) {
        return visibleGraphs(session).stream()
                .flatMap(graph -> graph.getNodes().stream()
                        .map(node -> new SelectedNode(new NodeRef(graph.getGraphId(), node.graphId()), node,
                                graph.getName())))
                .filter(selection -> sameWorld(selection.node(), loc))
                .filter(selection -> distance(selection.node(), loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(selection -> distance(selection.node(), loc)));
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        return selectExistingNode(session, loc)
                .map(selection -> {
                    session.selectedNode = selection.node();
                    session.selectedNodeRef = selection.ref();
                    session.selectedEdge = null;
                    session.selectedInterGraphEdge = null;
                    rememberEdgeEndpoint(session, selection);
                    return EditorResult.success(selectedNodeMessage(session, selection));
                })
                .orElseGet(() -> EditorResult.failure("commands.bkeditor.common.noNearbyNode"));
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        final Optional<SelectedEdge> local = visibleGraphs(session).stream()
                .flatMap(graph -> graph.getEdges().stream().map(edge -> new SelectedEdge(graph, edge)))
                .filter(edge -> edgeDistance(edge.graph(), edge.edge(), loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(edge -> edgeDistance(edge.graph(), edge.edge(), loc)));
        final Optional<InterGraphEdge> interGraph = session.visibleInterGraphEdges().stream()
                .filter(edge -> interGraphEdgeDistance(session, edge, loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(edge -> interGraphEdgeDistance(session, edge, loc)));
        if (local.isEmpty() && interGraph.isEmpty()) {
            return EditorResult.failure("commands.bkeditor.common.noNearbyEdge");
        }
        final double localDistance = local.map(edge -> edgeDistance(edge.graph(), edge.edge(), loc)).orElse(Double.MAX_VALUE);
        final double interDistance = interGraph.map(edge -> interGraphEdgeDistance(session, edge, loc)).orElse(Double.MAX_VALUE);
        session.selectedNode = null;
        session.selectedNodeRef = null;
        clearEdgeEndpoints(session);
        if (localDistance <= interDistance) {
            final SelectedEdge selected = local.get();
            session.selectedEdge = selected.edge();
            session.selectedEdgeGraphId = selected.graph().getGraphId();
            session.selectedInterGraphEdge = null;
            return EditorResult.success("Selected edge " + selected.edge().edgeId() + " in graph "
                    + selected.graph().getName() + " (" + selected.graph().getGraphId() + ").");
        }
        session.selectedEdge = null;
        session.selectedEdgeGraphId = -1;
        session.selectedInterGraphEdge = interGraph.get();
        return EditorResult.success("Selected inter-graph edge " + interGraph.get().edgeId() + ".");
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedNode != null) {
            return EditorResult.success("Selected node " + session.selectedNode.graphId() + " in graph "
                    + graphLabel(session, session.selectedNodeRef) + ".");
        }
        if (session.selectedEdge != null) {
            return EditorResult.success("Selected edge " + session.selectedEdge.edgeId() + " from "
                    + session.selectedEdge.source() + " to " + session.selectedEdge.target() + " in graph "
                    + graphLabel(session, session.selectedEdgeGraphId) + ".");
        }
        if (session.selectedInterGraphEdge != null) {
            return EditorResult.success("Selected inter-graph edge " + session.selectedInterGraphEdge.edgeId()
                    + " from " + session.selectedInterGraphEdge.source() + " to "
                    + session.selectedInterGraphEdge.target() + ".");
        }
        return EditorResult.failure("commands.bkeditor.common.noSelection");
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        session.selectedNode = null;
        session.selectedNodeRef = null;
        session.selectedEdge = null;
        session.selectedInterGraphEdge = null;
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
            return SelectionTeleportResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedNode != null) {
            return SelectionTeleportResult.success("Teleported to selected node.",
                    location(playerLocation, session.selectedNode.x(), session.selectedNode.y(), session.selectedNode.z()));
        }
        if (session.selectedEdge != null) {
            final Optional<Location> midpoint = edgeMidpoint(session, session.selectedEdgeGraphId, session.selectedEdge,
                    playerLocation);
            return midpoint.map(location -> SelectionTeleportResult.success("Teleported to selected edge.", location))
                    .orElseGet(() -> SelectionTeleportResult.failure("The selected edge is incomplete."));
        }
        if (session.selectedInterGraphEdge != null) {
            final Optional<Location> midpoint = interGraphEdgeMidpoint(session, session.selectedInterGraphEdge,
                    playerLocation);
            return midpoint.map(location -> SelectionTeleportResult.success("Teleported to selected edge.", location))
                    .orElseGet(() -> SelectionTeleportResult.failure("The selected edge is incomplete."));
        }
        return SelectionTeleportResult.failure("commands.bkeditor.common.noSelection");
    }

    /**
     * Handles player movement within an editor session.
     *
     * @param playerId editor player id
     * @param loc      current location
     * @return operation result
     */
    public EditorResult handleMovement(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
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
        return session.graph.getNodes().stream()
                .filter(node -> sameWorld(node, loc))
                .filter(node -> distance(node, loc) <= EDIT_NODE_SELECTION_RADIUS)
                .min(Comparator.comparingDouble(node -> distance(node, loc)))
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

    /**
     * Enables preview mode for the editor.
     *
     * @param playerId editor player id
     * @return operation result
     */
    public EditorResult preview(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        session.placementMode = PlacementMode.PREVIEW;
        return EditorResult.success("Preview mode enabled.");
    }

    /**
     * Manually places a node at the specified location.
     *
     * @param playerId editor player id
     * @param loc      node location
     * @return operation result
     */
    public EditorResult placeNode(final UUID playerId, final Location loc) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (edgeType == null) {
            return EditorResult.failure("Edge type is required.");
        }
        if (session.edgeEndpointOne == null || session.edgeEndpointTwo == null) {
            return EditorResult.failure("Select two graph nodes before creating an edge.");
        }

        if (session.edgeEndpointOne.ref().graphDbId() == session.edgeEndpointTwo.ref().graphDbId()) {
            final Graph graph = graphById(session, session.edgeEndpointOne.ref().graphDbId());
            if (graph == null || graph.getGraphId() != session.graph.getGraphId()) {
                return EditorResult.failure("Local edge authoring is only available for the active graph.");
            }
            final List<Edge> created = replaceRelationship(graph, session.edgeEndpointOne.ref().nodeId(),
                    session.edgeEndpointTwo.ref().nodeId(), distance(session.edgeEndpointOne.node(),
                            session.edgeEndpointTwo.node()), edgeType, Set.of());
            session.selectedEdge = created.getFirst();
            session.selectedEdgeGraphId = graph.getGraphId();
            session.selectedInterGraphEdge = null;
        } else {
            if (session.mode == EditorMode.CREATE || session.edgeEndpointOne.ref().graphDbId() < 0
                    || session.edgeEndpointTwo.ref().graphDbId() < 0) {
                return EditorResult.failure("Inter-graph edges require persisted graph endpoints.");
            }
            final List<InterGraphEdge> created = replaceInterGraphRelationship(session, session.edgeEndpointOne.ref(),
                    session.edgeEndpointTwo.ref(), distance(session.edgeEndpointOne.node(), session.edgeEndpointTwo.node()),
                    edgeType, Set.of());
            session.selectedInterGraphEdge = created.getFirst();
            session.selectedEdge = null;
            session.selectedEdgeGraphId = -1;
        }
        session.selectedNode = null;
        session.selectedNodeRef = null;
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (edgeType == null) {
            return EditorResult.failure("Edge type is required.");
        }
        if (session.selectedEdge == null && session.selectedInterGraphEdge == null) {
            return EditorResult.failure("Select a graph edge before changing its type.");
        }

        if (session.selectedInterGraphEdge != null) {
            final InterGraphEdge selected = session.selectedInterGraphEdge;
            final List<InterGraphEdge> created = replaceInterGraphRelationship(session, selected.source(),
                    selected.target(), selected.cost(), edgeType, selected.flags());
            session.selectedInterGraphEdge = created.getFirst();
        } else {
            final Edge selected = session.selectedEdge;
            final Graph graph = graphById(session, session.selectedEdgeGraphId);
            final List<Edge> created = replaceRelationship(graph, selected.source(), selected.target(), selected.cost(),
                    edgeType, selected.flags());
            session.selectedEdge = created.getFirst();
        }
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (edgeState == null) {
            return EditorResult.failure("Edge state is required.");
        }
        if (session.selectedEdge == null && session.selectedInterGraphEdge == null) {
            return EditorResult.failure("Select a graph edge before changing its state.");
        }

        if (session.selectedInterGraphEdge != null) {
            final InterGraphEdge selected = session.selectedInterGraphEdge;
            final List<InterGraphEdge> updated = session.workspaceNetwork.updateInterGraphRelationshipBlocked(
                    selected.source(), selected.target(), edgeState == EdgeState.BLOCKED);
            session.selectedInterGraphEdge = updated.stream()
                    .filter(edge -> edge.edgeId().equals(selected.edgeId()))
                    .findFirst()
                    .orElseGet(() -> updated.isEmpty() ? null : updated.getFirst());
            session.workspaceVersion++;
        } else {
            final Edge selected = session.selectedEdge;
            final Graph graph = graphById(session, session.selectedEdgeGraphId);
            final List<Edge> updated = graph.updateRelationshipBlocked(selected.source(), selected.target(),
                    edgeState == EdgeState.BLOCKED);
            session.selectedEdge = updated.stream()
                    .filter(edge -> edge.edgeId().equals(selected.edgeId()))
                    .findFirst()
                    .orElseGet(() -> updated.isEmpty() ? null : updated.getFirst());
        }
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedEdge == null && session.selectedInterGraphEdge == null) {
            return EditorResult.failure("Select a graph edge before removing it.");
        }

        final int removed;
        if (session.selectedInterGraphEdge != null) {
            final InterGraphEdge selected = session.selectedInterGraphEdge;
            removed = session.workspaceNetwork.removeInterGraphEdgesBetween(selected.source(), selected.target());
            session.selectedInterGraphEdge = null;
            session.workspaceVersion++;
        } else {
            final Edge selected = session.selectedEdge;
            final Graph graph = graphById(session, session.selectedEdgeGraphId);
            removed = graph.removeEdgesBetween(selected.source(), selected.target()).size();
        }
        session.selectedEdge = null;
        session.selectedEdgeGraphId = -1;
        session.selectedNode = null;
        session.selectedNodeRef = null;
        clearEdgeEndpoints(session);
        refreshVisualizer(playerId);
        return EditorResult.success("Removed " + removed + " selected edge record" + (removed == 1 ? "." : "s."));
    }

    private boolean sameWorld(final Node node, final Location loc) {
        return loc.getWorld() != null && Objects.equals(node.worldId(), loc.getWorld().getUID());
    }

    private void rememberEdgeEndpoint(final EditorSession session, final SelectedNode node) {
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

    private boolean sameNode(final SelectedNode first, final SelectedNode second) {
        return first != null && second != null && first.ref().equals(second.ref());
    }

    private void clearEdgeEndpoints(final EditorSession session) {
        session.edgeEndpointOne = null;
        session.edgeEndpointTwo = null;
    }

    private String selectedNodeMessage(final EditorSession session, final SelectedNode node) {
        if (session.edgeEndpointOne != null && session.edgeEndpointTwo != null) {
            return "Selected node " + node.node().graphId() + " in graph " + node.graphName() + " ("
                    + node.ref().graphDbId() + "). Edge endpoints: "
                    + endpointLabel(session.edgeEndpointOne) + " -> " + endpointLabel(session.edgeEndpointTwo) + ".";
        }
        return "Selected node " + node.node().graphId() + " in graph " + node.graphName() + " ("
                + node.ref().graphDbId() + ").";
    }

    private String endpointLabel(final SelectedNode node) {
        return node.graphName() + "/" + node.node().graphId();
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

    private List<Edge> replaceRelationship(final Graph graph, final UUID source, final UUID target,
                                           final double cost, final EdgeType edgeType, final Set<EdgeFlag> flags) {
        return edgeType == EdgeType.DIRECTED
                ? List.of(graph.replaceDirectedRelationship(source, target, cost, flags))
                : graph.replaceUndirectedRelationship(source, target, cost, flags);
    }

    private double edgeDistance(final Graph graph, final Edge edge, final Location loc) {
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
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

    private double interGraphEdgeDistance(final EditorSession session, final InterGraphEdge edge, final Location loc) {
        final Node source = node(session, edge.source());
        final Node target = node(session, edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Double.MAX_VALUE;
        }
        return segmentDistance(source, target, loc);
    }

    private double segmentDistance(final Node source, final Node target, final Location loc) {
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

    private Optional<Location> edgeMidpoint(final EditorSession session, final int graphId, final Edge edge,
                                            final Location loc) {
        final Graph graph = graphById(session, graphId);
        if (graph == null) {
            return Optional.empty();
        }
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Optional.empty();
        }
        return Optional.of(location(loc, (source.x() + target.x()) / 2.0D, (source.y() + target.y()) / 2.0D,
                (source.z() + target.z()) / 2.0D));
    }

    private Optional<Location> interGraphEdgeMidpoint(final EditorSession session, final InterGraphEdge edge,
                                                      final Location loc) {
        final Node source = node(session, edge.source());
        final Node target = node(session, edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Optional.empty();
        }
        return Optional.of(location(loc, (source.x() + target.x()) / 2.0D, (source.y() + target.y()) / 2.0D,
                (source.z() + target.z()) / 2.0D));
    }

    private Location location(final Location source, final double targetX, final double targetY, final double targetZ) {
        return new Location(source.getWorld(), targetX, targetY, targetZ, source.getYaw(), source.getPitch());
    }

    /**
     * Finishes the route creation session and persists changes.
     *
     * @param playerId editor player id
     * @return operation result
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public EditorResult finishRouteCreation(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }

        if (plugin == null) {
            try {
                graphService.saveGraph(session.graph);
                saveWarps(session);
                saveInterGraphEdges(session);
                unregisterVisualizer(playerId);
            } catch (final Exception e) {
                log.errorF("The graph could not be saved: {}", e.getMessage());
                return EditorResult.failure("Failed to save the graph, aborting editing");
            } finally {
                playerEditors.remove(playerId);
            }
            return EditorResult.success("Route creation finished.");
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                graphService.saveGraph(session.graph);
                saveWarps(session);
                saveInterGraphEdges(session);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    playerEditors.remove(playerId);
                    unregisterVisualizer(playerId);
                    final Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage("Route creation finished.");
                    }
                });
            } catch (final Exception e) {
                log.errorF("The graph could not be saved: {}", e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    final Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage("Failed to save the graph: " + e.getMessage());
                    }
                });
            }
        });
        return EditorResult.success("Saving editor session...");
    }

    private void saveWarps(final EditorSession session) {
        if (warpServiceInstance == null) {
            return;
        }
        for (final String key : session.pendingDeletions) {
            warpServiceInstance.removeWarp(key);
        }
        for (final Warp warp : session.pendingWarps.values()) {
            warpServiceInstance.saveWarp(warp);
        }
    }

    private void saveInterGraphEdges(final EditorSession session) {
        if (graphNetworkService == null || session.mode != EditorMode.EDIT) {
            return;
        }
        graphNetworkService.saveInterGraphEdges(session.workspaceNetwork.getInterGraphEdges());
    }

    /**
     * Cancels the current editor session.
     *
     * @param playerId editor player id
     * @return operation result
     */
    public EditorResult cancel(final UUID playerId) {
        if (playerEditors.remove(playerId) == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }

        unregisterVisualizer(playerId);
        return EditorResult.success("Editor session cancelled.");
    }

    /**
     * Adds a reference graph to the editor view.
     *
     * @param playerId  editor player id
     * @param graphName name of the graph to add
     * @return operation result
     */
    public EditorResult addReferenceGraph(final UUID playerId, final String graphName) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("commands.bkeditor.common.graphNotFound");
        }
        if (graph.get().getGraphId() == session.graph.getGraphId()) {
            return EditorResult.failure("The active graph is already visible.");
        }
        session.referenceGraphs.put(graph.get().getGraphId(), graph.get().copy());
        loadSessionInterGraphEdges(session);
        session.workspaceVersion++;
        refreshVisualizer(playerId);
        return EditorResult.success("Added reference graph " + graph.get().getName() + " to the editor view.");
    }

    /**
     * Removes a reference graph from the editor view.
     *
     * @param playerId  editor player id
     * @param graphName name of the graph to remove
     * @return operation result
     */
    public EditorResult removeReferenceGraph(final UUID playerId, final String graphName) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("commands.bkeditor.common.graphNotFound");
        }
        if (session.referenceGraphs.remove(graph.get().getGraphId()) == null) {
            return EditorResult.failure("That graph is not visible in the editor view.");
        }
        loadSessionInterGraphEdges(session);
        clearSelection(playerId);
        session.workspaceVersion++;
        refreshVisualizer(playerId);
        return EditorResult.success("Removed reference graph " + graph.get().getName() + " from the editor view.");
    }

    /**
     * Clears all reference graphs from the editor view.
     *
     * @param playerId editor player id
     * @return operation result
     */
    public EditorResult clearReferenceGraphs(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        session.referenceGraphs.clear();
        loadSessionInterGraphEdges(session);
        clearSelection(playerId);
        session.workspaceVersion++;
        refreshVisualizer(playerId);
        return EditorResult.success("Cleared reference graphs from the editor view.");
    }

    /**
     * Updates the traversal kind of the selected edge.
     *
     * @param playerId  editor player id
     * @param traversal new traversal kind
     * @return operation result
     */
    public EditorResult updateSelectedEdgeTraversal(final UUID playerId, final EdgeTraversal traversal) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (traversal == null) {
            return EditorResult.failure("Edge traversal is required.");
        }
        if (session.selectedEdge == null && session.selectedInterGraphEdge == null) {
            return EditorResult.failure("Select a graph edge before changing its traversal.");
        }
        final boolean teleport = traversal == EdgeTraversal.TELEPORT;
        if (session.selectedInterGraphEdge != null) {
            final InterGraphEdge selected = session.selectedInterGraphEdge;
            final List<InterGraphEdge> updated = session.workspaceNetwork.updateInterGraphRelationshipTeleport(
                    selected.source(), selected.target(), teleport);
            session.selectedInterGraphEdge = updated.stream()
                    .filter(edge -> edge.edgeId().equals(selected.edgeId()))
                    .findFirst()
                    .orElseGet(() -> updated.isEmpty() ? null : updated.getFirst());
            updateInterGraphTeleportNodeFlags(session);
            session.workspaceVersion++;
        } else {
            final Edge selected = session.selectedEdge;
            final Graph graph = graphById(session, session.selectedEdgeGraphId);
            final List<Edge> updated = graph.updateRelationshipTeleport(selected.source(), selected.target(), teleport);
            session.selectedEdge = updated.stream()
                    .filter(edge -> edge.edgeId().equals(selected.edgeId()))
                    .findFirst()
                    .orElseGet(() -> updated.isEmpty() ? null : updated.getFirst());
            updateLocalTeleportNodeFlags(graph);
        }
        refreshVisualizer(playerId);
        return EditorResult.success("Set selected edge traversal to " + traversal.configValue() + ".");
    }

    /**
     * Lists connections for the selected node.
     *
     * @param playerId editor player id
     * @return operation result containing connection details
     */
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity", "PMD.AvoidDeeplyNestedIfStmts"})
    public EditorResult selectedNodeConnections(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedNodeRef == null) {
            return EditorResult.failure("Select a graph node before inspecting connections.");
        }
        final List<Component> lines = new ArrayList<>();
        final Graph graph = graphById(session, session.selectedNodeRef.graphDbId());
        if (graph != null) {
            final Set<String> seen = new LinkedHashSet<>();
            for (final Edge edge : graph.getEdges()) {
                if (edge.source().equals(session.selectedNodeRef.nodeId())
                        || edge.target().equals(session.selectedNodeRef.nodeId())) {
                    final UUID otherId = edge.source().equals(session.selectedNodeRef.nodeId())
                            ? edge.target() : edge.source();
                    final String key = Set.of(edge.source(), edge.target()).toString();
                    if (edge.flags().contains(EdgeFlag.UNDIRECTED) && !seen.add(key)) {
                        continue;
                    }
                    lines.add(connectionLine(session, graph.getGraphId(), otherId, edge.flags(),
                            edge.source().equals(session.selectedNodeRef.nodeId()) ? "out" : "in", "local"));
                }
            }
        }
        for (final InterGraphEdge edge : session.visibleInterGraphEdges()) {
            if (edge.source().equals(session.selectedNodeRef) || edge.target().equals(session.selectedNodeRef)) {
                final NodeRef other = edge.source().equals(session.selectedNodeRef) ? edge.target() : edge.source();
                lines.add(connectionLine(session, other.graphDbId(), other.nodeId(), edge.flags(),
                        edge.source().equals(session.selectedNodeRef) ? "out" : "in", "inter-graph"));
            }
        }
        if (lines.isEmpty()) {
            return EditorResult.success("Selected node has no known connections.");
        }
        return EditorResult.success(clickableConnections(lines));
    }

    private Component clickableConnections(final List<Component> lines) {
        Component result = Component.text("Selected node connections: ");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                result = result.append(Component.text("; "));
            }
            result = result.append(lines.get(index));
        }
        return result.append(Component.text("."));
    }

    /**
     * Deletes a persisted graph from the storage.
     *
     * @param graphName name of the graph to delete
     * @return operation result
     */
    public EditorResult deletePersistedGraph(final String graphName) {
        final Optional<Graph> graph = graphService.getGraphByName(graphName);
        if (graph.isEmpty()) {
            return EditorResult.failure("commands.bkeditor.common.graphNotFound");
        }
        final int graphId = graph.get().getGraphId();
        final boolean active = playerEditors.values().stream()
                .anyMatch(session -> session.graph.getGraphId() == graphId);
        if (active) {
            return EditorResult.failure("That graph is currently open in an editor session.");
        }
        final int removedWarps = warpServiceInstance == null ? 0 : warpServiceInstance.removeWarpsTargeting(
                graph.get().getNodes().stream().map(Node::graphId).toList());
        final int removedEdges = graphNetworkService == null ? 0 : graphNetworkService.deleteInterGraphEdgesForGraph(graphId);
        graphService.deleteGraph(graphId);
        graphService.reloadGraphs();
        return EditorResult.success("Deleted graph " + graph.get().getName() + ", removed " + removedWarps
                + " warp" + (removedWarps == 1 ? "" : "s") + " and " + removedEdges + " inter-graph edge record"
                + (removedEdges == 1 ? "" : "s") + ".");
    }

    /**
     * Deletes the selected node from the active graph.
     *
     * @param playerId editor player id
     * @return operation result
     */
    public EditorResult deleteSelectedNode(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedNodeRef == null || session.selectedNode == null) {
            return EditorResult.failure("Select a graph node before deleting it.");
        }
        if (session.selectedNodeRef.graphDbId() != session.graph.getGraphId()) {
            return EditorResult.failure("Only active graph nodes can be deleted.");
        }
        final UUID nodeId = session.selectedNode.graphId();
        session.graph.removeNode(session.selectedNode);
        session.pendingWarps.values().removeIf(warp -> warp.targetNodeId().equals(nodeId));
        if (warpServiceInstance != null) {
            warpServiceInstance.getWarpsTargeting(nodeId).forEach(warp -> session.pendingDeletions.add(warp.key()));
        }
        session.selectedNode = null;
        session.selectedNodeRef = null;
        session.selectedEdge = null;
        session.selectedInterGraphEdge = null;
        clearEdgeEndpoints(session);
        refreshVisualizer(playerId);
        return EditorResult.success("Deleted selected node " + nodeId + ".");
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

    /**
     * Resumes node placement from the current position or an anchor.
     *
     * @param playerId editor player id
     * @return operation result
     */
    public EditorResult continuePlacement(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }

        if (session.continueRequiresNode) {
            session.placementMode = PlacementMode.WAITING_FOR_ANCHOR;
            return EditorResult.success("Walk through a node to continue placement.", WAITING_FOR_ANCHOR_ACTION_BAR);
        }

        session.placementMode = PlacementMode.AUTO;
        return EditorResult.success("Automatic placement resumed.");
    }

    /**
     * Undoes the last node placements.
     *
     * @param playerId editor player id
     * @param amount   number of nodes to undo
     * @return operation result
     */
    public EditorResult undo(final UUID playerId, final int amount) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
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

    /**
     * Returns a summary of the current editor settings.
     *
     * @param playerId editor player id
     * @return operation result containing settings summary
     */
    public EditorResult settingsSummary(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }

        return EditorResult.success("Settings: node-distance=" + session.nodeDistance
                + ", placement=" + session.placementMode.configValue()
                + ", continue-requires-node=" + session.continueRequiresNode
                + ", preset=" + session.preset + ".");
    }

    /**
     * Updates the minimum distance between automatically placed nodes.
     *
     * @param playerId     editor player id
     * @param nodeDistance new node distance
     * @return operation result
     */
    public EditorResult updateNodeDistance(final UUID playerId, final int nodeDistance) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (nodeDistance <= 0) {
            return EditorResult.failure("commands.bkeditor.common.nodeDistancePositive");
        }
        session.nodeDistance = nodeDistance;
        return EditorResult.success("Node distance set to " + nodeDistance + ".");
    }

    /**
     * Updates the placement mode for the editor.
     *
     * @param playerId      editor player id
     * @param placementMode new placement mode
     * @return operation result
     */
    public EditorResult updatePlacementMode(final UUID playerId, final PlacementMode placementMode) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (session.selectedNode == null) {
            return EditorResult.failure("Select a graph node before creating a selected warp.");
        }
        if (session.selectedNodeRef == null || session.selectedNodeRef.graphDbId() != session.graph.getGraphId()) {
            return EditorResult.failure("Selected warps can only target the active graph.");
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        final Node node = session.graph.addNode(new Node(null, loc));
        session.selectedNode = node;
        session.selectedNodeRef = new NodeRef(session.graph.getGraphId(), node.graphId());
        session.selectedEdge = null;
        session.selectedInterGraphEdge = null;
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
            warp = warpServiceInstance.getWarp(key).orElse(null);
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
            warp = warpServiceInstance.getWarp(key).orElse(null);
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
        final Set<Warp> warps = new LinkedHashSet<>(warpServiceInstance.getWarpsTargeting(targetNodeId));
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
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (warpServiceInstance == null) {
            return EditorResult.failure("commands.bkeditor.common.warpStorageUnavailable");
        }
        final Collection<UUID> graphNodeIds = session.graph.getNodes().stream().map(Node::graphId).toList();
        final Set<Warp> warps = new LinkedHashSet<>(all ? warpServiceInstance.getManagedWarps() : warpServiceInstance.getWarpsTargeting(graphNodeIds));

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
                Collectors.joining(", ")) + ".");
    }

    private EditorResult validateWarpOperation(final UUID playerId, final String key) {
        if (!playerEditors.containsKey(playerId)) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        return validateWarp(key);
    }

    private EditorResult validateWarp(final String key) {
        if (warpServiceInstance == null) {
            return EditorResult.failure("commands.bkeditor.common.warpStorageUnavailable");
        }
        return key == null || key.isBlank() ? EditorResult.failure("commands.bkeditor.common.warpKeyRequired")
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

    private void loadSessionInterGraphEdges(final EditorSession session) {
        session.workspaceNetwork = new GraphNetwork();
        addPersistedGraph(session.workspaceNetwork, session.graph);
        session.referenceGraphs.values().forEach(graph -> addPersistedGraph(session.workspaceNetwork, graph));
        if (graphNetworkService == null || session.graph.getGraphId() < 0) {
            return;
        }
        final Set<Integer> graphIds = visibleGraphs(session).stream()
                .map(Graph::getGraphId)
                .filter(id -> id >= 0)
                .collect(Collectors.toSet());
        graphNetworkService.loadInterGraphEdges(graphIds).stream()
                .filter(edge -> graphIds.contains(edge.source().graphDbId()) && graphIds.contains(edge.target().graphDbId()))
                .forEach(session.workspaceNetwork::addInterGraphEdge);
    }

    private void addPersistedGraph(final GraphNetwork network, final Graph graph) {
        if (graph.getGraphId() >= 0) {
            network.addGraph(graph);
        }
    }

    private List<Graph> visibleGraphs(final EditorSession session) {
        final List<Graph> graphs = new ArrayList<>();
        graphs.add(session.graph);
        graphs.addAll(session.referenceGraphs.values());
        return graphs;
    }

    private Graph graphById(final EditorSession session, final int graphId) {
        if (session.graph.getGraphId() == graphId) {
            return session.graph;
        }
        return session.referenceGraphs.get(graphId);
    }

    private Node node(final EditorSession session, final NodeRef ref) {
        final Graph graph = graphById(session, ref.graphDbId());
        return graph == null ? null : graph.getNodeById(ref.nodeId());
    }

    private List<InterGraphEdge> replaceInterGraphRelationship(final EditorSession session, final NodeRef source,
                                                               final NodeRef target, final double cost,
                                                               final EdgeType edgeType, final Set<EdgeFlag> flags) {
        final List<InterGraphEdge> created = edgeType == EdgeType.DIRECTED
                ? List.of(session.workspaceNetwork.replaceDirectedInterGraphRelationship(source, target, cost, flags))
                : session.workspaceNetwork.replaceUndirectedInterGraphRelationship(source, target, cost, flags);
        session.workspaceVersion++;
        return created;
    }

    private void updateLocalTeleportNodeFlags(final Graph graph) {
        for (final Node node : graph.getNodes()) {
            final boolean used = graph.getEdges().stream()
                    .anyMatch(edge -> edge.flags().contains(EdgeFlag.TELEPORT)
                            && (edge.source().equals(node.graphId()) || edge.target().equals(node.graphId())));
            setNodeFlag(graph, node, NodeFlag.LOCAL_TELEPORT, used);
        }
    }

    private void updateInterGraphTeleportNodeFlags(final EditorSession session) {
        final Set<NodeRef> teleportNodes = session.workspaceNetwork.getInterGraphEdges().stream()
                .filter(edge -> edge.flags().contains(EdgeFlag.TELEPORT))
                .flatMap(edge -> Stream.of(edge.source(), edge.target()))
                .collect(Collectors.toSet());
        for (final Graph graph : visibleGraphs(session)) {
            for (final Node node : graph.getNodes()) {
                setNodeFlag(graph, node, NodeFlag.INTERGRAPH_TELEPORT,
                        teleportNodes.contains(new NodeRef(graph.getGraphId(), node.graphId())));
            }
        }
    }

    private void setNodeFlag(final Graph graph, final Node node, final NodeFlag flag, final boolean present) {
        if (node.flags().contains(flag) == present) {
            return;
        }
        final Set<NodeFlag> flags = node.flags().isEmpty() ? EnumSet.noneOf(NodeFlag.class) : EnumSet.copyOf(node.flags());
        if (present) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
        graph.updateNode(new Node(node.dbId(), node.graphId(), node.x(), node.y(), node.z(), node.worldId(), flags));
    }

    private Component connectionLine(final EditorSession session, final int graphId, final UUID nodeId,
                                     final Set<EdgeFlag> flags, final String direction, final String scope) {
        final Node node = node(session, new NodeRef(graphId, nodeId));
        final String location = node == null ? "unknown location"
                : String.format(Locale.ROOT, "%.1f %.1f %.1f", node.x(), node.y(), node.z());
        final String traversal = flags.contains(EdgeFlag.TELEPORT) ? "teleport" : "normal";
        final String state = flags.contains(EdgeFlag.BLOCKED) ? "blocked" : "open";
        final String edgeDirection = flags.contains(EdgeFlag.UNDIRECTED) ? "undirected" : direction;
        return Component.text(scope + " " + edgeDirection + " to ")
                .append(Component.text(nodeId.toString()).clickEvent(ClickEvent.copyToClipboard(nodeId.toString())))
                .append(Component.text(" in graph " + graphLabel(session, graphId)
                        + " at " + location + " [" + traversal + ", " + state + "]"));
    }

    private String graphLabel(final EditorSession session, final NodeRef ref) {
        return ref == null ? "unknown" : graphLabel(session, ref.graphDbId());
    }

    private String graphLabel(final EditorSession session, final int graphId) {
        final Graph graph = graphById(session, graphId);
        return graph == null ? String.valueOf(graphId) : graph.getName() + " (" + graphId + ")";
    }

    private void registerVisualizer(final UUID playerId, final EditorSession session) {
        if (visualizerRegistry == null) {
            return;
        }
        visualizerRegistry.register(playerId,
                GraphVisualizerFactory.particleEditorWorkspace(plugin, loggerFactory, () -> session.graph,
                        session.referenceGraphs::values, session::visibleInterGraphEdges,
                        () -> session.workspaceVersion, playerId, effectExecutor));
    }

    private void unregisterVisualizer(final UUID playerId) {
        if (visualizerRegistry != null) {
            visualizerRegistry.unregister(playerId);
        }
    }

    /**
     * Updates whether continuation requires an existing node anchor.
     *
     * @param playerId             editor player id
     * @param continueRequiresNode true if anchor is required
     * @return operation result
     */
    public EditorResult updateContinueRequiresNode(final UUID playerId, final boolean continueRequiresNode) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        session.continueRequiresNode = continueRequiresNode;
        return EditorResult.success("Continue requires node set to " + continueRequiresNode + ".");
    }

    /**
     * Gets the graph currently being created or edited.
     *
     * @param playerId editor player id
     * @return the working graph
     */
    public Graph getWorkingGraph(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session == null ? null : session.graph;
    }

    /**
     * Updates the visualizer preset for the editor session.
     *
     * @param playerId editor player id
     * @param preset   preset name
     * @return operation result
     */
    public EditorResult updatePreset(final UUID playerId, final String preset) {
        final EditorSession session = playerEditors.get(playerId);
        if (session == null) {
            return EditorResult.failure("commands.bkeditor.common.notEditing");
        }
        if (!isSupportedPreset(preset)) {
            return EditorResult.failure("commands.bkeditor.common.unknownPreset");
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

    /**
     * Gets the current editor settings for a player.
     *
     * @param playerId editor player id
     * @return editor settings
     */
    public EditorSettings getSettings(final UUID playerId) {
        final EditorSession session = playerEditors.get(playerId);
        return session == null ? null : session.settings();
    }

    /**
     * Placement mode for nodes.
     */
    public enum PlacementMode {
        /**
         * Automatic placement.
         */
        AUTO("auto"),

        /**
         * Preview placement.
         */
        PREVIEW("preview"),

        /**
         * Waiting for anchor placement.
         */
        WAITING_FOR_ANCHOR("waiting-for-anchor");

        private final String serializedValue;

        PlacementMode(final String configValue) {
            this.serializedValue = configValue;
        }

        /**
         * Parses the placement mode from a string.
         *
         * @param input the input string
         * @return the placement mode
         */
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

        /**
         * Returns the config value.
         *
         * @return the config value
         */
        public String configValue() {
            return serializedValue;
        }
    }

    /**
     * Edge type.
     */
    public enum EdgeType {
        /**
         * Directed edge.
         */
        DIRECTED("directed"),

        /**
         * Undirected edge.
         */
        UNDIRECTED("undirected");

        private final String serializedValue;

        EdgeType(final String configValue) {
            this.serializedValue = configValue;
        }

        /**
         * Parses the edge type from a string.
         *
         * @param input the input string
         * @return the edge type
         */
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

        /**
         * Returns the config value.
         *
         * @return the config value
         */
        public String configValue() {
            return serializedValue;
        }
    }

    /**
     * Edge state.
     */
    public enum EdgeState {
        /**
         * Open edge.
         */
        OPEN("open"),

        /**
         * Blocked edge.
         */
        BLOCKED("blocked");

        private final String serializedValue;

        EdgeState(final String configValue) {
            this.serializedValue = configValue;
        }

        /**
         * Parses the edge state from a string.
         *
         * @param input the input string
         * @return the edge state
         */
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

        /**
         * Returns the config value.
         *
         * @return the config value
         */
        public String configValue() {
            return serializedValue;
        }
    }

    /**
     * Edge traversal kind.
     */
    public enum EdgeTraversal {
        /**
         * Normal traversal.
         */
        NORMAL("normal"),

        /**
         * Teleport traversal.
         */
        TELEPORT("teleport");

        private final String serializedValue;

        EdgeTraversal(final String configValue) {
            this.serializedValue = configValue;
        }

        /**
         * Parses the edge traversal from a string.
         *
         * @param input the input string
         * @return the edge traversal
         */
        public static Optional<EdgeTraversal> parse(final String input) {
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }
            final String normalized = input.toLowerCase(Locale.ROOT);
            for (final EdgeTraversal traversal : values()) {
                if (traversal.serializedValue.equals(normalized) || traversal.name().equalsIgnoreCase(normalized)) {
                    return Optional.of(traversal);
                }
            }
            return Optional.empty();
        }

        /**
         * Returns the config value.
         *
         * @return the config value
         */
        public String configValue() {
            return serializedValue;
        }
    }

    private enum EditorMode {
        CREATE,
        EDIT
    }

    /**
     * Settings for the editor session.
     *
     * @param nodeDistance         minimum distance between nodes
     * @param placementMode        placement mode
     * @param continueRequiresNode whether continuation requires an existing node
     * @param preset               visualizer preset
     */
    public record EditorSettings(int nodeDistance, PlacementMode placementMode, boolean continueRequiresNode,
                                 String preset) {

        /**
         * Returns a normalized version of the settings.
         *
         * @return normalized settings
         */
        public EditorSettings normalized() {
            return new EditorSettings(nodeDistance, Objects.requireNonNullElse(placementMode, PlacementMode.AUTO),
                    continueRequiresNode, preset == null || preset.isBlank() ? "default" : preset.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Editor operation result.
     *
     * @param success          whether the operation succeeded
     * @param message          user-facing message
     * @param actionBarMessage action bar message
     * @param component        rich component message
     * @param replacements     message replacements
     */
    public record EditorResult(boolean success, String message, String actionBarMessage, Component component,
                               Map<String, String> replacements) {

        /**
         * Creates a new editor result.
         *
         * @param success whether successful
         * @param message message
         */
        public EditorResult(final boolean success, final String message) {
            this(success, message, "", null, Map.of());
        }

        /**
         * Creates a new editor result.
         *
         * @param success   whether successful
         * @param component component
         */
        public EditorResult(final boolean success, final Component component) {
            this(success, "", "", component, Map.of());
        }

        /**
         * Creates a successful result.
         *
         * @param message message
         * @return successful result
         */
        public static EditorResult success(final String message) {
            return new EditorResult(true, message);
        }

        /**
         * Creates a successful result with replacements.
         *
         * @param message      message
         * @param replacements replacements
         * @return successful result
         */
        public static EditorResult success(final String message, final Map<String, String> replacements) {
            return new EditorResult(true, message, "", null, replacements == null ? Map.of() : Map.copyOf(replacements));
        }

        /**
         * Creates a successful result with a component.
         *
         * @param component component
         * @return successful result
         */
        public static EditorResult success(final Component component) {
            return new EditorResult(true, component);
        }

        /**
         * Creates a successful result with an action bar message.
         *
         * @param message          message
         * @param actionBarMessage action bar message
         * @return successful result
         */
        public static EditorResult success(final String message, final String actionBarMessage) {
            return new EditorResult(true, message, actionBarMessage, null, Map.of());
        }

        /**
         * Creates a failed result.
         *
         * @param message message
         * @return failed result
         */
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

        /**
         * Creates a successful teleport result.
         *
         * @param message     message
         * @param destination destination
         * @return successful teleport result
         */
        public static SelectionTeleportResult success(final String message, final Location destination) {
            return new SelectionTeleportResult(EditorResult.success(message), destination);
        }

        /**
         * Creates a failed teleport result.
         *
         * @param message message
         * @return failed teleport result
         */
        public static SelectionTeleportResult failure(final String message) {
            return new SelectionTeleportResult(EditorResult.failure(message), null);
        }
    }

    private static final class EditorSession {

        private final EditorMode mode;

        private final Deque<Node> createdNodes = new LinkedList<>();

        private final Map<String, Warp> pendingWarps = new ConcurrentHashMap<>();

        private final Set<String> pendingDeletions = ConcurrentHashMap.newKeySet();

        private final Map<Integer, Graph> referenceGraphs = new ConcurrentHashMap<>();

        private final Graph graph;

        private GraphNetwork workspaceNetwork = new GraphNetwork();

        private long workspaceVersion;

        private int nodeDistance;

        private PlacementMode placementMode;

        private boolean continueRequiresNode;

        private Node selectedAppendNode;

        private Node selectedNode;

        private NodeRef selectedNodeRef;

        private Edge selectedEdge;

        private int selectedEdgeGraphId = -1;

        private InterGraphEdge selectedInterGraphEdge;

        private SelectedNode edgeEndpointOne;

        private SelectedNode edgeEndpointTwo;

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

        private List<InterGraphEdge> visibleInterGraphEdges() {
            return workspaceNetwork.getInterGraphEdges();
        }
    }

    private record SelectedNode(NodeRef ref, Node node, String graphName) {
    }

    private record SelectedEdge(Graph graph, Edge edge) {
    }
}

