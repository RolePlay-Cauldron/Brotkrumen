package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Map;

/**
 * `/bk list` command.
 */
public final class BkListSubcommands {

    private final BkCommandContext commandContext;

    private final Localization localization;

    /**
     * Initializes a new instance of the BkListSubcommands class.
     *
     * @param commandContext the command context providing access to services and utilities
     *                       required to execute the subcommands.
     * @param localization   the localization service for translating command output
     */
    public BkListSubcommands(final BkCommandContext commandContext, final Localization localization) {
        this.commandContext = commandContext;
        this.localization = localization;
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
        final String entries = BkOutputFormatter.graphs(commandContext.graphService().getAllGraphs());
        if (entries.isBlank()) {
            context.getSource().getSender().sendMessage(
                    localization.getPrefixedMessage("commands.bk.list.graph.empty"));
        } else {
            context.getSource().getSender().sendMessage(
                    localization.getPrefixedMessage("commands.bk.list.graph.entries", Map.of("entries", entries)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int listNetworks(final CommandContext<CommandSourceStack> context) {
        final String entries = BkOutputFormatter.networks(commandContext.graphNetworkService().loadGraphNetworks());
        if (entries.isBlank()) {
            context.getSource().getSender().sendMessage(
                    localization.getPrefixedMessage("commands.bk.list.network.empty"));
        } else {
            context.getSource().getSender().sendMessage(
                    localization.getPrefixedMessage("commands.bk.list.network.entries", Map.of("entries", entries)));
        }
        return Command.SINGLE_SUCCESS;
    }
}
