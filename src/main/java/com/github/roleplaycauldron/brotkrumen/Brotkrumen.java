package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.BlockDisplayVisualiser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Starting point of the plugin.
 */
public class Brotkrumen extends JavaPlugin implements Listener {

    private BlockDisplayVisualiser visualiser;

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    @Override
    public void onEnable() {
        getLogger().info("brotkrumen.Brotkrumen enabled");
        final Graph graph = new Graph("TestGraph");

        graph.addNode(new Node(null, 130, 71, -110, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 140, 78, -125, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 135, 72, -105, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 120, 73, -110, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 125, 72, -115, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 115, 71, -125, getServer().getWorld("world").getUID()));
        graph.addNode(new Node(null, 130, 72, -120, getServer().getWorld("world").getUID()));

        visualiser = new BlockDisplayVisualiser(this, graph.getNodes());

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        visualiser.shutdown();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        visualiser.showFor(event.getPlayer());
    }
}
