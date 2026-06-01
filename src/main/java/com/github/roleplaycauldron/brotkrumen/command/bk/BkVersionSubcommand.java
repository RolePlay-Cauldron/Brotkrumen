package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.storage.database.Engine;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Locale;

/**
 * `/bk version` command.
 */
public final class BkVersionSubcommand {

    private final BkCommandContext commandContext;

    /**
     * Initializes a new instance of the BkVersionSubcommand class.
     *
     * @param commandContext the context of the `/bk version` command, providing access to various
     *                       services and utilities required for command handling
     */
    public BkVersionSubcommand(final BkCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the version subcommand.
     *
     * @return subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> version() {
        return Commands.literal("version")
                .executes(this::showVersion);
    }

    private int showVersion(final CommandContext<CommandSourceStack> context) {
        final String server = commandContext.plugin().getServer().getName() + " "
                + commandContext.plugin().getServer().getVersion() + " ("
                + commandContext.plugin().getServer().getBukkitVersion() + ")";
        final String pluginVersion = commandContext.plugin().getPluginMeta().getVersion();
        final Engine engine = commandContext.storage().getEngine();
        final String storageType = engine == null ? "unknown" : engine.name().toLowerCase(Locale.ROOT);
        final int schemaVersion = commandContext.storage().getSchemaVersion();
        final int onlinePlayers = commandContext.plugin().getServer().getOnlinePlayers().size();
        final int maxPlayers = commandContext.plugin().getServer().getMaxPlayers();
        final String diagnostics = BkOutputFormatter.diagnostics(pluginVersion, server, "none", storageType,
                schemaVersion, onlinePlayers, maxPlayers);

        final Component message = Component.text("Brotkrumen " + pluginVersion)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy Brotkrumen diagnostics")))
                .clickEvent(ClickEvent.copyToClipboard(diagnostics))
                .append(Component.newline())
                .append(Component.text("Server: " + server))
                .append(Component.newline())
                .append(Component.text("Hooks: none"))
                .append(Component.newline())
                .append(Component.text("Storage: " + storageType + " schema " + schemaVersion))
                .append(Component.newline())
                .append(Component.text("Online players: " + onlinePlayers + "/" + maxPlayers));
        commandContext.send(context, message);
        return Command.SINGLE_SUCCESS;
    }
}
