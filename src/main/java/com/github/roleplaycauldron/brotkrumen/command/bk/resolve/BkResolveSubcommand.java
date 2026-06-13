package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.command.bk.BkCommandContext;
import com.github.roleplaycauldron.brotkrumen.command.bk.BkCompletionSupport;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.WarpPermissionHelper;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.GuidedPathAutoTeleportVisualizer;
import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.BlockDisplayGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.render.ParticleGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GraphNetworkVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.ViewerLocationSource;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * `/bk resolve` command.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.AvoidCatchingGenericException"})
public final class BkResolveSubcommand {

    private static final String CANCEL_LITERAL = "cancel";

    private static final String PLAYER_LITERAL = "player";

    private static final String PLAYER_ARGUMENT = PLAYER_LITERAL;

    private static final String GRAPH_LITERAL = "graph";

    private static final String TARGET_ARGUMENT = "targets";

    private static final String GUIDED_PATH_CONFIG = "visualizer.guidedPath";

    private static final long NO_CLEANUP_DELAY_TICKS = 0L;

    private static final long MILLIS_PER_TICK = 50L;

    private final BkCommandContext commandContext;

    private final Localization localization;

    private final ResolveGoalNotifier goalNotifier;

    /**
     * Initializes an instance of the BkResolveSubcommand class, which is responsible for
     * handling the `/bk resolve` subcommand in the Brotkrumen plugin. This subcommand
     * facilitates operations related to target resolution within the plugin's graphing
     * and visualization system.
     *
     * @param commandContext the shared context for handling commands within the
     *                       Brotkrumen plugin. It provides access to necessary services,
     *                       utilities, and execution resources required to build
     *                       and process the resolve subcommand.
     * @param localization   the localization service
     */
    public BkResolveSubcommand(final BkCommandContext commandContext, final Localization localization) {
        this.commandContext = commandContext;
        this.localization = localization;
        this.goalNotifier = new ResolveGoalNotifier(localization);
    }

    /**
     * Builds the resolve subcommand.
     *
     * @return subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> resolve() {
        return Commands.literal("resolve")
                .then(Commands.literal(CANCEL_LITERAL)
                        .executes(this::cancelOwnGuidance)
                        .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> suggestOnlinePlayers(builder))
                                .executes(this::cancelGuidance)))
                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> suggestOnlinePlayers(builder))
                        .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.greedyString())
                                .suggests((context, builder) -> suggestTargets(builder))
                                .executes(this::resolve)));
    }

    /**
     * Adds online player suggestions.
     *
     * @param builder suggestions builder
     * @return suggestions
     */
    private CompletableFuture<Suggestions> suggestOnlinePlayers(final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();
        final List<String> names = commandContext.plugin().getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        BkCompletionSupport.onlinePlayers(names, remaining).forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Adds canonical target suggestions.
     *
     * @param builder suggestions builder
     * @return suggestions
     */
    private CompletableFuture<Suggestions> suggestTargets(final SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining();
        final SuggestionsBuilder tokenBuilder = builder.createOffset(
                builder.getStart() + BkCompletionSupport.currentTokenStart(remaining));
        final CompletableFuture<Suggestions> suggestions = new CompletableFuture<>();
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () -> {
            try {
                BkCompletionSupport.resolveTargetTail(commandContext.graphRepository().getAllGraphs(), remaining)
                        .forEach(tokenBuilder::suggest);
                suggestions.complete(tokenBuilder.build());
            } catch (final RuntimeException failure) {
                commandContext.loggerFactory().create(BkResolveSubcommand.class)
                        .error("Resolve target suggestions failed: " + failure.getMessage());
                suggestions.complete(tokenBuilder.build());
            }
        });
        return suggestions;
    }

    private int resolve(final CommandContext<CommandSourceStack> context) {
        final Player player = resolveTargetPlayer(context);
        if (player == null) {
            return 0;
        }

        final String targetInput = StringArgumentType.getString(context, TARGET_ARGUMENT);

        final ResolveTarget target;
        try {
            target = commandContext.targetParser().parse(targetInput);
        } catch (final TargetParseException ex) {
            sendKey(context, ex.getErrorKey(), ex.getReplacements());
            return 0;
        }

        final UUID playerId = player.getUniqueId();
        final ResolveLocation location = ResolveLocation.from(player.getLocation());
        final ResolveOptions options = commandContext.resolveOptions();

        final long token = commandContext.sessionManager().replaceWithPending(playerId);
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () ->
                prepareResolveRulesAsync(context, playerId, token, location, options, target));
        sendKey(context, "commands.bk.resolve.status.resolvingPath", Map.of("player", player.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private void prepareResolveRulesAsync(final CommandContext<CommandSourceStack> context,
                                          final UUID playerId, final long token,
                                          final ResolveLocation location,
                                          final ResolveOptions options,
                                          final ResolveTarget target) {
        try {
            final List<Warp> managedWarps = List.copyOf(commandContext.warpRepository().getManagedWarps());
            commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () ->
                    prepareResolveRules(context, playerId, token, location, options, target, managedWarps));
        } catch (final RuntimeException failure) {
            resolveFailed(context, playerId, token, failure);
        }
    }

    private void prepareResolveRules(final CommandContext<CommandSourceStack> context,
                                     final UUID playerId, final long token,
                                     final ResolveLocation location,
                                     final ResolveOptions options,
                                     final ResolveTarget target,
                                     final List<Warp> managedWarps) {
        if (!commandContext.sessionManager().isCurrent(playerId, token)) {
            return;
        }
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player == null) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            return;
        }
        final TeleportRules rules = resolveTeleportRules(player, options, target.teleportRules(), managedWarps);
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () ->
                resolveAsync(context, playerId, token, location, options, target, rules));
    }

    private TeleportRules resolveTeleportRules(final Player player, final ResolveOptions options,
                                               final String tpRulesInput, final List<Warp> managedWarps) {
        final List<Warp> allowedWarps = WarpPermissionHelper.allowedWarps(managedWarps, player::hasPermission);
        final TeleportRules baseRules = new TeleportRules(false, false, false, allowedWarps);
        if (tpRulesInput != null) {
            return baseRules.parse(tpRulesInput);
        }
        return baseRules.parse(options.teleportRules());
    }

    private void resolveAsync(final CommandContext<CommandSourceStack> context,
                              final UUID playerId, final long token, final ResolveLocation location,
                              final ResolveOptions options, final ResolveTarget target,
                              final TeleportRules rules) {
        try {
            final ResolveResult result = target.mode() == ResolveTarget.Mode.GRAPH
                    ? resolveGraphTarget(location, options, target, rules)
                    : resolveNodeTargets(location, options, target, rules);
            commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(),
                    () -> finishResolve(context, playerId, token, options, result));
        } catch (final RuntimeException failure) {
            resolveFailed(context, playerId, token, failure);
        }
    }

    private void resolveFailed(final CommandContext<CommandSourceStack> context, final UUID playerId,
                               final long token, final RuntimeException failure) {
        commandContext.loggerFactory().create(BkResolveSubcommand.class)
                .error("Resolve command failed: " + failure.getMessage());
        commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () -> {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            context.getSource().getSender().sendMessage(
                    localization.getPrefixedMessage("commands.bk.resolve.error.failed"));
        });
    }

    private ResolveResult resolveGraphTarget(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target,
                                             final TeleportRules rules) {
        final Graph graph = commandContext.resolveService().resolveGraph(target.graphKey()).orElse(null);
        if (graph == null) {
            return ResolveResult.failure("commands.bk.resolve.error.graphNotFound",
                    Map.of(GRAPH_LITERAL, target.graphKey()));
        }
        final NodeRef start = commandContext.resolveService()
                .nearestNodeRef(commandContext.graphRepository().getAllGraphs(), location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return resolveGraphFromWarpFallback(location, options, graph.getGraphId(), rules)
                    .orElseGet(() -> ResolveResult.failure("commands.bk.resolve.error.noNearbyNode"));
        }
        if (start.graphDbId() == graph.getGraphId()) {
            return ResolveResult.completedResult();
        }
        final List<GraphNetwork> networks = commandContext.resolveService().loadGraphNetworks();
        final GraphNetwork network = commandContext.resolveService()
                .networkContaining(networks, List.of(start.graphDbId(), graph.getGraphId()))
                .orElse(null);
        if (network == null) {
            return ResolveResult.failure("commands.bk.resolve.error.noNetworkToGraph",
                    Map.of(GRAPH_LITERAL, graph.getName()));
        }
        final PathResult path = commandContext.resolveService().findPath(network, start, graph.getGraphId(), rules);
        if (path.nodes().isEmpty()) {
            return ResolveResult.failure("commands.bk.resolve.error.noPathToGraph",
                    Map.of(GRAPH_LITERAL, graph.getName()));
        }
        return ResolveResult.path(network, path, options.backend(), "commands.bk.resolve.status.showingPathToGraph",
                Map.of(GRAPH_LITERAL, graph.getName()));
    }

    private ResolveResult resolveNodeTargets(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target,
                                             final TeleportRules rules) {
        final ResolveService.NodeRefTargetResolution resolution = commandContext.resolveService()
                .resolveNodeRefTargets(target.nodeIds());
        if (!resolution.success()) {
            return ResolveResult.failure(resolution.errorKey(), resolution.replacements());
        }
        final NodeRef start = commandContext.resolveService()
                .nearestNodeRef(commandContext.graphRepository().getAllGraphs(), location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return resolveNodeTargetsFromWarpFallback(location, options, resolution.nodeRefs(), rules)
                    .orElseGet(() -> ResolveResult.failure("commands.bk.resolve.error.noNearbyNode"));
        }
        if (resolution.nodeRefs().contains(start)) {
            return ResolveResult.completedResult();
        }
        final Collection<Integer> requiredGraphIds = Stream.concat(
                        Stream.of(start.graphDbId()),
                        resolution.nodeRefs().stream().map(NodeRef::graphDbId))
                .collect(Collectors.toSet());
        final GraphNetwork network = requiredGraphIds.size() == 1
                ? commandContext.resolveService().singleGraphNetwork(
                commandContext.graphRepository().getGraphById(start.graphDbId()).orElseThrow())
                : commandContext.resolveService()
                .networkContaining(commandContext.resolveService().loadGraphNetworks(), requiredGraphIds)
                .orElse(null);
        if (network == null) {
            return ResolveResult.failure("commands.bk.resolve.error.noNetworkToNodeTarget");
        }
        final PathResult path = commandContext.resolveService()
                .findPath(network, start, resolution.nodeRefs(), rules);
        if (path.nodes().isEmpty()) {
            return ResolveResult.failure("commands.bk.resolve.error.noPathToNodeTarget");
        }
        return ResolveResult.path(network, path, options.backend(), "commands.bk.resolve.status.showingPathToNodeTarget");
    }

    private void finishResolve(final CommandContext<CommandSourceStack> context,
                               final UUID playerId, final long token, final ResolveOptions options,
                               final ResolveResult result) {
        if (!commandContext.sessionManager().isCurrent(playerId, token)) {
            return;
        }
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player == null) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            return;
        }
        if (!result.success()) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            sendKey(context, result.messageKey(), result.replacements());
            return;
        }
        if (result.completed()) {
            onGuidedPathCompleted(playerId, token, options);
            return;
        }
        final Visualizer visualizer = result.path() == null
                ? graphVisualizer(playerId, result)
                : pathVisualizer(playerId, token, options, result);
        if (!commandContext.sessionManager().activate(playerId, token)) {
            visualizer.shutdown();
            return;
        }
        commandContext.visualizerRegistry().replace(playerId, visualizer);
        sendKey(context, result.messageKey(), result.replacements());
    }

    private Visualizer graphVisualizer(final UUID playerId,
                                       final ResolveResult result) {
        if (result.backend() == ResolveBackend.BLOCK_DISPLAY) {
            return GraphVisualizerFactory.blockDisplayGraph(commandContext.plugin(), commandContext.loggerFactory(),
                    result.graph(), playerId);
        }
        return GraphVisualizerFactory.particleGraph(commandContext.plugin(), commandContext.loggerFactory(),
                result.graph(), playerId, commandContext.effectExecutor());
    }

    private Visualizer pathVisualizer(final UUID playerId,
                                      final long token,
                                      final ResolveOptions options,
                                      final ResolveResult result) {
        final GraphNetworkDesignProfile profile = GraphNetworkDesignProfile.defaults();
        final GuidedPathOptions guidedPathOptions = resolveGuidedPathOptions(options);
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new GraphNetworkVisualSource(result.network()),
                result.path(),
                viewerLocationSource(playerId),
                guidedPathOptions,
                options.goalMarkerEnabled()
        );
        final Runnable completionCallback = () -> onGuidedPathCompleted(playerId, token, options);
        final ResolveAutoTeleportController autoTeleportController = autoTeleportController(playerId, token, result,
                source, options.autoTeleportOptions());
        final ResolveAwayCancellationController awayCancellationController = awayCancellationController(playerId, token,
                source, options.awayCancellationOptions());
        if (result.backend() == ResolveBackend.BLOCK_DISPLAY) {
            return new GuidedPathAutoTeleportVisualizer(commandContext.loggerFactory(), source,
                    new BlockDisplayGraphRenderer(commandContext.plugin(), playerId),
                    new ProfileGraphDesignResolver(profile), completionCallback, autoTeleportController,
                    awayCancellationController);
        }
        return new GuidedPathAutoTeleportVisualizer(commandContext.loggerFactory(), source,
                new ParticleGraphRenderer(commandContext.plugin(), playerId, commandContext.effectExecutor()),
                new ProfileGraphDesignResolver(profile), completionCallback, autoTeleportController,
                awayCancellationController);
    }

    private ResolveAutoTeleportController autoTeleportController(final UUID playerId, final long token,
                                                                 final ResolveResult result,
                                                                 final GuidedPathVisualGraphSource source,
                                                                 final ResolveAutoTeleportOptions options) {
        return new ResolveAutoTeleportController(result.network(), result.path(), source, options,
                () -> commandContext.plugin().getServer().getPlayer(playerId),
                (delayTicks, action) -> {
                    final org.bukkit.scheduler.BukkitTask task = commandContext.plugin().getServer().getScheduler()
                            .runTaskLater(commandContext.plugin(), action, delayTicks);
                    return task::cancel;
                },
                () -> System.currentTimeMillis() / MILLIS_PER_TICK,
                () -> commandContext.sessionManager().isCurrent(playerId, token),
                localization);
    }

    private ResolveAwayCancellationController awayCancellationController(final UUID playerId, final long token,
                                                                         final GuidedPathVisualGraphSource source,
                                                                         final ResolveAwayCancellationOptions options) {
        return new ResolveAwayCancellationController(source, options,
                () -> commandContext.plugin().getServer().getPlayer(playerId),
                () -> System.currentTimeMillis() / MILLIS_PER_TICK,
                () -> commandContext.sessionManager().isCurrent(playerId, token),
                () -> commandContext.sessionManager().clearIfCurrent(playerId, token),
                localization);
    }

    private GuidedPathOptions resolveGuidedPathOptions(final ResolveOptions options) {
        final ConfigurationSection section = commandContext.plugin().getConfig()
                .getConfigurationSection(GUIDED_PATH_CONFIG);
        final GuidedPathOptions configured = GuidedPathOptions.fromConfig(section);
        return new GuidedPathOptions(configured.windowSize(), options.finishRadius(), configured.lookBehind());
    }

    private ViewerLocationSource viewerLocationSource(final UUID playerId) {
        return () -> {
            final Player player = commandContext.plugin().getServer().getPlayer(playerId);
            return player == null ? null : player.getLocation();
        };
    }

    private void onGuidedPathCompleted(final UUID playerId, final long token, final ResolveOptions options) {
        if (!commandContext.sessionManager().markCompleted(playerId, token)) {
            return;
        }
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player != null) {
            goalNotifier.notify(player, options.goalOptions());
        }
        final long cleanupDelayTicks = options.finishCleanupDelayTicks();
        if (cleanupDelayTicks <= NO_CLEANUP_DELAY_TICKS) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            return;
        }
        commandContext.plugin().getServer().getScheduler().runTaskLater(commandContext.plugin(),
                () -> commandContext.sessionManager().clearIfCurrent(playerId, token), cleanupDelayTicks);
    }

    private Optional<ResolveResult> resolveGraphFromWarpFallback(final ResolveLocation location,
                                                                 final ResolveOptions options,
                                                                 final int targetGraphId,
                                                                 final TeleportRules rules) {
        final ResolveWarpStartSelector selector = new ResolveWarpStartSelector(commandContext.graphRepository(),
                commandContext.resolveService());
        final Optional<ResolveWarpStartSelector.Candidate> candidate = selector.selectGraph(options, location, targetGraphId,
                rules);
        return candidate.map(warp -> ResolveResult.path(warp.network(), warp.path(), options.backend(),
                "commands.bk.resolve.status.showingPathToGraph",
                Map.of(GRAPH_LITERAL, commandContext.graphRepository().getGraphById(targetGraphId)
                        .map(Graph::getName).orElse(Integer.toString(targetGraphId)))));
    }

    private Optional<ResolveResult> resolveNodeTargetsFromWarpFallback(final ResolveLocation location,
                                                                       final ResolveOptions options,
                                                                       final Collection<NodeRef> goals,
                                                                       final TeleportRules rules) {
        final ResolveWarpStartSelector selector = new ResolveWarpStartSelector(commandContext.graphRepository(),
                commandContext.resolveService());
        final Optional<ResolveWarpStartSelector.Candidate> candidate = selector.selectNodes(options, location, goals, rules);
        return candidate.map(warp -> ResolveResult.path(warp.network(), warp.path(), options.backend(),
                "commands.bk.resolve.status.showingPathToNodeTarget", Map.of()));
    }

    private int cancelOwnGuidance(final CommandContext<CommandSourceStack> context) {
        final CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof final Player player)) {
            sendKey(context, "commands.bk.resolve.error.consoleMustSpecifyPlayer");
            return 0;
        }
        return cancelGuidanceForPlayer(context, player);
    }

    private int cancelGuidance(final CommandContext<CommandSourceStack> context) {
        final Player player = resolveTargetPlayer(context);
        if (player == null) {
            return 0;
        }
        return cancelGuidanceForPlayer(context, player);
    }

    private int cancelGuidanceForPlayer(final CommandContext<CommandSourceStack> context, final Player player) {
        final boolean cancelled = commandContext.sessionManager().cancel(player.getUniqueId());
        if (cancelled) {
            sendKey(context, "commands.bk.resolve.status.cancelledForPlayer", Map.of(PLAYER_LITERAL, player.getName()));
        } else {
            sendKey(context, "commands.bk.resolve.error.noActiveGuidanceForPlayer", Map.of(PLAYER_LITERAL, player.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private Player resolveTargetPlayer(final CommandContext<CommandSourceStack> context) {
        final String playerName = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        final Player player = commandContext.plugin().getServer().getPlayerExact(playerName);
        if (player == null) {
            sendKey(context, "commands.bk.resolve.error.playerNotOnline", Map.of("player", playerName));
        }
        return player;
    }

    private void sendKey(final CommandContext<CommandSourceStack> context, final String key) {
        context.getSource().getSender().sendMessage(localization.getPrefixedMessage(key));
    }

    private void sendKey(final CommandContext<CommandSourceStack> context,
                         final String key,
                         final Map<String, String> replacements) {
        context.getSource().getSender().sendMessage(localization.getPrefixedMessage(key, replacements));
    }

    /**
     * Internal resolution result.
     */
    private record ResolveResult(Graph graph, GraphNetwork network, PathResult path,
                                 ResolveBackend backend,
                                 boolean completed,
                                 String messageKey,
                                 Map<String, String> replacements) {

        /**
         * Creates a successful graph resolution.
         *
         * @param graph   resolved graph
         * @param backend visual backend
         * @return result
         */
        /* default */
        static ResolveResult graph(final Graph graph, final ResolveBackend backend) {
            return new ResolveResult(graph, null, null, backend, false,
                    "commands.bk.resolve.status.showingGraph", Map.of(GRAPH_LITERAL, graph.getName()));
        }

        /**
         * Creates a successful path resolution.
         *
         * @param network    graph network
         * @param path       path result
         * @param backend    visual backend
         * @param messageKey status message key
         * @return result
         */
        /* default */
        static ResolveResult path(final GraphNetwork network, final PathResult path, final ResolveBackend backend,
                                  final String messageKey) {
            return path(network, path, backend, messageKey, Map.of());
        }

        /**
         * Creates a successful path resolution with replacements.
         *
         * @param network      graph network
         * @param path         path result
         * @param backend      visual backend
         * @param messageKey   status message key
         * @param replacements message replacements
         * @return result
         */
        /* default */
        static ResolveResult path(final GraphNetwork network, final PathResult path, final ResolveBackend backend,
                                  final String messageKey, final Map<String, String> replacements) {
            return new ResolveResult(null, network, path, backend, false, messageKey,
                    replacements == null ? Map.of() : Map.copyOf(replacements));
        }

        /**
         * Creates a failed resolution.
         *
         * @param messageKey error message key
         * @return result
         */
        /* default */
        static ResolveResult failure(final String messageKey) {
            return new ResolveResult(null, null, null, null, false, messageKey, Map.of());
        }

        /**
         * Creates a failed resolution with replacements.
         *
         * @param messageKey   error message key
         * @param replacements message replacements
         * @return result
         */
        /* default */
        static ResolveResult failure(final String messageKey, final Map<String, String> replacements) {
            return new ResolveResult(null, null, null, null, false, messageKey,
                    replacements == null ? Map.of() : Map.copyOf(replacements));
        }

        /**
         * Creates a completed resolution (already at destination).
         *
         * @return result
         */
        /* default */
        static ResolveResult completedResult() {
            return new ResolveResult(null, null, null, null, true,
                    "commands.bk.resolve.status.guidanceComplete", Map.of());
        }

        /**
         * Checks if resolution succeeded.
         *
         * @return true when resolved
         */
        /* default */ boolean success() {
            return graph != null || network != null || completed;
        }
    }
}
