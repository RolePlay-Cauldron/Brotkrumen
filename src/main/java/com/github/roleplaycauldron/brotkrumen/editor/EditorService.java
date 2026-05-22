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
import java.util.Deque;
import java.util.LinkedList;
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
        "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects"})
public class EditorService {

    private static final double EDIT_NODE_SELECTION_RADIUS = 1.5D;

    private static final String WAITING_FOR_ANCHOR_ACTION_BAR =
            "Placement continues after walking through a node. /bkeditor continue also resumes it.";

    private static final Set<String> SUPPORTED_PRESETS = Set.of("default", "ember", "prism");

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

    private static final class EditorSession {

        private final EditorMode mode;

        private final Deque<Node> createdNodes = new LinkedList<>();

        private final Graph graph;

        private int nodeDistance;

        private PlacementMode placementMode;

        private boolean continueRequiresNode;

        private Node selectedAppendNode;

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
