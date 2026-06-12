package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds selected graph element subcommands.
 */
public final class EditorSelectionSubcommands {

    private final EditorCommandContext commandContext;

    /**
     * Creates selection subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorSelectionSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the select subcommand.
     *
     * @return select subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> select() {
        return Commands.literal("select")
                .then(Commands.literal("node").executes(context -> commandContext.withPlayer(context, player ->
                        commandContext.getFeedback().send(player, commandContext.editorService().selectNearbyNode(
                                player.getUniqueId(), player.getLocation())))))
                .then(Commands.literal("edge").executes(context -> commandContext.withPlayer(context, player ->
                        commandContext.getFeedback().send(player, commandContext.editorService().selectNearbyEdge(
                                player.getUniqueId(), player.getLocation())))));
    }

    /**
     * Builds the selection management subcommand.
     *
     * @return selection subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> selection() {
        return Commands.literal("selection")
                .then(Commands.literal("show").executes(context -> commandContext.withPlayer(context, player ->
                        commandContext.getFeedback().send(player,
                                commandContext.editorService().showSelection(player.getUniqueId())))))
                .then(Commands.literal("clear").executes(context -> commandContext.withPlayer(context, player ->
                        commandContext.getFeedback().send(player,
                                commandContext.editorService().clearSelection(player.getUniqueId())))))
                .then(Commands.literal("connections").executes(context -> commandContext.withPlayer(context, player ->
                        commandContext.getFeedback().send(player,
                                commandContext.editorService().selectedNodeConnections(player.getUniqueId())))))
                .then(Commands.literal("teleport").executes(context -> commandContext.withPlayer(context,
                        this::teleport)));
    }

    private int teleport(final Player player) {
        final EditorService.SelectionTeleportResult result = commandContext.editorService().teleportToSelection(
                player.getUniqueId(), player.getLocation());
        if (result.destination() != null) {
            player.teleport(result.destination());
        }
        return commandContext.getFeedback().send(player, result.result());
    }
}
