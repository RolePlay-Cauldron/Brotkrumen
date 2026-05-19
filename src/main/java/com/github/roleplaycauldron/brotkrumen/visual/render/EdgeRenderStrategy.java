package com.github.roleplaycauldron.brotkrumen.visual.render;

/**
 * Strategy for rendering a visual edge.
 */
public enum EdgeRenderStrategy {
    /**
     * Render the edge as a continuous connection between endpoint nodes.
     */
    FULL_EDGE,

    /**
     * Render only endpoint node designs and skip the continuous edge body.
     */
    ENDPOINTS_ONLY
}
