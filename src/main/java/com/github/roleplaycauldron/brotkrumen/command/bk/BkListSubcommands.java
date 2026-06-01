package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * `/bk list` command.
 */
public final class BkListSubcommands {

    private final BkCommandContext commandContext;

    /**
     * Initializes a new instance of the BkListSubcommands class.
     *
     * @param commandContext the command context providing access to services and utilities
     *                       required to execute the subcommands.
     */
    public BkListSubcommands(final BkCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the list subcommand.
     *
     * @return subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> list() {
        return Commands.literal("list")
                .then(Commands.literal("graph").executes(this::listGraphs))
                .then(Commands.literal("network").executes(this::listNetworks));
    }

    private int listGraphs(final CommandContext<CommandSourceStack> context) {
        commandContext.send(context, BkOutputFormatter.graphs(commandContext.graphService().getAllGraphs()));
        return Command.SINGLE_SUCCESS;
    }

    private int listNetworks(final CommandContext<CommandSourceStack> context) {
        commandContext.send(context, BkOutputFormatter.networks(commandContext.graphNetworkService().loadGraphNetworks()));
        return Command.SINGLE_SUCCESS;
    }
}
