package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeDesign;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ParticleGraphRendererTest {

    @Test
    void mapsNodeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final EffectInstance effect = renderer.buildNodeEffect(ParticleNodeDesign.cube(Particle.HEART, 0.8f), null);

        assertNotNull(effect, "Node design should produce a Spellbook effect instance");
    }

    @Test
    void mapsEdgeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final Node source = new Node(UUID.randomUUID(), 0, 0, 0, null);
        final Node target = new Node(UUID.randomUUID(), 3, 4, 0, null);
        final EffectInstance effect = renderer.buildEdgeEffect(
                ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f), null, visualNode(source), visualNode(target));

        assertNotNull(effect, "Edge design should produce a Spellbook effect instance");
    }

    @Test
    void mapsCustomParticleNodeFactoryToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final AtomicBoolean invoked = new AtomicBoolean();
        final ParticleNodeDesign design = ParticleNodeDesign.shape(Particle.HEART, context -> {
            invoked.set(true);
            return new CubeShape(0.8f, 12);
        });

        final EffectInstance effect = renderer.buildNodeEffect(design, null);

        assertAll(
                () -> assertNotNull(effect, "Custom particle node factory should produce an effect instance"),
                () -> assertTrue(invoked.get(), "Custom particle node factory should be used")
        );
    }

    @Test
    void mapsCustomParticleEdgeFactoryToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final AtomicBoolean invoked = new AtomicBoolean();
        final ParticleEdgeDesign design = ParticleEdgeDesign.shape(Particle.END_ROD, context -> {
            invoked.set(true);
            return new MovingPointShape(context.distance(), 0.2f, 8, false);
        });
        final Node source = new Node(UUID.randomUUID(), 0, 0, 0, null);
        final Node target = new Node(UUID.randomUUID(), 3, 4, 0, null);

        final EffectInstance effect = renderer.buildEdgeEffect(design, null, visualNode(source), visualNode(target));

        assertAll(
                () -> assertNotNull(effect, "Custom particle edge factory should produce an effect instance"),
                () -> assertTrue(invoked.get(), "Custom particle edge factory should be used")
        );
    }

    @Test
    void cubeAndSphereParticlePresetsAreAvailable() {
        assertAll(
                () -> assertNotNull(ParticleNodeDesign.cube(Particle.FLAME, 0.4f).effectFactory(),
                        "Cube shape preset should expose an effect factory"),
                () -> assertNotNull(ParticleNodeDesign.sphere(Particle.END_ROD, 0.35f).effectFactory(),
                        "Sphere shape preset should expose an effect factory")
        );
    }

    private com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode visualNode(final Node node) {
        return new com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode(null, null, node);
    }
}
