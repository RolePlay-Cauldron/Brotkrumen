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
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void undirectedEdgesPreserveUndirectedFlag() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        final List<Edge> resultEdges = graph.addUndirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        assertEquals(2, resultEdges.size(), "Undirected creation should still create two adjacency edges");
        assertTrue(resultEdges.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)),
                "Both adjacency edges should retain undirected semantics");
        assertTrue(resultEdges.stream().noneMatch(edge -> edge.flags().contains(EdgeFlag.DIRECTED)),
                "Undirected adjacency edges should not be reclassified as directed");
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

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void testDirectedEdgeRemovalDoesNotRequireTargetAdjacency() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        final Edge edge = graph.addDirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        assertDoesNotThrow(() -> graph.removeEdge(edge), "Directed edge removal should not require target adjacency");
        assertNull(graph.getEdgeById(edge.edgeId()), "Directed edge should be removed from id index");
        assertTrue(graph.neighbors(nodeOne.graphId()).isEmpty(), "Directed edge should be removed from source adjacency");
    }

    @Test
    void testNodeRemovalCleansIncomingEdges() {
        final Graph graph = new Graph("Test");
        final Node nodeOne = new Node(uuidOne, 2, 3, 4, null);
        final Node nodeTwo = new Node(uuidTwo, 3, 4, 5, null);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        final Edge edge = graph.addDirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        graph.removeNode(nodeTwo);

        assertNull(graph.getEdgeById(edge.edgeId()), "Incoming edge should be removed from id index");
        assertTrue(graph.neighbors(nodeOne.graphId()).isEmpty(), "Incoming edge should be removed from source adjacency");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void copyPreservesIdsAndCanBeMutatedIndependently() {
        final Graph graph = new Graph(7, "Original");
        final Node nodeOne = graph.addNode(new Node(uuidOne, 2, 3, 4, null));
        final Node nodeTwo = graph.addNode(new Node(uuidTwo, 3, 4, 5, null));
        final Edge edge = graph.addDirectedEdge(nodeOne.graphId(), nodeTwo.graphId(), 1.0);

        final Graph copy = graph.copy();
        copy.setName("Changed");
        copy.removeEdge(copy.getEdgeById(edge.edgeId()));

        assertEquals(7, copy.getGraphId(), "Copy should preserve graph id");
        assertEquals("Original", graph.getName(), "Original graph name should be unchanged");
        assertEquals("Changed", copy.getName(), "Copy graph name should change independently");
        assertNotNull(graph.getEdgeById(edge.edgeId()), "Original graph edge should be unchanged");
        assertNull(copy.getEdgeById(edge.edgeId()), "Copy graph edge should be removed independently");
        assertNotSame(graph.getNodeById(nodeOne.graphId()), copy.getNodeById(nodeOne.graphId()),
                "Copy should contain detached node instances");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void relationshipReplacementRewritesPair() {
        final Graph graph = graphWithTwoNodes();
        graph.addUndirectedEdge(uuidOne, uuidTwo, 1.0);

        final Edge directed = graph.replaceDirectedRelationship(uuidTwo, uuidOne, 2.0, Set.of(EdgeFlag.BLOCKED));

        assertEquals(1, graph.getEdgesBetween(uuidOne, uuidTwo).size(),
                "Directed replacement should leave one edge record");
        assertEquals(uuidTwo, directed.source(), "Directed replacement should preserve requested source");
        assertEquals(uuidOne, directed.target(), "Directed replacement should preserve requested target");
        assertTrue(directed.flags().contains(EdgeFlag.DIRECTED), "Replacement should be directed");
        assertTrue(directed.flags().contains(EdgeFlag.BLOCKED), "Replacement should preserve blocked state");
        assertFalse(directed.flags().contains(EdgeFlag.UNDIRECTED), "Directed replacement should remove undirected flag");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void undirectedRelationshipReplacementCreatesReciprocalRecords() {
        final Graph graph = graphWithTwoNodes();
        graph.addDirectedEdge(uuidOne, uuidTwo, 1.0);

        final List<Edge> edges = graph.replaceUndirectedRelationship(uuidOne, uuidTwo, 3.0, Set.of(EdgeFlag.BLOCKED));

        assertEquals(2, edges.size(), "Undirected replacement should create reciprocal records");
        assertEquals(2, graph.getEdgesBetween(uuidOne, uuidTwo).size(),
                "Undirected replacement should leave two pair records");
        assertTrue(edges.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)),
                "Both records should be undirected");
        assertTrue(edges.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.BLOCKED)),
                "Both records should preserve blocked state");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void relationshipBlockedStateUpdatesAllPairRecords() {
        final Graph graph = graphWithTwoNodes();
        graph.addUndirectedEdge(uuidOne, uuidTwo, 1.0);

        final List<Edge> blocked = graph.updateRelationshipBlocked(uuidOne, uuidTwo, true);
        final List<Edge> opened = graph.updateRelationshipBlocked(uuidOne, uuidTwo, false);

        assertTrue(blocked.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.BLOCKED)),
                "Blocking should mark every record");
        assertTrue(opened.stream().noneMatch(edge -> edge.flags().contains(EdgeFlag.BLOCKED)),
                "Opening should remove blocked from every record");
        assertTrue(opened.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)),
                "Opening should preserve relationship type");
    }

    private Graph graphWithTwoNodes() {
        final Graph graph = new Graph("Test");
        graph.addNode(new Node(uuidOne, 2, 3, 4, null));
        graph.addNode(new Node(uuidTwo, 3, 4, 5, null));
        return graph;
    }
}
