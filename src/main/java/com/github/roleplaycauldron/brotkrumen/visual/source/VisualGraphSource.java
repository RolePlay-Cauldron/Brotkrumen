package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;

/**
 * Source of graph data for visual renderers.
 */
public interface VisualGraphSource {

    /**
     * Creates the current visual snapshot.
     *
     * @return current snapshot
     */
    VisualGraphSnapshot snapshot();

    /**
     * Gets the current source version.
     *
     * @return source version
     */
    long version();
}
