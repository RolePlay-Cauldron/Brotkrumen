package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor placement control subcommands.
 */
public final class EditorPlacementSubcommands {

    private EditorPlacementSubcommands() {
    }

    /**
     * Builds the preview subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the preview subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> preview(final EditorCommandContext commandContext) {
        return Commands.literal("preview")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().preview(player.getUniqueId()))));
    }

    /**
     * Builds the place subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the place subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> place(final EditorCommandContext commandContext) {
        return Commands.literal("place")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().placeNode(player.getUniqueId(), player.getLocation()))));
    }

    /**
     * Builds the continue subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the continue subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> continuePlacement(
            final EditorCommandContext commandContext) {
        return Commands.literal("continue")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().continuePlacement(player.getUniqueId()))));
    }

    /**
     * Builds the undo subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the undo subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> undo(final EditorCommandContext commandContext) {
        return Commands.literal("undo")
                .executes(context -> undo(commandContext, context, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> undo(commandContext, context,
                                IntegerArgumentType.getInteger(context, "amount"))));
    }

    private static int undo(final EditorCommandContext commandContext,
                            final CommandContext<CommandSourceStack> context,
                            final int amount) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().undo(player.getUniqueId(), amount)));
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? 0 : action.run(player);
    }

    /**
     * Functional interface for actions that require a player.
     */
    @FunctionalInterface
    private interface PlayerAction {
        /**
         * Executes the action for the given player.
         *
         * @param player The player.
         * @return The result of the action.
         */
        int run(Player player);
    }
}
