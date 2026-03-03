package com.github.roleplaycauldron.brotkrumen.visual;

import org.bukkit.entity.Player;

public interface NodeLayer {

    void showFor(Player player);

    void hideFor(Player player);

    void shutdown();

    boolean isViewer(Player player);
}
