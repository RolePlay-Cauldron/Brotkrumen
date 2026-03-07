package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The BlockDisplayVisualiser class is responsible for rendering visual representations
 * of graph nodes and edges using block displays within a defined area around a player.
 * This class extends the GraphVisualiser, making use of its graph and logging-related
 * functionalities. BlockDisplayVisualiser synchronizes, updates, and manages the visual
 * elements for nodes and edges based on proximity to a target player and defined parameters.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods"})
public class BlockDisplayVisualizer extends GraphVisualizer {

    private static final float DISPLAY_SCALE = 0.4f;

    private static final float EDGE_THICKNESS = 0.15f;

    private static final double EDGE_NODE_CLEARANCE = 0.8D;

    private static final float DISPLAY_HALF_EXTENT = DISPLAY_SCALE / 2.0f;

    private static final double VIEW_DISTANCE = 16.0D;

    private static final double SPAWN_DISTANCE_BUFFER = 16.0D;

    private static final int PATH_NODE_WINDOW_SIZE = 4;

    private static final int PATH_EDGE_WINDOW_SIZE = PATH_NODE_WINDOW_SIZE - 1;

    private static final double PATH_NODE_ACTIVATION_RADIUS_SQ = 16.0D;

    private final WrappedLogger log;

    private final Brotkrumen plugin;

    private final UUID ownerId;

    private final VisualMode mode;

    private final Map<UUID, Map<UUID, Edge>> edgesByNodes = new HashMap<>();

    private final Map<UUID, BlockDisplay> activeNodeDisplays = new HashMap<>();

    private final Map<UUID, BlockDisplay> activeEdgeDisplays = new HashMap<>();

    /**
     * A pool of reusable {@link BlockDisplay} objects used for managing and displaying
     * the visual representation of pathfinding nodes. The pool helps minimize
     * the overhead of creating and destroying displays by reusing existing ones
     * as needed during visual updates.
     * <p>
     * This field is primarily used to efficiently manage the node displays
     * visible to players during pathfinding visualization, ensuring that only
     * the necessary number of displays are active.
     */
    private final List<BlockDisplay> pathNodePool = new ArrayList<>();

    /**
     * A reusable pool of {@link BlockDisplay} objects used for visualizing edges
     * in pathfinding or graph-based visualizations. The objects in this pool
     * can be reused, spawned, or updated to reduce memory usage and minimize
     * performance overhead when rendering or managing edge displays.
     * <p>
     * This list acts as a cache of edge visualizations, synchronized and managed
     * to maintain efficient rendering of path edges in proximity to players or
     * defined graph structures. Objects in the pool may be dynamically
     * associated with edge data or cleared when no longer needed.
     */
    private final List<BlockDisplay> pathEdgePool = new ArrayList<>();

    private final Map<BlockDisplay, UUID> poolDisplayTargets = new HashMap<>();

    private final List<Node> path;

    private int pathNodeIndex;

    /**
     * Constructs a new {@code BlockDisplayVisualiser} instance for visualizing graph
     * structures and their associated displays, tailored for a specific player and visual mode.
     * The constructor initializes the required components, processes the provided
     * graph's edges, and prepares internal mappings of nodes and edges.
     *
     * @param plugin        The central plugin instance used to interface with the Minecraft
     *                      server environment and access shared resources.
     * @param loggerFactory A factory for creating loggers used for diagnostic and informational output.
     *                      The logger is scoped to this class.
     * @param graph         The graph instance whose nodes and edges are to be visualized.
     *                      This graph serves as the primary source of data for visualization.
     * @param ownerId       The UUID of the player who owns and interacts with the visualizer.
     *                      Used to customize the visualization experience for the specific player.
     * @param mode          The visual mode specifying the type and context of visualization.
     *                      This can determine how visuals appear or are managed for this instance.
     */
    public BlockDisplayVisualizer(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                  final Graph graph, final UUID ownerId, final VisualMode mode) {
        this(plugin, loggerFactory, graph, ownerId, mode, List.of());
    }

    /**
     * Constructs a new {@code BlockDisplayVisualiser} instance for visualizing graph
     * structures and their associated displays, tailored for a specific player and visual mode.
     * The constructor initializes the required components, processes the provided
     * graph's edges, and prepares internal mappings of nodes and edges.
     *
     * @param plugin        The central plugin instance used to interface with the Minecraft
     *                      server environment and access shared resources.
     * @param loggerFactory A factory for creating loggers used for diagnostic and informational output.
     *                      The logger is scoped to this class.
     * @param graph         The graph instance whose nodes and edges are to be visualized.
     *                      This graph serves as the primary source of data for visualization.
     * @param ownerId       The UUID of the player who owns and interacts with the visualizer.
     *                      Used to customize the visualization experience for the specific player.
     * @param mode          The visual mode specifying the type and context of visualization.
     *                      This can determine how visuals appear or are managed for this instance.
     * @param path          An optional list of nodes representing the path to follow.
     */
    public BlockDisplayVisualizer(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                  final Graph graph, final UUID ownerId, final VisualMode mode,
                                  final List<Node> path) {
        super(loggerFactory, graph);
        this.log = loggerFactory.create(BlockDisplayVisualizer.class);
        this.plugin = plugin;
        this.ownerId = ownerId;
        this.mode = mode;
        this.path = path;
        this.pathNodeIndex = 0;

        for (final Edge edge : graph.getEdges()) {
            edgesByNodes.computeIfAbsent(edge.source(), k -> new HashMap<>()).put(edge.target(), edge);
            edgesByNodes.computeIfAbsent(edge.target(), k -> new HashMap<>()).put(edge.source(), edge);
        }
    }

    @Override
    public void shutdown() {
        clearDisplays();
    }

    @Override
        /* default */
    void visibilityUpdate() {
        final Player player = plugin.getServer().getPlayer(ownerId);
        if (player == null) {
            log.errorF("Player for uuid '%s' was not found. This part should never be reached by default.", ownerId);
            return;
        }

        if (mode == VisualMode.PATH_FINDER) {
            updatePathFinderDisplays(player);
        } else {
            updateEditDisplays(player);
        }
    }

    /**
     * Updates the edit displays for the given player by synchronizing and managing
     * the visibility of node and edge displays within the specified view distance
     * and spawn buffer. This involves computing the set of nearby nodes and edges
     * and ensuring that the displays are correctly updated to reflect the current
     * state of the graph.
     *
     * @param player The player for whom the edit displays are being updated. This
     *               player will be shown or hidden displays based on their proximity
     *               to graph nodes and edges.
     */
    private void updateEditDisplays(final Player player) {
        final double spawnRadiusSq = spawnRadiusSquared(VIEW_DISTANCE, SPAWN_DISTANCE_BUFFER);
        final Location loc = player.getLocation();
        final Set<UUID> nearbyNodeIds = nodesWithin(loc, spawnRadiusSq);

        syncDisplays(activeNodeDisplays, nearbyNodeIds, nodeId -> {
            final Node node = graph.getNodeById(nodeId);
            return node != null ? spawnNodeDisplay(node.toCenterLocation(), Material.COAL_BLOCK) : null;
        }, player);

        final Set<UUID> activeEdgeIds = new HashSet<>();
        for (final UUID nodeId : nearbyNodeIds) {
            final Map<UUID, Edge> connections = edgesByNodes.get(nodeId);
            if (connections == null) {
                continue;
            }
            for (final Edge edge : connections.values()) {
                if (nearbyNodeIds.contains(edge.source()) && nearbyNodeIds.contains(edge.target())) {
                    activeEdgeIds.add(edge.edgeId());
                }
            }
        }

        syncDisplays(activeEdgeDisplays, activeEdgeIds, edgeId -> {
            final Edge edge = graph.getEdgeById(edgeId);
            return edge != null ? spawnEdgeDisplay(edge) : null;
        }, player);
    }

    /**
     * Synchronizes the display objects with the target display IDs. This method ensures
     * that displays not present in the target IDs are removed, and new displays
     * corresponding to the target IDs are created or shown to the player.
     *
     * @param registry  A mapping between UUIDs and the corresponding BlockDisplay objects.
     *                  This map is updated to reflect the latest display state.
     * @param targetIds A set of UUIDs representing the desired BlockDisplay objects to be
     *                  visible to the player.
     * @param spawner   A function that creates a new BlockDisplay object for a given UUID
     *                  if no valid display currently exists in the registry.
     * @param player    The player for whom the displays are being synchronized. New displays
     *                  are shown to this player.
     */
    private void syncDisplays(final Map<UUID, BlockDisplay> registry, final Set<UUID> targetIds,
                              final Function<UUID, BlockDisplay> spawner, final Player player) {
        registry.entrySet().removeIf(entry -> {
            if (!targetIds.contains(entry.getKey())) {
                removeDisplay(entry.getValue());
                return true;
            }
            return false;
        });

        for (final UUID id : targetIds) {
            BlockDisplay display = registry.get(id);
            if (display == null || !display.isValid()) {
                display = spawner.apply(id);
                if (display != null) {
                    registry.put(id, display);
                }
            }
            if (display != null) {
                player.showEntity(plugin, display);
            }
        }
    }

    private void updatePathFinderDisplays(final Player player) {
        final List<Node> allNodes = path != null ? path : new ArrayList<>(graph.getNodes());
        if (allNodes.isEmpty()) {
            return;
        }

        final Location loc = player.getLocation();
        final double locX = loc.getX();
        final double locY = loc.getY();
        final double locZ = loc.getZ();

        final int startIdx = Math.max(0, pathNodeIndex - 1);
        final int endIdx = Math.min(allNodes.size() - 1, pathNodeIndex + PATH_NODE_WINDOW_SIZE - 1);

        for (int i = endIdx; i >= startIdx; i--) {
            if (nodeDistanceSquared(locX, locY, locZ, allNodes.get(i)) < PATH_NODE_ACTIVATION_RADIUS_SQ) {
                pathNodeIndex = i;
                break;
            }
        }

        final double spawnRadiusSq = spawnRadiusSquared(VIEW_DISTANCE, SPAWN_DISTANCE_BUFFER);
        final List<Node> displayNodes = new ArrayList<>();
        for (int i = pathNodeIndex; i < Math.min(allNodes.size(), pathNodeIndex + PATH_NODE_WINDOW_SIZE); i++) {
            final Node node = allNodes.get(i);
            if (nodeDistanceSquared(locX, locY, locZ, node) <= spawnRadiusSq) {
                displayNodes.add(node);
            }
        }

        final List<Edge> visibleEdges = edgesBetweenNeighbours(displayNodes);

        updatePooledDisplays(pathNodePool, displayNodes, PATH_NODE_WINDOW_SIZE,
                (display, node) -> display.teleport(node.toCenterLocation()),
                () -> spawnNodeDisplay(player.getLocation(), Material.COAL_BLOCK), player,
                Node::graphId);

        updatePooledDisplays(pathEdgePool, visibleEdges, PATH_EDGE_WINDOW_SIZE,
                this::updateEdgeDisplayTransformation,
                () -> spawnPathEdgePlaceholder(player.getLocation()), player,
                Edge::edgeId);
    }

    /**
     * Updates the state of a pooled collection of {@code BlockDisplay} objects to synchronize with
     * a list of target data. This method ensures that the pool contains sufficient displays to match
     * the specified maximum size, and manages their visibility and updates using the provided updater
     * and spawner functions.
     *
     * @param <T>         The type of the target objects used to update the {@code BlockDisplay} instances.
     * @param pool        The list of pooled {@code BlockDisplay} objects to be updated. Displays are added
     *                    to this pool as needed to maintain the desired size.
     * @param targets     The list of target objects to be synchronized with the {@code BlockDisplay} pool.
     *                    These targets provide data for updating each valid display.
     * @param maxSize     The maximum size of the display pool. Ensures the pool contains at least this
     *                    many {@code BlockDisplay} objects by spawning new displays when necessary.
     * @param updater     A {@code BiConsumer} responsible for applying updates from a target object
     *                    to a {@code BlockDisplay}. This function customizes how displays are configured
     *                    based on target data.
     * @param spawner     A {@code Supplier} used to create new {@code BlockDisplay} objects when the pool
     *                    requires additional displays to meet the {@code maxSize}.
     * @param player      The player for whom the visibility of the {@code BlockDisplay} objects is managed.
     *                    Displays are shown or hidden based on their relevance to this player.
     * @param idExtractor A {@code Function} to extract a unique {@code UUID} for each target.
     */
    private <T> void updatePooledDisplays(final List<BlockDisplay> pool, final List<T> targets, final int maxSize,
                                          final BiConsumer<BlockDisplay, T> updater,
                                          final Supplier<BlockDisplay> spawner, final Player player,
                                          final Function<T, UUID> idExtractor) {
        while (pool.size() < maxSize) {
            final BlockDisplay display = spawner.get();
            if (display == null) {
                return;
            }
            pool.add(display);
        }

        for (int i = 0; i < pool.size(); i++) {
            final BlockDisplay display = pool.get(i);
            if (!display.isValid()) {
                continue;
            }

            if (i < targets.size()) {
                final T target = targets.get(i);
                final UUID targetId = idExtractor.apply(target);
                final UUID lastId = poolDisplayTargets.get(display);

                if (lastId == null || !lastId.equals(targetId)) {
                    player.hideEntity(plugin, display);
                    updater.accept(display, target);
                    player.showEntity(plugin, display);
                    poolDisplayTargets.put(display, targetId);
                } else {
                    player.showEntity(plugin, display);
                }
            } else {
                player.hideEntity(plugin, display);
                poolDisplayTargets.remove(display);
            }
        }
    }

    private List<Edge> edgesBetweenNeighbours(final List<Node> nodesInOrder) {
        final List<Edge> result = new ArrayList<>();
        for (int i = 0; i + 1 < nodesInOrder.size(); i++) {
            final Edge edge = findEdge(nodesInOrder.get(i).graphId(), nodesInOrder.get(i + 1).graphId());
            if (edge != null) {
                result.add(edge);
            }
        }
        return result;
    }

    private Edge findEdge(final UUID first, final UUID second) {
        final Map<UUID, Edge> connections = edgesByNodes.get(first);
        return connections != null ? connections.get(second) : null;
    }

    private BlockDisplay spawnNodeDisplay(final Location location, final Material material) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }

        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(material.createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(-DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT),
                    new Quaternionf(),
                    new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                    new Quaternionf()
            ));
        });
    }

    private BlockDisplay spawnPathEdgePlaceholder(final Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }

        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(-EDGE_THICKNESS / 2.0f, -EDGE_THICKNESS / 2.0f, 0.0f),
                    new Quaternionf(),
                    new Vector3f(EDGE_THICKNESS, EDGE_THICKNESS, 1.0f),
                    new Quaternionf()
            ));
        });
    }

    private BlockDisplay spawnEdgeDisplay(final Edge edge) {
        final Node source = graph.getNodeById(edge.source());
        if (source == null) {
            return null;
        }

        final Location loc = source.toCenterLocation();
        final BlockDisplay display = spawnPathEdgePlaceholder(loc);
        if (display != null) {
            updateEdgeDisplayTransformation(display, edge);
        }
        return display;
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void updateEdgeDisplayTransformation(final BlockDisplay display, final Edge edge) {
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
        if (source == null || target == null || !source.worldId().equals(target.worldId())) {
            return;
        }

        final Vector3f sourceCenter = new Vector3f((float) source.x() + 0.5f, (float) source.y() + 0.5f, (float) source.z() + 0.5f);
        final Vector3f targetCenter = new Vector3f((float) target.x() + 0.5f, (float) target.y() + 0.5f, (float) target.z() + 0.5f);
        final Vector3f direction = new Vector3f(targetCenter).sub(sourceCenter);
        final float distance = direction.length();

        if (distance <= 0.0f) {
            return;
        }

        final float displayLength = Math.max(0.0f, distance - (2.0f * (float) EDGE_NODE_CLEARANCE));
        final float startOffset = (distance - displayLength) / 2.0f;
        direction.normalize();

        final Vector3f displayStart = new Vector3f(sourceCenter).add(new Vector3f(direction).mul(startOffset));
        display.teleport(new Location(display.getWorld(), displayStart.x, displayStart.y, displayStart.z));

        final Quaternionf rotation = new Quaternionf().rotateTo(0.0f, 0.0f, 1.0f, direction.x, direction.y, direction.z);
        display.setTransformation(new Transformation(
                new Vector3f(-EDGE_THICKNESS / 2.0f, -EDGE_THICKNESS / 2.0f, 0.0f),
                rotation,
                new Vector3f(EDGE_THICKNESS, EDGE_THICKNESS, displayLength),
                new Quaternionf()
        ));
    }

    private void clearDisplays() {
        activeNodeDisplays.values().forEach(this::removeDisplay);
        activeEdgeDisplays.values().forEach(this::removeDisplay);
        pathNodePool.forEach(this::removeDisplay);
        pathEdgePool.forEach(this::removeDisplay);

        activeNodeDisplays.clear();
        activeEdgeDisplays.clear();
        pathNodePool.clear();
        pathEdgePool.clear();
        poolDisplayTargets.clear();
    }

    private void removeDisplay(final BlockDisplay display) {
        if (display != null && display.isValid()) {
            display.remove();
        }
        poolDisplayTargets.remove(display);
    }
}
