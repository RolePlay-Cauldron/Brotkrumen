package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.command.bk.BkCommand;
import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommand;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.EditorWaitingActionBarReminder;
import com.github.roleplaycauldron.brotkrumen.editor.WalkingListener;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathFinder;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSearchServiceImpl;
import com.github.roleplaycauldron.brotkrumen.graph.search.SearchRegistryImpl;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.service.AsyncGraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.AsyncGraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.AsyncWarpService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpServiceImpl;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Starting point of the plugin.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.DoNotUseThreads"})
public class Brotkrumen extends JavaPlugin implements Listener {

    /**
     * The logger factory for creating loggers.
     */
    private LoggerFactory loggerFactory;

    /**
     * The service for warp-related operations.
     */
    private WrappedLogger log;

    /**
     * The storage handler for database operations.
     */
    private Storage storage;

    /**
     * The registry for managing visualizers.
     */
    private VisualizerRegistry reg;

    /**
     * The runtime localization service.
     */
    private Localization localization;

    /**
     * The service for graph-related operations.
     */
    private GraphServiceImpl graphService;

    /**
     * The service for graph network-related operations.
     */
    private GraphNetworkServiceImpl graphNetworkService;

    /**
     * The metrics service for plugin statistics.
     */
    private Metrics metrics;

    /**
     * Executor for database-backed public API work.
     */
    private ExecutorService databaseExecutor;

    /**
     * Executor for public path search work.
     */
    private ExecutorService searchExecutor;

    /**
     * Public API facade provider.
     */
    private BrotkrumenApiProvider apiProvider;

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    /* default */
    static String localeResourcePath(final String localeTag) {
        return "language/" + localeTag.toLowerCase(Locale.ROOT) + ".yml";
    }

    @Override
    public void onEnable() {
        this.loggerFactory = new LoggerFactory(getSLF4JLogger());
        this.log = loggerFactory.create(Brotkrumen.class);

        saveDefaultConfig();
        loadLocalization(loggerFactory);

        final ConfigurationSection databaseSection = getConfig().getConfigurationSection("data");
        if (databaseSection == null || databaseSection.getKeys(false).isEmpty()) {
            log.error("Could not start the plugin because the data configuration is missing. Please check your config.yml file for errors.");
            return;
        }
        storage = new Storage(loggerFactory, databaseSection, getDataFolder());
        storage.initialize();

        databaseExecutor = Executors.newFixedThreadPool(2, namedThreadFactory("brotkrumen-db"));
        searchExecutor = Executors.newFixedThreadPool(2, namedThreadFactory("brotkrumen-search"));

        graphService = new GraphServiceImpl(storage);
        graphNetworkService = new GraphNetworkServiceImpl(storage, graphService);
        final WarpServiceImpl warpService = new WarpServiceImpl(storage);

        final ServicesManager servicesManager = getServer().getServicesManager();
        servicesManager.register(GraphService.class, graphService, this, ServicePriority.Normal);
        servicesManager.register(GraphNetworkService.class, graphNetworkService, this, ServicePriority.Normal);
        servicesManager.register(WarpService.class, warpService, this, ServicePriority.Normal);

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        final AsyncGraphService asyncGraphService = new AsyncGraphService(graphService, databaseExecutor);
        final AsyncGraphNetworkService asyncGraphNetworkService =
                new AsyncGraphNetworkService(graphNetworkService, databaseExecutor);
        final AsyncWarpService asyncWarpService = new AsyncWarpService(warpService, databaseExecutor);
        final SearchRegistryImpl searchRegistry = new SearchRegistryImpl();
        searchRegistry.register(new AStarAlgorithm());
        searchRegistry.register(new DijkstraAlgorithm());
        final PathSearchServiceImpl pathSearchService = new PathSearchServiceImpl(new PathFinder(searchRegistry),
                searchExecutor);
        apiProvider = new BrotkrumenApiProvider(asyncGraphService, asyncGraphNetworkService, asyncWarpService,
                pathSearchService, searchRegistry, reg);
        registerPublicApiServices(servicesManager, asyncGraphService, asyncGraphNetworkService, asyncWarpService,
                pathSearchService, searchRegistry);

        final EffectExecutor executor = new EffectExecutor(this);
        final EditorService editorService = new EditorService(reg, this, loggerFactory, executor, graphService,
                graphNetworkService, warpService);

        registerCommands(executor, editorService);
        registerListeners(editorService);

        metrics = new Metrics(this, 31_750);

        log.info("Brotkrumen enabled");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);

        if (reg != null) {
            reg.stopVisibilityUpdates();
        }

        if (storage != null) {
            storage.shutdown();
        }

        if (metrics != null) {
            metrics.shutdown();
        }
        shutdownExecutors();
    }

    private void registerPublicApiServices(final ServicesManager servicesManager,
                                           final AsyncGraphService asyncGraphService,
                                           final AsyncGraphNetworkService asyncGraphNetworkService,
                                           final AsyncWarpService asyncWarpService,
                                           final PathSearchServiceImpl pathSearchService,
                                           final SearchRegistryImpl searchRegistry) {
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.BrotkrumenApi.class, apiProvider,
                this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.service.GraphService.class,
                asyncGraphService, this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService.class,
                asyncGraphNetworkService, this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.service.WarpService.class,
                asyncWarpService, this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService.class,
                pathSearchService, this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry.class,
                searchRegistry, this, ServicePriority.Normal);
        servicesManager.register(com.github.roleplaycauldron.brotkrumen.api.visual.VisualizerService.class,
                reg, this, ServicePriority.Normal);
    }

    private void shutdownExecutors() {
        if (databaseExecutor != null) {
            databaseExecutor.shutdownNow();
        }
        if (searchExecutor != null) {
            searchExecutor.shutdownNow();
        }
    }

    private ThreadFactory namedThreadFactory(final String prefix) {
        final AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            final Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void loadLocalization(final LoggerFactory loggerFactory) {
        final String defaultLocaleTag = configuredDefaultLocaleTag(getConfig().getString("localization.defaultLocale"));
        saveConfiguredDefaultLocaleResource(defaultLocaleTag);

        this.localization = new Localization(
                loggerFactory.create(Localization.class),
                this,
                defaultLocaleTag
        );
    }

    private void registerCommands(final EffectExecutor executor, final EditorService editorService) {
        new EditorWaitingActionBarReminder(editorService).start(this);
        new EditorCommand(this, editorService, graphService);
        new BkCommand(this, graphService, graphNetworkService, editorService.warpService(), storage, reg, loggerFactory, executor, localization);
    }

    private void registerListeners(final EditorService editorService) {
        getServer().getPluginManager().registerEvents(new WalkingListener(log, editorService), this);
    }

    private void saveConfiguredDefaultLocaleResource(final String defaultLocaleTag) {
        final String resourcePath = localeResourcePath(defaultLocaleTag);
        if (getResource(resourcePath) != null) {
            saveResource(resourcePath, false);
        }
    }

    private String configuredDefaultLocaleTag(final String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return "en-us";
        }
        final String normalized = localeTag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            log.error("Invalid localization.defaultLocale '" + localeTag + "', using en-us.");
            return "en-us";
        }
        return normalized;
    }

    /**
     * Reloads localization files using the currently configured default locale.
     */
    public void reloadLocalization() {
        final String defaultLocaleTag = configuredDefaultLocaleTag(getConfig().getString("localization.defaultLocale"));
        saveConfiguredDefaultLocaleResource(defaultLocaleTag);
        localization.reload(defaultLocaleTag);
    }

    /**
     * Returns the runtime localization service.
     *
     * @return localization service
     */
    public Localization getLocalization() {
        return localization;
    }
}
