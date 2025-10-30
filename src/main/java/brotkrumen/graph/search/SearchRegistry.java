package brotkrumen.graph.search;

import brotkrumen.graph.Graph;
import brotkrumen.graph.TeleportRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SearchRegistry {
    private final List<PathAlgorithm> algorithms;

    public SearchRegistry() {
        this.algorithms = new ArrayList<>();
    }

    public void register(PathAlgorithm algo) {
        algorithms.add(Objects.requireNonNull(algo));
    }

    public void unregister(PathAlgorithm algo) {
        if (algorithms.contains(algo)) {
            algorithms.remove(algo);
            return;
        }
        throw new IllegalArgumentException(String.format("Algorithm %s is not registered and cannot be removed", algo.getClass()));
    }

    public List<PathAlgorithm> all() {
        return Collections.unmodifiableList(algorithms);
    }

    public PathAlgorithm select(Graph graph, TeleportRules rules) {
        return algorithms.stream()
                .filter(a -> a.suitable(graph, rules))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No suitable path algorithm registered"));
    }
}
