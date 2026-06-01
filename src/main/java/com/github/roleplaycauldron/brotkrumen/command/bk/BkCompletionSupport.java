package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Completion helpers for `/bk`.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class BkCompletionSupport {

    private BkCompletionSupport() {
    }

    /**
     * Filters online player names for suggestions.
     *
     * @param playerNames online player names
     * @param remaining   typed prefix
     * @return suggested names
     */
    public static List<String> onlinePlayers(final Collection<String> playerNames, final String remaining) {
        final String prefix = lower(remaining);
        return playerNames.stream()
                .filter(name -> lower(name).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Creates resolve target suggestions.
     *
     * @param graphs    available graphs
     * @param remaining typed prefix
     * @return target suggestions
     */
    public static List<String> resolveTargets(final Collection<Graph> graphs, final String remaining) {
        final String prefix = lower(remaining);
        if (prefix.isBlank()) {
            return List.of("graph:", "node:");
        }
        final Stream<String> base = Stream.of("graph:", "node:")
                .filter(value -> value.startsWith(prefix));
        final Stream<String> graphValues = prefix.startsWith("graph:")
                ? graphSuggestions(graphs, prefix.substring("graph:".length()))
                : Stream.empty();
        final Stream<String> nodeValues = prefix.startsWith("node:")
                ? nodeSuggestions(graphs, prefix.substring("node:".length()))
                : Stream.empty();
        return Stream.concat(base, Stream.concat(graphValues, nodeValues))
                .distinct()
                .sorted()
                .toList();
    }

    private static Stream<String> graphSuggestions(final Collection<Graph> graphs, final String filter) {
        return graphs.stream()
                .sorted(Comparator.comparingInt(Graph::getGraphId))
                .flatMap(graph -> Stream.of("graph:" + graph.getName()))
                .filter(value -> lower(value).startsWith("graph:" + filter));
    }

    private static Stream<String> nodeSuggestions(final Collection<Graph> graphs, final String filter) {
        return graphs.stream()
                .flatMap(graph -> graph.getNodes().stream())
                .map(Node::graphId)
                .map(UUID::toString)
                .distinct()
                .filter(value -> value.startsWith(filter))
                .map(value -> "node:" + value);
    }

    private static String lower(final String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
