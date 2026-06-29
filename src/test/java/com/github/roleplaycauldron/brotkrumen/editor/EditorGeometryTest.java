package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorGeometryTest {

    private static final double EPSILON = 0.0001D;

    @Test
    void distanceRejectsDifferentWorld() {
        final Node node = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, UUID.randomUUID());

        assertEquals(Double.MAX_VALUE, EditorGeometry.distance(node, location(UUID.randomUUID(), 0.0D, 0.0D, 0.0D)),
                "Nodes in a different world should not be selectable by distance");
    }

    @Test
    void segmentDistanceClampsToNearestEndpoint() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, worldId);

        assertEquals(1.0D, EditorGeometry.segmentDistance(source, target, location(worldId, 5.0D, 0.0D, 0.0D)),
                EPSILON, "Segment distance should clamp to the nearest endpoint");
    }

    @Test
    void edgeMidpointPreservesSourceWorldAndRotation() {
        final UUID worldId = UUID.randomUUID();
        final UUID sourceId = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();
        final Graph graph = new Graph(1, "Test");
        graph.addNode(new Node(sourceId, 0.0D, 0.0D, 0.0D, worldId));
        graph.addNode(new Node(targetId, 4.0D, 2.0D, 6.0D, worldId));
        final Edge edge = new Edge(UUID.randomUUID(), sourceId, targetId, 1.0D, Set.of());
        final Location origin = location(worldId, 10.0D, 20.0D, 30.0D);
        origin.setYaw(90.0f);
        origin.setPitch(30.0f);

        final Optional<Location> midpoint = EditorGeometry.edgeMidpoint(graph, edge, origin);

        assertTrue(midpoint.isPresent(), "Midpoint should exist for same-world endpoints");
        assertAll(
                () -> assertSame(origin.getWorld(), midpoint.get().getWorld(), "Midpoint should reuse source world"),
                () -> assertEquals(2.0D, midpoint.get().getX(), EPSILON, "Midpoint x should average endpoints"),
                () -> assertEquals(1.0D, midpoint.get().getY(), EPSILON, "Midpoint y should average endpoints"),
                () -> assertEquals(3.0D, midpoint.get().getZ(), EPSILON, "Midpoint z should average endpoints"),
                () -> assertEquals(90.0f, midpoint.get().getYaw(), EPSILON, "Midpoint should preserve yaw"),
                () -> assertEquals(30.0f, midpoint.get().getPitch(), EPSILON, "Midpoint should preserve pitch")
        );
    }

    private Location location(final UUID worldId, final double xCoordinate, final double yCoordinate,
                              final double zCoordinate) {
        final World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        return new Location(world, xCoordinate, yCoordinate, zCoordinate);
    }
}
