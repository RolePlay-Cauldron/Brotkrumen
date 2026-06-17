package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code GraphNetwork} class and its functionality.
 * <p>
 * This test suite verifies the correct implementation of graph network features,
 * including unified graph construction, inter-graph edge constraints, handling of
 * and undirected inter-graph edges.
 */
class GraphNetworkTest {

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void buildsUnifiedGraphWithLocalAndInterGraphEdges() {
        final Graph graphOne = new Graph(1, "One");
        final UUID nodeOneA = UUID.fromString("3643fcf8-5776-4624-a597-e73f9f8c12d2");
        final UUID nodeOneB = UUID.fromString("2f5619da-6f4d-4e04-857b-d0704b53ca32");
        graphOne.addNode(new Node(nodeOneA, 0, 64, 0, null));
        graphOne.addNode(new Node(nodeOneB, 1, 64, 0, null));
        graphOne.addDirectedEdge(nodeOneA, nodeOneB, 1.0, Set.of(EdgeFlag.DIRECTED));

        final Graph graphTwo = new Graph(2, "Two");
        final UUID nodeTwoA = UUID.fromString("bf2e04af-f412-4d3d-92de-1d91247fcd19");
        graphTwo.addNode(new Node(nodeTwoA, 10, 64, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);

        assertTrue(network.hasGraph(1), "Graph One should exist");
        assertEquals(graphOne, network.getGraph(1), "Graph One should be retrieved");
        assertEquals(2, network.getGraphs().size(), "Both graphs should be present");

        network.addDirectedInterGraphEdge(new NodeRef(1, nodeOneB), new NodeRef(2, nodeTwoA), 3.5);

        final GraphNetwork.UnifiedGraph unifiedGraph = network.toUnifiedGraph();

        assertEquals(3, unifiedGraph.graph().getNodes().size(), "All nodes should exist in unified graph");
        assertEquals(2, unifiedGraph.graph().getEdges().size(), "Local + inter-graph edge should exist");
    }

    @Test
    void rejectsInterGraphEdgeWithUnknownNode() {
        final Graph graphOne = new Graph(1, "One");
        final UUID existingNode = UUID.fromString("3643fcf8-5776-4624-a597-e73f9f8c12d2");
        graphOne.addNode(new Node(existingNode, 0, 64, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);

        assertThrows(IllegalArgumentException.class, () -> network.addInterGraphEdge(new InterGraphEdge(UUID.randomUUID(),
                new NodeRef(1, existingNode),
                new NodeRef(1, UUID.fromString("11111111-1111-1111-1111-111111111111")),
                1.0,
                Set.of(EdgeFlag.INTER_GRAPH),
                true)), "Invalid node references must be rejected");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void createsTwoDirectedEdgesForUndirectedInterGraphEdge() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID nodeOne = UUID.fromString("3643fcf8-5776-4624-a597-e73f9f8c12d2");
        final UUID nodeTwo = UUID.fromString("bf2e04af-f412-4d3d-92de-1d91247fcd19");
        graphOne.addNode(new Node(nodeOne, 0, 64, 0, null));
        graphTwo.addNode(new Node(nodeTwo, 10, 64, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);

        final List<InterGraphEdge> created = network.addUndirectedInterGraphEdge(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo), 4.0,
                Set.of(EdgeFlag.INTER_GRAPH, EdgeFlag.UNDIRECTED));

        assertEquals(2, created.size(), "Undirected inter-graph edge should create two directed edges");
        assertEquals(1, network.getOutgoingInterEdges(new NodeRef(1, nodeOne)).size(), "Forward edge should be present");
        assertEquals(1, network.getOutgoingInterEdges(new NodeRef(2, nodeTwo)).size(), "Backward edge should be present");
        assertTrue(created.stream().allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)),
                "Undirected inter-graph edges should keep undirected semantics");
        assertTrue(created.stream().noneMatch(edge -> edge.flags().contains(EdgeFlag.DIRECTED)),
                "Undirected inter-graph edges should not be reclassified as directed");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void removesGraphAndEdges() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID nodeOne = UUID.randomUUID();
        final UUID nodeTwo = UUID.randomUUID();
        graphOne.addNode(new Node(nodeOne, 0, 0, 0, null));
        graphTwo.addNode(new Node(nodeTwo, 10, 10, 10, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addUndirectedInterGraphEdge(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo), 5.0);

        assertEquals(2, network.getInterGraphEdges().size(), "Edges to/from both graphs should exist");

        network.removeGraph(2);

        assertFalse(network.hasGraph(2), "Graph should have been removed");
        assertEquals(0, network.getInterGraphEdges().size(), "Edges to/from removed graph should be gone");
    }

    @Test
    void removesSpecificInterGraphEdges() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID nodeOne = UUID.randomUUID();
        final UUID nodeTwo = UUID.randomUUID();
        graphOne.addNode(new Node(nodeOne, 0, 0, 0, null));
        graphTwo.addNode(new Node(nodeTwo, 10, 10, 10, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addDirectedInterGraphEdge(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo), 5.0);

        assertEquals(1, network.getOutgoingInterEdges(new NodeRef(1, nodeOne)).size(), "Edge should exist");

        network.removeInterGraphEdges(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo));
        assertEquals(0, network.getOutgoingInterEdges(new NodeRef(1, nodeOne)).size(), "Edges should be removed");
    }

    @Test
    void removesInterGraphEdgesTouchingNodeInEitherDirection() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID nodeOne = UUID.randomUUID();
        final UUID nodeTwo = UUID.randomUUID();
        graphOne.addNode(new Node(nodeOne, 0, 0, 0, null));
        graphTwo.addNode(new Node(nodeTwo, 10, 10, 10, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addUndirectedInterGraphEdge(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo), 5.0);

        assertEquals(2, network.removeInterGraphEdgesTouching(new NodeRef(1, nodeOne)),
                "Both directions should be removed when a node is deleted");
        assertTrue(network.getInterGraphEdges().isEmpty(), "No edge may reference the deleted node");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void cleansUpDisconnectedGraphs() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final Graph graphThree = new Graph(3, "Three");
        final UUID nodeOne = UUID.randomUUID();
        final UUID nodeTwo = UUID.randomUUID();
        graphOne.addNode(new Node(nodeOne, 0, 0, 0, null));
        graphTwo.addNode(new Node(nodeTwo, 10, 10, 10, null));
        graphThree.addNode(new Node(UUID.randomUUID(), 20, 20, 20, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addGraph(graphThree);

        network.addDirectedInterGraphEdge(new NodeRef(1, nodeOne), new NodeRef(2, nodeTwo), 1.0);

        assertFalse(network.removeDisconnectedGraphs(), "Should find and remove disconnected graph");
        assertFalse(network.hasGraph(3), "Graph Three should have been removed");
        assertTrue(network.hasGraph(1), "Graph One should remain");
        assertTrue(network.hasGraph(2), "Graph Two should remain");
    }
}
