package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeEffectContext;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeEffectContext;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutionConfig;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import com.github.roleplaycauldron.spellbook.effect.executor.RunningEffect;
import com.github.roleplaycauldron.spellbook.effect.location.FixedAnchor;
import com.github.roleplaycauldron.spellbook.effect.viewer.FixedViewerSource;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Spellbook particle/effect renderer for visual graph snapshots.
 */
public class ParticleGraphRenderer extends AbstractGraphRenderer<RunningEffect, RunningEffect> {

    private static final long PERIOD_TICKS = 2L;

    private final EffectExecutor executor;

    /**
     * Creates a particle renderer.
     *
     * @param plugin   plugin
     * @param viewerId viewer id
     * @param executor Spellbook effect executor
     */
    public ParticleGraphRenderer(final Brotkrumen plugin, final UUID viewerId, final EffectExecutor executor) {
        super(plugin, viewerId);
        this.executor = executor;
    }

    @Override
    protected RunningEffect updateNode(final RunningEffect handle, final VisualNode node,
                                       final GraphDesignResolver designs,
                                       final Player player) {
        cancel(handle);
        final EffectInstance effect = buildNodeEffect(designs.resolveParticleNode(node), node);
        return executor.start(effect, executionConfig(node.node().toCenterLocation(), node.node().toCenterLocation(), player));
    }

    @Override
    protected RunningEffect updateEdge(final RunningEffect handle, final VisualEdge edge,
                                       final VisualGraphSnapshot snapshot, final GraphDesignResolver designs,
                                       final Player player) {
        final Map<NodeRef, VisualNode> nodes = snapshot.nodesByRef();
        final VisualNode source = nodes.get(edge.source());
        final VisualNode target = nodes.get(edge.target());
        if (source == null || target == null) {
            return handle;
        }

        cancel(handle);
        final EffectInstance effect = buildEdgeEffect(designs.resolveParticleEdge(edge), edge, source, target);
        return executor.start(effect, executionConfig(source.node().toCenterLocation(), target.node().toCenterLocation(), player));
    }

    @Override
    protected void removeNode(final RunningEffect handle) {
        cancel(handle);
    }

    @Override
    protected void removeEdge(final RunningEffect handle) {
        cancel(handle);
    }

    /* default */ EffectInstance buildNodeEffect(final ParticleNodeDesign design, final VisualNode node) {
        return design.effectFactory().create(new ParticleNodeEffectContext(node));
    }

    /* default */ EffectInstance buildEdgeEffect(final ParticleEdgeDesign design, final VisualEdge edge,
                                                 final VisualNode source, final VisualNode target) {
        return design.effectFactory().create(new ParticleEdgeEffectContext(edge, source, target));
    }

    private EffectExecutionConfig executionConfig(final Location origin, final Location target, final Player player) {
        return EffectExecutionConfig.builder()
                .originAnchor(new FixedAnchor(origin))
                .targetAnchor(new FixedAnchor(target))
                .viewerSource(new FixedViewerSource(Collections.singletonList(player)))
                .periodTicks(PERIOD_TICKS)
                .maxRuns(-1)
                .build();
    }

    private void cancel(final RunningEffect effect) {
        if (effect != null) {
            effect.cancel();
        }
    }
}
