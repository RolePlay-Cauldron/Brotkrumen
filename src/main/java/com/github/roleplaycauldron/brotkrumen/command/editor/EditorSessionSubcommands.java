package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor lifecycle subcommands.
 */
public final class EditorSessionSubcommands {

    private EditorSessionSubcommands() {
    }

    /**
     * Builds the create subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the create subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create(final EditorCommandContext commandContext) {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> createGraph(commandContext, context)));
    }

    /**
     * Builds the edit subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the edit subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> edit(final EditorCommandContext commandContext) {
        return Commands.literal("edit")
                .then(Commands.argument("graphName", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            return commandContext.suggestGraphNames(builder);
                        })
                        .executes(context -> editGraph(commandContext, context)));
    }

    /**
     * Builds the rename subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the rename subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> rename(final EditorCommandContext commandContext) {
        return Commands.literal("rename")
                .then(Commands.argument("newName", StringArgumentType.word())
                        .executes(context -> withPlayer(commandContext, context, player -> {
                            commandContext.editorService().renameActiveGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, "newName"),
                                    result -> EditorCommandFeedback.send(commandContext, player, result));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })));
    }

    /**
     * Builds the finish subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the finish subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> finish(final EditorCommandContext commandContext) {
        return Commands.literal("finish")
                .executes(context -> withPlayer(commandContext, context, player -> EditorCommandFeedback.send(commandContext, player,
                        commandContext.editorService().finishRouteCreation(player.getUniqueId()))));
    }

    /**
     * Builds the cancel subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the cancel subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> cancel(final EditorCommandContext commandContext) {
        return Commands.literal("cancel")
                .executes(context -> withPlayer(commandContext, context, player -> EditorCommandFeedback.send(commandContext, player,
                        commandContext.editorService().cancel(player.getUniqueId()))));
    }

    private static int createGraph(final EditorCommandContext commandContext,
                                   final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> {
            commandContext.editorService().startGraphCreationAsync(player.getUniqueId(),
                    StringArgumentType.getString(context, "name"), commandContext.defaultSettings(),
                    result -> EditorCommandFeedback.send(commandContext, player, result));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    private static int editGraph(final EditorCommandContext commandContext,
                                 final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> {
            commandContext.editorService().startGraphEditAsync(player.getUniqueId(),
                    StringArgumentType.getString(context, "graphName"), commandContext.defaultSettings(),
                    result -> EditorCommandFeedback.send(commandContext, player, result));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
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
