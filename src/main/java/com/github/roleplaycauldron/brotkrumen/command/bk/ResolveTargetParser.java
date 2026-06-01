package com.github.roleplaycauldron.brotkrumen.command.bk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
     * @return parsed target
     * @throws TargetParseException if parsing fails
     */
    public ResolveTarget parse(final List<String> tokens) throws TargetParseException {
        if (tokens == null || tokens.isEmpty()) {
            throw new TargetParseException("commands.bk.resolve.parse.error.missingTarget");
        }

        String graphKey = null;
        final List<UUID> nodeIds = new ArrayList<>();
        for (final String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            final String normalized = token.toLowerCase(Locale.ROOT);
            if (normalized.startsWith(NETWORK_PREFIX)) {
                throw new TargetParseException("commands.bk.resolve.parse.error.networkNotSupported");
            }
            if (normalized.startsWith(GRAPH_PREFIX) || normalized.startsWith(GRAPH_SHORT_PREFIX)) {
                if (graphKey != null) {
                    throw new TargetParseException("commands.bk.resolve.parse.error.multipleGraphTargets");
                }
                graphKey = valueAfterPrefix(token);
                if (graphKey.isBlank()) {
                    throw new TargetParseException("commands.bk.resolve.parse.error.missingGraphTarget");
                }
                continue;
            }
            final Optional<UUID> nodeId = parseNodeToken(token);
            if (nodeId.isPresent()) {
                nodeIds.add(nodeId.get());
            } else {
                throw new TargetParseException("commands.bk.resolve.parse.error.unknownToken", Map.of("token", token));
            }
        }

        if (graphKey != null && !nodeIds.isEmpty()) {
            throw new TargetParseException("commands.bk.resolve.parse.error.mixedTargetModes");
        }
        if (graphKey != null) {
            return ResolveTarget.graph(graphKey);
        }
        if (!nodeIds.isEmpty()) {
            return ResolveTarget.nodes(nodeIds);
        }
        throw new TargetParseException("commands.bk.resolve.parse.error.missingTarget");
    }

    /**
     * Parses a raw target tail.
     *
     * @param rawTargets raw target text
     * @return parsed target
     * @throws TargetParseException if parsing fails
     */
    public ResolveTarget parse(final String rawTargets) throws TargetParseException {
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
}
