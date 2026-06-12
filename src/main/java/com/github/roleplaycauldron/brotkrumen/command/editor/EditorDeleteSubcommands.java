package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor deletion subcommands.
 */
public final class EditorDeleteSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private final EditorCommandContext commandContext;

    /**
     * Creates deletion subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorDeleteSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the delete subcommand.
     *
     * @return delete subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> delete() {
        return Commands.literal("delete")
                .then(Commands.literal("node").executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.editorService().deleteSelectedNodeAsync(player.getUniqueId(),
                            result -> commandContext.getFeedback().send(player, result));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("graph")
                        .then(Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> commandContext.suggestGraphNames(builder))
                                .executes(context -> commandContext.withPlayer(context, player -> {
                                    commandContext.runEditorOperationAsync(player, () ->
                                            commandContext.editorService().deletePersistedGraph(
                                                    StringArgumentType.getString(context, GRAPH_ARGUMENT)));
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
}
