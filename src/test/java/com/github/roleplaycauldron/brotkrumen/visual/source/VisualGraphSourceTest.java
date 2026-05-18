package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
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
    void singleGraphSourceDerivesTeleportRoles() {
        final Graph graph = new Graph(1, "Teleport Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID third = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addNode(new Node(third, 2, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.TELEPORT));
        graph.addDirectedEdge(second, third, 1.0D, Set.of(EdgeFlag.TELEPORT_GLOBAL));

        final VisualGraphSnapshot snapshot = new SingleGraphVisualSource(graph).snapshot();

        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.TELEPORT),
                "Local teleport edge should get teleport role");
        assertTrue(snapshot.edges().stream().anyMatch(edge -> edge.role() == VisualEdgeRole.GLOBAL_TELEPORT),
                "Global teleport edge should get global teleport role");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.TELEPORT_ENDPOINT)
                .anyMatch(node -> node.ref().nodeId().equals(first)), "Local teleport source should be an endpoint");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.TELEPORT_ENDPOINT)
                .anyMatch(node -> node.ref().nodeId().equals(second)), "Local teleport target should be an endpoint");
        assertTrue(snapshot.nodes().stream().filter(node -> node.role() == VisualNodeRole.TELEPORT_ENDPOINT)
                .anyMatch(node -> node.ref().nodeId().equals(third)), "Global teleport target should be an endpoint");
    }

    @Test
    void pathSourcesPreserveVisualRoles() {
        final Graph graph = new Graph(1, "Path Teleport Roles");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.TELEPORT));

        final PathVisualGraphSource source = new PathVisualGraphSource(new SingleGraphVisualSource(graph),
                List.of(new NodeRef(1, first), new NodeRef(1, second)));

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertTrue(snapshot.nodes().stream().allMatch(node -> node.role() == VisualNodeRole.TELEPORT_ENDPOINT),
                "Path source should keep node roles from delegate");
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
                List.of(new NodeRef(1, first), new NodeRef(1, second)));

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(2, snapshot.nodes().size(), "Only path nodes should be exposed");
        assertEquals(1, snapshot.edges().size(), "Only path edges should be exposed");
    }

    @Test
    void guidedPathSourceExposesInitialWindow() {
        final PathFixture fixture = pathFixture();
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                fixture.refs(),
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
                fixture.refs().subList(0, 2),
                () -> null,
                new GuidedPathOptions(4, 4.0D, 1)
        );

        final VisualGraphSnapshot snapshot = source.snapshot();

        assertEquals(2, snapshot.nodes().size(), "Short path should expose every path node");
        assertEquals(1, snapshot.edges().size(), "Short path should expose its edge");
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
        assertEquals(GuidedPathOptions.defaults(), missing, "Missing section should use built-in defaults");
    }

    @Test
    void guidedPathSourceAdvancesProgressMonotonically() {
        final PathFixture fixture = pathFixture();
        final MutableLocationSource location = new MutableLocationSource(new Location(null, 2.5D, 0.5D, 0.5D));
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                fixture.refs(),
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
                List.of(),
                () -> null,
                GuidedPathOptions.defaults()
        );
        final GuidedPathVisualGraphSource missing = new GuidedPathVisualGraphSource(
                new SingleGraphVisualSource(fixture.graph()),
                List.of(fixture.refs().getFirst(), new NodeRef(1, UUID.randomUUID()), fixture.refs().get(1)),
                () -> null,
                new GuidedPathOptions(3, 4.0D, 0)
        );

        assertTrue(empty.snapshot().nodes().isEmpty(), "Empty path should expose no nodes");
        assertTrue(empty.snapshot().edges().isEmpty(), "Empty path should expose no edges");

        final VisualGraphSnapshot missingSnapshot = missing.snapshot();
        assertEquals(2, missingSnapshot.nodes().size(), "Resolvable path nodes should still be exposed");
        assertTrue(missingSnapshot.edges().isEmpty(), "Segments through missing nodes should not be exposed");
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
