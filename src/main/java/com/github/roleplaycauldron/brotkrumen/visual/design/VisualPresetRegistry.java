package com.github.roleplaycauldron.brotkrumen.visual.design;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable runtime cache of loaded visual presets.
 */
public final class VisualPresetRegistry {

    private final Map<String, VisualPreset> presets;

    /**
     * Creates a registry from normalized presets.
     *
     * @param presets presets by normalized name
     */
    public VisualPresetRegistry(final Map<String, VisualPreset> presets) {
        this.presets = Collections.unmodifiableMap(new LinkedHashMap<>(presets));
    }

    /**
     * Normalizes a preset identifier for lookup and storage.
     *
     * @param name preset name
     * @return normalized name
     */
    public static String normalizePresetName(final String name) {
        return name == null ? "" : name.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    /**
     * Returns all cached preset names.
     *
     * @return preset names
     */
    public Set<String> presetNames() {
        return presets.keySet();
    }

    /**
     * Returns names compatible with a renderer.
     *
     * @param renderer renderer
     * @return compatible preset names
     */
    public Set<String> presetNames(final VisualRenderer renderer) {
        return presets.values().stream()
                .filter(preset -> preset.supports(renderer))
                .map(VisualPreset::name)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * Finds a preset by name.
     *
     * @param name preset name
     * @return preset when available
     */
    public Optional<VisualPreset> find(final String name) {
        return Optional.ofNullable(presets.get(normalizePresetName(name)));
    }

    /**
     * Finds a preset that supports the requested renderer.
     *
     * @param name     preset name
     * @param renderer renderer
     * @return compatible preset when available
     */
    public Optional<VisualPreset> find(final String name, final VisualRenderer renderer) {
        return find(name).filter(preset -> preset.supports(renderer));
    }

    /**
     * Checks whether a compatible preset exists.
     *
     * @param name     preset name
     * @param renderer renderer
     * @return true when compatible preset exists
     */
    public boolean supports(final String name, final VisualRenderer renderer) {
        return find(name, renderer).isPresent();
    }

    /**
     * Returns a compatible preset using preferred, configured fallback, then any compatible preset.
     *
     * @param preferredName preferred preset
     * @param fallbackName  configured fallback preset
     * @param renderer      renderer
     * @return compatible preset
     */
    public Optional<VisualPreset> resolve(final String preferredName, final String fallbackName,
                                          final VisualRenderer renderer) {
        return find(preferredName, renderer)
                .or(() -> find(fallbackName, renderer))
                .or(() -> presets.values().stream()
                        .filter(preset -> preset.supports(renderer))
                        .findFirst());
    }

    /**
     * Checks whether the registry contains at least one preset.
     *
     * @return true when empty
     */
    public boolean isEmpty() {
        return presets.isEmpty();
    }
}
