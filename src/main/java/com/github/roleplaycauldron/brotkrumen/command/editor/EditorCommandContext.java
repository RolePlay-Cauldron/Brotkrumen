package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared dependencies and helpers for editor subcommands.
 */
public final class EditorCommandContext {

    private static final String DEFAULT_NODE_DISTANCE_CONFIG = "editor.defaultNodeDistance";

    private static final String DEFAULT_PLACEMENT_MODE_CONFIG = "editor.defaultPlacementMode";

    private static final String CONTINUE_REQUIRES_NODE_CONFIG = "editor.continueRequiresNode";

    private static final String DEFAULT_PRESET_CONFIG = "editor.defaultPreset";

    private static final int FALLBACK_NODE_DISTANCE = 10;

    private final JavaPlugin javaPlugin;

    /**
     * The service for editor operations.
     */
    private final EditorService editorOperations;

    /**
     * The service for graph data.
     */
    private final GraphService graphs;

    /**
     * Initializes the editor command context.
     *
     * @param plugin        The JavaPlugin instance.
     * @param editorService The EditorService for editor operations.
     * @param graphService  The GraphService for graph data.
     */
    public EditorCommandContext(final JavaPlugin plugin, final EditorService editorService,
                                final GraphService graphService) {
        this.javaPlugin = plugin;
        this.editorOperations = editorService;
        this.graphs = graphService;
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
     * Retrieves the GraphService instance.
     *
     * @return The GraphService instance.
     */
    public GraphService graphService() {
        return graphs;
    }

    /**
     * Returns the plugin instance used by this command context.
     *
     * @return plugin instance
     */
    public JavaPlugin plugin() {
        return javaPlugin;
    }

    /**
     * Retrieves the default editor settings from the plugin configuration.
     *
     * @return The default EditorSettings.
     */
    public EditorService.EditorSettings defaultSettings() {
        final int nodeDistance = Math.max(1, javaPlugin.getConfig().getInt(DEFAULT_NODE_DISTANCE_CONFIG,
                FALLBACK_NODE_DISTANCE));
        final EditorService.PlacementMode placementMode = EditorService.PlacementMode.parse(
                        javaPlugin.getConfig().getString(DEFAULT_PLACEMENT_MODE_CONFIG, "auto"))
                .orElse(EditorService.PlacementMode.AUTO);
        final boolean continueRequiresNode = javaPlugin.getConfig().getBoolean(CONTINUE_REQUIRES_NODE_CONFIG, true);
        final String preset = javaPlugin.getConfig().getString(DEFAULT_PRESET_CONFIG, "default");
        return new EditorService.EditorSettings(nodeDistance, placementMode, continueRequiresNode, preset)
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
