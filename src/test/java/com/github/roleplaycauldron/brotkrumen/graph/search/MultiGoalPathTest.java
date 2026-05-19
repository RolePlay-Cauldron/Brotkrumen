package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The MultiGoalPathTest class contains unit tests for verifying the behavior of pathfinding
 * in a graph-based system. Tests focus on scenarios involving finding the shortest path
 * to multiple goals, determining the shortest entry point into a target graph,
 * and handling cases where the start node is already part of the target graph.
 */
class MultiGoalPathTest {

    @Test
    void findsShortestPathToMultipleGoalsInSingleGraph() {
        final Graph graph = new Graph("Test");
        final UUID start = UUID.randomUUID();
        final UUID goal1 = UUID.randomUUID();
        final UUID goal2 = UUID.randomUUID();
        final UUID middle = UUID.randomUUID();

        graph.addNode(new Node(start, 0, 0, 0, null));
        graph.addNode(new Node(middle, 5, 0, 0, null));
        graph.addNode(new Node(goal1, 10, 0, 0, null));
        graph.addNode(new Node(goal2, 2, 0, 0, null)); // Näher am Start

        graph.addDirectedEdge(start, middle, 5.0, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(middle, goal1, 5.0, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(start, goal2, 2.0, Set.of(EdgeFlag.DIRECTED));

        final PathFinder pathFinder = new PathFinder();
        final List<NodeRef> path = pathFinder.findPathResult(graph, start, Set.of(goal1, goal2), null,
                TeleportRules.disableTeleports()).nodes();

        assertEquals(2, path.size(), "Should find a path to both goals");
        assertEquals(new NodeRef(graph.getGraphId(), goal2), path.get(1), "Should target goal2 as it is closer");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void findsPathToTargetGraphViaShortestEntryPoint() {
        // Graph 1: Start -> EntryA (Cost 10), Start -> EntryB (Cost 5)
        // Graph 2: TargetGraph mit Nodes EntryA, EntryB

        final Graph graph1 = new Graph(1, "StartGraph");
        final UUID startNode = UUID.randomUUID();
        final UUID exitA = UUID.randomUUID();
        final UUID exitB = UUID.randomUUID();
        graph1.addNode(new Node(startNode, 0, 0, 0, null));
        graph1.addNode(new Node(exitA, 10, 0, 0, null));
        graph1.addNode(new Node(exitB, 5, 0, 0, null));
        graph1.addDirectedEdge(startNode, exitA, 10.0, Set.of(EdgeFlag.DIRECTED));
        graph1.addDirectedEdge(startNode, exitB, 5.0, Set.of(EdgeFlag.DIRECTED));

        final Graph graph2 = new Graph(2, "TargetGraph");
        final UUID targetA = UUID.randomUUID();
        final UUID targetB = UUID.randomUUID();
        graph2.addNode(new Node(targetA, 11, 0, 0, null));
        graph2.addNode(new Node(targetB, 6, 0, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph1);
        network.addGraph(graph2);

        network.addInterGraphEdge(new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, exitA), new NodeRef(2, targetA), 1.0, Set.of(EdgeFlag.INTER_GRAPH), true));
        network.addInterGraphEdge(new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, exitB), new NodeRef(2, targetB), 1.0, Set.of(EdgeFlag.INTER_GRAPH), true));

        final PathFinder pathFinder = new PathFinder();
        final List<NodeRef> path = pathFinder.findPathResult(network, new NodeRef(1, startNode), 2, null,
                TeleportRules.disableTeleports()).nodes();

        assertFalse(path.isEmpty(), "Path should be found");
        assertEquals(new NodeRef(2, targetB), path.getLast(), "Should target targetB as it is closer");
        assertEquals(3, path.size(), "Path: Start -> exitB -> targetB");
    }

    @Test
    void returnsStartNodeIfAlreadyInTargetGraph() {
        final Graph graph = new Graph(1, "Graph");
        final UUID node = UUID.randomUUID();
        graph.addNode(new Node(node, 0, 0, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);

        final PathFinder pathFinder = new PathFinder();
        final List<NodeRef> path = pathFinder.findPathResult(network, new NodeRef(1, node), 1, null,
                TeleportRules.disableTeleports()).nodes();

        assertEquals(1, path.size(), "Should return the start node if it is already in the target graph");
        assertEquals(new NodeRef(1, node), path.getFirst(), "Should return the start node if it is already in the target graph");
    }
}
