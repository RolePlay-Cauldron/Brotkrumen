package com.github.roleplaycauldron.brotkrumen;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("PMD")
public class CoordinatesCommand implements BasicCommand {

    public CoordinatesCommand(final JavaPlugin plugin) {
        //ToDo Temp Command, remove when not needed anymore
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register("coordinates", this);
        });
    }

    @Override
    public void execute(@NotNull final CommandSourceStack commandSourceStack, @NotNull final String[] args) {
        if (!(commandSourceStack.getSender() instanceof final Player player)) {
            return;
        }
        final Location loc = player.getLocation();
        final String locString = String.format("%d, %d, %d",
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
        player.sendMessage(Component.text(String.format("Coordinates: %s", locString))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, locString)));
    }
}
