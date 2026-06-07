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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
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

    /**
     * Creates resolve target suggestions for the current token in a greedy target tail.
     *
     * @param graphs     available graphs
     * @param rawTargets raw target tail
     * @return target suggestions
     */
    public static List<String> resolveTargetTail(final Collection<Graph> graphs, final String rawTargets) {
        final String targets = rawTargets == null ? "" : rawTargets;
        final int tokenStart = currentTokenStart(targets);
        final String currentToken = targets.substring(tokenStart);
        final String completed = targets.substring(0, tokenStart).trim();
        final List<String> completedTokens = completed.isBlank()
                ? List.of()
                : List.of(completed.split("\\s+"));
        return resolveTargetToken(graphs, completedTokens, currentToken);
    }

    /**
     * Finds the start offset of the current token in a raw target tail.
     *
     * @param rawTargets raw target tail
     * @return current token start offset
     */
    public static int currentTokenStart(final String rawTargets) {
        final String targets = rawTargets == null ? "" : rawTargets;
        for (int index = targets.length() - 1; index >= 0; index--) {
            if (Character.isWhitespace(targets.charAt(index))) {
                return index + 1;
            }
        }
        return 0;
    }

    /**
     * Creates teleport rule suggestions without prefixes.
     *
     * @param remaining typed prefix
     * @return rule suggestions
     */
    public static List<String> teleportRulesOnly(final String remaining) {
        final String prefix = lower(remaining);
        return Stream.of("DISABLED", "LOCAL_TP_ONLY", "WARPS_ONLY", "INTERGRAPH_TP_ONLY",
                        "LOCAL_INTERGRAPH_TP", "LOCAL_TP_WARP", "INTERGRAPH_WARP", "LOCAL_INTERGRAPH_WARP")
                .filter(value -> lower(value).startsWith(prefix))
                .sorted()
                .toList();
    }

    private static List<String> resolveTargetToken(final Collection<Graph> graphs,
                                                   final List<String> completedTokens,
                                                   final String currentToken) {
        final String current = lower(currentToken);
        final boolean hasGraph = completedTokens.stream().anyMatch(BkCompletionSupport::isGraphToken);
        final boolean hasNode = completedTokens.stream().anyMatch(BkCompletionSupport::isNodeToken);
        final boolean hasTeleport = completedTokens.stream().anyMatch(BkCompletionSupport::isTeleportToken);
        if (isTeleportToken(current) || hasGraph || hasNode) {
            return resolvePostTargetToken(graphs, current, hasGraph, hasNode, hasTeleport);
        }
        return resolveTargets(graphs, current);
    }

    private static List<String> resolvePostTargetToken(final Collection<Graph> graphs,
                                                       final String current,
                                                       final boolean hasGraph,
                                                       final boolean hasNode,
                                                       final boolean hasTeleport) {
        if (hasTeleport) {
            return List.of();
        }
        if (current.isBlank()) {
            return hasGraph ? List.of("teleport:") : List.of("node:", "teleport:");
        }
        if (isTeleportToken(current)) {
            return teleportRuleTargets(current);
        }
        if (hasNode && !hasGraph) {
            return resolveTargets(graphs, current).stream()
                    .filter(BkCompletionSupport::isNodeToken)
                    .toList();
        }
        return List.of();
    }

    private static List<String> teleportRuleTargets(final String current) {
        final String value = current.substring(current.indexOf(':') + 1);
        return teleportRulesOnly(value).stream()
                .map(rule -> "teleport:" + rule)
                .toList();
    }

    private static Stream<String> graphSuggestions(final Collection<Graph> graphs, final String filter) {
        return graphs.stream()
                .sorted(Comparator.comparingInt(Graph::getGraphId))
                .map(Graph::getName)
                .filter(name -> lower(name).startsWith(lower(filter)))
                .map(name -> "graph:" + name);
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

    private static boolean isGraphToken(final String token) {
        final String normalized = lower(token);
        return normalized.startsWith("graph:") || normalized.startsWith("g:");
    }

    private static boolean isNodeToken(final String token) {
        final String normalized = lower(token);
        return normalized.startsWith("node:") || normalized.startsWith("n:");
    }

    private static boolean isTeleportToken(final String token) {
        final String normalized = lower(token);
        return normalized.startsWith("teleport:") || normalized.startsWith("tp:");
    }
}
