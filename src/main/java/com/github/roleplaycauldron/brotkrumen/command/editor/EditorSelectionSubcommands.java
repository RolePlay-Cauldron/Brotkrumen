package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds selected graph element subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorSelectionSubcommands {

    private EditorSelectionSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> select(final EditorCommandContext commandContext) {
        return Commands.literal("select")
                .then(Commands.literal("node").executes(context -> withPlayer(commandContext, context, player ->
                        commandContext.send(player, commandContext.editorService().selectNearbyNode(
                                player.getUniqueId(), player.getLocation())))))
                .then(Commands.literal("edge").executes(context -> withPlayer(commandContext, context, player ->
                        commandContext.send(player, commandContext.editorService().selectNearbyEdge(
                                player.getUniqueId(), player.getLocation())))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> selection(final EditorCommandContext commandContext) {
        return Commands.literal("selection")
                .then(Commands.literal("show").executes(context -> withPlayer(commandContext, context, player ->
                        commandContext.send(player, commandContext.editorService().showSelection(player.getUniqueId())))))
                .then(Commands.literal("clear").executes(context -> withPlayer(commandContext, context, player ->
                        commandContext.send(player, commandContext.editorService().clearSelection(player.getUniqueId())))))
                .then(Commands.literal("teleport").executes(context -> withPlayer(commandContext, context,
                        player -> teleport(commandContext, player))));
    }

    private static int teleport(final EditorCommandContext commandContext, final Player player) {
        final EditorService.SelectionTeleportResult result = commandContext.editorService().teleportToSelection(
                player.getUniqueId(), player.getLocation());
        if (result.destination() != null) {
            player.teleport(result.destination());
        }
        return commandContext.send(player, result.result());
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? 0 : action.run(player);
    }

    @FunctionalInterface
    private interface PlayerAction {
        int run(Player player);
    }
}
