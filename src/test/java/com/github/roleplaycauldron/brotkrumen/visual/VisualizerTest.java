package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockDisplayDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.shape.LineShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.AvoidAccessibilityAlteration")
class VisualizerTest {

    @Test
    void skipsSnapshotApplyWhenVersionIsUnchanged() {
        final StubSource source = new StubSource();
        final StubRenderer renderer = new StubRenderer();
        final Visualizer visualizer = new Visualizer(null, source, renderer,
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
        final Visualizer visualizer = new Visualizer(null, source, renderer,
                ProfileGraphDesignResolver.defaults());

        visualizer.visibilityUpdate();
        visualizer.refresh();

        assertEquals(2, renderer.applyCount, "Refresh should force graph reconciliation");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void factoriesCreateSingleGraphAndNetworkVisualizers() {
        final Graph graph = new Graph(1, "Factory");
        graph.addNode(new Node(UUID.randomUUID(), 0, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final GraphNetworkDesignProfile profile = GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, ParticleDesignSet.emberPreset())
                .blockDisplayGraphDesign(1, BlockDisplayDesignSet.emberPreset())
                .build();

        final Visualizer graphVisualizer = GraphVisualizerFactory.blockDisplayGraph(null, null, graph, UUID.randomUUID());
        final Visualizer networkVisualizer = GraphVisualizerFactory.blockDisplayNetwork(null, null, network, UUID.randomUUID(), profile);
        final Visualizer particleNetworkVisualizer = GraphVisualizerFactory.particleNetwork(null, null, network, UUID.randomUUID(), null, profile);

        assertNotNull(graphVisualizer, "Factory should create a single-graph visualizer");
        assertNotNull(networkVisualizer, "Factory should create a network visualizer");
        assertNotNull(particleNetworkVisualizer, "Factory should create a preset particle network visualizer");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void factoriesCreateGuidedNetworkPathVisualizers() {
        final Graph graph = new Graph(1, "Guided Factory");
        final UUID nodeId = UUID.randomUUID();
        graph.addNode(new Node(nodeId, 0, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final PathResult path = new PathResult(List.of(new NodeRef(1, nodeId)), List.of());
        final GuidedPathOptions options = new GuidedPathOptions(2, 3.0D, 1);

        final Visualizer particleVisualizer = GraphVisualizerFactory.particleGuidedNetworkPath(null, null, network,
                path, UUID.randomUUID(), null, GraphNetworkDesignProfile.defaults(), options);
        final Visualizer blockDisplayVisualizer = GraphVisualizerFactory.blockDisplayGuidedNetworkPath(null, null,
                network, path, UUID.randomUUID(), GraphNetworkDesignProfile.defaults(), options);
        final Visualizer defaultOptionsVisualizer = GraphVisualizerFactory.blockDisplayGuidedNetworkPath(null, null,
                network, path, UUID.randomUUID(), GraphNetworkDesignProfile.defaults());

        assertNotNull(particleVisualizer, "Factory should create a guided particle visualizer");
        assertNotNull(blockDisplayVisualizer, "Factory should create a guided block-display visualizer");
        assertNotNull(defaultOptionsVisualizer, "Factory should create a guided visualizer with default options");
    }

    @Test
    void guidedParticleFactoryUsesProfileDesignsAndPreservesExplicitOverrides() throws ReflectiveOperationException {
        final Graph graph = new Graph(1, "Guided Profile");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 1, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final LocalVisualEdgeId edgeId = new LocalVisualEdgeId(1, UUID.randomUUID());
        final ParticleEdgeDesign explicit = ParticleEdgeDesign.line(Particle.SMOKE, 6);
        final ParticleEdgeDesign roleDesign = ParticleEdgeDesign.line(Particle.CLOUD, 4);
        final GraphNetworkDesignProfile profile = GraphNetworkDesignProfile.builder()
                .particleDefaultDesign(new ParticleDesignSet(
                        ParticleDesignSet.defaults().nodeDesigns(),
                        java.util.Map.of(
                                VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.line(Particle.FLAME, 20),
                                VisualEdgeRole.UNDIRECTED_LOCAL, roleDesign
                        )
                ))
                .particleEdgeOverride(edgeId, explicit)
                .build();
        final Visualizer visualizer = GraphVisualizerFactory.particleGuidedNetworkPath(null, null, network,
                new PathResult(List.of(new NodeRef(1, first), new NodeRef(1, second)), List.of()),
                UUID.randomUUID(), null, profile,
                GuidedPathOptions.defaults());
        final GraphDesignResolver resolver = designs(visualizer);
        final VisualEdge roleEdge = new VisualEdge(new LocalVisualEdgeId(1, UUID.randomUUID()), new NodeRef(1, first),
                new NodeRef(1, second), VisualEdgeKind.LOCAL, 1.0D, Set.of(), VisualEdgeRole.UNDIRECTED_LOCAL);
        final VisualEdge explicitEdge = new VisualEdge(edgeId, new NodeRef(1, first), new NodeRef(1, second),
                VisualEdgeKind.LOCAL, 1.0D, Set.of(), VisualEdgeRole.UNDIRECTED_LOCAL);

        assertAll(
                () -> assertSame(roleDesign, resolver.resolveParticleEdge(roleEdge),
                        "Guided particle edges should use configured profile role designs"),
                () -> assertInstanceOf(LineShape.class, shape(resolver.resolveParticleEdge(roleEdge).effect()),
                        "Configured line role design should not be forced into moving-point styling"),
                () -> assertSame(explicit, resolver.resolveParticleEdge(explicitEdge),
                        "Explicit guided particle edge override should be preserved"),
                () -> assertInstanceOf(LineShape.class, shape(resolver.resolveParticleEdge(explicitEdge).effect()),
                        "Explicit line override should not be replaced")
        );
    }

    private GraphDesignResolver designs(final Visualizer visualizer) throws ReflectiveOperationException {
        final Field field = Visualizer.class.getDeclaredField("designs");
        field.setAccessible(true);
        return (GraphDesignResolver) field.get(visualizer);
    }

    private Shape shape(final EffectInstance effect) throws ReflectiveOperationException {
        final Field field = EffectInstance.class.getDeclaredField("shape");
        field.setAccessible(true);
        return (Shape) field.get(effect);
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
