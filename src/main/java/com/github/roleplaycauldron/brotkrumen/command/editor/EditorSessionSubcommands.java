package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor lifecycle subcommands.
 */
public final class EditorSessionSubcommands {

    private final EditorCommandContext commandContext;

    /**
     * Creates lifecycle subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorSessionSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the create subcommand.
     *
     * @return create subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::createGraph));
    }

    /**
     * Builds the edit subcommand.
     *
     * @return edit subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> edit() {
        return Commands.literal("edit")
                .then(Commands.argument("graphName", StringArgumentType.word())
                        .suggests((context, builder) -> commandContext.suggestGraphNames(builder))
                        .executes(this::editGraph));
    }

    /**
     * Builds the rename subcommand.
     *
     * @return rename subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> rename() {
        return Commands.literal("rename")
                .then(Commands.argument("newName", StringArgumentType.word())
                        .executes(context -> commandContext.withPlayer(context, player -> {
                            commandContext.editorService().renameActiveGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, "newName"),
                                    result -> commandContext.getFeedback().send(player, result));
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    /**
     * Builds the finish subcommand.
     *
     * @return finish subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> finish() {
        return Commands.literal("finish")
                .executes(context -> commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                        commandContext.editorService().finishRouteCreation(player.getUniqueId()))));
    }

    /**
     * Builds the cancel subcommand.
     *
     * @return cancel subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> cancel() {
        return Commands.literal("cancel")
                .executes(context -> commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                        commandContext.editorService().cancel(player.getUniqueId()))));
    }

    private int createGraph(final CommandContext<CommandSourceStack> context) {
        return commandContext.withPlayer(context, player -> {
            commandContext.editorService().startGraphCreationAsync(player.getUniqueId(),
                    StringArgumentType.getString(context, "name"), commandContext.defaultSettings(),
                    result -> commandContext.getFeedback().send(player, result));
            return Command.SINGLE_SUCCESS;
        });
    }

    private int editGraph(final CommandContext<CommandSourceStack> context) {
        return commandContext.withPlayer(context, player -> {
            commandContext.editorService().startGraphEditAsync(player.getUniqueId(),
                    StringArgumentType.getString(context, "graphName"), commandContext.defaultSettings(),
                    result -> commandContext.getFeedback().send(player, result));
            return Command.SINGLE_SUCCESS;
        });
    }
}
