package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.EdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.NodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutionConfig;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import com.github.roleplaycauldron.spellbook.effect.executor.RunningEffect;
import com.github.roleplaycauldron.spellbook.effect.location.FixedAnchor;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
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
    protected RunningEffect updateNode(final RunningEffect handle, final VisualNode node, final NodeDesign design,
                                       final Player player) {
        cancel(handle);
        final EffectInstance effect = buildNodeEffect(design);
        return executor.start(effect, executionConfig(node.node().toCenterLocation(), node.node().toCenterLocation(), player));
    }

    @Override
    protected RunningEffect updateEdge(final RunningEffect handle, final VisualEdge edge,
                                       final VisualGraphSnapshot snapshot, final EdgeDesign design,
                                       final Player player) {
        final Map<NodeRef, VisualNode> nodes = snapshot.nodesByRef();
        final VisualNode source = nodes.get(edge.source());
        final VisualNode target = nodes.get(edge.target());
        if (source == null || target == null) {
            return handle;
        }

        cancel(handle);
        final EffectInstance effect = buildEdgeEffect(design, source.node(), target.node());
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

    /* default */ EffectInstance buildNodeEffect(final NodeDesign design) {
        return EffectBuilder.create()
                .shape(new CubeShape(design.scale(), 12))
                .particle(design.particle())
                .build();
    }

    /* default */ EffectInstance buildEdgeEffect(final EdgeDesign design, final Node source, final Node target) {
        final double deltaX = source.x() - target.x();
        final double deltaY = source.y() - target.y();
        final double deltaZ = source.z() - target.z();
        final float distance = (float) Math.max(0.1D, Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)));
        return EffectBuilder.create()
                .shape(new MovingPointShape(distance, Math.max(0.05f, design.thickness()), 8, false))
                .particle(design.particle())
                .build();
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
