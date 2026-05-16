package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;

/**
 * Source-based visualizer for one player.
 */
public class PlayerGraphVisualizer extends GraphVisualizer {

    /**
     * Creates a player graph visualizer.
     *
     * @param loggerFactory logger factory
     * @param source        visual graph source
     * @param renderer      renderer
     * @param designs       design resolver
     */
    public PlayerGraphVisualizer(final LoggerFactory loggerFactory, final VisualGraphSource source,
                                 final GraphRenderer renderer, final GraphDesignResolver designs) {
        super(loggerFactory, source, renderer, designs);
    }
}
