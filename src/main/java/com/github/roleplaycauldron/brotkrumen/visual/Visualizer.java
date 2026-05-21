package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;

/**
 * Source-backed visualization orchestrator for graph, network, path, and guided-path snapshots.
 */
public class Visualizer {

    /**
     * The loggerFactory for this class.
     */
    protected final LoggerFactory loggerFactory;

    private final VisualGraphSource source;

    private final GraphRenderer renderer;

    private final GraphDesignResolver designs;

    private long lastRenderedVersion = Long.MIN_VALUE;

    /**
     * Constructs a source-backed visualizer.
     *
     * @param loggerFactory the factory to create loggers for this instance
     * @param source        visual graph source
     * @param renderer      renderer
     * @param designs       design resolver
     */
    public Visualizer(final LoggerFactory loggerFactory, final VisualGraphSource source,
                      final GraphRenderer renderer, final GraphDesignResolver designs) {
        this.loggerFactory = loggerFactory;
        this.source = source;
        this.renderer = renderer;
        this.designs = designs;
    }

    /**
     * Releases rendered resources owned by this visualizer.
     */
    public void shutdown() {
        if (renderer != null) {
            renderer.shutdown();
        }
    }

    /**
     * Reconciles the visualizer with the latest source snapshot.
     */
    public void refresh() {
        lastRenderedVersion = Long.MIN_VALUE;
        visibilityUpdate();
    }

    /**
     * Updates rendered state or viewer-only visibility for the current source snapshot.
     */
    /* default */
    void visibilityUpdate() {
        if (source == null || renderer == null || designs == null) {
            return;
        }
        if (source.version() == lastRenderedVersion) {
            renderer.applyVisibilityOnly();
            return;
        }
        renderer.apply(source.snapshot(), designs);
        lastRenderedVersion = source.version();
    }
}
