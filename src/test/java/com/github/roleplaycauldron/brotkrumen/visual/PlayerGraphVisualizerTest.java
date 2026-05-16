package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerGraphVisualizerTest {

    @Test
    void skipsSnapshotApplyWhenVersionIsUnchanged() {
        final StubSource source = new StubSource();
        final StubRenderer renderer = new StubRenderer();
        final PlayerGraphVisualizer visualizer = new PlayerGraphVisualizer(null, source, renderer,
                ProfileGraphDesignResolver.defaults());

        visualizer.visibilityUpdate();
        visualizer.visibilityUpdate();

        assertEquals(1, renderer.applyCount, "Graph reconciliation should run once for unchanged version");
        assertEquals(1, renderer.visibilityOnlyCount, "Visibility-only update should run for unchanged version");
    }

    @Test
    void refreshForcesSnapshotApply() {
        final StubSource source = new StubSource();
        final StubRenderer renderer = new StubRenderer();
        final PlayerGraphVisualizer visualizer = new PlayerGraphVisualizer(null, source, renderer,
                ProfileGraphDesignResolver.defaults());

        visualizer.visibilityUpdate();
        visualizer.refresh();

        assertEquals(2, renderer.applyCount, "Refresh should force graph reconciliation");
    }

    @Test
    void factoriesCreateSingleGraphAndNetworkVisualizers() {
        final Graph graph = new Graph(1, "Factory");
        graph.addNode(new Node(UUID.randomUUID(), 0, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);

        final GraphVisualizer graphVisualizer = GraphVisualizerFactory.blockDisplayGraph(null, null, graph, UUID.randomUUID());
        final GraphVisualizer networkVisualizer = GraphVisualizerFactory.blockDisplayNetwork(null, null, network,
                UUID.randomUUID(), GraphNetworkDesignProfile.defaults());

        assertNotNull(graphVisualizer, "Factory should create a single-graph visualizer");
        assertNotNull(networkVisualizer, "Factory should create a network visualizer");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void factoriesCreateGuidedNetworkPathVisualizers() {
        final Graph graph = new Graph(1, "Guided Factory");
        final UUID nodeId = UUID.randomUUID();
        graph.addNode(new Node(nodeId, 0, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final List<NodeRef> path = List.of(new NodeRef(1, nodeId));
        final GuidedPathOptions options = new GuidedPathOptions(2, 3.0D, 1);

        final GraphVisualizer particleVisualizer = GraphVisualizerFactory.particleGuidedNetworkPath(null, null, network,
                path, UUID.randomUUID(), null, GraphNetworkDesignProfile.defaults(), options);
        final GraphVisualizer blockDisplayVisualizer = GraphVisualizerFactory.blockDisplayGuidedNetworkPath(null, null,
                network, path, UUID.randomUUID(), GraphNetworkDesignProfile.defaults(), options);
        final GraphVisualizer defaultOptionsVisualizer = GraphVisualizerFactory.blockDisplayGuidedNetworkPath(null, null,
                network, path, UUID.randomUUID(), GraphNetworkDesignProfile.defaults());

        assertNotNull(particleVisualizer, "Factory should create a guided particle visualizer");
        assertNotNull(blockDisplayVisualizer, "Factory should create a guided block-display visualizer");
        assertNotNull(defaultOptionsVisualizer, "Factory should create a guided visualizer with default options");
    }

    private static final class StubSource implements VisualGraphSource {

        @Override
        public VisualGraphSnapshot snapshot() {
            return new VisualGraphSnapshot(List.of(), List.of(), version());
        }

        @Override
        public long version() {
            return 1L;
        }
    }

    private static final class StubRenderer implements GraphRenderer {

        private int applyCount;

        private int visibilityOnlyCount;

        @Override
        public void apply(final VisualGraphSnapshot snapshot, final GraphDesignResolver designs) {
            applyCount++;
        }

        @Override
        public void applyVisibilityOnly() {
            visibilityOnlyCount++;
        }

        @Override
        public void shutdown() {
            // No resources in the stub.
        }
    }
}
