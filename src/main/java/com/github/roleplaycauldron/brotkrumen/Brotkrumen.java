package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.editor.EditorCommand;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.WalkingListener;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathFinder;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphServiceImpl;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockDisplayDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleDesignSet;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutionConfig;
import org.bukkit.command.CommandMap;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bukkit.command.CommandMap;
import com.github.roleplaycauldron.spellbook.effect.location.FixedAnchor;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MorphPointStrategies;
import com.github.roleplaycauldron.spellbook.effect.shape.MorphShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import com.github.roleplaycauldron.spellbook.effect.shape.SphereShape;
import com.github.roleplaycauldron.spellbook.effect.viewer.FixedViewerSource;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Starting point of the plugin.
 */
@SuppressWarnings("PMD")
public class Brotkrumen extends JavaPlugin implements Listener {

    private VisualizerRegistry reg;

    private LoggerFactory loggerFactory;

    private Graph graphOne;

    private Graph graphTwo;

    private GraphNetwork visualizerTestNetwork;

    private Storage storage;

    private EffectExecutor executor;

    private GraphNetworkDesignProfile visualizerTestProfile;

    private PathResult pathResult;

    private EditorService editorService;

    private GraphServiceImpl graphService;

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

        saveDefaultConfig();

        final ConfigurationSection databaseSection = getConfig().getConfigurationSection("data");
        if (databaseSection == null || databaseSection.getKeys(false).isEmpty()) {
            log.error("Could not start the plugin because the data configuration is missing. Please check your config.yml file for errors.");
            return;
        }

        storage = new Storage(loggerFactory, databaseSection, getDataFolder());
        storage.initialize();

        graphService = new GraphServiceImpl(storage);
        final GraphNetworkServiceImpl graphNetworkService = new GraphNetworkServiceImpl(storage, graphService);
        final ServicesManager servicesManager = getServer().getServicesManager();
        servicesManager.register(GraphService.class, graphService, this, ServicePriority.Normal);
        servicesManager.register(GraphNetworkService.class, graphNetworkService, this, ServicePriority.Normal);

        log.info("Brotkrumen enabled");

        createVisualizerTestGraphs();

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        this.executor = new EffectExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
        this.editorService = new EditorService(reg, this, loggerFactory);
        new CoordinatesCommand(this);
        final CommandMap commandMap = getServer().getCommandMap();
        commandMap.register("bkeditor", new EditorCommand(editorService, graphService));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new WalkingListener(log, editorService), this);
    }

    private void createVisualizerTestGraphs() {
        graphOne = new Graph(1, "Test graph editor");

        final Node nodeA = graphOne.addNode(new Node(null, 130, 71, -110, getServer().getWorld("world").getUID()));
        final Node nodeB = graphOne.addNode(new Node(null, 140, 78, -125, getServer().getWorld("world").getUID()));
        final Node nodeC = graphOne.addNode(new Node(null, 135, 72, -105, getServer().getWorld("world").getUID(), EnumSet.of(NodeFlag.INTERGRAPH_TELEPORT)));
        final Node nodeD = graphOne.addNode(new Node(null, 120, 73, -110, getServer().getWorld("world").getUID()));
        final Node nodeE = graphOne.addNode(new Node(null, 125, 72, -115, getServer().getWorld("world").getUID()));
        final Node nodeF = graphOne.addNode(new Node(null, 115, 71, -125, getServer().getWorld("world").getUID()));
        final Node nodeG = graphOne.addNode(new Node(null, 130, 72, -120, getServer().getWorld("world").getUID()));
        final Node nodeHTpG = graphOne.addNode(new Node(null, 127, 72, -101, getServer().getWorld("world").getUID(), EnumSet.of(NodeFlag.WARP)));
        final Node nodeHTpL1 = graphOne.addNode(new Node(null, 139, 72, -100, getServer().getWorld("world").getUID(), EnumSet.of(NodeFlag.LOCAL_TELEPORT)));
        final Node nodeHTpL2 = graphOne.addNode(new Node(null, 134, 72, -99, getServer().getWorld("world").getUID(), EnumSet.of(NodeFlag.LOCAL_TELEPORT)));

        graphOne.addEdge(nodeA.graphId(), nodeC.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeC.graphId(), nodeE.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeE.graphId(), nodeG.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeE.graphId(), nodeD.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeD.graphId(), nodeF.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeG.graphId(), nodeB.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeB.graphId(), nodeHTpG.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeHTpL1.graphId(), nodeHTpL2.graphId(), 1.0D, EnumSet.of(EdgeFlag.TELEPORT));
        graphOne.addEdge(nodeHTpL2.graphId(), nodeHTpL1.graphId(), 1.0D, EnumSet.of(EdgeFlag.TELEPORT));
        graphOne.addEdge(nodeC.graphId(), nodeHTpL1.graphId(), 1.0D, EnumSet.of(EdgeFlag.DIRECTED));

        graphTwo = new Graph(2, "Test Graph pathfinder");

        final Node node2A = graphTwo.addNode(new Node(null, 118, 71, -129, getServer().getWorld("world").getUID()));
        final Node node2B = graphTwo.addNode(new Node(null, 124, 72, -139, getServer().getWorld("world").getUID()));
        final Node node2C = graphTwo.addNode(new Node(null, 130, 73, -149, getServer().getWorld("world").getUID()));
        final Node node2D = graphTwo.addNode(new Node(null, 126, 71, -156, getServer().getWorld("world").getUID(), EnumSet.of(NodeFlag.INTERGRAPH_TELEPORT)));
        final Node node2E = graphTwo.addNode(new Node(null, 116, 71, -160, getServer().getWorld("world").getUID()));
        final Node node2F = graphTwo.addNode(new Node(null, 109, 71, -158, getServer().getWorld("world").getUID()));
        final Node node2G = graphTwo.addNode(new Node(null, 108, 70, -150, getServer().getWorld("world").getUID()));
        final Node node2H = graphTwo.addNode(new Node(null, 102, 69, -152, getServer().getWorld("world").getUID()));
        final Node node2I = graphTwo.addNode(new Node(null, 99, 69, -165, getServer().getWorld("world").getUID()));
        final Node node2J = graphTwo.addNode(new Node(null, 98, 68, -171, getServer().getWorld("world").getUID()));

        graphTwo.addEdge(node2A.graphId(), node2B.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2B.graphId(), node2C.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2C.graphId(), node2D.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2D.graphId(), node2E.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2E.graphId(), node2F.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2F.graphId(), node2G.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2G.graphId(), node2H.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2H.graphId(), node2I.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addEdge(node2I.graphId(), node2J.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));

        createVisualizerTestNetwork(nodeG, node2A, nodeC, node2D, nodeB, node2E);
    }

    private void createVisualizerTestNetwork(final Node graphOneExit, final Node graphTwoEntry,
                                             final Node tpNodeA, final Node tpNodeB,
                                             final Node startPoint, final Node endPoint) {
        visualizerTestNetwork = new GraphNetwork();
        visualizerTestNetwork.addGraph(graphOne);
        visualizerTestNetwork.addGraph(graphTwo);
        visualizerTestNetwork.addUndirectedInterGraphEdge(
                new NodeRef(graphOne.getGraphId(), graphOneExit.graphId()),
                new NodeRef(graphTwo.getGraphId(), graphTwoEntry.graphId()),
                1.0D,
                EnumSet.of(EdgeFlag.INTER_GRAPH, EdgeFlag.UNDIRECTED)
        );
        visualizerTestNetwork.addUndirectedInterGraphEdge(
                new NodeRef(graphOne.getGraphId(), tpNodeA.graphId()),
                new NodeRef(graphTwo.getGraphId(), tpNodeB.graphId()),
                1.0D,
                EnumSet.of(EdgeFlag.INTER_GRAPH, EdgeFlag.TELEPORT, EdgeFlag.UNDIRECTED)
        );
        visualizerTestProfile = sampleNetworkDesignProfile();

        final TeleportRules rules = new TeleportRules(true, true,
                true, Collections.emptySet());

        final PathFinder finder = new PathFinder();
        pathResult = finder.findPathResult(visualizerTestNetwork,
                new NodeRef(graphOne.getGraphId(), startPoint.graphId()),
                new NodeRef(graphTwo.getGraphId(), endPoint.graphId()),
                null,
                rules);
    }

    @Override
    public void onDisable() {
        if (reg != null) {
            reg.stopVisibilityUpdates();
        }

        if (storage != null) {
            storage.shutdown();
        }
    }

    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        registerVisualizerTest(event.getPlayer());
        shapeTest(event.getPlayer());
    }

    @EventHandler
    public void playerQuit(final PlayerQuitEvent event) {
        reg.unregister(event.getPlayer().getUniqueId());
    }

    private void shapeTest(final Player player) {
        final Shape sphere = new SphereShape(2, 60);
        final Shape cube = new CubeShape(2, 8);
//        final Shape helixShape = new HelixShape(2, 40, 2.0f, 5, 4.0f, 0.1f);
//        final Shape spiralHelixShape = new SpiralHelixShape(2, 40, 2.0f, 5, 4.0f, 0.1f);

        final Shape morph = MorphShape.between(sphere, cube)
                .afterSeconds(5, 3)
                .strategy(MorphPointStrategies.resampleToMax())
                .build();

        final var effect = EffectBuilder.create()
                .shape(morph)
                .particle(Particle.END_ROD)
                .build();

        // Konfiguration für die permanente Ausführung
        final EffectExecutionConfig config = EffectExecutionConfig.builder()
                .originAnchor(new FixedAnchor(player.getLocation()))
                .viewerSource(new FixedViewerSource(Collections.singletonList(player)))
                .periodTicks(2) // Alle 2 Ticks ausführen (10x pro Sekunde)
                .maxRuns(-1)    // Permanent ausführen
                .build();

        executor.start(effect, config);
    }

    private void registerVisualizerTest(final Player player) {
        //        final Visualizer visualizer = GraphVisualizerFactory.particleGraph(
//                this,
//                loggerFactory,
//                graphTwo,
//                player.getUniqueId(),
//                executor
//        );

        final Visualizer visualizer = GraphVisualizerFactory.particleNetwork(
                this,
                loggerFactory,
                visualizerTestNetwork,
                player.getUniqueId(),
                executor,
                visualizerTestProfile
        );

//        final Visualizer visualizer = GraphVisualizerFactory.particleGuidedNetworkPath(
//                this,
//                loggerFactory,
//                visualizerTestNetwork,
//                pathResult,
//                player.getUniqueId(),
//                executor,
//                visualizerTestProfile
//        );

//        final Visualizer visualizer = GraphVisualizerFactory.blockDisplayNetwork(
//                this,
//                loggerFactory,
//                visualizerTestNetwork,
//                player.getUniqueId(),
//                visualizerTestProfile
//        );
        reg.register(player.getUniqueId(), visualizer);
    }

    private GraphNetworkDesignProfile sampleNetworkDesignProfile() {
        return GraphNetworkDesignProfile.builder()
                .particleGraphDesign(graphOne.getGraphId(), ParticleDesignSet.emberPreset())
                .particleGraphDesign(graphTwo.getGraphId(), ParticleDesignSet.prismPreset())
                .blockDisplayGraphDesign(graphOne.getGraphId(), BlockDisplayDesignSet.emberPreset())
                .blockDisplayGraphDesign(graphTwo.getGraphId(), BlockDisplayDesignSet.prismPreset())
                .build();
    }

    public GraphService getGraphService() {
        return graphService;
    }
}
