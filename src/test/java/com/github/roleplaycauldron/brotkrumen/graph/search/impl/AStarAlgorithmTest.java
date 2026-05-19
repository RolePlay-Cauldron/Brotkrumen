package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the {@link AStarAlgorithm}.
 */
class AStarAlgorithmTest {

    private final UUID uuidOne = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

    private final UUID uuidTwo = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

    private final UUID uuidThree = UUID.fromString("5e074931-0be1-46be-ac64-e88eb686463b");

    private final UUID uuidFour = UUID.fromString("4d81b0bc-2071-4c58-ab3e-2d0217597427");

    private final UUID uuidFive = UUID.fromString("fa14be01-316e-4311-8fe1-5cb38c090eb1");

    private final UUID uuidSix = UUID.fromString("fe931e7e-330b-4cbc-8cbf-f6174c426652");

    private final UUID uuidSeven = UUID.fromString("4151132b-097d-46ab-941b-6491ceddf6cc");

    private Graph graph;

    private AStarAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        graph = new Graph("Test");

        graph.addNode(new Node(uuidOne, 0, 0, 0, null));
        graph.addNode(new Node(uuidTwo, 35, 15, 0, null));
        graph.addNode(new Node(uuidThree, 65, 5, 0, null));
        graph.addNode(new Node(uuidFour, 60, -20, 0, null));
        graph.addNode(new Node(uuidFive, 35, -5, 0, null));
        graph.addNode(new Node(uuidSix, 5, -25, 0, null));
        graph.addNode(new Node(uuidSeven, 90, -5, 0, null));

        graph.addUndirectedEdge(uuidOne, uuidTwo, 40, Set.of(EdgeFlag.BLOCKED));
        graph.addUndirectedEdge(uuidOne, uuidSix, 40);
        graph.addDirectedEdge(uuidOne, uuidFive, 40);
        graph.addUndirectedEdge(uuidTwo, uuidThree, 40);
        graph.addUndirectedEdge(uuidThree, uuidSeven, 40);
        graph.addUndirectedEdge(uuidSeven, uuidFour, 40);
        graph.addUndirectedEdge(uuidFour, uuidFive, 40);
        graph.addUndirectedEdge(uuidFive, uuidSix, 40);

        algorithm = new AStarAlgorithm();
    }

    @Test
    void testSuitability() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final Warp spawn = new Warp("Spawn", uuidOne, 1.0, true, false);
        final TeleportRules tpAllowed = new TeleportRules(true, true, true, List.of(spawn));

        assertTrue(algorithm.suitable(graph, tpNotAllowed), "Algorithm should be suitable");
        assertFalse(algorithm.suitable(graph, tpAllowed), "Algorithm should not be suitable with teleport allowed");
    }

    @Test
    void testAStarPathFinding() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final PathResult path = algorithm.findPathResult(graph, uuidOne, Set.of(uuidSeven), null, tpNotAllowed);

        assertNotNull(path, "The path should not be null");

        final List<UUID> actual = path.nodes().stream().map(NodeRef::nodeId).toList();
        final List<UUID> expected = List.of(uuidOne, uuidTwo, uuidThree, uuidSeven);

        assertIterableEquals(expected, actual, "The path should match expected");
    }

    @Test
    void testAStarBlockedPath() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final Predicate<Edge> filterWithoutBlocked = edge -> !edge.flags().contains(EdgeFlag.BLOCKED);

        final PathResult path = algorithm.findPathResult(graph, uuidOne, Set.of(uuidSeven), filterWithoutBlocked,
                tpNotAllowed);

        assertNotNull(path, "The path should not be null");

        final List<UUID> actual = path.nodes().stream().map(NodeRef::nodeId).toList();
        final List<UUID> expected = List.of(uuidOne, uuidFive, uuidFour, uuidSeven);

        assertIterableEquals(expected, actual, "The path should match expected");
    }
}
