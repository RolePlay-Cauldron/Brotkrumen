package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.BlockDisplayVisualizer;
import com.github.roleplaycauldron.brotkrumen.visual.VisualMode;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;

/**
 * Starting point of the plugin.
 */
@SuppressWarnings("PMD")
public class Brotkrumen extends JavaPlugin implements Listener {

    private VisualizerRegistry reg;

    private LoggerFactory loggerFactory;

    private Graph graph;

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    @Override
    public void onEnable() {
        loggerFactory = new LoggerFactory(getSLF4JLogger());
        final WrappedLogger log = loggerFactory.create(Brotkrumen.class);
        log.info("brotkrumen.Brotkrumen enabled");
        graph = new Graph("TestGraph");

        final Node nodeA = graph.addNode(new Node(null, 130, 71, -110, getServer().getWorld("world").getUID()));
        final Node nodeB = graph.addNode(new Node(null, 140, 78, -125, getServer().getWorld("world").getUID()));
        final Node nodeC = graph.addNode(new Node(null, 135, 72, -105, getServer().getWorld("world").getUID()));
        final Node nodeD = graph.addNode(new Node(null, 120, 73, -110, getServer().getWorld("world").getUID()));
        final Node nodeE = graph.addNode(new Node(null, 125, 72, -115, getServer().getWorld("world").getUID()));
        final Node nodeF = graph.addNode(new Node(null, 115, 71, -125, getServer().getWorld("world").getUID()));
        final Node nodeG = graph.addNode(new Node(null, 130, 72, -120, getServer().getWorld("world").getUID()));

        graph.addUndirectedEdge(nodeA.graphId(), nodeC.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graph.addUndirectedEdge(nodeC.graphId(), nodeE.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graph.addUndirectedEdge(nodeE.graphId(), nodeG.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graph.addUndirectedEdge(nodeE.graphId(), nodeD.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graph.addUndirectedEdge(nodeD.graphId(), nodeF.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graph.addUndirectedEdge(nodeG.graphId(), nodeB.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        reg.stopVisibilityUpdates();
    }

    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final BlockDisplayVisualizer visualiser = new BlockDisplayVisualizer(this, loggerFactory, graph,
                event.getPlayer().getUniqueId(), VisualMode.EDIT);
        reg.register(event.getPlayer().getUniqueId(), visualiser);
    }

    @EventHandler
    public void playerQuit(final PlayerQuitEvent event) {
        reg.unregister(event.getPlayer().getUniqueId());
    }
}
