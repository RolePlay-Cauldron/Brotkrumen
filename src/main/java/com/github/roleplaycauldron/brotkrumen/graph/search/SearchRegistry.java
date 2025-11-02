package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The registry for the different search algorithms.
 */
public class SearchRegistry {
    private final List<PathAlgorithm> algorithms;

    /**
     * The default constructor.
     */
    public SearchRegistry() {
        this.algorithms = new ArrayList<>();
    }

    /**
     * Register a new {@link PathAlgorithm} for the path searching.
     *
     * @param algo the {@link PathAlgorithm} to register
     */
    public void register(final PathAlgorithm algo) {
        algorithms.add(Objects.requireNonNull(algo));
    }

    /**
     * Unregister a {@link PathAlgorithm}.
     *
     * @param algo the {@link PathAlgorithm} to unregister
     * @throws IllegalArgumentException if the algorithm is not registered
     */
    public void unregister(final PathAlgorithm algo) {
        if (algorithms.contains(algo)) {
            algorithms.remove(algo);
            return;
        }
        throw new IllegalArgumentException(String.format("Algorithm %s is not registered and cannot be removed", algo.getClass()));
    }

    /**
     * Get all registered algorithms.
     *
     * @return an unmodifiable list of all registered algorithms
     */
    public List<PathAlgorithm> all() {
        return Collections.unmodifiableList(algorithms);
    }

    /**
     * Select the best algorithm for the given graph and teleport rules.
     *
     * @param graph the {@link Graph} to search in
     * @param rules the {@link TeleportRules} to use
     * @return the {@link PathAlgorithm} that is suitable for the given graph and teleport rules
     */
    public PathAlgorithm select(final Graph graph, final TeleportRules rules) {
        return algorithms.stream()
                .filter(a -> a.suitable(graph, rules))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No suitable path algorithm registered"));
    }
}
