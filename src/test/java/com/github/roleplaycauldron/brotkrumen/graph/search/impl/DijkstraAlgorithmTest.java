package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the {@link DijkstraAlgorithm}.
 */
class DijkstraAlgorithmTest {

    private final UUID uuidOne = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

    private final UUID uuidTwo = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

    private final UUID uuidThree = UUID.fromString("5e074931-0be1-46be-ac64-e88eb686463b");

    private final UUID uuidFour = UUID.fromString("4d81b0bc-2071-4c58-ab3e-2d0217597427");

    private final UUID uuidFive = UUID.fromString("fa14be01-316e-4311-8fe1-5cb38c090eb1");

    private final UUID uuidSix = UUID.fromString("fe931e7e-330b-4cbc-8cbf-f6174c426652");

    private final UUID uuidSeven = UUID.fromString("4151132b-097d-46ab-941b-6491ceddf6cc");

    private Graph graph;

    private DijkstraAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        graph = new Graph("Test");

        graph.addNode(new Node(uuidOne, 0, 0, 0));
        graph.addNode(new Node(uuidTwo, 35, 15, 0));
        graph.addNode(new Node(uuidThree, 65, 5, 0));
        graph.addNode(new Node(uuidFour, 60, -20, 0));
        graph.addNode(new Node(uuidFive, 35, -5, 0));
        graph.addNode(new Node(uuidSix, 5, -25, 0));
        graph.addNode(new Node(uuidSeven, 90, -5, 0));

        graph.addUndirectedEdge(uuidOne, uuidTwo, 40, Set.of(EdgeFlag.BLOCKED));
        graph.addUndirectedEdge(uuidOne, uuidSix, 40);
        graph.addDirectedEdge(uuidOne, uuidFive, 40);
        graph.addUndirectedEdge(uuidTwo, uuidThree, 40);
        graph.addUndirectedEdge(uuidThree, uuidSeven, 40);
        graph.addUndirectedEdge(uuidSeven, uuidFour, 40);
        graph.addUndirectedEdge(uuidFour, uuidFive, 40);
        graph.addUndirectedEdge(uuidFive, uuidSix, 40);

        graph.addUndirectedEdge(uuidSix, uuidSeven, 1.0, Set.of(EdgeFlag.TELEPORT));

        algorithm = new DijkstraAlgorithm();
    }

    @Test
    void testDijkstraSuitability() {
        final Warp spawn = new Warp("Spawn", uuidOne, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        assertTrue(algorithm.suitable(graph, rules), "The algorithm should be suitable, but it isn't");
    }

    @Test
    void testDijkstraPathFinderWithLocalTeleport() {
        final Warp spawn = new Warp("Spawn", uuidOne, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final List<Node> pathNodes = algorithm.findPath(graph, uuidOne, uuidSeven, null, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<UUID> pathIds = pathNodes.stream().map(Node::graphId).collect(Collectors.toList());
        final List<UUID> expected = List.of(uuidOne, uuidSix, uuidSeven);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }

    @Test
    void testDijkstraPathFinderWithGlobalTeleport() {
        final Warp spawn = new Warp("Spawn", uuidOne, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final List<Node> pathNodes = algorithm.findPath(graph, uuidThree, uuidOne, null, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<UUID> pathIds = pathNodes.stream().map(Node::graphId).collect(Collectors.toList());
        final List<UUID> expected = List.of(uuidThree, uuidOne);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }

    @Test
    void testBlockedPathWithoutGlobalTeleport() {
        final Warp spawn = new Warp("Spawn", uuidOne, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final Predicate<Edge> filterWithoutGlobalTeleport =
                edge -> !edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL) && !edge.flags().contains(EdgeFlag.BLOCKED);

        final List<Node> pathNodes = algorithm.findPath(graph, uuidThree, uuidOne, filterWithoutGlobalTeleport, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<UUID> pathIds = pathNodes.stream().map(Node::graphId).collect(Collectors.toList());
        final List<UUID> expected = List.of(uuidThree, uuidSeven, uuidSix, uuidOne);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }
}
