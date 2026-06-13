package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared dependencies and helpers for editor subcommands.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class EditorCommandContext {

    private static final String DEFAULT_NODE_DISTANCE_CONFIG = "editor.defaultNodeDistance";

    private static final String DEFAULT_PLACEMENT_MODE_CONFIG = "editor.defaultPlacementMode";

    private static final String DEFAULT_EDIT_PLACEMENT_MODE_CONFIG = "editor.defaultEditPlacementMode";

    private static final String PLACE_NODES_ON_GROUND_CONFIG = "editor.placeNodesOnGround";

    private static final String CONTINUE_REQUIRES_NODE_CONFIG = "editor.continueRequiresNode";

    private static final String DEFAULT_PRESET_CONFIG = "editor.defaultPreset";

    private static final int FALLBACK_NODE_DISTANCE = 10;

    private final JavaPlugin plugin;

    private final WrappedLogger log;

    private final Localization localization;

    private final EditorService editorOperations;

    private final EditorCommandFeedback feedback;

    /**
     * The service for graph data.
     */
    private final GraphRepository graphs;

    private final BukkitScheduler scheduler;

    /**
     * Initializes the editor command context.
     *
     * @param plugin           The BukkitScheduler instance.
     * @param loggerFactory    The LoggerFactory instance.
     * @param editorOperations The EditorService for editor operations.
     * @param graphRepository  The graph repository for graph data.
     * @param localization     The localization service.
     */
    public EditorCommandContext(final JavaPlugin plugin, final LoggerFactory loggerFactory,
                                final EditorService editorOperations, final GraphRepository graphRepository,
                                final Localization localization) {
        this.plugin = plugin;
        this.log = loggerFactory.create(EditorCommandContext.class);
        this.localization = localization;
        this.editorOperations = editorOperations;
        this.feedback = new EditorCommandFeedback(localization);
        this.graphs = graphRepository;
        this.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Retrieves the EditorService instance.
     *
     * @return The EditorService instance.
     */
    public EditorService editorService() {
        return editorOperations;
    }

    /**
     * Retrieves feedback rendering helpers.
     *
     * @return feedback renderer
     */
    public EditorCommandFeedback getFeedback() {
        return feedback;
    }

    /**
     * Retrieves The graph repository instance.
     *
     * @return The graph repository instance.
     */
    public GraphRepository graphRepository() {
        return graphs;
    }

    /**
     * Suggests persisted graph names asynchronously.
     *
     * @param builder suggestions builder
     * @return suggestions future
     */
    public CompletableFuture<Suggestions> suggestGraphNames(final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();
        final CompletableFuture<Suggestions> suggestions = new CompletableFuture<>();
        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                graphs.getAllGraphs().stream()
                        .map(Graph::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
                        .forEach(builder::suggest);
                suggestions.complete(builder.build());
            } catch (final RuntimeException failure) {
                log.warnF("Graph suggestions failed: %s", failure.getMessage());
                suggestions.complete(builder.build());
            }
        });
        return suggestions;
    }

    /**
     * Runs a database-backed editor operation asynchronously and sends feedback on the main thread.
     *
     * @param player    command player
     * @param operation editor operation
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void runEditorOperationAsync(final Player player, final Supplier<EditorService.EditorResult> operation) {
        scheduler.runTaskAsynchronously(plugin, () -> {
            final EditorService.EditorResult result;
            try {
                result = operation.get();
            } catch (final RuntimeException failure) {
                log.warnF("Editor command failed: %s", failure.getMessage());
                scheduler.runTask(plugin, () -> player.sendMessage(
                        localization.getPrefixedMessage("commands.bkeditor.error.commandFailed")));
                return;
            }
            scheduler.runTask(plugin, () -> feedback.send(player, result));
        });
    }

    /**
     * Runs an action that requires a player sender.
     *
     * @param context command context
     * @param action  player action
     * @return command result
     */
    public int withPlayer(final CommandContext<CommandSourceStack> context, final Function<Player, Integer> action) {
        final Player player = player(context);
        return player == null ? feedback.playerOnly(context) : action.apply(player);
    }

    /**
     * Retrieves the default editor settings from the plugin configuration.
     *
     * @return The default EditorSettings.
     */
    public EditorService.EditorSettings defaultSettings() {
        final int nodeDistance = Math.max(1, plugin.getConfig().getInt(DEFAULT_NODE_DISTANCE_CONFIG,
                FALLBACK_NODE_DISTANCE));
        final EditorService.PlacementMode placementMode = EditorService.PlacementMode.parse(
                        plugin.getConfig().getString(DEFAULT_PLACEMENT_MODE_CONFIG, "auto"))
                .orElse(EditorService.PlacementMode.AUTO);
        final EditorService.PlacementMode editPlacementMode = EditorService.PlacementMode.parse(
                        plugin.getConfig().getString(DEFAULT_EDIT_PLACEMENT_MODE_CONFIG, "preview"))
                .orElse(EditorService.PlacementMode.PREVIEW);
        final boolean placeNodesOnGround = plugin.getConfig().getBoolean(PLACE_NODES_ON_GROUND_CONFIG, false);
        final boolean continueRequiresNode = plugin.getConfig().getBoolean(CONTINUE_REQUIRES_NODE_CONFIG, true);
        final String preset = plugin.getConfig().getString(DEFAULT_PRESET_CONFIG, "default");
        return new EditorService.EditorSettings(nodeDistance, placementMode, editPlacementMode, placeNodesOnGround,
                continueRequiresNode, preset)
                .normalized();
    }

    /**
     * Retrieves the Player instance from the command context.
     *
     * @param context The command context.
     * @return The Player instance, or null if the sender is not a player.
     */
    public Player player(final CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof final Player player) {
            return player;
        }
        return null;
    }
}
