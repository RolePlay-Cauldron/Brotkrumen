package com.github.roleplaycauldron.brotkrumen.editor.command;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared dependencies and helpers for editor subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorCommandContext {

    private static final String DEFAULT_NODE_DISTANCE_CONFIG = "editor.defaultNodeDistance";

    private static final String DEFAULT_PLACEMENT_MODE_CONFIG = "editor.defaultPlacementMode";

    private static final String CONTINUE_REQUIRES_NODE_CONFIG = "editor.continueRequiresNode";

    private static final String DEFAULT_PRESET_CONFIG = "editor.defaultPreset";

    private static final int FALLBACK_NODE_DISTANCE = 10;

    private final JavaPlugin plugin;

    private final EditorService editorOperations;

    private final GraphService graphs;

    public EditorCommandContext(final JavaPlugin plugin, final EditorService editorService,
                                final GraphService graphService) {
        this.plugin = plugin;
        this.editorOperations = editorService;
        this.graphs = graphService;
    }

    public EditorService editorService() {
        return editorOperations;
    }

    public GraphService graphService() {
        return graphs;
    }

    public EditorService.EditorSettings defaultSettings() {
        final int nodeDistance = Math.max(1, plugin.getConfig().getInt(DEFAULT_NODE_DISTANCE_CONFIG,
                FALLBACK_NODE_DISTANCE));
        final EditorService.PlacementMode placementMode = EditorService.PlacementMode.parse(
                        plugin.getConfig().getString(DEFAULT_PLACEMENT_MODE_CONFIG, "auto"))
                .orElse(EditorService.PlacementMode.AUTO);
        final boolean continueRequiresNode = plugin.getConfig().getBoolean(CONTINUE_REQUIRES_NODE_CONFIG, true);
        final String preset = plugin.getConfig().getString(DEFAULT_PRESET_CONFIG, "default");
        return new EditorService.EditorSettings(nodeDistance, placementMode, continueRequiresNode, preset)
                .normalized();
    }

    public Player player(final CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof final Player player) {
            return player;
        }
        context.getSource().getSender().sendMessage("Only players can use the graph editor.");
        return null;
    }

    public int send(final Player player, final EditorService.EditorResult result) {
        if (result.message() != null && !result.message().isBlank()) {
            player.sendMessage(result.message());
        }
        if (result.actionBarMessage() != null && !result.actionBarMessage().isBlank()) {
            player.sendActionBar(Component.text(result.actionBarMessage()));
        }
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }
}
