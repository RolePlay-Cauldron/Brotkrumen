package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRoles;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.CouplingBetweenObjects")
class VisualGraphSourceTest {

    private static NodeRef nodeRef(final UUID nodeId) {
        return new NodeRef(1, nodeId);
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void singleGraphSourceCreatesStableSnapshot() {
        final Graph graph = new Graph(7, "Single");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(second, 1, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));

        final VisualGraphSnapshot snapshot = new SingleGraphVisualSource(graph).snapshot();

        assertEquals(2, snapshot.nodes().size(), "All nodes should be exposed");
        assertEquals(1, snapshot.edges().size(), "All edges should be exposed");
        assertTrue(snapshot.nodes().stream().anyMatch(node -> node.ref().equals(new NodeRef(7, first))),
                "Node identity should preserve graph membership");
        assertTrue(snapshot.edges().stream().allMatch(edge -> edge.id() instanceof LocalVisualEdgeId),
                "Local edge identity should preserve graph membership");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void networkSourceIncludesLocalAndEnabledInterGraphEdges() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID oneA = UUID.randomUUID();
        final UUID oneB = UUID.randomUUID();
        final UUID twoA = UUID.randomUUID();
        graphOne.addNode(new Node(oneA, 0, 0, 0, null));
        graphOne.addNode(new Node(oneB, 1, 0, 0, null));
        graphTwo.addNode(new Node(twoA, 2, 0, 0, null));
        graphOne.addDirectedEdge(oneA, oneB, 1.0D, Set.of(EdgeFlag.DIRECTED));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        final InterGraphEdge enabled = new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, oneB),
                new NodeRef(2, twoA), 2.0D, Set.of(EdgeFlag.INTER_GRAPH), true);
        final InterGraphEdge disabled = new InterGraphEdge(UUID.randomUUID(), new NodeRef(2, twoA),
                new NodeRef(1, oneA), 3.0D, Set.of(EdgeFlag.INTER_GRAPH), false);
        network.addInterGraphEdge(enabled);
        network.addInterGraphEdge(disabled);

        final VisualGraphSnapshot snapshot = new GraphNetworkVisualSource(network).snapshot();

        assertEquals(3, snapshot.nodes().size(), "All network nodes should be exposed");
        assertEquals(2, snapshot.edges().size(), "Local and enabled inter-graph edge should be exposed");
        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.kind() == VisualEdgeKind.INTER_GRAPH
                && edge.id().equals(new InterGraphVisualEdgeId(enabled.edgeId()))), "Enabled inter-graph edge should exist");
        assertFalse(snapshot.edges().stream().anyMatch(edge -> edge.id().equals(new InterGraphVisualEdgeId(disabled.edgeId()))),
                "Disabled inter-graph edge should be excluded");
    }

    @Test
    void sourceVersionChangesAfterGraphMutation() {
        final Graph graph = new Graph(4, "Mutable");
        final SingleGraphVisualSource source = new SingleGraphVisualSource(graph);
        final long before = source.version();

        graph.addNode(new Node(UUID.randomUUID(), 0, 0, 0, null));

        assertNotEquals(before, source.version(), "Graph source version should change after mutation");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void snapshotReusesImmutableNodeReferenceLookup() {
        final UUID first = UUID.randomUUID();
        final NodeRef firstRef = new NodeRef(1, first);
        final VisualNode firstNode = new VisualNode(null, firstRef, new Node(first, 0, 0, 0, null));
        final VisualGraphSnapshot snapshot = new VisualGraphSnapshot(new java.util.ArrayList<>(List.of(firstNode)),
                new java.util.ArrayList<>(), 1L);

        final Map<NodeRef, VisualNode> firstLookup = snapshot.nodesByRef();
        final Map<NodeRef, VisualNode> secondLookup = snapshot.nodesByRef();

        assertSame(firstLookup, secondLookup, "Snapshot should return the same precomputed node lookup");
        assertSame(firstNode, firstLookup.get(firstRef), "Lookup should contain nodes from the snapshot");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.nodes().clear(),
                "Snapshot node collection should be immutable");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.edges().clear(),
                "Snapshot edge collection should be immutable");
        assertThrows(UnsupportedOperationException.class, firstLookup::clear,
                "Snapshot node lookup should be immutable");
    }

    @Test
    void newSnapshotExposesUpdatedNodeReferenceLookup() {
        final Graph graph = new Graph(4, "Lookup Mutable");
        final UUID first = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        final SingleGraphVisualSource source = new SingleGraphVisualSource(graph);
        final VisualGraphSnapshot before = source.snapshot();
        final UUID second = UUID.randomUUID();

        graph.addNode(new Node(second, 1, 0, 0, null));
        final VisualGraphSnapshot after = source.snapshot();

        assertFalse(before.nodesByRef().containsKey(new NodeRef(4, second)),
                "Old snapshot lookup should remain tied to old snapshot contents");
        assertTrue(after.nodesByRef().containsKey(new NodeRef(4, second)),
                "New snapshot lookup should include newly emitted nodes");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void singleGraphSourceDerivesTeleportRoles() {
        final Graph graph = new Graph(1, "Teleport Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(second, 1, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(third, 2, 0, 0, null, Set.of(NodeFlag.WARP)));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.TELEPORT));

        final VisualGraphSnapshot snapshot = new SingleGraphVisualSource(graph).snapshot();

        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.TELEPORT),
                "Local teleport edge should get teleport role");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.LOCAL_TELEPORT)
                .anyMatch(node -> node.ref().nodeId().equals(first)), "Local teleport source should be an endpoint");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.LOCAL_TELEPORT)
                .anyMatch(node -> node.ref().nodeId().equals(second)), "Local teleport target should be an endpoint");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.WARP)
                .anyMatch(node -> node.ref().nodeId().equals(third)), "Warp target should be a warp node");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void sourcesDeriveLocalEdgeRolesWithPrecedence() {
        final Graph graph = new Graph(1, "Local Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        final UUID fourth = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(second, 1, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addNode(new Node(fourth, 3, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addUndirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.UNDIRECTED));
        graph.addDirectedEdge(third, fourth, 1.0D, Set.of(EdgeFlag.BLOCKED, EdgeFlag.TELEPORT));

        final VisualGraphSnapshot singleSnapshot = new SingleGraphVisualSource(graph).snapshot();
        final Graph targetGraph = new Graph(2, "Target");
        final UUID targetNode = UUID.randomUUID();
        targetGraph.addNode(new Node(targetNode, 4, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        network.addGraph(targetGraph);
        final InterGraphEdge interGraphEdge = new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, fourth),
                new NodeRef(2, targetNode), 1.0D, Set.of(EdgeFlag.INTER_GRAPH, EdgeFlag.DIRECTED), true);
        network.addInterGraphEdge(interGraphEdge);
        network.addUndirectedInterGraphEdge(new NodeRef(1, first), new NodeRef(2, targetNode), 1.0D);
        final VisualGraphSnapshot networkSnapshot = new GraphNetworkVisualSource(network).snapshot();

        assertTrue(singleSnapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.DIRECTED_LOCAL),
                "Directed local flag should derive directed visual role");
        assertTrue(singleSnapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.UNDIRECTED_LOCAL),
                "Undirected graph edges should derive undirected visual role");
        assertEquals(VisualEdgeRole.UNDIRECTED_LOCAL,
                VisualEdgeRoles.derive(VisualEdgeKind.LOCAL, Set.of(EdgeFlag.DIRECTED, EdgeFlag.UNDIRECTED)),
                "Undirected should win when mixed edge flags contain both direction flags");
        assertTrue(singleSnapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.BLOCKED),
                "Blocked flag should take precedence over teleport flags");
        assertTrue(networkSnapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.DIRECTED_INTER_GRAPH),
                "Directed inter-graph edges should derive directed inter-graph visual role");
        assertTrue(networkSnapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.UNDIRECTED_INTER_GRAPH),
                "Undirected inter-graph edges should derive undirected inter-graph visual role");
        assertEquals(VisualEdgeRole.TELEPORT,
                VisualEdgeRoles.derive(VisualEdgeKind.INTER_GRAPH, Set.of(EdgeFlag.TELEPORT, EdgeFlag.INTER_GRAPH,
                        EdgeFlag.DIRECTED)),
                "Teleport should win for intergraph teleport role derivation");
    }

    @Test
    void pathSourcesUseTraversalRoles() {
        final Graph graph = new Graph(1, "Path Teleport Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(second, 1, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.TELEPORT));

        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final PathVisualGraphSource source = new PathVisualGraphSource(new SingleGraphVisualSource(graph),
                new PathResult(List.of(firstRef, secondRef),
                        List.of(new PathSegment(firstRef, secondRef, TraversalKind.LOCAL_TELEPORT, null, null))));

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertTrue(snapshot.nodes().stream().allMatch(node -> node.role() == VisualNodeRole.LOCAL_TELEPORT),
                "Path source should derive node roles from traversal metadata");
        assertTrue(snapshot.edges().stream().allMatch(edge -> edge.role() == VisualEdgeRole.TELEPORT),
                "Path source should keep edge roles from delegate");
    }

    @Test
    void networkSourceVersionChangesAfterContainedGraphMutation() {
        final Graph graph = new Graph(1, "Mutable");
        graph.addNode(new Node(UUID.randomUUID(), 0, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final GraphNetworkVisualSource source = new GraphNetworkVisualSource(network);
        final long before = source.version();

        graph.addNode(new Node(UUID.randomUUID(), 1, 0, 0, null));

        assertNotEquals(before, source.version(), "Network source version should include contained graph mutations");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void editorWorkspaceSourceIncludesVisibleGraphsAndEdgesOnly() {
        final Graph active = new Graph(1, "Active");
        final Graph reference = new Graph(2, "Reference");
        final Graph hidden = new Graph(3, "Hidden");
        final UUID activeNode = UUID.randomUUID();
        final UUID referenceNode = UUID.randomUUID();
        final UUID hiddenNode = UUID.randomUUID();
        active.addNode(new Node(activeNode, 0, 0, 0, null));
        reference.addNode(new Node(referenceNode, 1, 0, 0, null));
        hidden.addNode(new Node(hiddenNode, 2, 0, 0, null));
        active.addDirectedEdge(activeNode, activeNode, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final InterGraphEdge visible = new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, activeNode),
                new NodeRef(2, referenceNode), 1.0D, Set.of(EdgeFlag.INTER_GRAPH), true);
        final InterGraphEdge hiddenEdge = new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, activeNode),
                new NodeRef(3, hiddenNode), 1.0D, Set.of(EdgeFlag.INTER_GRAPH), true);
        final long[] version = {1L};
        final EditorWorkspaceVisualSource source = new EditorWorkspaceVisualSource(() -> active, () -> List.of(reference),
                () -> List.of(visible, hiddenEdge), () -> version[0]);

        final VisualGraphSnapshot snapshot = source.snapshot();
        final long before = source.version();
        version[0]++;

        assertEquals(2, snapshot.nodes().size(), "Active and reference nodes should be visible");
        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.id().equals(new InterGraphVisualEdgeId(visible.edgeId()))),
                "Inter-graph edge between visible graphs should be exposed");
        assertFalse(snapshot.edges().stream().anyMatch(edge -> edge.id().equals(new InterGraphVisualEdgeId(hiddenEdge.edgeId()))),
                "Inter-graph edge to hidden graph should be excluded");
        assertNotEquals(before, source.version(), "Workspace source version should include workspace state");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void networkSourceDerivesTeleportNodeRolesWithPrecedence() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID local = UUID.randomUUID();
        final UUID shared = UUID.randomUUID();
        final UUID global = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        graphOne.addNode(new Node(local, 0, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graphOne.addNode(new Node(shared, 1, 0, 0, null,
                Set.of(NodeFlag.LOCAL_TELEPORT, NodeFlag.INTERGRAPH_TELEPORT)));
        graphOne.addNode(new Node(global, 2, 0, 0, null, Set.of(NodeFlag.WARP)));
        graphTwo.addNode(new Node(target, 3, 0, 0, null, Set.of(NodeFlag.INTERGRAPH_TELEPORT)));
        graphOne.addDirectedEdge(local, shared, 1.0D, Set.of(EdgeFlag.TELEPORT));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addDirectedInterGraphEdge(new NodeRef(1, shared), new NodeRef(2, target), 1.0D,
                Set.of(EdgeFlag.TELEPORT, EdgeFlag.INTER_GRAPH));
        final VisualGraphSnapshot snapshot = new GraphNetworkVisualSource(network).snapshot();

        assertEquals(VisualNodeRole.LOCAL_TELEPORT, nodeRole(snapshot, new NodeRef(1, local)),
                "Local-only teleport node should keep local role");
        assertEquals(VisualNodeRole.INTERGRAPH_TELEPORT, nodeRole(snapshot, new NodeRef(1, shared)),
                "Intergraph teleport role should win over local teleport");
        assertEquals(VisualNodeRole.WARP, nodeRole(snapshot, new NodeRef(1, global)),
                "Warp role should win when present");
        assertEquals(VisualNodeRole.INTERGRAPH_TELEPORT, nodeRole(snapshot, new NodeRef(2, target)),
                "Intergraph target should be classified");
        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.kind() == VisualEdgeKind.INTER_GRAPH
                && edge.role() == VisualEdgeRole.TELEPORT), "Intergraph teleport edge should use teleport role");
    }

    @Test
    void visualSourcesCanonicalizeUndirectedRelationships() {
        final Graph graph = new Graph(1, "Canonical Local");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addUndirectedEdge(first, second, 1.0D);

        final VisualGraphSnapshot snapshot = new SingleGraphVisualSource(graph).snapshot();

        assertEquals(1, snapshot.edges().size(), "Undirected local relationship should appear once");
        assertEquals(VisualEdgeRole.UNDIRECTED_LOCAL, snapshot.edges().iterator().next().role(),
                "Canonical edge should keep undirected role");
    }

    @Test
    void networkSourceCanonicalizesUndirectedIntergraphRelationships() {
        final Graph graphOne = new Graph(1, "One");
        final Graph graphTwo = new Graph(2, "Two");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graphOne.addNode(new Node(first, 0, 0, 0, null));
        graphTwo.addNode(new Node(second, 1, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addUndirectedInterGraphEdge(new NodeRef(1, first), new NodeRef(2, second), 1.0D);

        final VisualGraphSnapshot snapshot = new GraphNetworkVisualSource(network).snapshot();

        assertEquals(1, snapshot.edges().size(), "Undirected intergraph relationship should appear once");
        assertEquals(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, snapshot.edges().iterator().next().role(),
                "Canonical intergraph edge should keep undirected role");
    }

    @Test
    void pathSourceFiltersByNodeReferences() {
        final Graph graph = new Graph(1, "Path");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.DIRECTED));

        final PathVisualGraphSource source = new PathVisualGraphSource(new SingleGraphVisualSource(graph),
                new PathResult(List.of(new NodeRef(1, first), new NodeRef(1, second)), List.of()));

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(2, snapshot.nodes().size(), "Only path nodes should be exposed");
        assertEquals(1, snapshot.edges().size(), "Only path edges should be exposed");
    }

    @Test
    void pathSourceIncludesReverseUndirectedEdge() {
        final NodeRef first = new NodeRef(1, UUID.randomUUID());
        final NodeRef second = new NodeRef(1, UUID.randomUUID());
        final VisualNode firstNode = new VisualNode(new VisualNodeId(first), first, new Node(first.nodeId(), 0, 0, 0, null));
        final VisualNode secondNode = new VisualNode(new VisualNodeId(second), second, new Node(second.nodeId(), 1, 0, 0, null));
        final VisualEdge reverseUndirected = new VisualEdge(new LocalVisualEdgeId(1, UUID.randomUUID()), second, first,
                VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.UNDIRECTED), VisualEdgeRole.UNDIRECTED_LOCAL);
        final PathVisualGraphSource source = new PathVisualGraphSource(
                fixedSnapshotSource(List.of(firstNode, secondNode), List.of(reverseUndirected)),
                new PathResult(List.of(first, second), List.of())
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(1, snapshot.edges().size(), "Undirected reverse-order edge should match path adjacency");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void structuredPathSourceUsesTraversalRolesInsteadOfStoredTeleportMetadata() {
        final Graph graph = new Graph(1, "Path Metadata");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null, Set.of(NodeFlag.WARP)));
        graph.addNode(new Node(second, 1, 0, 0, null, Set.of(NodeFlag.LOCAL_TELEPORT)));
        graph.addNode(new Node(third, 2, 0, 0, null, Set.of(NodeFlag.WARP)));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final NodeRef thirdRef = new NodeRef(1, third);
        final PathResult result = new PathResult(List.of(firstRef, secondRef, thirdRef),
                List.of(new PathSegment(firstRef, secondRef, TraversalKind.NORMAL, null, null),
                        new PathSegment(secondRef, thirdRef, TraversalKind.WARP, null, "target")));

        final VisualGraphSnapshot snapshot = new PathVisualGraphSource(new SingleGraphVisualSource(graph), result).snapshot();

        assertEquals(VisualNodeRole.DEFAULT, nodeRole(snapshot, firstRef),
                "Stored warp metadata should not show in path mode when the segment did not use it");
        assertEquals(VisualNodeRole.WARP, nodeRole(snapshot, secondRef), "Warp segment source should be marked");
        assertEquals(VisualNodeRole.WARP, nodeRole(snapshot, thirdRef), "Warp segment target should be marked");
    }

    @Test
    void guidedPathSourceExposesInitialWindow() {
        final PathFixture fixture = pathFixture();
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                () -> null,
                new GuidedPathOptions(3, 4.0D, 1)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(fixture.refs().subList(0, 3), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Initial window should expose the first path nodes");
        assertEquals(2, snapshot.edges().size(), "Initial window should expose edges between consecutive path nodes");
    }

    @Test
    void guidedPathSourceExposesWholeShortPath() {
        final PathFixture fixture = pathFixture();
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs().subList(0, 2), List.of()),
                () -> null,
                new GuidedPathOptions(4, 4.0D, 1)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(2, snapshot.nodes().size(), "Short path should expose every path node");
        assertEquals(1, snapshot.edges().size(), "Short path should expose its edge");
    }

    @Test
    void guidedPathSourceIncludesReverseUndirectedPathEdge() {
        final NodeRef first = new NodeRef(1, UUID.randomUUID());
        final NodeRef second = new NodeRef(1, UUID.randomUUID());
        final VisualNode firstNode = new VisualNode(new VisualNodeId(first), first, new Node(first.nodeId(), 0, 0, 0, null));
        final VisualNode secondNode = new VisualNode(new VisualNodeId(second), second, new Node(second.nodeId(), 1, 0, 0, null));
        final VisualEdge reverseUndirected = new VisualEdge(new LocalVisualEdgeId(1, UUID.randomUUID()), second, first,
                VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.UNDIRECTED), VisualEdgeRole.UNDIRECTED_LOCAL);
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                fixedSnapshotSource(List.of(firstNode, secondNode), List.of(reverseUndirected)),
                new PathResult(List.of(first, second), List.of()),
                () -> null,
                new GuidedPathOptions(2, 1.0D, 0)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(List.of(first, second), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Guided window should still follow path traversal order");
        assertEquals(1, snapshot.edges().size(),
                "Undirected path-adjacent edge should match even when canonicalized in reverse");
    }

    @Test
    void guidedPathSourceRejectsReverseDirectedPathEdge() {
        final NodeRef first = new NodeRef(1, UUID.randomUUID());
        final NodeRef second = new NodeRef(1, UUID.randomUUID());
        final VisualNode firstNode = new VisualNode(new VisualNodeId(first), first, new Node(first.nodeId(), 0, 0, 0, null));
        final VisualNode secondNode = new VisualNode(new VisualNodeId(second), second, new Node(second.nodeId(), 1, 0, 0, null));
        final VisualEdge reverseDirected = new VisualEdge(new LocalVisualEdgeId(1, UUID.randomUUID()), second, first,
                VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.DIRECTED), VisualEdgeRole.DIRECTED_LOCAL);
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                fixedSnapshotSource(List.of(firstNode, secondNode), List.of(reverseDirected)),
                new PathResult(List.of(first, second), List.of()),
                () -> null,
                new GuidedPathOptions(2, 1.0D, 0)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertTrue(snapshot.edges().isEmpty(),
                "Directed reverse-order edge should not match a forward path segment");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedPathSourceChecksViewerDistanceToCurrentEdges() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final Graph graph = new Graph(1, "Long Edge");
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 10, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 5.5D, 1.2D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(new SingleGraphVisualSource(graph),
                new PathResult(List.of(firstRef, secondRef), List.of()), location, new GuidedPathOptions(2, 1.0D, 0));

        assertTrue(source.viewerWithinCurrentPath(1.0D),
                "Viewer near the edge segment should remain in range even away from nodes");
        assertFalse(source.viewerWithinCurrentPath(0.5D), "Viewer outside edge range should be out of range");

        location.currentLocation = new Location(null, 5.5D, 4.0D, 0.5D);

        assertFalse(source.viewerWithinCurrentPath(1.0D), "Viewer far from nodes and edge should be out of range");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedPathOptionsLoadConfigDefaultsAndFallbacks() {
        final MemoryConfiguration config = new MemoryConfiguration();
        config.set("windowSize", 6);
        config.set("activationRadius", 2.5D);

        final GuidedPathOptions configured = GuidedPathOptions.fromConfig(config);
        final GuidedPathOptions missing = GuidedPathOptions.fromConfig(null);

        assertEquals(6, configured.windowSize(), "Configured window size should be loaded");
        assertEquals(2.5D, configured.activationRadius(), "Configured activation radius should be loaded");
        assertEquals(GuidedPathOptions.defaults().lookBehind(), configured.lookBehind(),
                "Missing look-behind should use fallback default");
        assertFalse(configured.keepLookBehindOnCompletion(),
                "Missing completion look-behind flag should use fallback default");
        assertEquals(GuidedPathOptions.defaults(), missing, "Missing section should use built-in defaults");
    }

    @Test
    void guidedPathSourceAdvancesProgressMonotonically() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 2.0D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 1)
        );

        VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(fixture.refs().subList(1, 4), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Reaching the third path node should advance the visible window with look-behind");

        location.currentLocation = new Location(null, 0.5D, 0.5D, 0.5D);
        snapshot = source.snapshot();

        assertEquals(fixture.refs().subList(1, 4), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Moving behind progress should not move the window backwards");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedPathSourceHandlesEmptyAndMissingPathNodes() {
        final PathFixture fixture = pathFixture();
        final GuidedPathVisualGraphSource empty = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                PathResult.empty(),
                () -> null,
                GuidedPathOptions.defaults()
        );
        final GuidedPathVisualGraphSource missing = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(List.of(fixture.refs().getFirst(), new NodeRef(1, UUID.randomUUID()), fixture.refs().get(1)),
                        List.of()),
                () -> null,
                new GuidedPathOptions(3, 4.0D, 0)
        );

        assertTrue(empty.snapshot().nodes().isEmpty(), "Empty path should expose no nodes");
        assertTrue(empty.snapshot().edges().isEmpty(), "Empty path should expose no edges");

        final VisualGraphSnapshot missingSnapshot = missing.snapshot();
        assertEquals(2, missingSnapshot.nodes().size(), "Resolvable path nodes should still be exposed");
        assertTrue(missingSnapshot.edges().isEmpty(), "Segments through missing nodes should not be exposed");
    }

    @Test
    void guidedPathSourceReportsCompletionAtFinalNode() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 3.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 1)
        );

        source.snapshot();

        assertTrue(source.complete(), "Reaching the final path node should report completion");
        assertEquals(fixture.refs().size() - 1, source.currentProgressIndex(),
                "Final node should become the current progress index");
    }

    @Test
    void guidedPathSourceShowsOnlyGoalWhenFinalReachedWithoutLookBehind() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 3.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 0)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(List.of(fixture.refs().getLast()), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Final window with no look-behind should expose only the goal node");
        assertTrue(snapshot.edges().isEmpty(), "Final window with only one node should expose no path edges");
    }

    @Test
    void guidedPathSourceHidesLookBehindWhenFinalReachedByDefault() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 3.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 2)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(List.of(fixture.refs().getLast()), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Final window should hide previous nodes by default");
        assertTrue(snapshot.edges().isEmpty(), "Final window with only the goal node should expose no path edges");
    }

    @Test
    void guidedPathSourceShowsLookBehindWhenFinalReachedAndConfigured() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 3.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 2, true)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(fixture.refs().subList(1, 4), snapshot.nodes().stream().map(VisualNode::ref).toList(),
                "Final window should expose previous nodes when completion look-behind is enabled");
        assertEquals(2, snapshot.edges().size(), "Final window should expose edges between final visible nodes");
    }

    @Test
    void guidedPathSourceDoesNotReportCompletionAtIntermediateNode() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 1.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                new PathResult(fixture.refs(), List.of()),
                location,
                new GuidedPathOptions(3, 1.0D, 1)
        );

        source.snapshot();

        assertFalse(source.complete(), "Reaching intermediate path nodes should not report completion");
    }

    @Test
    void guidedPathSourceDoesNotReportCompletionForEmptyPath() {
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                fixedSnapshotSource(List.of(), List.of()),
                PathResult.empty(),
                () -> new Location(null, 0.0D, 0.0D, 0.0D),
                GuidedPathOptions.defaults()
        );

        source.snapshot();

        assertFalse(source.complete(), "Empty guided paths should never report completion");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedPathSourceMarksIntergraphAndWarpSegmentsFromPathResult() {
        final Graph graph = new Graph(1, "Guided Metadata");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final NodeRef thirdRef = new NodeRef(1, third);
        final PathResult result = new PathResult(List.of(firstRef, secondRef, thirdRef),
                List.of(new PathSegment(firstRef, secondRef, TraversalKind.INTERGRAPH_TELEPORT, null, null),
                        new PathSegment(secondRef, thirdRef, TraversalKind.WARP, null, "target")));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(new SingleGraphVisualSource(graph),
                result, () -> null, new GuidedPathOptions(4, 4.0D, 0));

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(VisualNodeRole.INTERGRAPH_TELEPORT, nodeRole(snapshot, firstRef),
                "Intergraph teleport source should be marked");
        assertEquals(VisualNodeRole.WARP, nodeRole(snapshot, secondRef),
                "Warp role should win for a node that is also an intergraph teleport endpoint");
        assertEquals(VisualNodeRole.WARP, nodeRole(snapshot, thirdRef), "Warp target should be marked");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void pathAndGuidedSourcesShareTraversalRolePrecedence() {
        final Graph graph = new Graph(1, "Shared Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final NodeRef thirdRef = new NodeRef(1, third);
        final PathResult result = new PathResult(List.of(firstRef, secondRef, thirdRef),
                List.of(new PathSegment(firstRef, secondRef, TraversalKind.INTERGRAPH_TELEPORT, null, null),
                        new PathSegment(secondRef, thirdRef, TraversalKind.WARP, null, "target")));

        final VisualGraphSnapshot pathSnapshot = new PathVisualGraphSource(new SingleGraphVisualSource(graph), result)
                .snapshot();
        final VisualGraphSnapshot guidedSnapshot = new GuidedPathVisualGraphSource(new SingleGraphVisualSource(graph),
                result, () -> null, new GuidedPathOptions(4, 4.0D, 0)).snapshot();

        assertEquals(nodeRole(pathSnapshot, firstRef), nodeRole(guidedSnapshot, firstRef),
                "Path and guided path should agree on first segment role");
        assertEquals(nodeRole(pathSnapshot, secondRef), nodeRole(guidedSnapshot, secondRef),
                "Path and guided path should share precedence for overlapping roles");
        assertEquals(nodeRole(pathSnapshot, thirdRef), nodeRole(guidedSnapshot, thirdRef),
                "Path and guided path should agree on warp target role");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedPathGoalMarkerRoleIsOptionalAndOnlyAppliedToFinalNode() {
        final Graph graph = new Graph(1, "Goal Marker");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final NodeRef thirdRef = new NodeRef(1, third);
        final PathResult result = new PathResult(List.of(firstRef, secondRef, thirdRef),
                List.of(new PathSegment(firstRef, secondRef, TraversalKind.INTERGRAPH_TELEPORT, null, null),
                        new PathSegment(secondRef, thirdRef, TraversalKind.WARP, null, "goal")));
        final GuidedPathVisualGraphSource markerEnabled = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(graph), result, () -> null, new GuidedPathOptions(4, 1.0D, 0), true);
        final GuidedPathVisualGraphSource markerDisabled = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(graph), result, () -> null, new GuidedPathOptions(4, 1.0D, 0), false);

        final VisualGraphSnapshot enabledSnapshot = markerEnabled.snapshot();
        final VisualGraphSnapshot disabledSnapshot = markerDisabled.snapshot();

        assertEquals(VisualNodeRole.INTERGRAPH_TELEPORT, nodeRole(enabledSnapshot, firstRef),
                "Only the final node should get goal marker styling");
        assertEquals(VisualNodeRole.WARP, nodeRole(enabledSnapshot, secondRef),
                "Intermediate traversal role should remain unchanged");
        assertEquals(VisualNodeRole.GUIDED_PATH_GOAL, nodeRole(enabledSnapshot, thirdRef),
                "Final node should receive the goal marker role when enabled");
        assertEquals(VisualNodeRole.WARP, nodeRole(disabledSnapshot, thirdRef),
                "Final node should use normal traversal role when marker is disabled");
    }

    private VisualGraphSource fixedSnapshotSource(final List<VisualNode> nodes, final List<VisualEdge> edges) {
        return new VisualGraphSource() {
            @Override
            public VisualGraphSnapshot snapshot() {
                return new VisualGraphSnapshot(nodes, edges, 1L);
            }

            @Override
            public long version() {
                return 1L;
            }
        };
    }

    private PathFixture pathFixture() {
        final Graph graph = new Graph(1, "Guided");
        final List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        for (int i = 0; i < ids.size(); i++) {
            graph.addNode(new Node(ids.get(i), i, 0, 0, null));
        }
        for (int i = 0; i + 1 < ids.size(); i++) {
            graph.addDirectedEdge(ids.get(i), ids.get(i + 1), 1.0D, Set.of(EdgeFlag.DIRECTED));
        }
        return new PathFixture(graph, ids.stream().map(VisualGraphSourceTest::nodeRef).toList());
    }

    private VisualNodeRole nodeRole(final VisualGraphSnapshot snapshot, final NodeRef ref) {
        return snapshot.nodes().stream()
                .filter(node -> node.ref().equals(ref))
                .map(VisualNode::role)
                .findFirst()
                .orElseThrow();
    }

    private record PathFixture(Graph graph, List<NodeRef> refs) {
    }

    private static final class MutableLocationSource implements ViewerLocationSource {

        private Location currentLocation;

        private MutableLocationSource(final Location location) {
            this.currentLocation = location;
        }

        @Override
        public Location location() {
            return currentLocation;
        }
    }
}
