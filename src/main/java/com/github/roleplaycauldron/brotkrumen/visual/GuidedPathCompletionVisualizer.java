package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;

/**
 * Guided-path visualizer that notifies once when the final guided node is reached.
 */
public final class GuidedPathCompletionVisualizer extends Visualizer {

    private final GuidedPathVisualGraphSource guidedPathSource;

    private final Runnable completionCallback;

    private boolean completionReported;

    /**
     * Creates a guided-path completion-aware visualizer.
     *
     * @param loggerFactory      logger factory
     * @param guidedPathSource   guided path source
     * @param renderer           renderer
     * @param designs            design resolver
     * @param completionCallback callback invoked once after completion
     */
    public GuidedPathCompletionVisualizer(final LoggerFactory loggerFactory,
                                          final GuidedPathVisualGraphSource guidedPathSource,
                                          final GraphRenderer renderer,
                                          final GraphDesignResolver designs,
                                          final Runnable completionCallback) {
        super(loggerFactory, guidedPathSource, renderer, designs);
        this.guidedPathSource = guidedPathSource;
        this.completionCallback = completionCallback;
        this.completionReported = false;
    }

    @Override
    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    void visibilityUpdate() {
        super.visibilityUpdate();
        if (!completionReported && guidedPathSource.complete()) {
            completionReported = true;
            if (completionCallback != null) {
                completionCallback.run();
            }
        }
    }
}
