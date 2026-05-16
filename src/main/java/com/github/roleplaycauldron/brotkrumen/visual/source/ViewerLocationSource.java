package com.github.roleplaycauldron.brotkrumen.visual.source;

import org.bukkit.Location;

/**
 * Supplies the current viewer location for guided visual sources.
 */
@FunctionalInterface
public interface ViewerLocationSource {

    /**
     * Gets the viewer location.
     *
     * @return current viewer location, or {@code null} if unavailable
     */
    Location location();
}
