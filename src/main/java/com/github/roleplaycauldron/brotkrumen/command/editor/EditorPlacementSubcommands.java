package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor placement control subcommands.
 */
public final class EditorPlacementSubcommands {

    private final EditorCommandContext commandContext;

    /**
     * Creates placement subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorPlacementSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the preview subcommand.
     *
     * @return preview subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> preview() {
        return Commands.literal("preview")
                .executes(context -> commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                        commandContext.editorService().preview(player.getUniqueId()))));
    }

    /**
     * Builds the place subcommand.
     *
     * @return place subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> place() {
        return Commands.literal("place")
                .executes(context -> commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                        commandContext.editorService().placeNode(player.getUniqueId(), player.getLocation()))));
    }

    /**
     * Builds the continue subcommand.
     *
     * @return continue subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> continuePlacement() {
        return Commands.literal("continue")
                .executes(context -> commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                        commandContext.editorService().continuePlacement(player.getUniqueId()))));
    }

    /**
     * Builds the undo subcommand.
     *
     * @return undo subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> undo() {
        return Commands.literal("undo")
                .executes(context -> undo(context, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> undo(context, IntegerArgumentType.getInteger(context, "amount"))));
    }

    private int undo(final CommandContext<CommandSourceStack> context, final int amount) {
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().undo(player.getUniqueId(), amount)));
    }
}
