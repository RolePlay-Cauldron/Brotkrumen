package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor workspace view subcommands.
 */
public final class EditorViewSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private EditorViewSubcommands() {
    }

    /**
     * Builds the view subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the view subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> view(final EditorCommandContext commandContext) {
        return Commands.literal("view")
                .then(Commands.literal("add").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> {
                            commandContext.editorService().addReferenceGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, GRAPH_ARGUMENT),
                                    result -> EditorCommandFeedback.send(commandContext, player, result));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("remove").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> {
                            commandContext.editorService().removeReferenceGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, GRAPH_ARGUMENT),
                                    result -> EditorCommandFeedback.send(commandContext, player, result));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("clear").executes(context -> withPlayer(commandContext, context,
                        player -> {
                            commandContext.editorService().clearReferenceGraphsAsync(player.getUniqueId(),
                                    result -> EditorCommandFeedback.send(commandContext, player, result));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> graphArgument(
            final EditorCommandContext commandContext) {
        return Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> commandContext.suggestGraphNames(builder));
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? EditorCommandFeedback.playerOnly(commandContext, context) : action.run(player);
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


