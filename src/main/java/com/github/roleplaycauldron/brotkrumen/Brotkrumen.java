package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphServiceImpl;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutionConfig;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import com.github.roleplaycauldron.spellbook.effect.executor.RunningEffect;
import com.github.roleplaycauldron.spellbook.effect.location.FixedAnchor;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
import com.github.roleplaycauldron.spellbook.effect.shape.SpiralHelixShape;
import com.github.roleplaycauldron.spellbook.effect.viewer.FixedViewerSource;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starting point of the plugin.
 */
@SuppressWarnings("PMD")
public class Brotkrumen extends JavaPlugin implements Listener {

    private final Map<UUID, List<RunningEffect>> activeEffects = new ConcurrentHashMap<>();

    private VisualizerRegistry reg;

    private LoggerFactory loggerFactory;

    private Graph graphOne;

    private Graph graphTwo;

    private List<Node> graphTwoPath;

    private SearchRegistry searchRegistry;

    private GraphServiceImpl graphService;

    private GraphNetworkServiceImpl graphNetworkService;

    private Storage storage;

    private Map<Integer, Graph> graphMap;

    private List<GraphNetwork> graphNetworks;

    private EffectExecutor executor;

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
        graphNetworkService = new GraphNetworkServiceImpl(storage, graphService);
        final ServicesManager servicesManager = getServer().getServicesManager();
        servicesManager.register(GraphService.class, graphService, this, ServicePriority.Normal);
        servicesManager.register(GraphNetworkService.class, graphNetworkService, this, ServicePriority.Normal);

        new CoordinatesCommand(this);
        log.info("Brotkrumen enabled");

        searchRegistry = new SearchRegistry();
        searchRegistry.register(new AStarAlgorithm());
        searchRegistry.register(new DijkstraAlgorithm());

        graphOne = new Graph("Test graph editor");

        final Node nodeA = graphOne.addNode(new Node(null, 130, 71, -110, getServer().getWorld("world").getUID()));
        final Node nodeB = graphOne.addNode(new Node(null, 140, 78, -125, getServer().getWorld("world").getUID()));
        final Node nodeC = graphOne.addNode(new Node(null, 135, 72, -105, getServer().getWorld("world").getUID()));
        final Node nodeD = graphOne.addNode(new Node(null, 120, 73, -110, getServer().getWorld("world").getUID()));
        final Node nodeE = graphOne.addNode(new Node(null, 125, 72, -115, getServer().getWorld("world").getUID()));
        final Node nodeF = graphOne.addNode(new Node(null, 115, 71, -125, getServer().getWorld("world").getUID()));
        final Node nodeG = graphOne.addNode(new Node(null, 130, 72, -120, getServer().getWorld("world").getUID()));

        graphOne.addEdge(nodeA.graphId(), nodeC.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeC.graphId(), nodeE.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeE.graphId(), nodeG.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeE.graphId(), nodeD.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeD.graphId(), nodeF.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphOne.addEdge(nodeG.graphId(), nodeB.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        graphTwo = new Graph("Test Graph pathfinder");

        final Node node2A = graphTwo.addNode(new Node(null, 118, 71, -129, getServer().getWorld("world").getUID()));
        final Node node2B = graphTwo.addNode(new Node(null, 124, 72, -139, getServer().getWorld("world").getUID()));
        final Node node2C = graphTwo.addNode(new Node(null, 130, 73, -149, getServer().getWorld("world").getUID()));
        final Node node2D = graphTwo.addNode(new Node(null, 126, 71, -156, getServer().getWorld("world").getUID()));
        final Node node2E = graphTwo.addNode(new Node(null, 116, 71, -160, getServer().getWorld("world").getUID()));
        final Node node2F = graphTwo.addNode(new Node(null, 109, 71, -158, getServer().getWorld("world").getUID()));
        final Node node2G = graphTwo.addNode(new Node(null, 108, 70, -150, getServer().getWorld("world").getUID()));
        final Node node2H = graphTwo.addNode(new Node(null, 102, 69, -152, getServer().getWorld("world").getUID()));
        final Node node2I = graphTwo.addNode(new Node(null, 99, 69, -165, getServer().getWorld("world").getUID()));
        final Node node2J = graphTwo.addNode(new Node(null, 98, 68, -171, getServer().getWorld("world").getUID()));

        graphTwo.addUndirectedEdge(node2A.graphId(), node2B.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2B.graphId(), node2C.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2C.graphId(), node2D.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2D.graphId(), node2E.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2E.graphId(), node2F.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2F.graphId(), node2G.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2G.graphId(), node2H.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2H.graphId(), node2I.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));
        graphTwo.addUndirectedEdge(node2I.graphId(), node2J.graphId(), 1.0D, EnumSet.of(EdgeFlag.UNDIRECTED));

        final PathAlgorithm pathAlgo = searchRegistry.select(graphTwo, TeleportRules.disableTeleports());
        graphTwoPath = pathAlgo.findPath(graphTwo, node2A.graphId(), Set.of(node2J.graphId()), null, TeleportRules.disableTeleports());
//        graphTwoPath = List.of(node2A, node2B, node2C, node2D, node2E, node2F, node2G, node2H, node2I, node2J);

        getServer().getPluginManager().registerEvents(this, this);

        this.executor = new EffectExecutor(this);
    }

    private void loadGraphs() {
        graphMap = new HashMap<>();
        graphNetworks = new ArrayList<>();

        graphService.getAllGraphs().forEach(graph -> graphMap.put(graph.getGraphId(), graph));
        graphNetworks.addAll(graphNetworkService.loadGraphNetworks());
    }

    @Override
    public void onDisable() {
        reg.stopVisibilityUpdates();

        storage.shutdown();
    }

    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
//        final ConfigurationSection nodeSection = getConfig().getConfigurationSection("NodeEffect");
//        final EffectLibConfig nodeConfig = new EffectLibConfig(nodeSection.getString("class"), nodeSection);
//        final ConfigurationSection edgeSection = getConfig().getConfigurationSection("EdgeEffect");
//        final EffectLibConfig edgeConfig = new EffectLibConfig(edgeSection.getString("class"), edgeSection);
//
//        final ParticleVisualizer particleVisualizer = new ParticleVisualizer(this, loggerFactory, graphTwo,
//                new EffectManager(this), nodeConfig, edgeConfig, event.getPlayer().getUniqueId(), VisualMode.EDIT, graphTwoPath);
//
//        reg.register(event.getPlayer().getUniqueId(), particleVisualizer);
        final Location joinLocation = event.getPlayer().getLocation();
        final UUID uuid = event.getPlayer().getUniqueId();

        // 1. Effekt: Eine Helix aus Flammen, die zum Ziel "schaut"
        final EffectInstance sphereShape = EffectBuilder.create()
                .shape(new CubeShape(3f, 20))
                .particle(Particle.FLAME)
                .build();

        // 2. Effekt: Eine vertikale Linie aus Herzchen
        final EffectInstance lineEffect = EffectBuilder.create()
                .shape(new MovingPointShape(2f, 0.2f, 4, false))
                .particle(Particle.FLAME)
                .build();

        final EffectInstance helixEffect = EffectBuilder.create()
                .shape(new SpiralHelixShape(2, 30, 4, 10, 3, 0.2f, true))
                .lookAtTarget()
                .particle(Particle.FLAME)
                .build();

        final Location targetLocation = joinLocation.clone().add(20, 1, 20);
        // Konfiguration für die permanente Ausführung
        final EffectExecutionConfig config = EffectExecutionConfig.builder()
                .originAnchor(new FixedAnchor(joinLocation))
                .targetAnchor(new FixedAnchor(targetLocation))
                .viewerSource(new FixedViewerSource(Collections.singletonList(event.getPlayer())))
                .periodTicks(2) // Alle 2 Ticks ausführen (10x pro Sekunde)
                .maxRuns(-1)    // Permanent ausführen
                .build();

        final RunningEffect runningSphere = executor.start(sphereShape, config);
        final RunningEffect runningLine = executor.start(lineEffect, config);
        final RunningEffect runningHelix = executor.start(helixEffect, config);

        activeEffects.put(uuid, new ArrayList<>(List.of(runningHelix, runningSphere, runningLine)));
    }

    @EventHandler
    public void playerQuit(final PlayerQuitEvent event) {
//        reg.unregister(event.getPlayer().getUniqueId());

        final List<RunningEffect> effects = activeEffects.remove(event.getPlayer().getUniqueId());
        if (effects != null) {
            effects.forEach(RunningEffect::cancel);
        }
    }
}
