package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of the path algorithm registry.
 */
public class SearchRegistryImpl implements SearchRegistry {
    private final List<PathAlgorithm> algorithms;

    /**
     * The default constructor.
     */
    public SearchRegistryImpl() {
        this.algorithms = new ArrayList<>();
    }

    @Override
    public void register(final PathAlgorithm algo) {
        algorithms.add(Objects.requireNonNull(algo));
    }

    @Override
    public void unregister(final PathAlgorithm algo) {
        if (algorithms.contains(algo)) {
            algorithms.remove(algo);
            return;
        }
        throw new IllegalArgumentException(String.format("Algorithm %s is not registered and cannot be removed", algo.getClass()));
    }

    @Override
    public List<PathAlgorithm> all() {
        return Collections.unmodifiableList(algorithms);
    }

    @Override
    public PathAlgorithm select(final Graph graph, final TeleportRules rules) {
        return algorithms.stream()
                .filter(a -> a.suitable(graph, rules))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No suitable path algorithm registered"));
    }
}
