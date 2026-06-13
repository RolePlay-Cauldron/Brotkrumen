package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveAutoTeleportController;
import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveAwayCancellationController;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;

/**
 * Guided-path visualizer that also updates an auto-teleport controller.
 */
public final class GuidedPathAutoTeleportVisualizer extends GuidedPathCompletionVisualizer {

    private final ResolveAutoTeleportController autoTeleportController;

    private final ResolveAwayCancellationController awayCancellationController;

    /**
     * Creates a guided-path auto-teleport visualizer.
     *
     * @param loggerFactory          the logger factory
     * @param guidedPathSource       the guided path source
     * @param renderer               the graph renderer
     * @param designs                the graph design resolver
     * @param completionCallback     the completion callback
     * @param autoTeleportController the auto teleport controller
     * @param awayCancellationController the away-cancellation controller
     */
    public GuidedPathAutoTeleportVisualizer(final LoggerFactory loggerFactory,
                                            final GuidedPathVisualGraphSource guidedPathSource,
                                            final GraphRenderer renderer,
                                            final GraphDesignResolver designs,
                                            final Runnable completionCallback,
                                            final ResolveAutoTeleportController autoTeleportController,
                                            final ResolveAwayCancellationController awayCancellationController) {
        super(loggerFactory, guidedPathSource, renderer, designs, completionCallback);
        this.autoTeleportController = autoTeleportController;
        this.awayCancellationController = awayCancellationController;
    }

    @Override
        /* default */ void visibilityUpdate() {
        super.visibilityUpdate();
        if (autoTeleportController != null) {
            autoTeleportController.tick();
        }
        if (awayCancellationController != null) {
            awayCancellationController.tick();
        }
    }
}
