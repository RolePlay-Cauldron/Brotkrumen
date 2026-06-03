package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

/**
 * Shared feedback helpers for editor subcommands.
 */
public final class EditorCommandFeedback {

    private EditorCommandFeedback() {
    }

    /**
     * Retrieves the localization service from the plugin.
     *
     * @param commandContext editor command context
     * @return localization service
     */
    public static Localization localization(final EditorCommandContext commandContext) {
        return ((Brotkrumen) commandContext.plugin()).getLocalization();
    }

    /**
     * Sends a player-only error message.
     *
     * @param commandContext editor command context
     * @param context        command context
     * @return command failure result
     */
    public static int playerOnly(final EditorCommandContext commandContext,
                                 final CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(localization(commandContext)
                .getPrefixedMessage("commands.common.playerOnlyEditor"));
        return 0;
    }

    /**
     * Sends an editor result message to a player.
     *
     * @param commandContext editor command context
     * @param player         target player
     * @param result         editor result
     * @return command result status
     */
    public static int send(final EditorCommandContext commandContext,
                           final Player player,
                           final EditorService.EditorResult result) {
        final Localization localization = localization(commandContext);
        if (result.message() != null && !result.message().isBlank()) {
            if (result.message().startsWith("commands.")) {
                player.sendMessage(localization.getPrefixedMessage(result.message(), result.replacements()));
            } else {
                player.sendMessage(localization.getPrefixedMessageFromString(result.message()));
            }
        }
        if (result.component() != null) {
            player.sendMessage(localization.getPrefixedMessage(result.component()));
        }
        if (result.actionBarMessage() != null && !result.actionBarMessage().isBlank()) {
            if (result.actionBarMessage().startsWith("commands.")) {
                player.sendActionBar(localization.getFormattedMessage(result.actionBarMessage(), result.replacements()));
            } else {
                player.sendActionBar(localization.getMessageFromString(result.actionBarMessage()));
            }
        }
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }
}
