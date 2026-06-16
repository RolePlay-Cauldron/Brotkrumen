package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * `/bk reload` command.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class BkReloadSubcommand {

    private final BkCommandContext commandContext;

    private final Localization localization;

    /**
     * Initializes a new instance of the BkReloadSubcommand class.
     *
     * @param commandContext the command context
     * @param localization   the localization service
     */
    public BkReloadSubcommand(final BkCommandContext commandContext, final Localization localization) {
        this.commandContext = commandContext;
        this.localization = localization;
    }

    /**
     * Builds the reload subcommand.
     *
     * @return subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> reload() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("brotkrumen.command.bk.reload"))
                .executes(this::execute);
    }

    private int execute(final CommandContext<CommandSourceStack> context) {
        commandContext.plugin().reloadConfig();
        commandContext.plugin().reloadLocalization();
        commandContext.plugin().reloadVisualPresets();
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () -> {
            try {
                commandContext.graphRepository().reloadGraphs();
                commandContext.graphNetworkRepository().reloadGraphNetworks();
                commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () ->
                        context.getSource().getSender().sendMessage(
                                localization.getPrefixedMessage("commands.bk.reload.success")));
            } catch (final RuntimeException failure) {
                commandContext.loggerFactory().create(BkReloadSubcommand.class)
                        .error("Reload failed: " + failure.getMessage());
                commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () ->
                        context.getSource().getSender().sendMessage(
                                localization.getPrefixedMessage("commands.bk.reload.error.failed")));
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
