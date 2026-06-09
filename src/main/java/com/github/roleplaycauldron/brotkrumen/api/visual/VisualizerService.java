package com.github.roleplaycauldron.brotkrumen.api.visual;

import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;

import java.util.UUID;

/**
 * Synchronous, main-thread-bound visualizer lifecycle service.
 */
public interface VisualizerService {

    /**
     * Shows a visualizer for a viewer.
     *
     * @param viewerId   viewer id
     * @param visualizer visualizer to show
     */
    void show(UUID viewerId, Visualizer visualizer);

    /**
     * Replaces a viewer visualizer.
     *
     * @param viewerId   viewer id
     * @param visualizer replacement visualizer
     */
    void replace(UUID viewerId, Visualizer visualizer);

    /**
     * Hides a viewer visualizer.
     *
     * @param viewerId viewer id
     */
    void hide(UUID viewerId);

    /**
     * Refreshes a viewer visualizer.
     *
     * @param viewerId viewer id
     */
    void refresh(UUID viewerId);

    /**
     * Refreshes all visualizers.
     */
    void refreshAll();
}
