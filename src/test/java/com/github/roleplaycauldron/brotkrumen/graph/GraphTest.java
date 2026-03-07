package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the {@link Graph} class.
 */
class GraphTest {
    private final UUID uuidOne = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

    private final UUID uuidTwo = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

    private final UUID uuidThree = UUID.fromString("5e074931-0be1-46be-ac64-e88eb686463b");

    private final UUID uuidSeven = UUID.fromString("4151132b-097d-46ab-941b-6491ceddf6cc");

    @Test
    void testGraphCreateNode() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        assertEquals(2, graph.getNodes().size(), "The size of the graph should be 2");
        assertTrue(graph.getNodes().contains(nodeOne), "The node should exist in the graph");
    }

    @Test
    void testGraphCreateDirectedEdge() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        final Node nodeThree = new Node(uuidThree, 5, 7, 8, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        final List<Edge> edge = graph.addUndirectedEdge(nodeTwo.graphId(), nodeThree.graphId(), 1.0, Set.of(EdgeFlag.BLOCKED));
        assertNotNull(graph.getEdgeById(edge.getFirst().edgeId()), "There should exist an Edge in the graph");
    }

    @Test
    void testGraphCreateUndirectedEdge() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        final Node nodeThree = new Node(uuidThree, 5, 7, 8, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        graph.addDirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);
        graph.addDirectedEdge(nodeTwo.graphId(), nodeThree.graphId(), 1.0, Set.of(EdgeFlag.BLOCKED));

        assertEquals(1, graph.neighbors(uuidTwo).size(), "The size of the neighbours should be 1");
    }

    @Test
    void testGraphCreateUndirectedEdgeException() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addUndirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0),
                "An IllegalArgumentException should have been thrown");
    }

    @Test
    void testGraphException() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(nodeOne),
                "An IllegalArgumentException should have been thrown");
    }

    @Test
    void testAddEdgeWithoutFlags() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, null),
                "An IllegalArgumentException should have been thrown while trying to add an edge without flags");
    }

    @Test
    void testAddEdgeWithOneWayFlag() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        final List<Edge> resultEdges = graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, Set.of(EdgeFlag.DIRECTED));
        assertEquals(1, resultEdges.size(), "The size of the result edges should be 1");
    }

    @Test
    void testAddEdgeWithTwoWayFlag() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        final List<Edge> resultEdges = graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, Set.of(EdgeFlag.TELEPORT));
        assertEquals(2, resultEdges.size(), "The size of the result edges should be 2");
    }

    @Test
    void testThrowExceptionOnNodeInUseAndRemovalOfNonExistent() {
        final Map<UUID, Node> nodes = new HashMap<>();
        nodes.put(uuidOne, new Node(uuidOne, 2, 3, 4, null));
        nodes.put(uuidTwo, new Node(uuidTwo, 4, 5, 6, null));

        final Map<UUID, List<Edge>> adjacency = new HashMap<>();
        adjacency.put(uuidOne, List.of(new Edge(UUID.randomUUID(), uuidTwo, uuidOne, 10, Set.of(EdgeFlag.DIRECTED))));

        final Graph graph = new Graph(-1, "Test", nodes, adjacency);

        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(new Node(uuidSeven, 2, 3, 4, null)),
                "An IllegalArgumentException should have been thrown because the node is not in use");

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(new Node(uuidOne, 2, 5, 8, null)),
                "An IllegalArgumentException should have been thrown because the node-id is already in use");
    }

    @Test
    void testEdgeRemoval() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        final List<Edge> edges = graph.addUndirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        graph.removeEdge(graph.getEdgeById(edges.getFirst().edgeId()));
        assertNull(graph.getEdgeById(edges.getFirst().edgeId()), "The edge should have been removed");
    }
}
