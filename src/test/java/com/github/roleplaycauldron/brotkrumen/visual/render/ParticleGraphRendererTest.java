package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.design.EdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.NodeDesign;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParticleGraphRendererTest {

    @Test
    void mapsNodeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final EffectInstance effect = renderer.buildNodeEffect(new NodeDesign(Material.DIAMOND_BLOCK, Particle.HEART, 0.8f));

        assertNotNull(effect, "Node design should produce a Spellbook effect instance");
    }

    @Test
    void mapsEdgeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final Node source = new Node(UUID.randomUUID(), 0, 0, 0, null);
        final Node target = new Node(UUID.randomUUID(), 3, 4, 0, null);
        final EffectInstance effect = renderer.buildEdgeEffect(
                new EdgeDesign(Material.RED_STAINED_GLASS, Particle.END_ROD, 0.2f, 0.8D), source, target);

        assertNotNull(effect, "Edge design should produce a Spellbook effect instance");
    }
}
