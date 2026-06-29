package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BlockDisplayGraphRendererTest {

    private static final float EPSILON = 0.0001f;

    private static Vector3f centerlineStart(final BlockDisplayGraphRenderer.EdgePlacement placement,
                                            final BlockEdgeDesign design) {
        final float halfThickness = design.thickness() / 2.0f;
        final Vector3f offset = placement.rotation().transform(new Vector3f(halfThickness, halfThickness, 0.0f));
        return new Vector3f(placement.displayOrigin()).add(offset);
    }

    private static void assertVectorEquals(final Vector3f expected, final Vector3f actual) {
        assertEquals(expected.x, actual.x, EPSILON, "x");
        assertEquals(expected.y, actual.y, EPSILON, "y");
        assertEquals(expected.z, actual.z, EPSILON, "z");
    }

    @Test
    void edgePlacementCentersRotatedThicknessOnVerticalEdge() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(UUID.randomUUID(), 0.0D, 4.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(0.5f, 1.0f, 0.5f), centerlineStart(placement, design));
        assertEquals(3.0f, placement.length(), EPSILON, "The placement length should be 3.0");
    }

    @Test
    void edgePlacementCentersRotatedThicknessOnDiagonalEdge() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(UUID.randomUUID(), 3.0D, 4.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(0.8f, 0.9f, 0.5f), centerlineStart(placement, design));
        assertEquals(4.0f, placement.length(), EPSILON, "The placement length should be 4.0");
    }

    @Test
    void edgePlacementUsesBlockCentersForFractionalNodeCoordinates() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 10.8D, 64.0D, -3.2D, worldId);
        final Node target = new Node(UUID.randomUUID(), 12.1D, 64.0D, -3.2D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(11.0f, 64.5f, -3.5f), centerlineStart(placement, design));
        assertEquals(1.0f, placement.length(), EPSILON, "The placement length should be 1.0");
    }

    @Test
    void edgePlacementRejectsZeroLengthEdges() {
        final UUID worldId = UUID.randomUUID();
        final UUID nodeId = UUID.randomUUID();
        final Node source = new Node(nodeId, 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(nodeId, 0.0D, 0.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        assertNull(BlockDisplayGraphRenderer.edgePlacement(source, target, design), "The placement should be null for zero-length edges");
    }

    @Test
    void retainsAndHidesBlockDisplaysInsideSpawnBuffer() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final World world = mock(World.class);
        final List<BlockDisplay> spawned = new ArrayList<>();
        final Brotkrumen plugin = plugin(worldId, viewerId, world, spawned);
        final BlockDisplayGraphRenderer renderer = new BlockDisplayGraphRenderer(plugin, viewerId);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            renderer.apply(snapshot(worldId, 4, 6), ProfileGraphDesignResolver.defaults());
        }

        assertEquals(3, spawned.size(), "Two nodes and one retained edge should be spawned");
        verify(player(plugin, viewerId)).showEntity(plugin, spawned.get(0));
        verify(player(plugin, viewerId)).hideEntity(plugin, spawned.get(1));
        verify(player(plugin, viewerId)).hideEntity(plugin, spawned.get(2));
        spawned.forEach(display -> verify(display, never()).remove());
    }

    private Brotkrumen plugin(final UUID worldId, final UUID viewerId, final World world,
                              final List<BlockDisplay> spawned) {
        final Brotkrumen plugin = mock(Brotkrumen.class);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.viewDistance", 4.0D);
        config.set("visualizer.spawnDistanceBuffer", 2.0D);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPlayer(viewerId)).thenReturn(player);
        when(world.getUID()).thenReturn(worldId);
        when(player.getLocation()).thenReturn(new Location(world, 0.5D, 0.5D, 0.5D));
        when(world.spawn(any(Location.class), eq(BlockDisplay.class),
                any(Consumer.class))).thenAnswer(invocation -> {
            final BlockDisplay display = mock(BlockDisplay.class);
            when(display.isValid()).thenReturn(true);
            when(display.getWorld()).thenReturn(world);
            final Consumer<BlockDisplay> consumer = invocation.getArgument(2);
            consumer.accept(display);
            spawned.add(display);
            return display;
        });
        return plugin;
    }

    private Player player(final Brotkrumen plugin, final UUID viewerId) {
        return plugin.getServer().getPlayer(viewerId);
    }

    private VisualGraphSnapshot snapshot(final UUID worldId, final int visibleX, final int bufferedX) {
        final UUID firstNodeId = UUID.randomUUID();
        final UUID secondNodeId = UUID.randomUUID();
        final VisualNode first = visualNode(worldId, firstNodeId, visibleX);
        final VisualNode second = visualNode(worldId, secondNodeId, bufferedX);
        final VisualEdge edge = new VisualEdge(
                new LocalVisualEdgeId(1, UUID.randomUUID()),
                new NodeRef(1, firstNodeId),
                new NodeRef(1, secondNodeId),
                VisualEdgeKind.LOCAL,
                1.0D,
                Set.of(),
                VisualEdgeRole.DEFAULT_LOCAL
        );
        return new VisualGraphSnapshot(List.of(first, second), List.of(edge), 1L);
    }

    private VisualNode visualNode(final UUID worldId, final UUID nodeId, final int nodeX) {
        final NodeRef ref = new NodeRef(1, nodeId);
        return new VisualNode(new VisualNodeId(ref), ref, new Node(nodeId, nodeX, 0, 0, worldId));
    }
}
