package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor workspace view subcommands.
 */
public final class EditorViewSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private final EditorCommandContext commandContext;

    /**
     * Creates view subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorViewSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the view subcommand.
     *
     * @return view subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> view() {
        return Commands.literal("view")
                .then(Commands.literal("add").then(graphArgument()
                        .executes(context -> commandContext.withPlayer(context, player -> {
                            commandContext.editorService().addReferenceGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, GRAPH_ARGUMENT),
                                    result -> commandContext.getFeedback().send(player, result));
                            return Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("remove").then(graphArgument()
                        .executes(context -> commandContext.withPlayer(context, player -> {
                            commandContext.editorService().removeReferenceGraphAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, GRAPH_ARGUMENT),
                                    result -> commandContext.getFeedback().send(player, result));
                            return Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("clear").executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.editorService().clearReferenceGraphsAsync(player.getUniqueId(),
                            result -> commandContext.getFeedback().send(player, result));
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> graphArgument() {
        return Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> commandContext.suggestGraphNames(builder));
    }
}
