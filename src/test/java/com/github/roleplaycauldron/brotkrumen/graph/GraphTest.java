package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Graph class.
 */
class GraphTest {

    @Test
    void testGraphCreateNode() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        assertEquals(2, graph.getNodes().size(), "The size of the graph should be 2");
        assertTrue(graph.getNodes().contains(nodeOne), "The node should exist in the graph");
    }

    @Test
    void testGraphCreateDirectedEdge() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        final Node nodeThree = new Node(3, 5, 7, 8);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        graph.addUndirectedEdge(nodeTwo.graphId(), nodeThree.graphId(), 1.0, Set.of(EdgeFlag.BLOCKED));

        assertNotNull(graph.getEdgeById(1), "The edge 1 should exist in the graph");
    }

    @Test
    void testGraphCreateUndirectedEdge() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        final Node nodeThree = new Node(3, 5, 7, 8);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        graph.addDirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);
        graph.addDirectedEdge(nodeTwo.graphId(), nodeThree.graphId(), 1.0, Set.of(EdgeFlag.BLOCKED));

        assertEquals(1, graph.neighbors(2).size(), "The size of the neighbours should be 1");
    }

    @Test
    void testGraphCreateUndirectedEdgeException() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addUndirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0),
                "An IllegalArgumentException should have been thrown");
    }

    @Test
    void testGraphException() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(nodeOne),
                "An IllegalArgumentException should have been thrown");
    }

    @Test
    void testAddEdgeWithoutFlags() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, null),
                "An IllegalArgumentException should have been thrown while trying to add an edge without flags");
    }

    @Test
    void testAddEdgeWithOneWayFlag() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        final List<Edge> resultEdges = graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, Set.of(EdgeFlag.DIRECTED));
        assertEquals(1, resultEdges.size(), "The size of the result edges should be 1");
    }

    @Test
    void testAddEdgeWithTwoWayFlag() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        final List<Edge> resultEdges = graph.addEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0, Set.of(EdgeFlag.TELEPORT));
        assertEquals(2, resultEdges.size(), "The size of the result edges should be 2");
    }

    @Test
    void testAddNodeWithRemovalAndReAcquire() {
        final Map<Integer, Node> nodes = new HashMap<>();
        final Node nodeOne = new Node(1, 2, 3, 4);
        nodes.put(1, nodeOne);
        nodes.put(2, new Node(2, 4, 5, 6));

        final IdRegistry idRegistry = new IdRegistry(1, 1);
        idRegistry.seedNodeIds(List.of(1, 2));

        final Map<Integer, List<Edge>> adjacency = new HashMap<>();
        adjacency.put(1, List.of(new Edge(1, 2, 1, 10, Set.of(EdgeFlag.DIRECTED))));

        final Graph graph = new Graph(nodes, adjacency, idRegistry);

        graph.removeNode(nodeOne);
        assertNull(graph.getNodeById(1), "The node should have been removed");

        graph.addNode(new Node(-1, 8, 9, 10));
        assertNotNull(graph.getNodeById(1), "The node with the id 1 should have been re-acquired");
    }

    @Test
    void testThrowExceptionOnNodeInUseAndRemovalOfNonExistent() {
        final Map<Integer, Node> nodes = new HashMap<>();
        nodes.put(1, new Node(1, 2, 3, 4));
        nodes.put(2, new Node(2, 4, 5, 6));

        final IdRegistry idRegistry = new IdRegistry(1, 1);
        idRegistry.seedNodeIds(List.of(1, 2));

        final Map<Integer, List<Edge>> adjacency = new HashMap<>();
        adjacency.put(1, List.of(new Edge(1, 2, 1, 10, Set.of(EdgeFlag.DIRECTED))));

        final Graph graph = new Graph(nodes, adjacency, idRegistry);

        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(new Node(10, 2, 3, 4)),
                "An IllegalArgumentException should have been thrown because the node is not in use");

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(new Node(1, 2, 5, 8)),
                "An IllegalArgumentException should have been thrown because the node-id is already in use");
    }

    @Test
    void testEdgeRemoval() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addUndirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        graph.removeEdge(graph.getEdgeById(1));
        assertNull(graph.getEdgeById(1), "The edge should have been removed");
    }
}
