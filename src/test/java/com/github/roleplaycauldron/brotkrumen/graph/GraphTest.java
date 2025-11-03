package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

        graph.addUndirectedEdge(nodeTwo.id(), nodeThree.id(), 1.0, Set.of(EdgeFlag.BLOCKED));

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

        graph.addDirectedEdge(nodeOne.id(), nodeTwo.id(), 1.0);
        graph.addDirectedEdge(nodeTwo.id(), nodeThree.id(), 1.0, Set.of(EdgeFlag.BLOCKED));

        assertEquals(1, graph.neighbors(2).size(), "The size of the neighbours should be 1");
    }

    @Test
    void testGraphCreateUndirectedEdgeException() {
        final Graph graph = new Graph();
        final Node nodeOne = new Node(1, 2, 3, 4);
        final Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addUndirectedEdge(nodeOne.id(), nodeTwo.id(), 1.0),
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
}
