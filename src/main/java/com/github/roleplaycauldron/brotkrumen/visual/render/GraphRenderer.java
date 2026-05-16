package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;

/**
 * Renderer for visual graph snapshots.
 */
public interface GraphRenderer {

    /**
     * Applies a snapshot and reconciles rendered state.
     *
     * @param snapshot snapshot
     * @param designs  design resolver
     */
    void apply(VisualGraphSnapshot snapshot, GraphDesignResolver designs);

    /**
     * Applies viewer visibility rules without graph structure reconciliation.
     */
    void applyVisibilityOnly();

    /**
     * Shuts down renderer resources.
     */
    void shutdown();
}
