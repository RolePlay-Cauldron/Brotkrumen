package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Brigadier command registration for graph editor operations.
 */
public class EditorCommand {

    /**
     * Context containing dependencies and helpers for the editor command.
     */
    private final EditorCommandContext commandContext;

    /**
     * Initializes the editor command and registers it with the plugin lifecycle.
     *
     * @param plugin        The JavaPlugin instance.
     * @param editorService The EditorService for managing editor sessions.
     * @param graphService  The GraphService for graph data operations.
     */
    public EditorCommand(final JavaPlugin plugin, final EditorService editorService, final GraphService graphService) {
        this.commandContext = new EditorCommandContext(plugin, editorService, graphService);

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar()
                .register(commandTree(), "Graph editor commands"));
    }

    private LiteralCommandNode<CommandSourceStack> commandTree() {
        return Commands.literal("bkeditor")
                .then(EditorSessionSubcommands.create(commandContext))
                .then(EditorSessionSubcommands.edit(commandContext))
                .then(EditorSessionSubcommands.rename(commandContext))
                .then(EditorSessionSubcommands.finish(commandContext))
                .then(EditorSessionSubcommands.cancel(commandContext))
                .then(EditorPlacementSubcommands.preview(commandContext))
                .then(EditorPlacementSubcommands.place(commandContext))
                .then(EditorPlacementSubcommands.continuePlacement(commandContext))
                .then(EditorPlacementSubcommands.undo(commandContext))
                .then(EditorSettingsSubcommands.settings(commandContext))
                .then(EditorViewSubcommands.view(commandContext))
                .then(EditorSelectionSubcommands.select(commandContext))
                .then(EditorSelectionSubcommands.selection(commandContext))
                .then(EditorEdgeSubcommands.edge(commandContext))
                .then(EditorDeleteSubcommands.delete(commandContext))
                .then(EditorWarpSubcommands.warp(commandContext))
                .build();
    }
}
