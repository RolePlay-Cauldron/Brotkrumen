package com.github.roleplaycauldron.brotkrumen.command.bk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Parser for flexible `/bk resolve` target tokens.
 */
@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
public final class ResolveTargetParser {

    private static final String GRAPH_PREFIX = "graph:";

    private static final String GRAPH_SHORT_PREFIX = "g:";

    private static final String NODE_PREFIX = "node:";

    private static final String NODE_SHORT_PREFIX = "n:";

    private static final String NETWORK_PREFIX = "network:";

    /**
     * Creates a parser.
     */
    public ResolveTargetParser() {
    }

    /**
     * Parses resolver target tokens.
     *
     * @param tokens target tokens
     * @return parsed result
     */
    public ParseResult parse(final List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return ParseResult.failure("Please specify a graph or node target.");
        }

        String graphKey = null;
        final List<UUID> nodeIds = new ArrayList<>();
        for (final String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            final String normalized = token.toLowerCase(Locale.ROOT);
            if (normalized.startsWith(NETWORK_PREFIX)) {
                return ParseResult.failure("Network targets are not supported by /bk resolve.");
            }
            if (normalized.startsWith(GRAPH_PREFIX) || normalized.startsWith(GRAPH_SHORT_PREFIX)) {
                if (graphKey != null) {
                    return ParseResult.failure("Please specify only one graph target.");
                }
                graphKey = valueAfterPrefix(token);
                if (graphKey.isBlank()) {
                    return ParseResult.failure("Please specify a graph name or id.");
                }
                continue;
            }
            final Optional<UUID> nodeId = parseNodeToken(token);
            if (nodeId.isPresent()) {
                nodeIds.add(nodeId.get());
            } else {
                return ParseResult.failure("Unknown resolve target token: " + token);
            }
        }

        if (graphKey != null && !nodeIds.isEmpty()) {
            return ParseResult.failure("Graph and node targets cannot be mixed.");
        }
        if (graphKey != null) {
            return ParseResult.success(ResolveTarget.graph(graphKey));
        }
        if (!nodeIds.isEmpty()) {
            return ParseResult.success(ResolveTarget.nodes(nodeIds));
        }
        return ParseResult.failure("Please specify a graph or node target.");
    }

    /**
     * Parses a raw target tail.
     *
     * @param rawTargets raw target text
     * @return parsed result
     */
    public ParseResult parse(final String rawTargets) {
        if (rawTargets == null || rawTargets.isBlank()) {
            return parse(List.of());
        }
        return parse(List.of(rawTargets.trim().split("\\s+")));
    }

    private Optional<UUID> parseNodeToken(final String token) {
        final String normalized = token.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith(NODE_PREFIX) && !normalized.startsWith(NODE_SHORT_PREFIX)) {
            return Optional.empty();
        }
        final String value = valueAfterPrefix(token);
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String valueAfterPrefix(final String token) {
        final int separator = token.indexOf(':');
        return separator < 0 ? "" : token.substring(separator + 1);
    }

    /**
     * Parse result.
     *
     * @param target parsed target
     * @param error  error message
     */
    public record ParseResult(ResolveTarget target, String error) {

        /**
         * Creates a success result.
         *
         * @param target parsed target
         * @return success result
         */
        public static ParseResult success(final ResolveTarget target) {
            return new ParseResult(target, null);
        }

        /**
         * Creates a failure result.
         *
         * @param error error message
         * @return failure result
         */
        public static ParseResult failure(final String error) {
            return new ParseResult(null, error);
        }

        /**
         * Checks if parsing succeeded.
         *
         * @return true when a target was parsed
         */
        public boolean success() {
            return target != null;
        }
    }
}
