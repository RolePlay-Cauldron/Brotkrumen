package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.TestVisualDesigns;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class VisualPresetProfileFactoryTest {

    @Test
    void resolveProfilesUseEachGraphStoredPresetAndFallbackPerRenderer() {
        final Graph emberGraph = new Graph(1, "Ember", "ember", "ember");
        final Graph prismGraph = new Graph(2, "Prism", "prism", "missing-block");
        final Graph fallbackGraph = new Graph(3, "Fallback", "missing-particle", "prism");
        final GraphDesignResolver resolver = new ProfileGraphDesignResolver(VisualPresetProfileFactory.forGraphs(
                List.of(emberGraph, prismGraph, fallbackGraph),
                registry(),
                settings("ember", "ember"),
                Map.of(),
                Map.of()
        ));

        assertEquals(Particle.FLAME, resolver.resolveParticleNode(node(emberGraph)).particle(),
                "First graph should use its ember Spellbook effect preset");
        assertEquals(Particle.END_ROD, resolver.resolveParticleNode(node(prismGraph)).particle(),
                "Second graph should use its prism Spellbook effect preset");
        assertEquals(Particle.FLAME, resolver.resolveParticleNode(node(fallbackGraph)).particle(),
                "Missing particle preset should fall back to configured spellbookEffect default");
        assertEquals(Material.COAL_BLOCK, resolver.resolveBlockNode(node(prismGraph)).blockMaterial(),
                "Missing block-display preset should fall back independently to the block default");
        assertEquals(Material.LAPIS_BLOCK, resolver.resolveBlockNode(node(fallbackGraph)).blockMaterial(),
                "Valid block-display stored preset should still resolve even when particle falls back");
    }

    @Test
    void dynamicEditorResolverUsesVisibleGraphPresetsAndTemporaryActiveRendererOverride() {
        final Graph active = new Graph(1, "Active", "ember", "ember");
        final Graph reference = new Graph(2, "Reference", "prism", "prism");
        final DynamicPresetGraphDesignResolver resolver = new DynamicPresetGraphDesignResolver(
                () -> active,
                () -> List.of(reference),
                this::registry,
                () -> settings("ember", "ember"),
                () -> Map.of(active.getGraphId(), "prism"),
                Map::of
        );

        assertEquals(Particle.END_ROD, resolver.resolveParticleNode(node(active)).particle(),
                "Temporary Spellbook effect override should apply to the active graph");
        assertEquals(Particle.END_ROD, resolver.resolveParticleNode(node(reference)).particle(),
                "Reference graph should use its own stored particle preset");
        assertEquals(Material.COAL_BLOCK, resolver.resolveBlockNode(node(active)).blockMaterial(),
                "Spellbook effect override should not alter block-display rendering");
        assertEquals(Material.LAPIS_BLOCK, resolver.resolveBlockNode(node(reference)).blockMaterial(),
                "Reference graph should use its own stored block-display preset");
    }

    @Test
    void sourceGraphPresetIsUsedForLocalEdgesInMultiGraphProfiles() {
        final Graph emberGraph = new Graph(1, "Ember", "ember", "ember");
        final Graph prismGraph = new Graph(2, "Prism", "prism", "prism");
        final GraphDesignResolver resolver = new ProfileGraphDesignResolver(VisualPresetProfileFactory.forGraphs(
                List.of(emberGraph, prismGraph), registry(), settings("ember", "ember"), Map.of(), Map.of()));

        assertEquals(Particle.FLAME, resolver.resolveParticleEdge(edge(emberGraph)).particle(),
                "Local edges from the ember graph should use ember particle designs");
        assertEquals(Particle.END_ROD, resolver.resolveParticleEdge(edge(prismGraph)).particle(),
                "Local edges from the prism graph should use prism particle designs");
        assertEquals(Material.ORANGE_WOOL, resolver.resolveBlockEdge(edge(emberGraph)).blockMaterial(),
                "Block-display local edges from the ember graph should use ember designs");
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS, resolver.resolveBlockEdge(edge(prismGraph)).blockMaterial(),
                "Block-display local edges from the prism graph should use prism designs");
    }

    private VisualPresetRegistry registry() {
        return new VisualPresetRegistry(Map.of(
                "ember", new VisualPreset("ember", TestVisualDesigns.emberParticle(), TestVisualDesigns.emberBlock()),
                "prism", new VisualPreset("prism", TestVisualDesigns.prismParticle(), TestVisualDesigns.prismBlock())
        ));
    }

    private VisualizerRenderSettings settings(final String spellbookEffectDefault, final String blockDisplayDefault) {
        return new VisualizerRenderSettings(VisualRenderer.SPELLBOOK_EFFECT, spellbookEffectDefault, blockDisplayDefault);
    }

    private VisualNode node(final Graph graph) {
        final UUID nodeId = UUID.nameUUIDFromBytes(("node:" + graph.getGraphId()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final NodeRef ref = new NodeRef(graph.getGraphId(), nodeId);
        return new VisualNode(new VisualNodeId(ref), ref, new Node(nodeId, 0.0D, 0.0D, 0.0D, null));
    }

    private VisualEdge edge(final Graph graph) {
        final NodeRef source = new NodeRef(graph.getGraphId(), UUID.randomUUID());
        final NodeRef target = new NodeRef(graph.getGraphId(), UUID.randomUUID());
        return new VisualEdge(new LocalVisualEdgeId(graph.getGraphId(), UUID.randomUUID()), source, target,
                VisualEdgeKind.LOCAL, 1.0D, Set.of(), VisualEdgeRole.DEFAULT_LOCAL);
    }
}
