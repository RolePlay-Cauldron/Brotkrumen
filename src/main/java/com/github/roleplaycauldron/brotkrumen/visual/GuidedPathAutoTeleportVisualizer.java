package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveAutoTeleportController;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;

/**
 * Guided-path visualizer that also updates an auto-teleport controller.
 */
public final class GuidedPathAutoTeleportVisualizer extends GuidedPathCompletionVisualizer {

    private final ResolveAutoTeleportController autoTeleportController;

    /**
     * Creates a guided-path auto-teleport visualizer.
     */
    public GuidedPathAutoTeleportVisualizer(final LoggerFactory loggerFactory,
                                            final GuidedPathVisualGraphSource guidedPathSource,
                                            final GraphRenderer renderer,
                                            final GraphDesignResolver designs,
                                            final Runnable completionCallback,
                                            final ResolveAutoTeleportController autoTeleportController) {
        super(loggerFactory, guidedPathSource, renderer, designs, completionCallback);
        this.autoTeleportController = autoTeleportController;
    }

    @Override
    /* default */ void visibilityUpdate() {
        super.visibilityUpdate();
        if (autoTeleportController != null) {
            autoTeleportController.tick();
        }
    }
}
