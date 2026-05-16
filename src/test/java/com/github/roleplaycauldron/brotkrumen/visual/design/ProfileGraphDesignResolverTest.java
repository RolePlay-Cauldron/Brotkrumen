package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileGraphDesignResolverTest {

    @Test
    void graphSpecificDesignOverridesNetworkDefaultWithoutLeaking() {
        final NodeRef graphARef = new NodeRef(1, UUID.randomUUID());
        final NodeRef graphBRef = new NodeRef(2, UUID.randomUUID());
        final DesignSet defaultSet = designSet(Material.STONE, Particle.FLAME);
        final DesignSet graphASet = designSet(Material.DIAMOND_BLOCK, Particle.HEART);

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .defaultDesign(defaultSet)
                .graphDesign(1, graphASet)
                .build());

        assertEquals(graphASet.node(), resolver.resolveNode(visualNode(graphARef)), "Graph-specific design should win");
        assertEquals(defaultSet.node(), resolver.resolveNode(visualNode(graphBRef)), "Graph design should not leak");
    }

    @Test
    void explicitNodeAndEdgeOverridesWin() {
        final NodeRef ref = new NodeRef(1, UUID.randomUUID());
        final NodeDesign nodeOverride = new NodeDesign(Material.EMERALD_BLOCK, Particle.HAPPY_VILLAGER, 0.7f);
        final InterGraphVisualEdgeId edgeId = new InterGraphVisualEdgeId(UUID.randomUUID());
        final EdgeDesign edgeOverride = new EdgeDesign(Material.RED_STAINED_GLASS, Particle.SMOKE, 0.3f, 0.1D);
        final VisualEdge edge = new VisualEdge(edgeId, ref, new NodeRef(2, UUID.randomUUID()),
                VisualEdgeKind.INTER_GRAPH, 1.0D, Set.of(EdgeFlag.INTER_GRAPH));

        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .nodeOverride(ref, nodeOverride)
                .edgeOverride(edgeId, edgeOverride)
                .build());

        assertEquals(nodeOverride, resolver.resolveNode(visualNode(ref)), "Explicit node override should win");
        assertEquals(edgeOverride, resolver.resolveEdge(edge), "Explicit edge override should win");
    }

    @Test
    void interGraphStrategyCanUseSourceAndTargetGraph() {
        final NodeRef source = new NodeRef(1, UUID.randomUUID());
        final NodeRef target = new NodeRef(2, UUID.randomUUID());
        final DesignSet sourceSet = designSet(Material.GOLD_BLOCK, Particle.CRIT);
        final DesignSet targetSet = designSet(Material.LAPIS_BLOCK, Particle.WITCH);
        final VisualEdge edge = new VisualEdge(new InterGraphVisualEdgeId(UUID.randomUUID()), source, target,
                VisualEdgeKind.INTER_GRAPH, 1.0D, Set.of(EdgeFlag.INTER_GRAPH));

        final ProfileGraphDesignResolver sourceResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .graphDesign(1, sourceSet)
                .graphDesign(2, targetSet)
                .interGraphStrategy(InterGraphEdgeDesignStrategy.SOURCE_GRAPH)
                .build());
        final ProfileGraphDesignResolver targetResolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .graphDesign(1, sourceSet)
                .graphDesign(2, targetSet)
                .interGraphStrategy(InterGraphEdgeDesignStrategy.TARGET_GRAPH)
                .build());

        assertEquals(sourceSet.interGraphEdge(), sourceResolver.resolveEdge(edge), "Source strategy should use source graph");
        assertEquals(targetSet.interGraphEdge(), targetResolver.resolveEdge(edge), "Target strategy should use target graph");
    }

    private VisualNode visualNode(final NodeRef ref) {
        return new VisualNode(new VisualNodeId(ref), ref, new Node(ref.nodeId(), 0, 0, 0, null));
    }

    private DesignSet designSet(final Material material, final Particle particle) {
        final NodeDesign node = new NodeDesign(material, particle, 0.4f);
        final EdgeDesign localEdge = new EdgeDesign(material, particle, 0.2f, 0.5D);
        final EdgeDesign interGraphEdge = new EdgeDesign(material, particle, 0.25f, 0.6D);
        return new DesignSet(node, localEdge, interGraphEdge);
    }
}
