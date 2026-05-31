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
        final VisualNode teleportNode = visualNode(ref, VisualNodeRole.LOCAL_TELEPORT);
        final VisualEdge teleportEdge = new VisualEdge(new InterGraphVisualEdgeId(UUID.randomUUID()), ref,
                new NodeRef(1, UUID.randomUUID()), VisualEdgeKind.LOCAL, 1.0D, Set.of(EdgeFlag.TELEPORT),
                VisualEdgeRole.TELEPORT);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, set)
                .build());

        assertEquals(set.nodeDesign(VisualNodeRole.LOCAL_TELEPORT), resolver.resolveParticleNode(teleportNode),
                "Teleport endpoint node role should use role design");
        assertEquals(set.edgeDesign(VisualEdgeRole.TELEPORT), resolver.resolveParticleEdge(teleportEdge),
                "Teleport edge role should use role design");
        assertEquals(EdgeRenderStrategy.ENDPOINTS_ONLY, resolver.resolveEdgeRenderStrategy(teleportEdge),
                "Teleport edges should default to endpoint-only rendering");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.LOCAL_TELEPORT),
                "Preset should include local teleport node design");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.INTERGRAPH_TELEPORT),
                "Preset should include intergraph teleport node design");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.WARP),
                "Preset should include warp node design");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.GUIDED_PATH_GOAL),
                "Preset should include guided goal marker node design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.TELEPORT),
                "Teleport edge role should fall back to default edge design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.DIRECTED_LOCAL),
                "Preset should include directed edge design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.UNDIRECTED_LOCAL),
                "Preset should include undirected edge design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.BLOCKED),
                "Preset should include blocked edge design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.DIRECTED_INTER_GRAPH),
                "Preset should include directed inter-graph edge design");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.UNDIRECTED_INTER_GRAPH),
                "Preset should include undirected inter-graph edge design");
        assertNotEquals(BlockDisplayDesignSet.prismPreset().edgeDesign(VisualEdgeRole.DIRECTED_LOCAL).blockMaterial(),
                BlockDisplayDesignSet.prismPreset().edgeDesign(VisualEdgeRole.UNDIRECTED_LOCAL).blockMaterial(),
                "Block-display directed and undirected edge roles should be visually distinct");
        assertNotNull(ParticleDesignSet.prismPreset().nodeDesign(VisualNodeRole.DEFAULT).effect(),
                "Particle node presets should expose EffectInstance data");
        assertNotNull(ParticleDesignSet.prismPreset().edgeDesign(VisualEdgeRole.DEFAULT_LOCAL).effect(),
                "Particle edge presets should expose EffectInstance data");
        assertNotNull(BlockDisplayDesignSet.prismPreset().nodeDesign(VisualNodeRole.GUIDED_PATH_GOAL),
                "Block-display presets should include guided goal marker node design");
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

        assertEquals(nodeOverride, resolver.resolveParticleNode(visualNode(ref, VisualNodeRole.LOCAL_TELEPORT)),
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

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void guidedGoalMarkerRoleResolvesAndFallsBack() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final VisualNode goalNode = visualNode(ref, VisualNodeRole.GUIDED_PATH_GOAL);
        final ParticleNodeDesign goalParticle = ParticleNodeDesign.sphere(Particle.HAPPY_VILLAGER, 0.8f);
        final BlockNodeDesign goalBlock = new BlockNodeDesign(Material.SEA_LANTERN, 0.85f);
        final Map<VisualNodeRole, ParticleNodeDesign> particleFallbackNodes = new EnumMap<>(VisualNodeRole.class);
        particleFallbackNodes.put(VisualNodeRole.DEFAULT, ParticleNodeDesign.cube(Particle.FLAME, 0.4f));
        final Map<VisualNodeRole, BlockNodeDesign> blockFallbackNodes = new EnumMap<>(VisualNodeRole.class);
        blockFallbackNodes.put(VisualNodeRole.DEFAULT, new BlockNodeDesign(Material.STONE, 0.4f));
        final Map<VisualEdgeRole, ParticleEdgeDesign> fallbackParticleEdges = new EnumMap<>(VisualEdgeRole.class);
        fallbackParticleEdges.put(VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.line(Particle.FLAME, 8));
        final Map<VisualEdgeRole, BlockEdgeDesign> fallbackBlockEdges = new EnumMap<>(VisualEdgeRole.class);
        fallbackBlockEdges.put(VisualEdgeRole.DEFAULT_LOCAL, new BlockEdgeDesign(Material.STONE, 0.2f, 0.5D));
        final ProfileGraphDesignResolver configuredResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, new ParticleDesignSet(Map.of(
                        VisualNodeRole.DEFAULT, ParticleNodeDesign.cube(Particle.FLAME, 0.4f),
                        VisualNodeRole.GUIDED_PATH_GOAL, goalParticle
                ), Map.of(VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.line(Particle.FLAME, 8))))
                .blockDisplayGraphDesign(1, new BlockDisplayDesignSet(Map.of(
                        VisualNodeRole.DEFAULT, new BlockNodeDesign(Material.STONE, 0.4f),
                        VisualNodeRole.GUIDED_PATH_GOAL, goalBlock
                ), Map.of(VisualEdgeRole.DEFAULT_LOCAL, new BlockEdgeDesign(Material.STONE, 0.2f, 0.5D))))
                .build());
        final ProfileGraphDesignResolver fallbackResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .particleGraphDesign(1, new ParticleDesignSet(particleFallbackNodes, fallbackParticleEdges))
                .blockDisplayGraphDesign(1, new BlockDisplayDesignSet(blockFallbackNodes, fallbackBlockEdges))
                .build());

        assertEquals(goalParticle, configuredResolver.resolveParticleNode(goalNode),
                "Configured particle goal marker role should be used");
        assertEquals(goalBlock, configuredResolver.resolveBlockNode(goalNode),
                "Configured block-display goal marker role should be used");
        assertEquals(particleFallbackNodes.get(VisualNodeRole.DEFAULT), fallbackResolver.resolveParticleNode(goalNode),
                "Missing particle goal marker role should fall back to default node design");
        assertEquals(blockFallbackNodes.get(VisualNodeRole.DEFAULT), fallbackResolver.resolveBlockNode(goalNode),
                "Missing block goal marker role should fall back to default node design");
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
        nodes.put(VisualNodeRole.LOCAL_TELEPORT, ParticleNodeDesign.sphere(particle, 0.5f));
        nodes.put(VisualNodeRole.INTERGRAPH_TELEPORT, ParticleNodeDesign.sphere(particle, 0.55f));
        nodes.put(VisualNodeRole.WARP, ParticleNodeDesign.sphere(particle, 0.6f));
        nodes.put(VisualNodeRole.GUIDED_PATH_GOAL, ParticleNodeDesign.sphere(particle, 0.65f));
        final Map<VisualEdgeRole, ParticleEdgeDesign> edges = new EnumMap<>(VisualEdgeRole.class);
        edges.put(VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.movingPoint(particle, 0.2f));
        edges.put(VisualEdgeRole.DIRECTED_LOCAL, ParticleEdgeDesign.movingPoint(particle, 0.2f));
        edges.put(VisualEdgeRole.UNDIRECTED_LOCAL, ParticleEdgeDesign.line(particle, 20));
        edges.put(VisualEdgeRole.BLOCKED, ParticleEdgeDesign.line(particle, 20));
        edges.put(VisualEdgeRole.INTER_GRAPH, ParticleEdgeDesign.movingPoint(particle, 0.25f));
        edges.put(VisualEdgeRole.TELEPORT, ParticleEdgeDesign.movingPoint(particle, 0.3f));
        edges.put(VisualEdgeRole.DIRECTED_INTER_GRAPH, ParticleEdgeDesign.movingPoint(particle, 0.25f));
        edges.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, ParticleEdgeDesign.line(particle, 20));
        return new ParticleDesignSet(nodes, edges);
    }

    private BlockDisplayDesignSet blockSet(final Material nodeMaterial, final Material edgeMaterial) {
        final Map<VisualNodeRole, BlockNodeDesign> nodes = new EnumMap<>(VisualNodeRole.class);
        nodes.put(VisualNodeRole.DEFAULT, new BlockNodeDesign(nodeMaterial, 0.4f));
        nodes.put(VisualNodeRole.LOCAL_TELEPORT, new BlockNodeDesign(nodeMaterial, 0.5f));
        nodes.put(VisualNodeRole.INTERGRAPH_TELEPORT, new BlockNodeDesign(nodeMaterial, 0.55f));
        nodes.put(VisualNodeRole.WARP, new BlockNodeDesign(nodeMaterial, 0.6f));
        nodes.put(VisualNodeRole.GUIDED_PATH_GOAL, new BlockNodeDesign(nodeMaterial, 0.65f));
        final Map<VisualEdgeRole, BlockEdgeDesign> edges = new EnumMap<>(VisualEdgeRole.class);
        edges.put(VisualEdgeRole.DEFAULT_LOCAL, new BlockEdgeDesign(edgeMaterial, 0.2f, 0.5D));
        edges.put(VisualEdgeRole.DIRECTED_LOCAL, new BlockEdgeDesign(edgeMaterial, 0.2f, 0.5D));
        edges.put(VisualEdgeRole.UNDIRECTED_LOCAL, new BlockEdgeDesign(edgeMaterial, 0.2f, 0.5D));
        edges.put(VisualEdgeRole.BLOCKED, new BlockEdgeDesign(edgeMaterial, 0.2f, 0.5D));
        edges.put(VisualEdgeRole.INTER_GRAPH, new BlockEdgeDesign(edgeMaterial, 0.25f, 0.6D));
        edges.put(VisualEdgeRole.TELEPORT, new BlockEdgeDesign(edgeMaterial, 0.3f, 0.5D));
        edges.put(VisualEdgeRole.DIRECTED_INTER_GRAPH, new BlockEdgeDesign(edgeMaterial, 0.25f, 0.6D));
        edges.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, new BlockEdgeDesign(edgeMaterial, 0.25f, 0.6D));
        return new BlockDisplayDesignSet(nodes, edges);
    }
}
