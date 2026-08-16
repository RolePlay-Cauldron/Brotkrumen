package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;

/**
 * `/bk reload` command.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class BkReloadSubcommand {

    private static final Component FALLBACK_FAILURE_MESSAGE =
            Component.text("Reload failed. Check the console for details.");

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

    /* default */ int execute(final CommandContext<CommandSourceStack> context) {
        try {
            commandContext.plugin().reloadConfig();
            commandContext.plugin().reloadLocalization();
            if (localization.healthState() != Localization.HealthState.READY) {
                reportFailure(context,
                        new IllegalStateException("Localization reload completed with state " + localization.healthState()));
                return 0;
            }
            commandContext.plugin().reloadVisualPresets();
            commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () -> {
                try {
                    commandContext.graphRepository().reloadGraphs();
                    commandContext.graphNetworkRepository().reloadGraphNetworks();
                    sendSuccess(context);
                } catch (final RuntimeException failure) {
                    reportFailure(context, failure);
                }
            });
            return Command.SINGLE_SUCCESS;
        } catch (final RuntimeException failure) {
            reportFailure(context, failure);
            return 0;
        }
    }

    private void sendSuccess(final CommandContext<CommandSourceStack> context) {
        commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () ->
                context.getSource().getSender().sendMessage(
                        localization.getPrefixedMessage("commands.bk.reload.success")));
    }

    private void reportFailure(final CommandContext<CommandSourceStack> context, final RuntimeException failure) {
        commandContext.loggerFactory().create(BkReloadSubcommand.class)
                .error("Reload failed: " + failure.getMessage(), failure);
        commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(), () ->
                context.getSource().getSender().sendMessage(failureMessage()));
    }

    private Component failureMessage() {
        if (localization.healthState() == Localization.HealthState.FAILED) {
            return FALLBACK_FAILURE_MESSAGE;
        }
        return localization.getPrefixedMessage("commands.bk.reload.error.failed");
    }
}
