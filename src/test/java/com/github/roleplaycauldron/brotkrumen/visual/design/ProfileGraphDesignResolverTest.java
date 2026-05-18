package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileGraphDesignResolverTest {

    @Test
    void graphSpecificDesignOverridesNetworkDefaultWithoutLeaking() {
        final NodeRef graphARef = new NodeRef(1, UUID.randomUUID());
        final NodeRef graphBRef = new NodeRef(2, UUID.randomUUID());
        final ParticleDesignSet defaultSet = particleSet(Particle.FLAME);
        final ParticleDesignSet graphASet = particleSet(Particle.HEART);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleDefaultDesign(defaultSet)
                .particleGraphDesign(1, graphASet)
                .build());

        assertEquals(Particle.HEART, resolver.resolveParticleNode(visualNode(graphARef)).particle(),
                "Graph-specific design should win");
        assertEquals(Particle.FLAME, resolver.resolveParticleNode(visualNode(graphBRef)).particle(),
                "Graph design should not leak");
    }

    @Test
    void explicitNodeAndEdgeOverridesWin() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final ParticleNodeDesign nodeOverride = ParticleNodeDesign.sphere(Particle.HAPPY_VILLAGER, 0.7f);
        final InterGraphVisualEdgeId edgeId = new InterGraphVisualEdgeId(UUID.randomUUID());
        final ParticleEdgeDesign edgeOverride = ParticleEdgeDesign.movingPoint(Particle.SMOKE, 0.3f);
        final VisualEdge edge = new VisualEdge(edgeId, ref, new NodeRef(2, UUID.randomUUID()),
                VisualEdgeKind.INTER_GRAPH, 1.0D, Set.of(EdgeFlag.INTER_GRAPH));

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleNodeOverride(ref, nodeOverride)
                .particleEdgeOverride(edgeId, edgeOverride)
                .build());

        assertEquals(nodeOverride, resolver.resolveParticleNode(visualNode(ref)), "Explicit node override should win");
        assertEquals(edgeOverride, resolver.resolveParticleEdge(edge), "Explicit edge override should win");
    }

    @Test
    void interGraphStrategyCanUseSourceAndTargetGraph() {
        final NodeRef source = new NodeRef(1, UUID.randomUUID());
        final NodeRef target = new NodeRef(2, UUID.randomUUID());
        final ParticleDesignSet sourceSet = particleSet(Particle.CRIT);
        final ParticleDesignSet targetSet = particleSet(Particle.WITCH);
        final VisualEdge edge = new VisualEdge(new InterGraphVisualEdgeId(UUID.randomUUID()), source, target,
                VisualEdgeKind.INTER_GRAPH, 1.0D, Set.of(EdgeFlag.INTER_GRAPH));

        final ProfileGraphDesignResolver sourceResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, sourceSet)
                .particleGraphDesign(2, targetSet)
                .interGraphStrategy(InterGraphEdgeDesignStrategy.SOURCE_GRAPH)
                .build());
        final ProfileGraphDesignResolver targetResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, sourceSet)
                .particleGraphDesign(2, targetSet)
                .interGraphStrategy(InterGraphEdgeDesignStrategy.TARGET_GRAPH)
                .build());

        assertEquals(Particle.CRIT, sourceResolver.resolveParticleEdge(edge).particle(),
                "Source strategy should use source graph");
        assertEquals(Particle.WITCH, targetResolver.resolveParticleEdge(edge).particle(),
                "Target strategy should use target graph");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void roleSpecificDesignsWinBeforeFallbacks() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final ParticleDesignSet set = ParticleDesignSet.emberPreset();
        final VisualNode teleportNode = visualNode(ref, VisualNodeRole.TELEPORT_ENDPOINT);
        final VisualEdge teleportEdge = new VisualEdge(new InterGraphVisualEdgeId(UUID.randomUUID()), ref,
                new NodeRef(1, UUID.randomUUID()), VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.TELEPORT),
                VisualEdgeRole.TELEPORT);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, set)
                .build());

        assertEquals(set.nodeDesign(VisualNodeRole.TELEPORT_ENDPOINT), resolver.resolveParticleNode(teleportNode),
                "Teleport endpoint node role should use role design");
        assertEquals(set.edgeDesign(VisualEdgeRole.TELEPORT), resolver.resolveParticleEdge(teleportEdge),
                "Teleport edge role should use role design");
        assertEquals(EdgeRenderStrategy.ENDPOINTS_ONLY, resolver.resolveEdgeRenderStrategy(teleportEdge),
                "Teleport edges should default to endpoint-only rendering");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.TELEPORT_ENDPOINT),
                "Preset should include teleport endpoint node design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.TELEPORT),
                "Preset should include teleport edge design");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void explicitOverridesStillWinBeforeRoleDesigns() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final ParticleNodeDesign nodeOverride = ParticleNodeDesign.sphere(Particle.HAPPY_VILLAGER, 0.7f);
        final InterGraphVisualEdgeId edgeId = new InterGraphVisualEdgeId(UUID.randomUUID());
        final ParticleEdgeDesign edgeOverride = ParticleEdgeDesign.movingPoint(Particle.SMOKE, 0.3f);
        final VisualEdge edge = new VisualEdge(edgeId, ref, new NodeRef(1, UUID.randomUUID()),
                VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.TELEPORT), VisualEdgeRole.TELEPORT);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, ParticleDesignSet.emberPreset())
                .particleNodeOverride(ref, nodeOverride)
                .particleEdgeOverride(edgeId, edgeOverride)
                .edgeRenderStrategy(VisualEdgeRole.TELEPORT, EdgeRenderStrategy.FULL_EDGE)
                .build());

        assertEquals(nodeOverride, resolver.resolveParticleNode(visualNode(ref, VisualNodeRole.TELEPORT_ENDPOINT)),
                "Explicit node override should win before role design");
        assertEquals(edgeOverride, resolver.resolveParticleEdge(edge), "Explicit edge override should win before role design");
        assertEquals(EdgeRenderStrategy.FULL_EDGE, resolver.resolveEdgeRenderStrategy(edge),
                "Configured render strategy should override teleport default");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void rendererSpecificGraphDesignsResolveIndependently() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final ParticleDesignSet particleSet = particleSet(Particle.HEART);
        final BlockDisplayDesignSet blockSet = blockSet(Material.DIAMOND_BLOCK, Material.GOLD_BLOCK);
        final VisualEdge edge = new VisualEdge(new InterGraphVisualEdgeId(UUID.randomUUID()), ref,
                new NodeRef(1, UUID.randomUUID()), VisualEdgeKind.LOCAL, 1.0D, Set.of());

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, particleSet)
                .blockDisplayGraphDesign(1, blockSet)
                .build());

        assertEquals(Particle.HEART, resolver.resolveParticleNode(visualNode(ref)).particle(),
                "Particle resolver should use the particle graph set");
        assertEquals(Material.DIAMOND_BLOCK, resolver.resolveBlockNode(visualNode(ref)).blockMaterial(),
                "Block resolver should use the block-display graph set");
        assertEquals(Particle.HEART, resolver.resolveParticleEdge(edge).particle(),
                "Particle edge should not require block-display edge data");
        assertEquals(Material.GOLD_BLOCK, resolver.resolveBlockEdge(edge).blockMaterial(),
                "Block-display edge should not require particle edge data");
    }

    @Test
    void rendererSpecificOverridesWinForTheirRendererOnly() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final ParticleNodeDesign particleOverride = ParticleNodeDesign.sphere(Particle.WITCH, 0.4f);
        final BlockNodeDesign blockOverride = new BlockNodeDesign(Material.AMETHYST_BLOCK, 0.8f);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleNodeOverride(ref, particleOverride)
                .blockDisplayNodeOverride(ref, blockOverride)
                .build());

        assertAll(
                () -> assertEquals(particleOverride, resolver.resolveParticleNode(visualNode(ref)),
                        "Particle override should win for particle resolution"),
                () -> assertEquals(blockOverride, resolver.resolveBlockNode(visualNode(ref)),
                        "Block-display override should win for block-display resolution")
        );
    }

    private VisualNode visualNode(final NodeRef ref) {
        return visualNode(ref, VisualNodeRole.DEFAULT);
    }

    private VisualNode visualNode(final NodeRef ref, final VisualNodeRole role) {
        return new VisualNode(new VisualNodeId(ref), ref, new Node(ref.nodeId(), 0, 0, 0, null), role);
    }

    private ParticleDesignSet particleSet(final Particle particle) {
        final Map<VisualNodeRole, ParticleNodeDesign> nodes = new EnumMap<>(VisualNodeRole.class);
        nodes.put(VisualNodeRole.DEFAULT, ParticleNodeDesign.cube(particle, 0.4f));
        nodes.put(VisualNodeRole.TELEPORT_ENDPOINT, ParticleNodeDesign.sphere(particle, 0.5f));
        final Map<VisualEdgeRole, ParticleEdgeDesign> edges = new EnumMap<>(VisualEdgeRole.class);
        edges.put(VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.movingPoint(particle, 0.2f));
        edges.put(VisualEdgeRole.INTER_GRAPH, ParticleEdgeDesign.movingPoint(particle, 0.25f));
        edges.put(VisualEdgeRole.TELEPORT, ParticleEdgeDesign.movingPoint(particle, 0.3f));
        edges.put(VisualEdgeRole.GLOBAL_TELEPORT, ParticleEdgeDesign.movingPoint(particle, 0.35f));
        return new ParticleDesignSet(nodes, edges);
    }

    private BlockDisplayDesignSet blockSet(final Material nodeMaterial, final Material edgeMaterial) {
        final Map<VisualNodeRole, BlockNodeDesign> nodes = new EnumMap<>(VisualNodeRole.class);
        nodes.put(VisualNodeRole.DEFAULT, new BlockNodeDesign(nodeMaterial, 0.4f));
        nodes.put(VisualNodeRole.TELEPORT_ENDPOINT, new BlockNodeDesign(nodeMaterial, 0.5f));
        final Map<VisualEdgeRole, BlockEdgeDesign> edges = new EnumMap<>(VisualEdgeRole.class);
        edges.put(VisualEdgeRole.DEFAULT_LOCAL, new BlockEdgeDesign(edgeMaterial, 0.2f, 0.5D));
        edges.put(VisualEdgeRole.INTER_GRAPH, new BlockEdgeDesign(edgeMaterial, 0.25f, 0.6D));
        edges.put(VisualEdgeRole.TELEPORT, new BlockEdgeDesign(edgeMaterial, 0.3f, 0.5D));
        edges.put(VisualEdgeRole.GLOBAL_TELEPORT, new BlockEdgeDesign(edgeMaterial, 0.35f, 0.5D));
        return new BlockDisplayDesignSet(nodes, edges);
    }
}
