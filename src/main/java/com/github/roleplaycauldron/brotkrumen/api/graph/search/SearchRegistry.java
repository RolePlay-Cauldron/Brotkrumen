package com.github.roleplaycauldron.brotkrumen.api.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;

import java.util.List;

/**
 * Registry for path algorithms used by public path searches.
 */
public interface SearchRegistry {

    /**
     * Registers a path algorithm.
     *
     * @param algorithm path algorithm
     */
    void register(PathAlgorithm algorithm);

    /**
     * Unregisters a path algorithm.
     *
     * @param algorithm path algorithm
     */
    void unregister(PathAlgorithm algorithm);

    /**
     * Returns all registered algorithms.
     *
     * @return read-only algorithms
     */
    List<PathAlgorithm> all();

    /**
     * Selects the first suitable algorithm.
     *
     * @param graph graph to search
     * @param rules teleport rules
     * @return selected algorithm
     */
    PathAlgorithm select(Graph graph, TeleportRules rules);
}
